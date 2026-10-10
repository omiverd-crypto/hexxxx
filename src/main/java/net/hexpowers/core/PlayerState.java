package net.hexpowers.core;

import java.util.Arrays;

/** Per-player runtime state (energy, cooldowns, toggles). No Minecraft dependencies. */
public final class PlayerState {
    public static final float FLIGHT_DRAIN_PER_TICK = 0.18f;
    public static final float FLIGHT_MIN_ENERGY = 10f;

    public enum CastCheck { OK, NO_PATH, COOLDOWN, NO_ENERGY }

    public HexPath path = HexPath.NONE;
    public float energy;
    public int selected;
    public boolean flying;
    public int shieldTicks;
    public int pactTicks;
    private final int[] cooldowns = new int[Ability.values().length];

    public void setPath(HexPath p) {
        path = p;
        selected = 0;
        energy = p.maxEnergy;
        flying = false;
        shieldTicks = 0;
        pactTicks = 0;
        Arrays.fill(cooldowns, 0);
    }

    public Ability selectedAbility() {
        var list = path.abilities();
        return list.isEmpty() ? null : list.get(Math.floorMod(selected, list.size()));
    }

    public void cycle(int delta) {
        int n = path.abilities().size();
        if (n > 0) selected = Math.floorMod(selected + delta, n);
    }

    public int cooldownLeft(Ability a) {
        return cooldowns[a.ordinal()];
    }

    public void setCooldown(Ability a, int ticks) {
        cooldowns[a.ordinal()] = Math.max(0, ticks);
    }

    public CastCheck check(Ability a) {
        if (a == null || a.path != path) return CastCheck.NO_PATH;
        if (cooldowns[a.ordinal()] > 0) return CastCheck.COOLDOWN;
        if (energy < a.cost) return CastCheck.NO_ENERGY;
        return CastCheck.OK;
    }

    public void spend(Ability a) {
        energy = Math.max(0f, energy - a.cost);
        cooldowns[a.ordinal()] = a.cooldown;
    }

    /**
     * Advance one server tick.
     * @return true if flight was force-stopped this tick because energy ran out.
     */
    public boolean tick() {
        for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0) cooldowns[i]--;
        }
        if (shieldTicks > 0) shieldTicks--;
        if (pactTicks > 0) pactTicks--;
        if (!path.active()) return false;
        if (flying) {
            energy = Math.max(0f, energy - FLIGHT_DRAIN_PER_TICK);
            if (energy <= 0f) {
                flying = false;
                return true;
            }
        } else {
            energy = Math.min(path.maxEnergy, energy + path.regenPerTick);
        }
        return false;
    }
}
