package md.utm.isa.distributeddb.consistent_hash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.utm.isa.distributeddb.Entity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsistentHash {
    private final NavigableMap<Long, Node> ring = new TreeMap<>();
    private static final AtomicInteger id = new AtomicInteger(0);

    public void add(Node address, int count) {
        for (int i=0; i< count; i++) {
            Node node = new Node(id.getAndIncrement(), address.getAddress(), address.getTcpAddress());
            ring.put((long) node.hashCode(), node);
        }
    }

    public void remove(Node node) {
        for (Map.Entry<Long, Node> entry: ring.entrySet()) {
            if (node.getAddress().equals(entry.getValue().getAddress())) {
                ring.remove(entry.getKey());
            }
        }
        log.info("HERE");
    }

    public Integer getSize() {
        return ring.size();
    }

    public Node getNearestNode(Entity entity) {
        int hashedId = entity.getValue().hashCode();
        SortedMap<Long, Node> tailMap = ring.tailMap((long)hashedId);
        Long nodeHashKey = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();

        return ring.get(nodeHashKey);
    }

    public List<Node> getNearestNode(Entity entity, int n) {
        int hashedId = entity.getKey().hashCode();
        SortedMap<Long, Node> tailMap = ring.tailMap((long)hashedId);
        Iterator<Node> iterator = tailMap.isEmpty() ? ring.values().iterator() : tailMap.values().iterator();
        Iterator<Node> headIterator = ring.headMap((long) hashedId).values().iterator();

        Map<String, Node> nodes = new HashMap<>();

        while ((iterator.hasNext() || headIterator.hasNext()) && n > 0) {
            Node next = iterator.hasNext() ? iterator.next() : headIterator.next();
            if (!nodes.containsKey(next.getAddress())) {
                nodes.put(next.getAddress(), next);
                n --;
            }
        }
        return nodes.values().stream().toList();
    }
}
