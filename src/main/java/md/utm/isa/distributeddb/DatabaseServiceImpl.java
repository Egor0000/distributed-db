package md.utm.isa.distributeddb;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {
    private final TcpClient tcpClient;
    private final ConcurrentMap<String, String> db = new ConcurrentHashMap<>();
    private final String servers;
    private final String httpServers;
    private final String leader;
    private final String id;
    private String name;
    private final ObjectMapper om = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public DatabaseServiceImpl(TcpClient tcpClient, @Value("${serverss}") String servers,
                               @Value("${servers-http}") String httpServers,
                               @Value("${name}") String name,
                               @Value("${leader}") String leader,
                               @Value("${data.id}") String id) {
        this.tcpClient = tcpClient;
        this.name = name;
        this.leader = leader;
        this.id = id;
        this.servers = servers;
        this.httpServers = httpServers;
    }

    @Override
    public String set(Entity entity) throws Exception {
        log.info("{}", id);

        String[] array = servers.split(",");
        List<String> list = getServersAlive(array);

        if (list.size() < 2) {
            return "Cannot set new data. Cause to much servers are down";
        }
        Collections.shuffle(list);
        list = list.subList(0, Math.min(2, list.size()));
        log.info("List {}", list);

        try {
            for (String server : list) {
                String[] address = server.split(":");
                if (!address[0].equals(name) && id.equals(leader)) {
                    try {
                        log.info("Address {} : {}", address[0], address[1]);
                        tcpClient.send(address[0], Integer.parseInt(address[1]), entity);
                    } catch (Exception ex) {

                    }
                }  else if (id.equals(leader)) {
                    saveToDb(entity);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return "Success";
    }

    @Override
    public Entity read(String key) {
        log.info("{}", id);
        Entity serverEntity = new Entity(key, db.get(key), null);
        if (serverEntity.getValue()!=null) {
            return serverEntity;
        }
        try {
            List<String> list = getServersAlive(httpServers.split(","));
            if (list.size() < 2) {
                log.error("Cannot get data. Too much servers are down");
                return null;
            }

            for (String server : list) {
                String[] address = server.split(":");
                if (!address[0].equals(name) && id.equals(leader)) {
                    try {
                        HttpRequest request = HttpRequest.newBuilder().header( "content-type", "application/json")
                                .uri(new URI("http://"+ address[0] + ":" + Integer.parseInt(address[1]) + "?key=" + key))
                                .GET()
                                .build();
                        String json = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
                        log.info("Received response from server {}: {}", id, json);
                        Entity e = om.readValue(json, Entity.class);
                        if (e.getKey().equals(key) && e.getValue()!=null) {
                            return e;
                        }
                    } catch (Exception ex) {

                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return new Entity(key, db.get(key), null);
    }

    @Override
    public void delete(String key) {
        log.info("{}", id);
        db.remove(key);

        List<String> list = getServersAlive(httpServers.split(","));
        if (list.size() < 2) {
            log.error("Cannot update data. Too much servers are down");
        }

        try {
            for (String server : getServersAlive(httpServers.split(","))) {
                String[] address = server.split(":");

                if (!address[0].equals(name) && id.equals(leader)) {
                    try {
                        HttpRequest request = HttpRequest.newBuilder().header( "content-type", "application/json")
                                .uri(new URI("http://"+ address[0] + ":" + Integer.parseInt(address[1]) + "?key=" + key))
                                .DELETE()
                                .build();
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    } catch (Exception ex) {

                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String update(Entity entity) {
        log.info("{}", id);

        List<String> list = getServersAlive(servers.split(","));
        if (list.size() < 2) {
            log.error("Cannot update data. Too much servers are down");
        }

        try {
            for (String server : getServersAlive(servers.split(","))) {
                String[] address = server.split(":");
                if (!address[0].equals(name) && id.equals(leader)) {
                    try {
                        entity.setMethod("UPDATE");
                        tcpClient.send(address[0], Integer.parseInt(address[1]), entity);
                    } catch (Exception ex) {

                    }
                }  else if (id.equals(leader)) {
                    saveToDb(entity);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return saveToDb(entity);
    }

    @Override
    public String saveToDb(Entity entity) {
        if (entity.getKey() == null) {
            return "Cannot save null key";
        }

        if (entity.getMethod() == null || !entity.getMethod().equals("UPDATE") ||
                (entity.getMethod().equals("UPDATE") && db.containsKey(entity.getKey()))) {
            if ( entity.getMethod() != null && entity.getMethod().equals("UPDATE")) {
                log.info("Updating entity {} on server {}", entity, name);
            } else {
                log.info("Saving entity {} on server {}", entity, name);

            }
            entity.setMethod(null);
            return db.put(entity.getKey(), entity.getValue());
        }

        return null;
    }

    private List<String> getServersAlive(String[] servers) {
        //todo add dynamic server assignment
        List<String> serversAlive = new ArrayList<>(Collections.nCopies(10, "null"));
        for (int i =1; i<=3; i++) {
            Long currentMillis = System.currentTimeMillis();
            log.info("Server {} time {}", i, currentMillis-ServerHeathcheck.healthCehckTimer.get(i));
            if (currentMillis-ServerHeathcheck.healthCehckTimer.get(i) > 5000) {
                log.info("Sever {} is dead", i);
            } else {
                log.info("{}:{}", servers.length, i);
                serversAlive.add(servers[i-1]);
            }
        }

        return serversAlive;
    }
}
