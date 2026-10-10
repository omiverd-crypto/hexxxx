package net.hexpowers.core;

import java.util.Arrays;
import java.util.List;

/** The three "paths" a player can follow. Pure data: no Minecraft classes. */
public enum HexPath {
    NONE("none", 0xB8B8C0, 0f, 0f, 0.05f),
    SCARLET("scarlet", 0xE4122D, 100f, 0.30f, 0.090f),
    WICCAN("wiccan", 0x3E9BFF, 100f, 0.28f, 0.105f),
    AGATHA("agatha", 0xA845FF, 100f, 0.25f, 0.075f);

    public final String id;
    public final int color;
    public final float maxEnergy;
    public final float regenPerTick;
    public final float flySpeed;
    private List<Ability> cache;

    HexPath(String id, int color, float maxEnergy, float regenPerTick, float flySpeed) {
        this.id = id;
        this.color = color;
        this.maxEnergy = maxEnergy;
        this.regenPerTick = regenPerTick;
        this.flySpeed = flySpeed;
    }

    public boolean active() {
        return this != NONE;
    }

    public String tag() {
        return "hexpowers_path_" + id;
    }

    public String translationKey() {
        return "path.hexpowers." + id;
    }

    public String energyKey() {
        return "energy.hexpowers." + id;
    }

    public List<Ability> abilities() {
        if (cache == null) {
            cache = Arrays.stream(Ability.values()).filter(a -> a.path == this).toList();
        }
        return cache;
    }

    public HexPath next() {
        HexPath[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static HexPath byId(String id) {
        for (HexPath p : values()) {
            if (p.id.equalsIgnoreCase(id)) return p;
        }
        return NONE;
    }

    public static HexPath byOrdinal(int i) {
        HexPath[] v = values();
        return (i >= 0 && i < v.length) ? v[i] : NONE;
    }
}
