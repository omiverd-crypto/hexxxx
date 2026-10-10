package net.hexpowers.client;

import net.hexpowers.core.Ability;
import net.hexpowers.core.HexPath;
import net.hexpowers.net.SyncPayload;

/** What the client last heard from the server; the HUD interpolates between syncs. */
public final class ClientState {
    private ClientState() {}

    public static volatile HexPath path = HexPath.NONE;
    public static volatile float energy;
    public static volatile float displayEnergy;
    public static volatile int selected;
    public static volatile boolean flying;
    public static volatile int shieldTicks;
    public static volatile int pactTicks;
    private static volatile int[] cooldowns = new int[SyncPayload.MAX_ABILITIES];
    private static volatile long syncedAtMs;

    public static void apply(SyncPayload p) {
        HexPath old = path;
        path = p.hexPath();
        energy = p.energy();
        if (old != path) displayEnergy = energy;
        selected = p.selected();
        flying = p.flying();
        shieldTicks = p.shieldTicks();
        pactTicks = p.pactTicks();
        cooldowns = p.cooldowns();
        syncedAtMs = System.currentTimeMillis();
    }

    public static void reset() {
        path = HexPath.NONE;
        energy = 0;
        displayEnergy = 0;
        flying = false;
        shieldTicks = 0;
        pactTicks = 0;
        cooldowns = new int[SyncPayload.MAX_ABILITIES];
    }

    /** Remaining cooldown in ticks, extrapolated since the last sync so the bar moves smoothly. */
    public static float cooldownTicks(Ability a) {
        int idx = a.indexInPath();
        if (idx < 0 || idx >= cooldowns.length) return 0;
        float elapsedTicks = (System.currentTimeMillis() - syncedAtMs) / 50f;
        return Math.max(0f, cooldowns[idx] - elapsedTicks);
    }

    public static float effectTicks(int syncedTicks) {
        float elapsedTicks = (System.currentTimeMillis() - syncedAtMs) / 50f;
        return Math.max(0f, syncedTicks - elapsedTicks);
    }
}
