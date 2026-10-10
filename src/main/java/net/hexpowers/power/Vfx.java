package net.hexpowers.power;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.hexpowers.core.FxType;
import net.hexpowers.core.HexPath;
import net.hexpowers.net.FxPayload;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** Server-side API for the textured magic visuals (rendered on the client by FxRenderer). */
public final class Vfx {
    private Vfx() {}

    private static final double RANGE = 128.0;

    public static void send(ServerWorld w, FxType type, HexPath style, Vec3d pos, Vec3d vec,
                            float size, int life, int delay, int attach) {
        FxPayload p = new FxPayload(type.ordinal(), style.ordinal(), pos.x, pos.y, pos.z,
                (float) vec.x, (float) vec.y, (float) vec.z, life, delay, size, attach);
        for (ServerPlayerEntity sp : PlayerLookup.around(w, pos, RANGE)) {
            ServerPlayNetworking.send(sp, p);
        }
    }

    public static void handSigil(ServerWorld w, ServerPlayerEntity p, HexPath style, float size, int life) {
        send(w, FxType.HAND_SIGIL, style, p.getEyePos(), Vec3d.ZERO, size, life, 0, p.getId());
    }

    public static void ground(ServerWorld w, HexPath style, Vec3d pos, float radius, int life, int delay, Entity follow) {
        send(w, FxType.GROUND_SIGIL, style, pos, Vec3d.ZERO, radius, life, delay, follow == null ? -1 : follow.getId());
    }

    public static void beam(ServerWorld w, HexPath style, Vec3d from, Vec3d to, float width, int life, int delay) {
        send(w, FxType.BEAM, style, from, to.subtract(from), width, life, delay, -1);
    }

    public static void ringFwd(ServerWorld w, HexPath style, Vec3d origin, Vec3d travel, float radius, int life, int delay) {
        send(w, FxType.RING_FWD, style, origin, travel, radius, life, delay, -1);
    }

    public static void ringGround(ServerWorld w, HexPath style, Vec3d pos, float radius, int life, int delay) {
        send(w, FxType.RING_GROUND, style, pos, Vec3d.ZERO, radius, life, delay, -1);
    }

    public static void dome(ServerWorld w, HexPath style, Vec3d pos, float radius, int life, Entity follow) {
        send(w, FxType.DOME, style, pos, Vec3d.ZERO, radius, life, 0, follow == null ? -1 : follow.getId());
    }

    public static void waveSphere(ServerWorld w, HexPath style, Vec3d pos, float radius, int life, int delay) {
        send(w, FxType.WAVE_SPHERE, style, pos, Vec3d.ZERO, radius, life, delay, -1);
    }

    public static void pillar(ServerWorld w, HexPath style, Vec3d base, float radius, float height, int life, int delay, Entity follow) {
        send(w, FxType.PILLAR, style, base, new Vec3d(0, height, 0), radius, life, delay, follow == null ? -1 : follow.getId());
    }

    public static void flash(ServerWorld w, HexPath style, Vec3d pos, float size, int life, int delay) {
        send(w, FxType.FLASH, style, pos, Vec3d.ZERO, size, life, delay, -1);
    }

    public static void swirl(ServerWorld w, HexPath style, Vec3d pos, float radius, int life, int delay) {
        send(w, FxType.SWIRL, style, pos, Vec3d.ZERO, radius, life, delay, -1);
    }

    public static void ravens(ServerWorld w, HexPath style, Vec3d origin, Vec3d travel, int count, int life, int delay) {
        send(w, FxType.RAVENS, style, origin, travel, count, life, delay, -1);
    }
}
