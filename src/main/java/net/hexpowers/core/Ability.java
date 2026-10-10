package net.hexpowers.core;

/** Every castable ability. Cost is in energy points, cooldown in server ticks (20 = 1s). */
public enum Ability {
    // Scarlet Witch (Wanda Maximoff): chaos magic
    CHAOS_BOLT(HexPath.SCARLET, "chaos_bolt", 18, 30),
    TELEKINETIC_WAVE(HexPath.SCARLET, "telekinetic_wave", 25, 100),
    REALITY_WARP(HexPath.SCARLET, "reality_warp", 35, 240),
    HEX_SHIELD(HexPath.SCARLET, "hex_shield", 30, 300),
    CHAOS_NOVA(HexPath.SCARLET, "chaos_nova", 70, 800),

    // Wiccan (Billy Maximoff): spellcasting, teleportation, healing
    ARCANE_BOLT(HexPath.WICCAN, "arcane_bolt", 14, 20),
    BLINK(HexPath.WICCAN, "blink", 15, 60),
    ASTRAL_DASH(HexPath.WICCAN, "astral_dash", 20, 80),
    MENDING_LIGHT(HexPath.WICCAN, "mending_light", 30, 200),
    RECALL(HexPath.WICCAN, "recall", 40, 1200),

    // Agatha Harkness: darkhold witchcraft
    SOUL_DRAIN(HexPath.AGATHA, "soul_drain", 20, 60),
    RAVEN_SWARM(HexPath.AGATHA, "raven_swarm", 25, 120),
    WITCHES_ROAD(HexPath.AGATHA, "witches_road", 35, 300),
    UNDYING_PACT(HexPath.AGATHA, "undying_pact", 45, 1800),
    DARKHOLD_CURSE(HexPath.AGATHA, "darkhold_curse", 65, 700);

    public final HexPath path;
    public final String id;
    public final float cost;
    public final int cooldown;

    Ability(HexPath path, String id, float cost, int cooldown) {
        this.path = path;
        this.id = id;
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public String translationKey() {
        return "ability.hexpowers." + id;
    }

    public int indexInPath() {
        return path.abilities().indexOf(this);
    }
}
