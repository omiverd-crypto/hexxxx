package net.hexpowers.client.fx;

import net.hexpowers.net.FxPayload;

import java.util.ArrayList;
import java.util.List;

/** Client-side list of running magic visuals. */
public final class FxManager {
    private FxManager() {}

    public static final class Active {
        public final FxPayload p;
        public final long startMs;
        public final long seed;

        Active(FxPayload p, long startMs) {
            this.p = p;
            this.startMs = startMs;
            long s = Double.doubleToLongBits(p.x() * 31.0 + p.y() * 17.0 + p.z() * 13.0) ^ (startMs * 0x9E3779B97F4A7C15L);
            this.seed = s == 0 ? 1 : s;
        }
    }

    private static final int MAX = 240;
    private static final List<Active> ACTIVE = new ArrayList<>();

    public static void add(FxPayload p) {
        if (ACTIVE.size() >= MAX) ACTIVE.remove(0);
        ACTIVE.add(new Active(p, System.currentTimeMillis() + p.delay() * 50L));
    }

    public static List<Active> list() {
        return ACTIVE;
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
