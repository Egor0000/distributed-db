package md.utm.isa.distributeddb;

public interface DatabaseService {
    String set(Entity entity) throws Exception;
    Entity read(String key);
    void delete(String key);

    String saveToDb(Entity entity);
}
