package net.hexpowers.power;

import net.hexpowers.core.Ability;
import net.hexpowers.core.Geometry;
import net.hexpowers.core.HexPath;
import net.hexpowers.core.PlayerState;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;

/** Server-side implementation of every ability. Each method returns true if the spell was actually cast. */
public final class AbilityExecutor {
    private AbilityExecutor() {}

    private static final Random RNG = new Random();
    private static final EntityType<?>[] WARP_POOL = {
            EntityType.SHEEP, EntityType.COW, EntityType.PIG, EntityType.CHICKEN, EntityType.RABBIT, EntityType.CAT
    };

    public static boolean execute(Ability a, ServerWorld w, ServerPlayerEntity p, PlayerState s) {
        return switch (a) {
            case CHAOS_BOLT -> bolt(w, p, HexPath.SCARLET, 9f, 4f, false);
            case TELEKINETIC_WAVE -> telekineticWave(w, p);
            case REALITY_WARP -> realityWarp(w, p);
            case HEX_SHIELD -> hexShield(w, p, s);
            case CHAOS_NOVA -> chaosNova(w, p);
            case ARCANE_BOLT -> bolt(w, p, HexPath.WICCAN, 7f, 0f, true);
            case BLINK -> blink(w, p);
            case ASTRAL_DASH -> astralDash(w, p);
            case MENDING_LIGHT -> mendingLight(w, p);
            case RECALL -> recall(w, p);
            case SOUL_DRAIN -> soulDrain(w, p);
            case RAVEN_SWARM -> ravenSwarm(w, p);
            case WITCHES_ROAD -> witchesRoad(w, p);
            case UNDYING_PACT -> undyingPact(w, p, s);
            case DARKHOLD_CURSE -> darkholdCurse(w, p);
        };
    }

    // ------------------------------------------------------------------ Scarlet Witch

