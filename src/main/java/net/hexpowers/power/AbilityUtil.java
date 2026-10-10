package net.hexpowers.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Target filtering + damage/knockback helpers shared by all abilities. */
public final class AbilityUtil {
    private AbilityUtil() {}

    /** Something an aimed ability may hit: alive, not the caster, not a teammate, not the caster's pet. */
    public static boolean valid(ServerPlayerEntity p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator()) return false;
        if (e instanceof TameableEntity t && t.isOwner(p)) return false;
        return !e.isTeammate(p);
    }

    /** Something an area ability may damage: hostile mobs, other players (vanilla PvP rules apply) or mobs targeting us. */
    public static boolean foe(ServerPlayerEntity p, LivingEntity e) {
        if (!valid(p, e)) return false;
        if (e instanceof Monster || e instanceof PlayerEntity) return true;
        return e instanceof MobEntity m && m.getTarget() == p;
    }

    /** Allies that area heals / buffs can help. */
    public static boolean ally(ServerPlayerEntity p, LivingEntity e) {
        if (e == p) return true;
        if (!e.isAlive()) return false;
        if (e instanceof TameableEntity t && t.isOwner(p)) return true;
        return e instanceof PlayerEntity || e.isTeammate(p);
    }

    public static List<LivingEntity> around(ServerWorld w, Vec3d c, double r, java.util.function.Predicate<LivingEntity> filter) {
        Box box = new Box(c, c).expand(r);
        double r2 = r * r;
        return w.getEntitiesByClass(LivingEntity.class, box, e -> filter.test(e) && e.squaredDistanceTo(c) <= r2);
    }

    public static void hurt(ServerPlayerEntity p, LivingEntity e, float amount) {
        e.damage(p.getServerWorld().getDamageSources().indirectMagic(p, p), amount);
    }

    public static void knock(LivingEntity e, Vec3d v) {
        e.addVelocity(v.x, v.y, v.z);
        e.velocityModified = true;
    }

    public static Vec3d center(LivingEntity e) {
        return new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
    }

    /** Where the hand sigil floats (2 blocks ahead, slightly right and low); must match FxRenderer. Beams start here. */
    public static Vec3d hand(ServerPlayerEntity p) {
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0));
        right = right.lengthSquared() < 1.0e-6 ? Vec3d.ZERO : right.normalize().multiply(0.35);
        return p.getEyePos().add(dir.multiply(2.0)).add(right).add(0, -0.25, 0);
    }
}
