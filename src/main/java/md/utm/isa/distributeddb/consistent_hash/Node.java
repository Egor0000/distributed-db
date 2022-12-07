package md.utm.isa.distributeddb.consistent_hash;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node {
    private final Integer id;
    private final String address;
    private final String tcpAddress;
}
