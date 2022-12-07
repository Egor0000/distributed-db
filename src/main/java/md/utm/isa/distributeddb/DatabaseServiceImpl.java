package md.utm.isa.distributeddb;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import md.utm.isa.distributeddb.consistent_hash.ConsistentHash;
import md.utm.isa.distributeddb.consistent_hash.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {
    private final TcpClient tcpClient;
    private final ConcurrentMap<String, String> db = new ConcurrentHashMap<>();
    private final String servers;
    private final String httpServers;
    private final String leader;
    private final String id;
    private final ConsistentHash consistentHash;
    private String name;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final HashMap<String, String> addressRegister = new HashMap<>();

    private final AtomicLong request = new AtomicLong(0);

    public DatabaseServiceImpl(TcpClient tcpClient, @Value("${serverss}") String servers,
                               @Value("${servers-http}") String httpServers,
                               @Value("${name}") String name,
                               @Value("${leader}") String leader,
                               @Value("${data.id}") String id, ConsistentHash consistentHash) {
        this.tcpClient = tcpClient;
        this.name = name;
        this.leader = leader;
        this.id = id;
        this.servers = servers;
        this.httpServers = httpServers;
        this.consistentHash = consistentHash;

        String[] httpAddresses = httpServers.split(",");
        String[] tcpAddresses = servers.split(",");

        for (String address: httpAddresses) {
            addressRegister.put(address, address.split("-")[2]);
        }

        for (int i = 0; i < httpAddresses.length; i++) {
            consistentHash.add(new Node(null, httpAddresses[i],tcpAddresses[i]), 5);
        }

        Thread t = new Thread(this::synchronize);
        t.setName("synchronisation-thread");
        t.start();
    }

    @Override
    public String set(Entity entity) throws Exception {
        int toLive = Double.valueOf(Math.ceil(servers.split(",").length/2.0)).intValue();
        log.info("Required servers to live: {}", toLive);

        updateNodeTable();

        List<Node> assignedServers = consistentHash.getNearestNode(entity, 2);

        if (assignedServers.size() < toLive) {
            log.error("To much servers are down");
        }
        log.info("Nodes assigned by consistent hash:  {}", assignedServers);

        List<String> list = assignedServers.stream().map(Node::getTcpAddress).collect(Collectors.toList());

        try {
            for (String server : list) {
                String[] address = server.split(":");
                if (!address[0].equals(name)) {
                    try {
                        log.info("Address {} : {}", address[0], address[1]);
                        tcpClient.send(address[0], Integer.parseInt(address[1]), entity);
                    } catch (Exception ex) {

                    }
                }  else {
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
            List<Node> assignedServers = consistentHash.getNearestNode(new Entity(key, null, null), 2);
            log.info("Nodes assigned by consistent hash:  {}", assignedServers);

            List<String> list = assignedServers.stream().map(Node::getAddress).collect(Collectors.toList());

            for (String server : list) {
                String[] address = server.split(":");
                if (!address[0].equals(name)) {
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
                        log.error("Error ", ex);
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
        String r = db.remove(key);

        if (r == null) {
            return;
        }

        List<Node> assignedServers = consistentHash.getNearestNode(new Entity(key, null, null), 2);
        log.info("Nodes assigned by consistent hash:  {}", assignedServers);

        List<String> list = assignedServers.stream().map(Node::getAddress).collect(Collectors.toList());

        try {
            for (String server : list) {
                String[] address = server.split(":");

                if (!address[0].equals(name)) {
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
    public String update(Entity entity, boolean sync) {
        log.info("{}", id);

//        List<String> list = getServersAlive(servers.split(","));
//        if (list.size() < 2) {
//            log.error("Cannot update data. Too much servers are down");
//        }

        List<Node> assignedServers = consistentHash.getNearestNode(entity, 2);
        log.info("Nodes assigned by consistent hash:  {}", assignedServers);

        List<String> list = assignedServers.stream().map(Node::getTcpAddress).collect(Collectors.toList());

        try {
            for (String server : list) {
                String[] address = server.split(":");
                if (!address[0].equals(name)) {
                    try {
                        entity.setMethod("UPDATE");
                        tcpClient.send(address[0], Integer.parseInt(address[1]), entity);
                    } catch (Exception ex) {

                    }
                }  else {
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

//    private List<String> getServersAlive(String[] servers) {
//        //todo add dynamic server assignment
//        List<String> serversAlive = new ArrayList<>(Collections.nCopies(10, "null"));
//        for (int i =1; i<=3; i++) {
//            Long currentMillis = System.currentTimeMillis();
//            log.info("Server {} time {}", i, currentMillis- ServerHealthcheck.healthCehckTimer.get(i));
//            if (currentMillis- ServerHealthcheck.healthCehckTimer.get(i) > 5000) {
//                log.info("Sever {} is dead", i);
//            } else {
//                log.info("{}:{}", servers.length, i);
//                serversAlive.add(servers[i-1]);
//            }
//        }
//
//        return serversAlive;
//    }



    private void updateNodeTable() {
        String[] list = httpServers.split(",");
        for (String address: list) {
            Long timestamp = ServerHealthcheck.serverHealthcheck.get(addressRegister.get(address));

            log.info("Server {} {}", addressRegister.get(address), ServerHealthcheck.serverHealthcheck.size());
            if (timestamp != null) {
                log.info("Timestamp {}", System.currentTimeMillis() - timestamp);
                if (timestamp - System.currentTimeMillis() > 5000) {
                    log.info("Removeing server {}", address);
                    consistentHash.remove(new Node(null, address, null));
                    log.info("Consistent hash size {}", consistentHash.getSize());
                }
            }
        }
    }

    private void synchronize() {
        while (true) {
            try {
                Random generator = new Random();
                Object[] keys = db.values().toArray();
                if (keys.length > 0) {
                    String randomKey= (String) keys[generator.nextInt(keys.length)];
                    log.info("Starting synchronisation... {}", randomKey);
                    update(new Entity(randomKey, db.get(randomKey), "UPDATE"), true);
                }
                Thread.sleep(5000);
            } catch (Exception ex) {
                log.info("ERROR", ex);
            }
        }
    }
}
