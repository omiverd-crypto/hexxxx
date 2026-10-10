package net.hexpowers.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Marches a ray from the caster's eyes. Stops at the first solid block or (optionally) living entity. */
public final class Trace {
    public final Vec3d end;
    public final LivingEntity entity;

    private Trace(Vec3d end, LivingEntity entity) {
        this.end = end;
        this.entity = entity;
    }

    public static Trace cast(ServerWorld w, ServerPlayerEntity p, double range, boolean entities) {
        Vec3d from = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d last = from;
        for (double d = 0.3; d <= range; d += 0.25) {
            Vec3d pt = from.add(dir.multiply(d));
            BlockPos bp = BlockPos.ofFloored(pt);
            if (!w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()) {
                return new Trace(last, null);
            }
            if (entities) {
                LivingEntity hit = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(pt, pt).expand(1.5), x -> AbilityUtil.valid(p, x))) {
                    if (e.getBoundingBox().expand(0.25).contains(pt)) {
                        double dd = e.squaredDistanceTo(pt);
                        if (dd < best) {
                            best = dd;
                            hit = e;
                        }
                    }
                }
                if (hit != null) return new Trace(pt, hit);
            }
            last = pt;
        }
        return new Trace(last, null);
    }
}
