package md.utm.isa.distributeddb;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DatabaseServiceImpl implements DatabaseService {
    private final TcpClient tcpClient;
    private final ConcurrentMap<String, String> db = new ConcurrentHashMap<>();
    private final String servers;
    private final String leader;
    private final String id;
    private String name;

    public DatabaseServiceImpl(TcpClient tcpClient, @Value("${serverss}") String servers,
                               @Value("${name}") String name,
                               @Value("${leader}") String leader,
                               @Value("${data.id}") String id) {
        this.tcpClient = tcpClient;
        this.name = name;
        this.leader = leader;
        this.id = id;
        this.servers = servers;
    }

    @Override
    public String set(Entity entity) throws Exception {
        try {
            if (id.equals(leader)) {
                for (String server : servers.split(",")) {
                    String[] address = server.split(":");
                    if (!address[0].equals(name)) {
                        try {
                            tcpClient.send(address[0], Integer.parseInt(address[1]), entity);
                        } catch (Exception ex) {

                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return db.put(entity.getKey(), entity.getValue());
    }

    @Override
    public Entity read(String key) {
        return new Entity(key, db.get(key));
    }

    @Override
    public void delete(String key) {
        db.remove(key);
    }

    @Override
    public String saveToDb(Entity entity) {
        return db.put(entity.getKey(), entity.getValue());
    }
}
