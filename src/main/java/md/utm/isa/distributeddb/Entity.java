package md.utm.isa.distributeddb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Entity implements Serializable {
    private String key;
    private String value;
    private String method;
}
