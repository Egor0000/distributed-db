package md.utm.isa.distributeddb;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping()
@RequiredArgsConstructor
public class ServerController {
    private final DatabaseService service;

    @PostMapping(value = "/", consumes = "application/json")
    String create(@RequestBody Entity body) throws Exception {
         return service.set(body);
    }

    @GetMapping("/")
    Entity read(@RequestParam String key) {
        return service.read(key);
    }

    @DeleteMapping("/")
    void delete(@RequestParam String key) {
        service.delete(key);
    }
}
