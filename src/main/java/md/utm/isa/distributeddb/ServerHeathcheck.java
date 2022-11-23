package md.utm.isa.distributeddb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class ServerHeathcheck {
    public final static List<Long> healthCehckTimer = new ArrayList<>(Collections.nCopies(10, System.currentTimeMillis()));
}
