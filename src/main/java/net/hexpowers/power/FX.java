package net.hexpowers.power;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Particle / sound helpers. Everything is server-side so every nearby player sees the effects. */
public final class FX {
    private FX() {}

    public static final int RED = 0xE4122D;
    public static final int BLUE = 0x3E9BFF;
    public static final int PURPLE = 0xA845FF;
    public static final int DARK = 0x1B1030;

    private static Vector3f rgb(int c) {
        return new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
    }

    public static void dust(ServerWorld w, Vec3d p, int color, float size, int count, double spread) {
        w.spawnParticles(new DustParticleEffect(rgb(color), size), p.x, p.y, p.z, count, spread, spread, spread, 0.0);
    }

    public static void puff(ServerWorld w, ParticleEffect type, Vec3d p, int count, double spread, double speed) {
        w.spawnParticles(type, p.x, p.y, p.z, count, spread, spread, spread, speed);
    }

    /** A bright burst of coloured energy. */
    public static void burst(ServerWorld w, Vec3d c, int color, int n) {
        dust(w, c, color, 1.6f, n, 0.5);
        puff(w, ParticleTypes.END_ROD, c, Math.max(1, n / 2), 0.4, 0.12);
        puff(w, ParticleTypes.ELECTRIC_SPARK, c, Math.max(1, n / 2), 0.3, 0.4);
    }

    /** A glowing line of energy from a to b. */
    public static void trail(ServerWorld w, Vec3d from, Vec3d to, int color, double step) {
        Vec3d d = to.subtract(from);
        double len = d.length();
        if (len < 0.01) return;
        Vec3d dir = d.multiply(1.0 / len);
        int i = 0;
        for (double t = 0; t <= len; t += step, i++) {
            Vec3d pt = from.add(dir.multiply(t));
            dust(w, pt, color, 1.0f, 1, 0.03);
            if (i % 3 == 0) puff(w, ParticleTypes.END_ROD, pt, 1, 0.04, 0.0);
        }
    }

    /** Horizontal ring. */
    public static void ring(ServerWorld w, Vec3d c, double radius, int color, int points, double angleOffset, float size) {
        for (int i = 0; i < points; i++) {
            double a = angleOffset + (Math.PI * 2.0 * i) / points;
            dust(w, c.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), color, size, 1, 0.02);
        }
    }

    /** Ring in the plane perpendicular to dir (used for forward-moving shockwaves). */
    public static void ringFacing(ServerWorld w, Vec3d c, Vec3d dir, double radius, int color, int points) {
        Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 1.0e-6) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(dir).normalize();
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points;
            Vec3d pt = c.add(right.multiply(Math.cos(a) * radius)).add(up.multiply(Math.sin(a) * radius));
            dust(w, pt, color, 1.2f, 1, 0.02);
        }
    }

    public static void sound(ServerWorld w, Vec3d p, SoundEvent s, float volume, float pitch) {
        w.playSound(null, p.x, p.y, p.z, s, SoundCategory.PLAYERS, volume, pitch);
    }
}
