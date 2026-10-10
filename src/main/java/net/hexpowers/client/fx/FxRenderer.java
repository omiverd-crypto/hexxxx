package net.hexpowers.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.hexpowers.core.FxType;
import net.hexpowers.core.HexPath;
import net.hexpowers.net.FxPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Iterator;

/**
 * Draws the textured magic visuals (sigils, beams, domes, pillars, ravens) in the world.
 * All geometry is built in camera-relative coordinates to avoid float jitter far from the origin.
 */
public final class FxRenderer {
    private FxRenderer() {}

    private static Identifier tex(String n) {
        return Identifier.of("hexpowers", "textures/fx/" + n + ".png");
    }

    private static final Identifier SIGIL_CHAOS = tex("sigil_chaos");
    private static final Identifier SIGIL_ARCANE = tex("sigil_arcane");
    private static final Identifier SIGIL_HEX = tex("sigil_hex");
    private static final Identifier RUNES = tex("rune_ring");
    private static final Identifier HEX_GRID = tex("hex_grid");
    private static final Identifier GLOW = tex("glow");
    private static final Identifier FLARE = tex("flare");
    private static final Identifier BEAM = tex("beam");
    private static final Identifier PILLAR = tex("pillar");
    private static final Identifier SWIRL = tex("swirl");
    private static final Identifier SHOCK = tex("shock_ring");
    private static final Identifier RAVEN = tex("raven");
    private static final int RAVEN_FRAMES = 4;
    private static final int[] RAVEN_SEQ = {0, 1, 2, 3, 2, 1};

