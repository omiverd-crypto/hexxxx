package net.hexpowers.power;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.hexpowers.HexPowers;
import net.hexpowers.core.Ability;
import net.hexpowers.core.HexPath;
import net.hexpowers.core.PlayerState;
import net.hexpowers.core.Scheduler;
import net.hexpowers.net.CastPayload;
import net.hexpowers.net.SyncPayload;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns all per-player state on the server and drives the game loop for the mod. */
public final class PowerManager {
    private PowerManager() {}

    public static final Scheduler SCHEDULER = new Scheduler(e -> HexPowers.LOGGER.error("Scheduled hex task failed", e));
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    /** Persisted in the player's NBT (command tags) so a stale "allowFlying" can be cleaned up after a crash/relog. */
    private static final String FLIGHT_TAG = "hexpowers_flight";

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(PowerManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity p = handler.player;
            PlayerState s = STATES.get(p.getUuid());
            if (s != null && s.flying) stopFlight(p, s, false);
            STATES.remove(p.getUuid());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PlayerState s = get(newPlayer);
            s.flying = false;
            s.shieldTicks = 0;
            s.pactTicks = 0;
            s.energy = s.path.maxEnergy;
            sync(newPlayer, s);
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register(PowerManager::allowDeath);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            STATES.clear();
            SCHEDULER.clear();
            ZoneManager.clear();
        });
        ServerPlayNetworking.registerGlobalReceiver(CastPayload.ID, (payload, context) -> handle(context.player(), payload.action()));
    }

    // ------------------------------------------------------------ state access

    public static PlayerState get(ServerPlayerEntity p) {
        return STATES.computeIfAbsent(p.getUuid(), u -> {
            PlayerState s = new PlayerState();
            s.path = readPath(p);
            s.energy = s.path.maxEnergy;
            return s;
        });
    }

    private static HexPath readPath(ServerPlayerEntity p) {
        for (HexPath h : HexPath.values()) {
            if (h.active() && p.getCommandTags().contains(h.tag())) return h;
        }
        return HexPath.NONE;
    }

    public static void setPath(ServerPlayerEntity p, HexPath np) {
        PlayerState s = get(p);
        if (s.flying) stopFlight(p, s, true);
        for (HexPath h : HexPath.values()) p.removeCommandTag(h.tag());
        if (np.active()) p.addCommandTag(np.tag());
        s.setPath(np);
        sync(p, s);
    }

    public static void refill(ServerPlayerEntity p) {
        PlayerState s = get(p);
        s.energy = s.path.maxEnergy;
        for (Ability a : s.path.abilities()) s.setCooldown(a, 0);
        sync(p, s);
    }

    public static void sync(ServerPlayerEntity p, PlayerState s) {
        ServerPlayNetworking.send(p, SyncPayload.of(s));
    }

    // ------------------------------------------------------------ events

    private static void onJoin(ServerPlayerEntity p) {
        if (p.getCommandTags().contains(FLIGHT_TAG)) {
            p.removeCommandTag(FLIGHT_TAG);
            if (canFly(p)) {
                PlayerAbilities ab = p.getAbilities();
                ab.allowFlying = false;
                ab.flying = false;
                ab.setFlySpeed(0.05f);
                p.sendAbilitiesUpdate();
            }
        }
        PlayerState s = get(p);
        s.flying = false;
        s.energy = s.path.maxEnergy;
        sync(p, s);
    }

    /** Agatha's Undying Pact: cheat death once while it is active. */
    private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
        if (entity instanceof ServerPlayerEntity p) {
            PlayerState s = STATES.get(p.getUuid());
            if (s != null && s.path == HexPath.AGATHA && s.pactTicks > 0) {
                s.pactTicks = 0;
                p.setHealth(6.0f);
                p.clearStatusEffects();
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 1));
                ServerWorld w = p.getServerWorld();
                w.sendEntityStatus(p, (byte) 35);
                FX.burst(w, p.getPos().add(0, 1, 0), FX.PURPLE, 40);
                Vfx.waveSphere(w, HexPath.AGATHA, p.getPos().add(0, 1, 0), 7f, 18, 0);
                Vfx.ringGround(w, HexPath.AGATHA, p.getPos(), 7f, 16, 0);
                Vfx.pillar(w, HexPath.AGATHA, p.getPos(), 1.2f, 7f, 26, 0, null);
                Vfx.flash(w, HexPath.AGATHA, p.getPos().add(0, 1, 0), 6f, 12, 0);
                FX.sound(w, p.getPos(), SoundEvents.ITEM_TOTEM_USE, 1.0f, 0.8f);
                p.sendMessage(Text.translatable("message.hexpowers.pact_triggered"), true);
                sync(p, s);
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------ input

    private static void handle(ServerPlayerEntity p, int action) {
        PlayerState s = get(p);
        if (!s.path.active()) {
            p.sendMessage(Text.translatable("message.hexpowers.no_path"), true);
            return;
        }
        switch (action) {
            case CastPayload.CAST -> cast(p, s);
            case CastPayload.NEXT -> { s.cycle(1); sync(p, s); }
            case CastPayload.PREV -> { s.cycle(-1); sync(p, s); }
            case CastPayload.FLIGHT -> toggleFlight(p, s);
            default -> { }
        }
    }

    private static void cast(ServerPlayerEntity p, PlayerState s) {
        if (!p.isAlive() || p.isSpectator()) return;
        Ability a = s.selectedAbility();
        if (a == null) return;
        switch (s.check(a)) {
            case COOLDOWN -> {
                p.sendMessage(Text.translatable("message.hexpowers.cooldown",
                        Text.translatable(a.translationKey()), String.format("%.1f", s.cooldownLeft(a) / 20f)), true);
                return;
            }
            case NO_ENERGY -> {
                p.sendMessage(Text.translatable("message.hexpowers.no_energy", Text.translatable(s.path.energyKey())), true);
                return;
            }
            case NO_PATH -> {
                return;
            }
            case OK -> { }
        }
        ServerWorld w = p.getServerWorld();
        if (AbilityExecutor.execute(a, w, p, s)) {
            s.spend(a);
            sync(p, s);
        } else {
            p.sendMessage(Text.translatable("message.hexpowers.no_target"), true);
        }
    }

    // ------------------------------------------------------------ flight

    private static boolean canFly(ServerPlayerEntity p) {
        return !p.isCreative() && !p.isSpectator();
    }

    private static void toggleFlight(ServerPlayerEntity p, PlayerState s) {
        if (!canFly(p)) {
            p.sendMessage(Text.translatable("message.hexpowers.flight_creative"), true);
            return;
        }
        if (s.flying) {
            stopFlight(p, s, true);
            p.sendMessage(Text.translatable("message.hexpowers.flight_off"), true);
        } else if (s.energy < PlayerState.FLIGHT_MIN_ENERGY) {
            p.sendMessage(Text.translatable("message.hexpowers.no_energy", Text.translatable(s.path.energyKey())), true);
        } else {
            s.flying = true;
            PlayerAbilities ab = p.getAbilities();
            ab.allowFlying = true;
            ab.flying = true;
            ab.setFlySpeed(s.path.flySpeed);
            p.sendAbilitiesUpdate();
            p.addCommandTag(FLIGHT_TAG);
            p.addVelocity(0, 0.45, 0);
            p.velocityModified = true;
            ServerWorld w = p.getServerWorld();
            FX.burst(w, p.getPos().add(0, 0.3, 0), s.path.color, 16);
            Vfx.ground(w, s.path, p.getPos(), 1.9f, 30, 0, null);
            Vfx.ringGround(w, s.path, p.getPos(), 4f, 12, 0);
            Vfx.flash(w, s.path, p.getPos().add(0, 0.6, 0), 2.6f, 9, 0);
            FX.sound(w, p.getPos(), SoundEvents.ENTITY_PHANTOM_FLAP, 1.0f, 1.3f);
            p.sendMessage(Text.translatable("message.hexpowers.flight_on"), true);
        }
        sync(p, s);
    }

    private static void stopFlight(ServerPlayerEntity p, PlayerState s, boolean softLanding) {
        s.flying = false;
        p.removeCommandTag(FLIGHT_TAG);
        if (canFly(p)) {
            PlayerAbilities ab = p.getAbilities();
            ab.allowFlying = false;
            ab.flying = false;
            ab.setFlySpeed(0.05f);
            p.sendAbilitiesUpdate();
            if (softLanding) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
        }
    }

    // ------------------------------------------------------------ tick

    private static void tick(MinecraftServer server) {
        SCHEDULER.tick();
        ZoneManager.tick(server);
        int now = server.getTicks();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            PlayerState s = get(p);
            if (!s.path.active()) continue;
            boolean outOfEnergy = s.tick();
            if (!p.isAlive()) continue;
            if (outOfEnergy) {
                stopFlight(p, s, true);
                p.sendMessage(Text.translatable("message.hexpowers.flight_exhausted"), true);
                sync(p, s);
            }
            ServerWorld w = p.getServerWorld();
            if (s.shieldTicks > 0) shieldTick(w, p, s, now);
            if (now % 6 == 0) ambient(w, p, s);
            if (now % 4 == 0) sync(p, s);
        }
    }

    private static void shieldTick(ServerWorld w, ServerPlayerEntity p, PlayerState s, int now) {
        if (now % 3 == 0) AbilityExecutor.reflectProjectiles(w, p);
        if (now % 5 == 0) {
            for (LivingEntity e : AbilityUtil.around(w, p.getPos().add(0, 1, 0), 2.6, x -> AbilityUtil.foe(p, x))) {
                Vec3d away = AbilityUtil.center(e).subtract(p.getPos().add(0, 1, 0)).normalize();
                AbilityUtil.knock(e, away.multiply(0.55).add(0, 0.12, 0));
            }
        }
    }

    /** Subtle aura so other players can see who has which powers. */
    private static void ambient(ServerWorld w, ServerPlayerEntity p, PlayerState s) {
        Vec3d at = p.getPos().add(0, 1.0, 0);
        switch (s.path) {
            case SCARLET -> {
                FX.dust(w, at, FX.RED, 1.0f, 2, 0.4);
                if (p.getRandom().nextInt(4) == 0) FX.puff(w, ParticleTypes.ELECTRIC_SPARK, at, 1, 0.4, 0.05);
            }
            case WICCAN -> {
                FX.dust(w, at, FX.BLUE, 1.0f, 2, 0.4);
                if (p.getRandom().nextInt(4) == 0) FX.puff(w, ParticleTypes.END_ROD, at, 1, 0.4, 0.01);
            }
            case AGATHA -> {
                FX.dust(w, at, FX.PURPLE, 1.0f, 2, 0.4);
                if (p.getRandom().nextInt(3) == 0) FX.puff(w, ParticleTypes.WITCH, at, 1, 0.4, 0.0);
            }
            default -> { }
        }
        if (s.pactTicks > 0) FX.puff(w, ParticleTypes.SOUL, p.getPos().add(0, 0.3, 0), 1, 0.35, 0.02);
        if (s.flying) {
            FX.dust(w, p.getPos().add(0, 0.2, 0), s.path.color, 1.5f, 3, 0.25);
            if (p.getRandom().nextInt(3) == 0) Vfx.flash(w, s.path, p.getPos().add(0, 0.1, 0), 1.3f, 8, 0);
        }
    }
}