    /** Shared beam used by Chaos Bolt (red, splash damage) and Arcane Bolt (blue, marks the target). */
    private static boolean bolt(ServerWorld w, ServerPlayerEntity p, HexPath st, float damage, float splash, boolean mark) {
        Trace t = Trace.cast(w, p, st == HexPath.SCARLET ? 40 : 36, true);
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d hand = AbilityUtil.hand(p);
        Vfx.handSigil(w, p, st, 1.15f, 13);
        Vfx.beam(w, st, hand, t.end, st == HexPath.SCARLET ? 0.55f : 0.42f, 11, 2);
        Vfx.flash(w, st, t.end, 2.6f, 9, 2);
        Vfx.ringFwd(w, st, t.end, dir.multiply(-0.5), 2.4f, 9, 2);
        FX.puff(w, ParticleTypes.ELECTRIC_SPARK, t.end, 14, 0.4, 0.35);
        if (t.entity != null) {
            AbilityUtil.hurt(p, t.entity, damage);
            AbilityUtil.knock(t.entity, dir.multiply(0.9).add(0, 0.3, 0));
            if (mark) {
                t.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 80, 0));
            } else {
                t.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0));
            }
        }
        if (splash > 0) {
            for (LivingEntity e : AbilityUtil.around(w, t.end, 2.5, x -> AbilityUtil.foe(p, x) && x != t.entity)) {
                AbilityUtil.hurt(p, e, splash);
                Vec3d away = AbilityUtil.center(e).subtract(t.end).normalize();
                AbilityUtil.knock(e, away.multiply(0.6).add(0, 0.25, 0));
            }
        }
        boolean red = st == HexPath.SCARLET;
        FX.sound(w, p.getPos(), red ? SoundEvents.ENTITY_BLAZE_SHOOT : SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, red ? 0.7f : 1.4f);
        FX.sound(w, t.end, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.35f, 1.6f);
        return true;
    }

    private static boolean telekineticWave(ServerWorld w, ServerPlayerEntity p) {
        Vec3d eye = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d hand = AbilityUtil.hand(p);
        List<LivingEntity> hit = w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(12),
                e -> AbilityUtil.valid(p, e) && Geometry.inCone(eye.x, eye.y, eye.z, dir.x, dir.y, dir.z,
                        e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ(), 12, 55));
        for (LivingEntity e : hit) {
            AbilityUtil.knock(e, dir.multiply(1.8).add(0, 0.45, 0));
            if (AbilityUtil.foe(p, e)) AbilityUtil.hurt(p, e, 3f);
        }
        Vfx.handSigil(w, p, HexPath.SCARLET, 1.5f, 14);
        for (int i = 0; i < 4; i++) {
            Vfx.ringFwd(w, HexPath.SCARLET, hand, dir.multiply(12), 5.0f, 12, 2 + i * 2);
        }
        Vfx.flash(w, HexPath.SCARLET, hand, 2.8f, 8, 1);
        FX.sound(w, p.getPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.2f, 0.6f);
        FX.sound(w, p.getPos(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, 0.8f, 1.2f);
        return true;
    }

    /** Hostile mobs nearby are rewritten into harmless farm animals. */
    private static boolean realityWarp(ServerWorld w, ServerPlayerEntity p) {
        List<MobEntity> mobs = w.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(12),
                e -> e instanceof Monster && e.isAlive() && e.getMaxHealth() <= 100f && e.squaredDistanceTo(p) <= 144.0);
        if (mobs.isEmpty()) return false;
        Vec3d c = p.getPos();
        Vfx.ground(w, HexPath.SCARLET, c, 12.5f, 70, 0, null);
        Vfx.swirl(w, HexPath.SCARLET, c, 10.0f, 56, 0);
        Vfx.ringGround(w, HexPath.SCARLET, c, 13.0f, 20, 0);
        Vfx.handSigil(w, p, HexPath.SCARLET, 1.3f, 12);
        for (MobEntity m : mobs) {
            Vec3d pos = m.getPos();
            EntityType<?> type = WARP_POOL[RNG.nextInt(WARP_POOL.length)];
            Entity spawned = type.spawn(w, BlockPos.ofFloored(pos), SpawnReason.MOB_SUMMONED);
            if (spawned != null) {
                spawned.refreshPositionAndAngles(pos.x, pos.y, pos.z, m.getYaw(), 0f);
                m.discard();
                Vfx.flash(w, HexPath.SCARLET, pos.add(0, 0.9, 0), 2.4f, 10, 6);
                Vfx.pillar(w, HexPath.SCARLET, pos, 0.7f, 2.8f, 22, 6, null);
                FX.puff(w, ParticleTypes.WITCH, pos.add(0, 0.8, 0), 16, 0.5, 0.1);
            }
        }
        FX.sound(w, c, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 1.2f, 0.7f);
        FX.sound(w, c, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
        return true;
    }

    private static boolean hexShield(ServerWorld w, ServerPlayerEntity p, PlayerState s) {
        s.shieldTicks = 200;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 1));
        Vfx.dome(w, HexPath.SCARLET, p.getPos(), 2.4f, 200, p);
        Vfx.ground(w, HexPath.SCARLET, p.getPos(), 2.3f, 200, 0, p);
        Vfx.ringGround(w, HexPath.SCARLET, p.getPos(), 5.0f, 14, 0);
        Vfx.flash(w, HexPath.SCARLET, p.getPos().add(0, 1, 0), 3.2f, 9, 0);
        FX.sound(w, p.getPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
        return true;
    }

    /** Ultimate: a huge chaos sigil, a vortex, a pillar and an expanding shockwave. */
    private static boolean chaosNova(ServerWorld w, ServerPlayerEntity p) {
        Vec3d c = p.getPos();
        p.addVelocity(0, 0.5, 0);
        p.velocityModified = true;
        Vfx.ground(w, HexPath.SCARLET, c, 14.5f, 76, 0, null);
        Vfx.swirl(w, HexPath.SCARLET, c, 11.5f, 62, 0);
        Vfx.pillar(w, HexPath.SCARLET, c, 1.9f, 15f, 44, 4, null);
        Vfx.handSigil(w, p, HexPath.SCARLET, 1.7f, 12);
        FX.sound(w, c, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.5f);
        for (int i = 1; i <= 10; i++) {
            final int step = i;
            PowerManager.SCHEDULER.later(i, () -> FX.puff(w, ParticleTypes.ELECTRIC_SPARK, c.add(0, 1, 0), 10, step * 0.9, 0.3));
        }
        PowerManager.SCHEDULER.later(6, () -> {
            for (LivingEntity e : AbilityUtil.around(w, c, 13, x -> AbilityUtil.valid(p, x))) {
                Vec3d off = AbilityUtil.center(e).subtract(c);
                double dist = Math.max(0.5, off.length());
                Vec3d away = off.normalize();
                AbilityUtil.knock(e, away.multiply(1.9).add(0, 0.7, 0));
                if (AbilityUtil.foe(p, e)) {
                    AbilityUtil.hurt(p, e, (float) Math.max(4.0, 16.0 * (1.0 - dist / 14.0)));
                }
            }
            Vfx.waveSphere(w, HexPath.SCARLET, c.add(0, 1, 0), 14f, 16, 0);
            Vfx.ringGround(w, HexPath.SCARLET, c, 15f, 18, 0);
            Vfx.ringGround(w, HexPath.SCARLET, c, 15f, 18, 3);
            Vfx.ringGround(w, HexPath.SCARLET, c, 15f, 18, 6);
            Vfx.flash(w, HexPath.SCARLET, c.add(0, 1, 0), 9f, 14, 0);
            FX.puff(w, ParticleTypes.EXPLOSION_EMITTER, c.add(0, 1, 0), 1, 0, 0);
            FX.sound(w, c, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.6f, 0.7f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Wiccan

    private static boolean blink(ServerWorld w, ServerPlayerEntity p) {
        Trace t = Trace.cast(w, p, 28, false);
        Vec3d dir = p.getRotationVec(1.0f);
        double eyeOff = p.getEyeY() - p.getY();
        Vec3d from = p.getPos();
        for (int i = 0; i <= 10; i++) {
            Vec3d eyeAt = t.end.subtract(dir.multiply(i * 0.5));
            Vec3d feet = new Vec3d(eyeAt.x, eyeAt.y - eyeOff, eyeAt.z);
            if (feet.squaredDistanceTo(from) < 4.0) break;
            if (w.isSpaceEmpty(p, p.getBoundingBox().offset(feet.subtract(from)))) {
                Vfx.ground(w, HexPath.WICCAN, from, 2.3f, 24, 0, null);
                Vfx.pillar(w, HexPath.WICCAN, from, 0.8f, 3.4f, 16, 0, null);
                Vfx.flash(w, HexPath.WICCAN, from.add(0, 1, 0), 3.0f, 9, 0);
                FX.puff(w, ParticleTypes.REVERSE_PORTAL, from.add(0, 1, 0), 30, 0.4, 0.2);
                p.teleport(w, feet.x, feet.y, feet.z, p.getYaw(), p.getPitch());
                p.fallDistance = 0;
                Vfx.ground(w, HexPath.WICCAN, feet, 2.3f, 24, 0, null);
                Vfx.pillar(w, HexPath.WICCAN, feet, 0.8f, 3.4f, 16, 0, null);
                Vfx.flash(w, HexPath.WICCAN, feet.add(0, 1, 0), 3.0f, 9, 0);
                FX.puff(w, ParticleTypes.REVERSE_PORTAL, feet.add(0, 1, 0), 30, 0.4, 0.2);
                FX.sound(w, from, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                FX.sound(w, feet, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.8f);
                return true;
            }
        }
        return false;
    }

    private static boolean astralDash(ServerWorld w, ServerPlayerEntity p) {
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d eye = p.getEyePos();
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(6),
                x -> AbilityUtil.foe(p, x) && Geometry.inCone(eye.x, eye.y, eye.z, dir.x, dir.y, dir.z,
                        x.getX(), x.getY() + x.getHeight() * 0.5, x.getZ(), 6, 22))) {
            AbilityUtil.hurt(p, e, 4f);
            AbilityUtil.knock(e, dir.multiply(0.8).add(0, 0.3, 0));
        }
        Vfx.handSigil(w, p, HexPath.WICCAN, 1.3f, 9);
        for (int i = 0; i < 3; i++) {
            Vfx.ringFwd(w, HexPath.WICCAN, eye.add(dir.multiply(0.8)), dir.multiply(7), 2.6f, 10, 1 + i * 2);
        }
        Vfx.flash(w, HexPath.WICCAN, eye.add(dir.multiply(1.0)), 3.2f, 8, 0);
        p.setVelocity(dir.multiply(2.1).add(0, 0.15, 0));
        p.velocityModified = true;
        p.fallDistance = 0;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 80, 0));
        for (int i = 1; i <= 8; i++) {
            PowerManager.SCHEDULER.later(i, () -> {
                if (!p.isAlive() || p.isRemoved()) return;
                Vec3d at = p.getPos().add(0, 1, 0);
                FX.dust(w, at, FX.BLUE, 1.6f, 6, 0.3);
                FX.puff(w, ParticleTypes.END_ROD, at, 3, 0.25, 0.02);
                Vfx.flash(w, HexPath.WICCAN, at, 1.6f, 6, 0);
            });
        }
        FX.sound(w, p.getPos(), SoundEvents.ENTITY_PHANTOM_FLAP, 1.2f, 1.6f);
        FX.sound(w, p.getPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.8f);
        return true;
    }

    private static boolean mendingLight(ServerWorld w, ServerPlayerEntity p) {
        Vec3d c = p.getPos();
        for (LivingEntity e : AbilityUtil.around(w, c, 8, x -> AbilityUtil.ally(p, x))) {
            e.heal(8f);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
            FX.puff(w, ParticleTypes.HAPPY_VILLAGER, e.getPos().add(0, 1, 0), 10, 0.4, 0.0);
            Vfx.pillar(w, HexPath.WICCAN, e.getPos(), 0.6f, 3.2f, 24, 4, e);
        }
        for (LivingEntity e : AbilityUtil.around(w, c, 8, x -> AbilityUtil.valid(p, x) && x.getType().isIn(EntityTypeTags.UNDEAD))) {
            AbilityUtil.hurt(p, e, 6f);
            e.setOnFireFor(4);
        }
        Vfx.ground(w, HexPath.WICCAN, c, 8.6f, 40, 0, null);
        Vfx.waveSphere(w, HexPath.WICCAN, c.add(0, 0.5, 0), 8.5f, 22, 0);
        Vfx.ringGround(w, HexPath.WICCAN, c, 9f, 18, 0);
        Vfx.pillar(w, HexPath.WICCAN, c, 1.3f, 7f, 28, 0, null);
        Vfx.flash(w, HexPath.WICCAN, c.add(0, 1, 0), 4f, 12, 0);
        for (int i = 1; i <= 8; i++) {
            final int r = i;
            PowerManager.SCHEDULER.later(i, () -> FX.puff(w, ParticleTypes.END_ROD, c.add(0, 0.5, 0), 8, r * 0.5, 0.08));
        }
        FX.sound(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.2f);
        FX.sound(w, c, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        return true;
    }

    /** After a short channel, teleport to the respawn point (bed / anchor) or world spawn. */
    private static boolean recall(ServerWorld w, ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        ServerWorld target = server.getWorld(p.getSpawnPointDimension());
        if (target == null) target = server.getOverworld();
        BlockPos sp = p.getSpawnPointPosition();
        Vec3d dest;
        if (sp != null && target.getRegistryKey().equals(p.getSpawnPointDimension())
                && (target.getBlockState(sp).getBlock() instanceof BedBlock || target.getBlockState(sp).isOf(Blocks.RESPAWN_ANCHOR))) {
            dest = Vec3d.ofBottomCenter(sp.up());
        } else {
            dest = Vec3d.ofBottomCenter(target.getTopPosition(Heightmap.Type.MOTION_BLOCKING, target.getSpawnPos()));
        }
        final ServerWorld targetWorld = target;
        final Vec3d destination = dest;
        Vfx.ground(w, HexPath.WICCAN, p.getPos(), 2.6f, 44, 0, p);
        Vfx.pillar(w, HexPath.WICCAN, p.getPos(), 1.1f, 6f, 44, 0, p);
        FX.sound(w, p.getPos(), SoundEvents.BLOCK_PORTAL_TRIGGER, 0.6f, 1.8f);
        for (int i = 0; i < 10; i++) {
            final int k = i;
            PowerManager.SCHEDULER.later(i * 4, () -> {
                if (!p.isAlive() || p.isRemoved()) return;
                double ang = k * 0.9;
                for (int j = 0; j < 3; j++) {
                    double a = ang + j * (Math.PI * 2 / 3);
                    double y = (k / 10.0) * 2.0;
                    Vec3d at = p.getPos().add(Math.cos(a) * (1.4 - k * 0.1), y, Math.sin(a) * (1.4 - k * 0.1));
                    FX.puff(w, ParticleTypes.END_ROD, at, 1, 0.02, 0.0);
                }
            });
        }
        PowerManager.SCHEDULER.later(40, () -> {
            if (!p.isAlive() || p.isRemoved()) return;
            Vfx.flash(w, HexPath.WICCAN, p.getPos().add(0, 1, 0), 5f, 12, 0);
            Vfx.ringGround(w, HexPath.WICCAN, p.getPos(), 5f, 14, 0);
            p.teleport(targetWorld, destination.x, destination.y, destination.z, p.getYaw(), p.getPitch());
            p.fallDistance = 0;
            Vfx.flash(targetWorld, HexPath.WICCAN, destination.add(0, 1, 0), 5f, 12, 0);
            Vfx.ringGround(targetWorld, HexPath.WICCAN, destination, 5f, 14, 0);
            Vfx.pillar(targetWorld, HexPath.WICCAN, destination, 1.0f, 5f, 18, 0, null);
            FX.sound(targetWorld, destination, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Agatha Harkness

    private static boolean soulDrain(ServerWorld w, ServerPlayerEntity p) {
        Trace t = Trace.cast(w, p, 24, true);
        if (t.entity == null) return false;
        LivingEntity e = t.entity;
        Vec3d tc = AbilityUtil.center(e);
        Vec3d hand = AbilityUtil.hand(p);
        Vfx.handSigil(w, p, HexPath.AGATHA, 1.1f, 13);
        Vfx.beam(w, HexPath.AGATHA, hand, tc, 0.5f, 11, 2);
        Vfx.beam(w, HexPath.AGATHA, tc, p.getPos().add(0, 1.1, 0), 0.32f, 9, 6);
        Vfx.ground(w, HexPath.AGATHA, e.getPos(), 1.9f, 22, 2, e);
        Vfx.flash(w, HexPath.AGATHA, tc, 2.4f, 9, 2);
        Vfx.flash(w, HexPath.AGATHA, p.getPos().add(0, 1.1, 0), 2.2f, 9, 7);
        for (int i = 0; i <= 8; i++) {
            final int k = i;
            PowerManager.SCHEDULER.later(i + 1, () -> {
                Vec3d from = AbilityUtil.center(e);
                Vec3d to = p.getPos().add(0, 1.1, 0);
                Vec3d at = from.add(to.subtract(from).multiply(k / 8.0));
                FX.puff(w, ParticleTypes.SOUL, at, 2, 0.1, 0.01);
            });
        }
        AbilityUtil.hurt(p, e, 7f);
        p.heal(5f);
        FX.sound(w, p.getPos(), SoundEvents.ENTITY_VEX_CHARGE, 1.0f, 0.6f);
        FX.sound(w, tc, SoundEvents.ENTITY_VEX_DEATH, 1.2f, 0.8f);
        return true;
    }

    private static boolean ravenSwarm(ServerWorld w, ServerPlayerEntity p) {
        Vec3d eye = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d hand = AbilityUtil.hand(p);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(14),
                x -> AbilityUtil.foe(p, x) && Geometry.inCone(eye.x, eye.y, eye.z, dir.x, dir.y, dir.z,
                        x.getX(), x.getY() + x.getHeight() * 0.5, x.getZ(), 14, 38))) {
            AbilityUtil.hurt(p, e, 6f);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));
            AbilityUtil.knock(e, dir.multiply(0.4));
        }
        Vfx.handSigil(w, p, HexPath.AGATHA, 1.5f, 15);
        Vfx.ravens(w, HexPath.AGATHA, hand, dir.multiply(14), 16, 16, 2);
        Vfx.ringFwd(w, HexPath.AGATHA, hand, dir.multiply(10), 4.0f, 12, 2);
        Vfx.ringFwd(w, HexPath.AGATHA, hand, dir.multiply(10), 4.0f, 12, 5);
        Vfx.flash(w, HexPath.AGATHA, hand, 2.6f, 8, 1);
        for (int i = 0; i < 10; i++) {
            final int k = i;
            PowerManager.SCHEDULER.later(i + 1, () -> {
                double d = 1.0 + k * 1.4;
                Vec3d at = eye.add(dir.multiply(d));
                double spread = 0.4 + d * 0.22;
                FX.dust(w, at, FX.DARK, 1.8f, 8, spread);
                FX.puff(w, ParticleTypes.SQUID_INK, at, 3, spread * 0.6, 0.03);
                if (k % 3 == 0) FX.sound(w, at, SoundEvents.ENTITY_BAT_TAKEOFF, 0.8f, 0.8f + RNG.nextFloat() * 0.5f);
            });
        }
        FX.sound(w, p.getPos(), SoundEvents.ENTITY_PHANTOM_FLAP, 1.2f, 0.7f);
        return true;
    }

    private static boolean witchesRoad(ServerWorld w, ServerPlayerEntity p) {
        Trace t = Trace.cast(w, p, 24, false);
        BlockPos bp = BlockPos.ofFloored(t.end);
        for (int i = 0; i < 8; i++) {
            BlockPos below = bp.down();
            if (!w.getBlockState(below).getCollisionShape(w, below).isEmpty()) break;
            bp = below;
        }
        Vec3d c = new Vec3d(t.end.x, bp.getY() + 0.1, t.end.z);
        ZoneManager.add(new ZoneManager.Zone(w.getRegistryKey(), c, 5.5, 160, p.getUuid()));
        Vfx.handSigil(w, p, HexPath.AGATHA, 1.2f, 12);
        Vfx.ground(w, HexPath.AGATHA, c, 6.2f, 160, 3, null);
        Vfx.ringGround(w, HexPath.AGATHA, c, 7f, 16, 3);
        Vfx.pillar(w, HexPath.AGATHA, c, 0.55f, 6f, 160, 6, null);
        Vfx.flash(w, HexPath.AGATHA, c.add(0, 0.6, 0), 3.4f, 10, 3);
        FX.sound(w, c, SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 1.4f, 0.7f);
        return true;
    }

    private static boolean undyingPact(ServerWorld w, ServerPlayerEntity p, PlayerState s) {
        s.pactTicks = 600;
        Vfx.dome(w, HexPath.AGATHA, p.getPos(), 2.1f, 44, p);
        Vfx.ground(w, HexPath.AGATHA, p.getPos(), 2.2f, 70, 0, p);
        Vfx.pillar(w, HexPath.AGATHA, p.getPos(), 0.9f, 4.6f, 34, 0, p);
        Vfx.flash(w, HexPath.AGATHA, p.getPos().add(0, 1, 0), 3.2f, 10, 0);
        FX.puff(w, ParticleTypes.TOTEM_OF_UNDYING, p.getPos().add(0, 1, 0), 20, 0.5, 0.2);
        FX.sound(w, p.getPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.5f);
        FX.sound(w, p.getPos(), SoundEvents.ENTITY_VEX_DEATH, 1.0f, 0.5f);
        return true;
    }

    /** Ultimate: a ritual circle that rots everything inside it. */
    private static boolean darkholdCurse(ServerWorld w, ServerPlayerEntity p) {
        Trace t = Trace.cast(w, p, 30, true);
        Vec3d c = t.entity != null ? t.entity.getPos() : t.end;
        double radius = 7;
        for (LivingEntity e : AbilityUtil.around(w, c, radius, x -> AbilityUtil.foe(p, x))) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 160, 1));
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 1));
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0));
            AbilityUtil.hurt(p, e, 4f);
        }
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 0));
        Vfx.handSigil(w, p, HexPath.AGATHA, 1.5f, 14);
        Vfx.ground(w, HexPath.AGATHA, c, 8.2f, 96, 2, null);
        Vfx.swirl(w, HexPath.AGATHA, c, 7.0f, 74, 2);
        Vfx.pillar(w, HexPath.AGATHA, c, 1.5f, 13f, 64, 6, null);
        Vfx.ringGround(w, HexPath.AGATHA, c, 9f, 18, 4);
        Vfx.ringGround(w, HexPath.AGATHA, c, 9f, 18, 8);
        Vfx.flash(w, HexPath.AGATHA, c.add(0, 1, 0), 6f, 12, 4);
        for (int i = 0; i < 16; i++) {
            final int k = i;
            PowerManager.SCHEDULER.later(i + 1, () -> {
                FX.puff(w, ParticleTypes.SOUL_FIRE_FLAME, c.add(0, 0.2 + k * 0.15, 0), 8, 0.5, 0.12);
                FX.puff(w, ParticleTypes.WITCH, c.add(0, 1.0, 0), 10, radius * 0.5, 0.05);
            });
        }
        FX.sound(w, c, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.7f);
        FX.sound(w, c, SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.5f);
        return true;
    }

    /** Used by PowerManager to reflect projectiles while the Hex Shield is up. */
    public static void reflectProjectiles(ServerWorld w, ServerPlayerEntity p) {
        for (ProjectileEntity proj : w.getEntitiesByClass(ProjectileEntity.class, p.getBoundingBox().expand(3.0),
                e -> e.getOwner() != p && e.getVelocity().lengthSquared() > 0.04)) {
            proj.setOwner(p);
            proj.setVelocity(proj.getVelocity().multiply(-1.0));
            proj.velocityModified = true;
            Vfx.flash(w, HexPath.SCARLET, proj.getPos(), 1.6f, 7, 0);
            FX.puff(w, ParticleTypes.ELECTRIC_SPARK, proj.getPos(), 8, 0.2, 0.3);
        }
    }
}
