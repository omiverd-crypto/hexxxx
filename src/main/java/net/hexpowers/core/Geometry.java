package net.hexpowers.core;

public final class Geometry {
    private Geometry() {}

    /** Is point (px,py,pz) inside the cone starting at o, looking along d, with given range and half-angle? */
    public static boolean inCone(double ox, double oy, double oz,
                                 double dx, double dy, double dz,
                                 double px, double py, double pz,
                                 double range, double halfAngleDeg) {
        double vx = px - ox, vy = py - oy, vz = pz - oz;
        double dist = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (dist > range) return false;
        if (dist < 1.0e-6) return true;
        double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dl < 1.0e-9) return false;
        double cos = (vx * dx + vy * dy + vz * dz) / (dist * dl);
        return cos >= Math.cos(Math.toRadians(halfAngleDeg));
    }
}
