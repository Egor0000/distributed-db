package md.utm.isa.distributeddb;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DatabaseServiceProxy {
    private final String[] servers;
    private AtomicInteger counter = new AtomicInteger(0);
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper om = new ObjectMapper();


    public DatabaseServiceProxy(@Value("${servers-http}") String servers) {
        this.servers = servers.split(",");
    }

    public String set(Entity entity) throws Exception {
        int idx = counter.getAndIncrement();
        counter.set(counter.get()%servers.length);

        HttpRequest request = HttpRequest.newBuilder().header( "content-type", "application/json")
                .uri(new URI("http://"+ servers[idx]))
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(entity)))
                .build();

        log.info("Proxying CREATE to server {}", servers[idx]);
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public Entity read(String key) {
        int idx = counter.getAndIncrement();
        counter.set(counter.get()%servers.length);

        try {
            HttpRequest request = HttpRequest.newBuilder().header( "content-type", "application/json")
                    .uri(new URI("http://"+ servers[idx] + "?key=" + key))
                    .GET()
                    .build();
            log.info("Proxying READ to server {}", servers[idx]);
            String json = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return  om.readValue(json, Entity.class);
        } catch (Exception ex) {

        }

        return null;
    }

    public void delete(String key) {
        int idx = counter.getAndIncrement();
        counter.set(counter.get()%servers.length);

        try {
            HttpRequest request = HttpRequest.newBuilder().header( "content-type", "application/json")
                    .uri(new URI("http://"+ servers[idx] + "?key=" + key))
                    .DELETE()
                    .build();

            log.info("Proxying DELETE to server {}", servers[idx]);
            httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception ex) {

        }
    }

    public String update(Entity entity) {
        int idx = counter.getAndIncrement();
        counter.set(counter.get()%servers.length);

        try {
            HttpRequest request = HttpRequest.newBuilder().header( "content-type", "application/json")
                    .uri(new URI("http://"+ servers[idx]))
                    .PUT(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(entity)))
                    .build();

            log.info("Proxying UPDATE to server {}", servers[idx]);
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception ex) {

        }
        return "Invalid request";
    }
}
