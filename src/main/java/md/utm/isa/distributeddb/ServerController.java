package md.utm.isa.distributeddb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping()
@Slf4j()
public class ServerController {
    private final DatabaseService service;
    private final DatabaseServiceProxy proxy;
    private final String gateway;

    public ServerController(DatabaseService service, DatabaseServiceProxy proxy, @Value("${gateway}") String gateway) {
        this.service = service;
        this.proxy = proxy;
        this.gateway = gateway;
    }

    @PostMapping(value = "/", consumes = "application/json")
    String create(@RequestBody Entity body, HttpServletRequest request) throws Exception {
        if (request.getRemoteAddr().equals(gateway)) {
            return proxy.set(body);
        } else {
            return service.set(body);
        }
    }

    @GetMapping("/")
    Entity read(@RequestParam String key, HttpServletRequest request) {
        if (request.getRemoteAddr().equals(gateway)) {
            return proxy.read(key);
        } else {
            return service.read(key);
        }
    }

    @PutMapping("/")
    String update(@RequestBody Entity entity, HttpServletRequest request) {
        if (request.getRemoteAddr().equals(gateway)) {
            return proxy.update(entity);
        } else {
            return service.update(entity, false);
        }
    }

    @DeleteMapping("/")
    void delete(@RequestParam String key, HttpServletRequest request) {
        if (request.getRemoteAddr().equals(gateway)) {
            proxy.delete(key);
        } else {
            service.delete(key);
        }
    }
}
