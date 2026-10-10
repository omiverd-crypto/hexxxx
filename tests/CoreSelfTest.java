import net.hexpowers.core.*;
import java.util.*;

public class CoreSelfTest {
    static int passed = 0;
    static void check(boolean cond, String name) {
        if (!cond) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("  ok  " + name);
    }

    public static void main(String[] args) {
        // --- paths & abilities
        for (HexPath p : HexPath.values()) {
            if (p.active()) check(p.abilities().size() == 5, p.id + " has 5 abilities");
        }
        check(HexPath.NONE.abilities().isEmpty(), "NONE has no abilities");
        check(HexPath.SCARLET.next() == HexPath.WICCAN && HexPath.AGATHA.next() == HexPath.NONE
              && HexPath.NONE.next() == HexPath.SCARLET, "path cycle NONE>S>W>A>NONE");
        check(HexPath.byId("AGATHA") == HexPath.AGATHA && HexPath.byId("nope") == HexPath.NONE, "byId");
        Set<String> ids = new HashSet<>();
        for (Ability a : Ability.values()) {
            check(ids.add(a.id), "unique id " + a.id);
            check(a.cost > 0 && a.cost <= a.path.maxEnergy, "cost sane " + a.id);
            check(a.cooldown > 0, "cooldown > 0 " + a.id);
        }
        check(Ability.BLINK.indexInPath() == 1, "BLINK is index 1 in Wiccan list");

        // --- state: casting
        PlayerState s = new PlayerState();
        check(s.check(Ability.CHAOS_BOLT) == PlayerState.CastCheck.NO_PATH, "no path -> NO_PATH");
        s.setPath(HexPath.SCARLET);
        check(s.energy == 100f && s.selectedAbility() == Ability.CHAOS_BOLT, "setPath fills energy, selects first");
        check(s.check(Ability.ARCANE_BOLT) == PlayerState.CastCheck.NO_PATH, "wrong-path ability rejected");
        check(s.check(Ability.CHAOS_BOLT) == PlayerState.CastCheck.OK, "can cast fresh");
        s.spend(Ability.CHAOS_BOLT);
        check(s.energy == 82f, "energy spent");
        check(s.check(Ability.CHAOS_BOLT) == PlayerState.CastCheck.COOLDOWN, "cooldown blocks");
        for (int i = 0; i < Ability.CHAOS_BOLT.cooldown; i++) s.tick();
        check(s.cooldownLeft(Ability.CHAOS_BOLT) == 0, "cooldown expires after N ticks");
        s.energy = 5;
        check(s.check(Ability.CHAOS_BOLT) == PlayerState.CastCheck.NO_ENERGY, "low energy blocks");

        // --- regen caps at max
        s.energy = 99.9f;
        for (int i = 0; i < 100; i++) s.tick();
        check(s.energy == 100f, "regen capped at max");
        s.energy = 0;
        for (int i = 0; i < 100; i++) s.tick();
        check(Math.abs(s.energy - 30f) < 0.01f, "regen 0.30/tick * 100 = 30");

        // --- cycle wraparound
        s.setPath(HexPath.AGATHA);
        s.cycle(-1);
        check(s.selectedAbility() == Ability.DARKHOLD_CURSE, "cycle -1 wraps to last");
        s.cycle(1);
        check(s.selectedAbility() == Ability.SOUL_DRAIN, "cycle +1 wraps to first");
        PlayerState none = new PlayerState();
        none.cycle(1);
        check(none.selectedAbility() == null, "cycle on NONE is safe");

        // --- flight drain
        s.setPath(HexPath.WICCAN);
        s.flying = true;
        int ticks = 0;
        boolean stopped = false;
        while (!stopped && ticks < 10000) { stopped = s.tick(); ticks++; }
        check(stopped && !s.flying && s.energy == 0f, "flight stops at 0 energy");
        check(ticks > 400 && ticks < 700, "flight lasts ~30s (" + ticks + " ticks)");

        // --- shield / pact timers
        s.shieldTicks = 3; s.pactTicks = 2;
        s.tick(); s.tick();
        check(s.shieldTicks == 1 && s.pactTicks == 0, "timers count down and floor at 0");
        s.tick(); s.tick();
        check(s.shieldTicks == 0 && s.pactTicks == 0, "timers never negative");

        // --- scheduler
        List<String> log = new ArrayList<>();
        Scheduler sch = new Scheduler(e -> log.add("ERR " + e.getMessage()));
        sch.later(2, () -> log.add("b"));
        sch.later(1, () -> { log.add("a"); sch.later(1, () -> log.add("a2")); });
        sch.later(0, () -> log.add("zero-is-next-tick"));
        sch.later(3, () -> { throw new IllegalStateException("boom"); });
        sch.later(3, () -> log.add("c"));
        sch.tick(); // t1: a, zero
        check(log.equals(List.of("a", "zero-is-next-tick")), "tick1 runs due tasks only: " + log);
        sch.tick(); // t2: b, a2
        check(log.contains("b") && log.contains("a2"), "nested schedule runs next tick");
        sch.tick(); // t3: error + c
        check(log.contains("ERR boom") && log.contains("c"), "exception isolated, later task still runs");
        check(sch.pending() == 0, "scheduler drained");

        // --- cone geometry
        check(Geometry.inCone(0,0,0, 1,0,0, 5,0,0, 10, 30), "point straight ahead inside");
        check(!Geometry.inCone(0,0,0, 1,0,0, -5,0,0, 10, 30), "point behind outside");
        check(!Geometry.inCone(0,0,0, 1,0,0, 11,0,0, 10, 30), "beyond range outside");
        check(Geometry.inCone(0,0,0, 1,0,0, 5,2,0, 10, 30), "inside half-angle (21.8deg)");
        check(!Geometry.inCone(0,0,0, 1,0,0, 5,4,0, 10, 30), "outside half-angle (38.7deg)");
        check(Geometry.inCone(0,0,0, 0,0,1, 0,0,0, 10, 30), "origin itself inside");
        check(!Geometry.inCone(0,0,0, 0,0,0, 1,0,0, 10, 30), "zero direction -> false, no NaN");

        System.out.println("\nALL " + passed + " CHECKS PASSED");
    }
}
