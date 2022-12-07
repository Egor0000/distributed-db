package md.utm.isa.distributeddb;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class Entity implements Serializable {
    private String key;
    private String value;
    private String method;
}
