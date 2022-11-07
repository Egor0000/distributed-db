package md.utm.isa.distributeddb;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class DatabaseServiceImpl implements DatabaseService {
    private final TcpClient tcpClient;
    private final ConcurrentMap<String, String> db = new ConcurrentHashMap<>();
    @Override
    public String set(Entity entity) throws Exception {
        try {
            tcpClient.send("localhost", 4445, entity);
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
}
