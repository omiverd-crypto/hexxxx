package net.hexpowers.core;

/** Kinds of client-side magic visuals the server can ask clients to draw. */
public enum FxType {
    /** Spinning sigil circle in front of the caster's hand, facing where they aim (attached to entity). */
    HAND_SIGIL,
    /** Big flat sigil on the ground (optionally following an entity). */
    GROUND_SIGIL,
    /** Energy beam from pos to pos+vec. */
    BEAM,
    /** Ring perpendicular to vec that flies from pos to pos+vec while growing. */
    RING_FWD,
    /** Flat shockwave ring expanding on the ground. */
    RING_GROUND,
    /** Hex-pattern bubble around pos (optionally following an entity). */
    DOME,
    /** Hex-pattern sphere that expands outwards and fades. */
    WAVE_SPHERE,
    /** Rising column of energy; vec.y is its height. */
    PILLAR,
    /** Camera-facing flare. */
    FLASH,
    /** Flat spiral vortex. */
    SWIRL,
    /** Flock of ravens flying from pos along vec. size = number of birds. */
    RAVENS;

    public static FxType byOrdinal(int i) {
        FxType[] v = values();
        return (i >= 0 && i < v.length) ? v[i] : FLASH;
    }
}
