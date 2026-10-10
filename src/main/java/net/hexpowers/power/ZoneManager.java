package net.hexpowers.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Agatha's "Witches' Road": a ritual circle that helps its owner and allies and hexes enemies. */
public final class ZoneManager {
    private ZoneManager() {}

    public static final class Zone {
        final RegistryKey<World> dim;
        final Vec3d center;
        final double radius;
        final UUID owner;
        int left;
        int age;

        public Zone(RegistryKey<World> dim, Vec3d center, double radius, int ticks, UUID owner) {
            this.dim = dim;
            this.center = center;
            this.radius = radius;
            this.left = ticks;
            this.owner = owner;
        }
    }

    private static final List<Zone> ZONES = new ArrayList<>();

    public static void add(Zone z) {
        ZONES.add(z);
    }

    public static void clear() {
        ZONES.clear();
    }

    public static void tick(MinecraftServer server) {
        Iterator<Zone> it = ZONES.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            ServerWorld w = server.getWorld(z.dim);
            if (w == null) {
                it.remove();
                continue;
            }
            if (--z.left <= 0) {
                FX.burst(w, z.center.add(0, 0.5, 0), FX.PURPLE, 20);
                it.remove();
                continue;
            }
            z.age++;
            if (z.age % 2 == 0) visuals(w, z);
            if (z.age % 10 == 0) effects(server, w, z);
        }
    }

    private static void visuals(ServerWorld w, Zone z) {
        // the big rotating sigil / pillar are drawn client-side; add a little atmosphere
        double a = Math.random() * Math.PI * 2;
        double r = Math.random() * z.radius;
        FX.puff(w, ParticleTypes.SOUL_FIRE_FLAME, z.center.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), 2, 0.05, 0.03);
        FX.puff(w, ParticleTypes.WITCH, z.center.add(0, 0.8, 0), 3, z.radius * 0.4, 0.0);
    }

    private static void effects(MinecraftServer server, ServerWorld w, Zone z) {
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(z.owner);
        for (LivingEntity e : AbilityUtil.around(w, z.center.add(0, 1, 0), z.radius + 1.0, x -> x.isAlive() && !x.isSpectator())) {
            if (e.getY() < z.center.y - 2 || e.getY() > z.center.y + 4) continue;
            if (owner != null && AbilityUtil.ally(owner, e)) {
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0));
            } else if (owner != null && AbilityUtil.foe(owner, e)) {
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0));
                AbilityUtil.hurt(owner, e, 1.5f);
            }
        }
    }
}