    // ---- per-frame state
    private static Matrix4f M;
    private static double CX, CY, CZ;
    private static Vec3d camPos, camRight, camUp;
    private static BufferBuilder bb;
    private static Identifier curTex;
    private static boolean curAdd;

    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(FxRenderer::render);
    }

    // =====================================================================================
    //  frame
    // =====================================================================================
    private static void render(WorldRenderContext ctx) {
        if (FxManager.list().isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || ctx.matrixStack() == null) return;

        Camera cam = ctx.camera();
        camPos = cam.getPos();
        CX = camPos.x;
        CY = camPos.y;
        CZ = camPos.z;
        Quaternionf q = cam.getRotation();
        Vector3f r = new Vector3f(1, 0, 0).rotate(q);
        Vector3f u = new Vector3f(0, 1, 0).rotate(q);
        camRight = new Vec3d(r.x, r.y, r.z);
        camUp = new Vec3d(u.x, u.y, u.z);
        M = ctx.matrixStack().peek().getPositionMatrix();

        float delta = mc.getRenderTickCounter().getTickDelta(false);
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        for (int pass = 0; pass < 2; pass++) {
            for (Iterator<FxManager.Active> it = FxManager.list().iterator(); it.hasNext(); ) {
                FxManager.Active a = it.next();
                double age = (now - a.startMs) / 50.0;
                if (pass == 0) {
                    if (age > a.p.life()) {
                        it.remove();
                        continue;
                    }
                }
                if (age < 0) continue;
                boolean ravens = FxType.byOrdinal(a.p.type()) == FxType.RAVENS;
                if ((pass == 1) != ravens) continue;
                Vec3d p = new Vec3d(a.p.x(), a.p.y(), a.p.z());
                if (p.squaredDistanceTo(camPos) > 200 * 200) continue;
                try {
                    draw(a, age, delta, mc);
                } catch (RuntimeException ignored) {
                    // never let a visual crash the frame
                }
            }
            flush();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // =====================================================================================
    //  dispatch
    // =====================================================================================
    private static void draw(FxManager.Active a, double age, float delta, MinecraftClient mc) {
        FxPayload p = a.p;
        FxType type = FxType.byOrdinal(p.type());
        int style = p.style();
        double life = Math.max(1, p.life());
        double size = p.size();
        Vec3d pos = new Vec3d(p.x(), p.y(), p.z());
        Vec3d vec = new Vec3d(p.vx(), p.vy(), p.vz());
        double time = System.currentTimeMillis() / 50.0;

        Entity follow = null;
        if (p.attach() >= 0) {
            follow = mc.world.getEntityById(p.attach());
            if (follow == null || (follow instanceof LivingEntity le && !le.isAlive())) return;
        }

        switch (type) {
            case HAND_SIGIL -> {
                if (follow == null) return;
                Vec3d eye = follow.getLerpedPos(delta).add(0, follow.getEyeHeight(follow.getPose()), 0);
                Vec3d dir = follow.getRotationVec(delta);
                Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0));
                right = right.lengthSquared() < 1.0e-6 ? Vec3d.ZERO : right.normalize().multiply(0.35);
                Vec3d c = eye.add(dir.multiply(2.0)).add(right).add(0, -0.25, 0);
                double sc = easeOutBack(age / 4.0) * 0.62;   // keep it from covering the crosshair in first person
                float al = (float) (smooth(age / 2.5) * smooth((life - age) / 5.0));
                sigilStack(c, dir, size * sc, style, al, age * 0.30);
                if (age < 4) flare(c, size * 1.1 * (1 - age / 4), style, 0.9f * (float) (1 - age / 4));
            }
            case GROUND_SIGIL -> {
                Vec3d c = (follow != null ? follow.getLerpedPos(delta) : pos).add(0, 0.07, 0);
                double fin = Math.min(10, life * 0.3), fout = Math.min(16, life * 0.4);
                double sc = easeOutBack(age / (fin * 1.6));
                float al = (float) (smooth(age / fin) * smooth((life - age) / fout));
                sigilStack(c, new Vec3d(0, 1, 0), size * sc, style, al, age * 0.028);
            }
            case BEAM -> beam(pos, pos.add(vec), size, style, age, life, a.seed, time);
            case RING_FWD -> {
                double pr = clamp01(age / life);
                Vec3d n = vec.lengthSquared() < 1.0e-8 ? new Vec3d(0, 1, 0) : vec.normalize();
                Vec3d c = pos.add(vec.multiply(easeOut(pr)));
                double rad = size * (0.35 + 0.65 * easeOut(pr));
                float al = (float) (Math.pow(1 - pr, 1.2) * smooth(age / 1.5));
                orientedQuad(SHOCK, true, c, n, rad, 0, col(style, 0.25), al * 0.95f);
                orientedQuad(SHOCK, true, c.add(n.multiply(0.06)), n, rad * 0.78, 0, col(style, 0.7), al * 0.5f);
            }
            case RING_GROUND -> {
                double pr = clamp01(age / life);
                Vec3d c = pos.add(0, 0.08, 0);
                double rad = size * (0.1 + 0.9 * easeOut(pr));
                float al = (float) Math.pow(1 - pr, 1.1);
                orientedQuad(SHOCK, true, c, new Vec3d(0, 1, 0), rad, 0, col(style, 0.25), al);
                orientedQuad(SHOCK, true, c.add(0, 0.02, 0), new Vec3d(0, 1, 0), rad * 0.8, 0, col(style, 0.7), al * 0.5f);
            }
            case DOME -> {
                Vec3d c = follow != null ? follow.getLerpedPos(delta).add(0, follow.getHeight() * 0.5, 0) : pos;
                double rad = size * easeOutBack(age / 6.0);
                float al = (float) (smooth(age / 6.0) * smooth((life - age) / 14.0));
                double pulse = 0.85 + 0.15 * Math.sin(time * 0.35);
                sphere(c, rad, 0.0, col(style, 0.1), (float) (0.55 * al * pulse), 1.6, time * 0.004);
                sphere(c, rad * 1.02, 0.0, col(style, 0.55), (float) (0.18 * al), 3.0, -time * 0.003);
            }
            case WAVE_SPHERE -> {
                double pr = clamp01(age / life);
                double rad = size * easeOut(pr);
                float al = (float) Math.pow(1 - pr, 1.3);
                sphere(pos, rad, 0.0, col(style, 0.2), 0.75f * al, 1.2, 0);
                sphere(pos, rad * 0.94, 0.0, col(style, 0.6), 0.25f * al, 2.5, 0);
            }
            case PILLAR -> {
                Vec3d base = follow != null ? follow.getLerpedPos(delta) : pos;
                double grow = easeOut(clamp01(age / Math.max(2, life * 0.25)));
                double h = Math.abs(vec.y) * grow;
                float al = (float) (smooth(age / 4.0) * smooth((life - age) / Math.max(3, life * 0.4)));
                cylinder(base, size, h, 0.35, col(style, 0.1), 0.85f * al, time * 0.05);
                cylinder(base, size * 0.38, h * 1.04, 0.5, col(style, 0.75), 0.95f * al, time * 0.09);
                glowQuadFlat(base.add(0, 0.08, 0), size * 2.6, style, 0.55f * al);
            }
            case FLASH -> {
                double pr = clamp01(age / life);
                double sc = size * (0.35 + easeOut(pr) * 1.1);
                float al = (float) Math.pow(1 - pr, 1.6);
                flare(pos, sc, style, al);
                billboard(GLOW, true, pos, sc * 1.4, col(style, 0.0), al * 0.7f);
            }
            case SWIRL -> {
                Vec3d c = pos.add(0, 0.09, 0);
                double fin = Math.min(8, life * 0.25);
                double sc = easeOutBack(age / (fin * 1.4));
                float al = (float) (smooth(age / fin) * smooth((life - age) / Math.max(4, life * 0.4)));
                orientedQuad(SWIRL, true, c, new Vec3d(0, 1, 0), size * sc, -age * 0.16, col(style, 0.0), 0.95f * al);
                orientedQuad(SWIRL, true, c.add(0, 0.02, 0), new Vec3d(0, 1, 0), size * sc * 0.6, age * 0.24, col(style, 0.6), 0.6f * al);
                glowQuadFlat(c, size * sc * 1.1, style, 0.4f * al);
            }
            case RAVENS -> ravens(pos, vec, (int) size, style, age, life, a.seed);
        }
    }

    // =====================================================================================
    //  composite effects
    // =====================================================================================
    private static Identifier sigilTexture(int style) {
        return switch (HexPath.byOrdinal(style)) {
            case WICCAN -> SIGIL_ARCANE;
            case AGATHA -> SIGIL_HEX;
            default -> SIGIL_CHAOS;
        };
    }

    /** Layered magic circle: glow + main sigil + two counter-rotating rune rings + hot core. */
    private static void sigilStack(Vec3d c, Vec3d n, double radius, int style, float alpha, double spin) {
        if (radius < 0.01 || alpha <= 0.002f) return;
        float[] base = col(style, 0.0);
        float[] hot = col(style, 0.6);
        double off = 0.012 * Math.max(1.0, radius * 0.12);
        orientedQuad(GLOW, true, c, n, radius * 1.3, 0, base, 0.45f * alpha);
        orientedQuad(sigilTexture(style), true, c.add(n.multiply(off)), n, radius, spin, base, 0.95f * alpha);
        orientedQuad(RUNES, true, c.add(n.multiply(off * 2)), n, radius * 1.03, -spin * 1.7, hot, 0.8f * alpha);
        orientedQuad(RUNES, true, c.add(n.multiply(off * 3)), n, radius * 0.58, spin * 2.6, hot, 0.5f * alpha);
        orientedQuad(GLOW, true, c.add(n.multiply(off * 4)), n, radius * 0.24, 0, hot, 0.8f * alpha);
    }

    private static void flare(Vec3d c, double size, int style, float alpha) {
        if (alpha <= 0.002f || size <= 0.01) return;
        billboard(FLARE, true, c, size, col(style, 0.55), alpha);
        billboardRot(FLARE, true, c, size * 0.7, Math.PI / 4, col(style, 0.0), alpha * 0.8f);
    }

    private static void glowQuadFlat(Vec3d c, double radius, int style, float alpha) {
        orientedQuad(GLOW, true, c, new Vec3d(0, 1, 0), radius, 0, col(style, 0.0), alpha);
    }

    private static void beam(Vec3d a, Vec3d b, double width, int style, double age, double life, long seed, double time) {
        Vec3d axis = b.subtract(a);
        double len = axis.length();
        if (len < 0.05) return;
        float fade = (float) (smooth(age / 1.2) * smooth((life - age) / Math.max(1.0, life * 0.65)));
        if (fade <= 0.002f) return;
        double w = width * (0.5 + 0.5 * fade);

        ribbon(a, b, w * 3.4, col(style, 0.0), 0.55f * fade, -age * 0.35, 2.5);
        ribbon(a, b, w * 1.25, col(style, 0.75), 1.0f * fade, -age * 0.6, 3.0);

        Vec3d ax = axis.multiply(1.0 / len);
        Vec3d p1 = ax.crossProduct(new Vec3d(0, 1, 0));
        if (p1.lengthSquared() < 1.0e-6) p1 = ax.crossProduct(new Vec3d(1, 0, 0));
        p1 = p1.normalize();
        Vec3d p2 = ax.crossProduct(p1).normalize();
        int n = Math.max(8, (int) (len * 1.6));
        n = Math.min(n, 48);
        HexPath path = HexPath.byOrdinal(style);
        double amp = Math.min(0.9, 0.22 + len * 0.02) * (0.6 + 0.4 * fade);
        switch (path) {
            case SCARLET -> {
                // jagged chaos lightning, re-rolled every ~2 ticks
                long frame = (long) (time / 2.0);
                for (int s = 0; s < 3; s++) {
                    Vec3d prev = a;
                    double uAcc = 0;
                    for (int i = 1; i <= n; i++) {
                        double t = i / (double) n;
                        double env = Math.pow(Math.sin(Math.PI * t), 0.8) * amp;
                        double ox = (hash(seed + s * 977L + i * 31L + frame * 7919L) - 0.5) * 2 * env;
                        double oy = (hash(seed + s * 613L + i * 57L + frame * 4241L + 99) - 0.5) * 2 * env;
                        Vec3d pt = a.add(axis.multiply(t)).add(p1.multiply(ox)).add(p2.multiply(oy));
                        ribbon(prev, pt, w * 0.55, col(style, 0.45), 0.9f * fade, uAcc, 1.6);
                        uAcc += prev.distanceTo(pt) / 1.6;
                        prev = pt;
                    }
                }
            }
            case WICCAN -> {
                // two clean counter-phase helices
                for (int s = 0; s < 2; s++) {
                    Vec3d prev = a;
                    double uAcc = 0;
                    for (int i = 1; i <= n; i++) {
                        double t = i / (double) n;
                        double env = Math.sin(Math.PI * t) * amp * 0.9;
                        double ph = t * len * 1.7 - time * 0.55 + s * Math.PI;
                        Vec3d pt = a.add(axis.multiply(t)).add(p1.multiply(Math.cos(ph) * env)).add(p2.multiply(Math.sin(ph) * env));
                        ribbon(prev, pt, w * 0.6, col(style, 0.5), 0.85f * fade, uAcc, 1.4);
                        uAcc += prev.distanceTo(pt) / 1.4;
                        prev = pt;
                    }
                }
            }
            default -> {
                // slow, smoky wisps
                for (int s = 0; s < 3; s++) {
                    Vec3d prev = a;
                    double uAcc = 0;
                    double phase = hash(seed + s * 101L) * 6.28;
                    for (int i = 1; i <= n; i++) {
                        double t = i / (double) n;
                        double env = Math.sin(Math.PI * t) * amp * 1.2;
                        double o1 = Math.sin(t * 5.0 + time * 0.12 + phase) * env;
                        double o2 = Math.cos(t * 4.0 - time * 0.10 + phase * 1.7) * env;
                        Vec3d pt = a.add(axis.multiply(t)).add(p1.multiply(o1)).add(p2.multiply(o2));
                        ribbon(prev, pt, w * 0.95, col(style, 0.15), 0.5f * fade, uAcc, 2.0);
                        uAcc += prev.distanceTo(pt) / 2.0;
                        prev = pt;
                    }
                }
            }
        }
        flare(a, width * 3.0, style, 0.8f * fade);
        flare(b, width * 4.5, style, fade);
    }

    private static void ravens(Vec3d origin, Vec3d vec, int count, int style, double age, double life, long seed) {
        double pr = age / life;
        Vec3d dir = vec.lengthSquared() < 1.0e-8 ? new Vec3d(0, 0, 1) : vec.normalize();
        Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0));
        side = side.lengthSquared() < 1.0e-6 ? new Vec3d(1, 0, 0) : side.normalize();
        Vec3d up = side.crossProduct(dir).normalize();
        double range = vec.length();
        for (int i = 0; i < count; i++) {
            double h1 = hash(seed + i * 131L), h2 = hash(seed + i * 257L + 5), h3 = hash(seed + i * 389L + 9), h4 = hash(seed + i * 521L + 13);
            double delay = h1 * 0.22;
            double t = clamp01((pr - delay) / (1.0 - delay));
            if (t <= 0) continue;
            double spreadAng = (h2 - 0.5) * 0.9;
            double spreadUp = (h3 - 0.5) * 0.7;
            double dist = range * easeOut(t) * (0.7 + 0.4 * h4);
            Vec3d pt = origin.add(dir.multiply(dist))
                    .add(side.multiply(Math.tan(spreadAng) * dist * 0.55 + Math.sin(age * 0.5 + i) * 0.15))
                    .add(up.multiply(Math.tan(spreadUp) * dist * 0.4 + Math.cos(age * 0.45 + i * 2) * 0.12));
            float al = (float) (smooth(t / 0.12) * smooth((1 - t) / 0.3));
            int frame = RAVEN_SEQ[(int) ((age * 1.8 + i * 2) % RAVEN_SEQ.length + RAVEN_SEQ.length) % RAVEN_SEQ.length];
            double sz = 0.75 + 0.55 * h4;
            billboardSheet(RAVEN, pt, sz, frame, RAVEN_FRAMES, al);
        }
    }

    // =====================================================================================
    //  primitives
    // =====================================================================================
    private static void use(Identifier t, boolean additive) {
        if (bb != null && (t != curTex || additive != curAdd)) flush();
        if (bb == null) {
            bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            curTex = t;
            curAdd = additive;
        }
    }

    private static void flush() {
        if (bb == null) return;
        BufferBuilder b = bb;
        bb = null;
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, curTex);
        if (curAdd) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private static void vtx(Vec3d p, float u, float v, float[] c, float a) {
        bb.vertex(M, (float) (p.x - CX), (float) (p.y - CY), (float) (p.z - CZ)).texture(u, v).color(c[0], c[1], c[2], Math.max(0f, Math.min(1f, a)));
    }

    private static void quadUV(Vec3d c, Vec3d right, Vec3d up, double hw, double hh,
                               float u0, float v0, float u1, float v1, float[] col, float a) {
        Vec3d rx = right.multiply(hw), uy = up.multiply(hh);
        vtx(c.subtract(rx).subtract(uy), u0, v1, col, a);
        vtx(c.add(rx).subtract(uy), u1, v1, col, a);
        vtx(c.add(rx).add(uy), u1, v0, col, a);
        vtx(c.subtract(rx).add(uy), u0, v0, col, a);
    }

    /** Textured square of half-size `radius`, lying in the plane perpendicular to n, spun by `angle`. */
    private static void orientedQuad(Identifier t, boolean add, Vec3d c, Vec3d n, double radius, double angle, float[] col, float a) {
        if (a <= 0.002f || radius <= 0.001) return;
        n = n.normalize();
        Vec3d ref = Math.abs(n.y) > 0.99 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d right = ref.crossProduct(n).normalize();
        Vec3d up = n.crossProduct(right);
        double ca = Math.cos(angle), sa = Math.sin(angle);
        Vec3d r2 = right.multiply(ca).add(up.multiply(sa));
        Vec3d u2 = right.multiply(-sa).add(up.multiply(ca));
        use(t, add);
        quadUV(c, r2, u2, radius, radius, 0, 0, 1, 1, col, a);
    }

    private static void billboard(Identifier t, boolean add, Vec3d c, double size, float[] col, float a) {
        if (a <= 0.002f || size <= 0.001) return;
        use(t, add);
        quadUV(c, camRight, camUp, size, size, 0, 0, 1, 1, col, a);
    }

    private static void billboardRot(Identifier t, boolean add, Vec3d c, double size, double ang, float[] col, float a) {
        if (a <= 0.002f || size <= 0.001) return;
        double ca = Math.cos(ang), sa = Math.sin(ang);
        Vec3d r2 = camRight.multiply(ca).add(camUp.multiply(sa));
        Vec3d u2 = camRight.multiply(-sa).add(camUp.multiply(ca));
        use(t, add);
        quadUV(c, r2, u2, size, size, 0, 0, 1, 1, col, a);
    }

    private static void billboardSheet(Identifier t, Vec3d c, double size, int frame, int frames, float a) {
        if (a <= 0.002f) return;
        use(t, false);
        float u0 = frame / (float) frames, u1 = (frame + 1) / (float) frames;
        quadUV(c, camRight, camUp, size, size, u0, 0, u1, 1, new float[]{1f, 1f, 1f}, a);
    }

    /** Camera-facing ribbon from a to b; u runs along the length (tile = world length of one texture repeat). */
    private static void ribbon(Vec3d a, Vec3d b, double width, float[] col, float al, double uStart, double tile) {
        if (al <= 0.002f || width <= 0.001) return;
        Vec3d axis = b.subtract(a);
        double len = axis.length();
        if (len < 1.0e-4) return;
        Vec3d mid = a.add(axis.multiply(0.5));
        Vec3d side = axis.crossProduct(camPos.subtract(mid));
        if (side.lengthSquared() < 1.0e-10) return;
        side = side.normalize().multiply(width * 0.5);
        float u0 = (float) uStart, u1 = (float) (uStart + len / tile);
        use(BEAM, true);
        vtx(a.subtract(side), u0, 0f, col, al);
        vtx(a.add(side), u0, 1f, col, al);
        vtx(b.add(side), u1, 1f, col, al);
        vtx(b.subtract(side), u1, 0f, col, al);
    }

    /** Bubble with a honeycomb texture; brighter towards the silhouette (fresnel-like). */
    private static void sphere(Vec3d c, double r, double unused, float[] col, float baseAlpha, double fresnel, double scroll) {
        if (baseAlpha <= 0.002f || r <= 0.01) return;
        final int LAT = 14, LON = 28;
        double tileW = 3 * 0.34;
        int tilesU = Math.max(4, (int) Math.round(2 * Math.PI * r / tileW));
        double tilesV = Math.PI * r / (tileW * 0.577);
        use(HEX_GRID, true);
        for (int i = 0; i < LAT; i++) {
            double th0 = Math.PI * i / LAT, th1 = Math.PI * (i + 1) / LAT;
            for (int j = 0; j < LON; j++) {
                double ph0 = 2 * Math.PI * j / LON, ph1 = 2 * Math.PI * (j + 1) / LON;
                sv(c, r, th0, ph0, (float) (j / (double) LON * tilesU + scroll), (float) (i / (double) LAT * tilesV), col, baseAlpha, fresnel);
                sv(c, r, th0, ph1, (float) ((j + 1) / (double) LON * tilesU + scroll), (float) (i / (double) LAT * tilesV), col, baseAlpha, fresnel);
                sv(c, r, th1, ph1, (float) ((j + 1) / (double) LON * tilesU + scroll), (float) ((i + 1) / (double) LAT * tilesV), col, baseAlpha, fresnel);
                sv(c, r, th1, ph0, (float) (j / (double) LON * tilesU + scroll), (float) ((i + 1) / (double) LAT * tilesV), col, baseAlpha, fresnel);
            }
        }
    }

    private static void sv(Vec3d c, double r, double th, double ph, float u, float v, float[] col, float baseAlpha, double fresnel) {
        double nx = Math.sin(th) * Math.cos(ph), ny = Math.cos(th), nz = Math.sin(th) * Math.sin(ph);
        Vec3d p = c.add(nx * r, ny * r, nz * r);
        Vec3d view = camPos.subtract(p);
        double vl = view.length();
        double d = vl < 1.0e-6 ? 0 : Math.abs((nx * view.x + ny * view.y + nz * view.z) / vl);
        double fr = 0.2 + 0.8 * Math.pow(1.0 - Math.min(1.0, d), fresnel);
        vtx(p, u, v, col, (float) (baseAlpha * fr));
    }

    /** Open-ended energy column, brightest at the bottom and fading upwards. */
    private static void cylinder(Vec3d base, double r, double h, double tilesVPerBlock, float[] col, float alpha, double scrollV) {
        if (alpha <= 0.002f || r <= 0.01 || h <= 0.05) return;
        final int SEG = 24, SLICES = 10;
        double tilesU = Math.max(1, Math.round(2 * Math.PI * r / 1.6));
        double tilesV = Math.max(0.6, h * tilesVPerBlock);
        use(PILLAR, true);
        for (int s = 0; s < SLICES; s++) {
            double t0 = s / (double) SLICES, t1 = (s + 1) / (double) SLICES;
            float a0 = alpha * (float) (Math.pow(1 - t0, 0.7) * smooth(t0 / 0.06));
            float a1 = alpha * (float) (Math.pow(1 - t1, 0.7) * smooth(t1 / 0.06));
            for (int j = 0; j < SEG; j++) {
                double g0 = 2 * Math.PI * j / SEG, g1 = 2 * Math.PI * (j + 1) / SEG;
                float u0 = (float) (j / (double) SEG * tilesU), u1 = (float) ((j + 1) / (double) SEG * tilesU);
                float v0 = (float) (t0 * tilesV - scrollV), v1 = (float) (t1 * tilesV - scrollV);
                vtx(base.add(Math.cos(g0) * r, t0 * h, Math.sin(g0) * r), u0, v0, col, a0);
                vtx(base.add(Math.cos(g1) * r, t0 * h, Math.sin(g1) * r), u1, v0, col, a0);
                vtx(base.add(Math.cos(g1) * r, t1 * h, Math.sin(g1) * r), u1, v1, col, a1);
                vtx(base.add(Math.cos(g0) * r, t1 * h, Math.sin(g0) * r), u0, v1, col, a1);
            }
        }
    }

    // =====================================================================================
    //  helpers
    // =====================================================================================
    private static float[] col(int style, double whiteMix) {
        int rgb = HexPath.byOrdinal(style).color;
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float m = (float) whiteMix;
        return new float[]{r + (1 - r) * m, g + (1 - g) * m, b + (1 - b) * m};
    }

    private static double clamp01(double x) {
        return x < 0 ? 0 : Math.min(x, 1);
    }

    private static double smooth(double x) {
        x = clamp01(x);
        return x * x * (3 - 2 * x);
    }

    private static double easeOut(double x) {
        x = clamp01(x);
        return 1 - (1 - x) * (1 - x) * (1 - x);
    }

    private static double easeOutBack(double x) {
        x = clamp01(x);
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    /** Cheap deterministic hash -> [0,1). */
    private static double hash(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return (x & 0xFFFFFFL) / (double) 0x1000000;
    }
}
