package net.hexpowers.client;

import net.hexpowers.core.Ability;
import net.hexpowers.core.HexPath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.List;

/** Right-hand panel: path name, energy bar, five abilities with cost + cooldown, key hints. */
public final class PowerHud {
    private PowerHud() {}

    private static final int PANEL_W = 150;
    private static final int ROW_H = 13;

    private static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int rgb, int other, float t) {
        int r = (int) (((rgb >> 16) & 255) * (1 - t) + ((other >> 16) & 255) * t);
        int g = (int) (((rgb >> 8) & 255) * (1 - t) + ((other >> 8) & 255) * t);
        int b = (int) ((rgb & 255) * (1 - t) + (other & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HexPath path = ClientState.path;
        if (mc.player == null || mc.options.hudHidden || !path.active()) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        long ms = System.currentTimeMillis();

        drawScreenGlow(ctx, sw, sh, path, ms);

        List<Ability> abilities = path.abilities();
        int panelH = 16 + 14 + abilities.size() * ROW_H + 18;
        int x = sw - PANEL_W - 6;
        int y = sh - panelH - 6;
        // keep clear of the centered hotbar on narrow windows
        if (x < sw / 2 + 96) y -= 26;

        int color = path.color;
        // panel background + accent border
        ctx.fill(x, y, x + PANEL_W, y + panelH, argb(0xB0, 0x0A0A12));
        ctx.fillGradient(x, y, x + PANEL_W, y + 2, argb(0xFF, color), argb(0x20, color));
        ctx.fill(x, y, x + 2, y + panelH, argb(0xFF, color));
        ctx.drawBorder(x, y, PANEL_W, panelH, argb(0x60, color));

        // title
        Text title = Text.translatable(path.translationKey());
        ctx.drawTextWithShadow(mc.textRenderer, title, x + 7, y + 5, argb(0xFF, color));
        if (ClientState.flying) {
            Text fly = Text.translatable("hud.hexpowers.flying");
            ctx.drawTextWithShadow(mc.textRenderer, fly, x + PANEL_W - 6 - mc.textRenderer.getWidth(fly), y + 5, 0xFFFFFFFF);
        }

        // energy bar
        float target = ClientState.energy;
        ClientState.displayEnergy += (target - ClientState.displayEnergy) * 0.18f;
        float frac = Math.max(0f, Math.min(1f, ClientState.displayEnergy / path.maxEnergy));
        int bx = x + 7, by = y + 18, bw = PANEL_W - 14, bh = 8;
        ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, argb(0xFF, 0x000000));
        ctx.fill(bx, by, bx + bw, by + bh, argb(0xFF, mix(color, 0x000000, 0.80f)));
        int fillW = Math.round(bw * frac);
        if (fillW > 0) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(ms / 220.0);
            int top = mix(color, 0xFFFFFF, 0.25f + (frac >= 0.99f ? 0.15f * pulse : 0f));
            ctx.fillGradient(bx, by, bx + fillW, by + bh, argb(0xFF, top), argb(0xFF, mix(color, 0x000000, 0.35f)));
            ctx.fill(bx, by, bx + fillW, by + 1, argb(0x80, 0xFFFFFF));
        }
        for (int i = 1; i < 10; i++) {
            int tx = bx + bw * i / 10;
            ctx.fill(tx, by + bh - 2, tx + 1, by + bh, argb(0x70, 0x000000));
        }
        String eText = Math.round(ClientState.energy) + "/" + Math.round(path.maxEnergy);
        Text eName = Text.translatable(path.energyKey());
        ctx.drawTextWithShadow(mc.textRenderer, eName, bx, by + bh + 3, argb(0xFF, mix(color, 0xFFFFFF, 0.35f)));
        ctx.drawTextWithShadow(mc.textRenderer, eText, bx + bw - mc.textRenderer.getWidth(eText), by + bh + 3, 0xFFDDDDDD);

        // ability rows
        int ry = by + bh + 16;
        for (int i = 0; i < abilities.size(); i++) {
            Ability a = abilities.get(i);
            boolean sel = i == ClientState.selected;
            float cd = ClientState.cooldownTicks(a);
            boolean affordable = ClientState.energy >= a.cost;
            int rx = x + 5, rw = PANEL_W - 10;

            if (sel) {
                ctx.fillGradient(rx, ry, rx + rw, ry + ROW_H - 1, argb(0x70, color), argb(0x20, color));
                ctx.fill(rx, ry, rx + 2, ry + ROW_H - 1, argb(0xFF, color));
            } else {
                ctx.fill(rx, ry, rx + rw, ry + ROW_H - 1, argb(0x30, 0x000000));
            }
            if (cd > 0f) {
                float cf = Math.min(1f, cd / a.cooldown);
                ctx.fill(rx, ry, rx + Math.round(rw * cf), ry + ROW_H - 1, argb(0x90, 0x000000));
            }
            int textColor = cd > 0f ? 0xFF8A8A92 : (affordable ? 0xFFFFFFFF : 0xFFE06060);
            Text name = Text.translatable(a.translationKey());
            ctx.drawTextWithShadow(mc.textRenderer, name, rx + 5, ry + 2, textColor);

            String right = cd > 0f ? String.format("%.1fs", cd / 20f) : String.valueOf((int) a.cost);
            int rightColor = cd > 0f ? 0xFFFFD27A : argb(0xFF, mix(color, 0xFFFFFF, 0.3f));
            ctx.drawTextWithShadow(mc.textRenderer, right, rx + rw - 4 - mc.textRenderer.getWidth(right), ry + 2, rightColor);
            ry += ROW_H;
        }

        // key hints
        String hint = keyName(HexPowersClient.castKey()) + " " + Text.translatable("hud.hexpowers.hint_cast").getString()
                + "  " + keyName(HexPowersClient.prevKey()) + "/" + keyName(HexPowersClient.nextKey())
                + "  " + keyName(HexPowersClient.flightKey()) + " " + Text.translatable("hud.hexpowers.hint_fly").getString();
        ctx.drawTextWithShadow(mc.textRenderer, hint, x + 7, y + panelH - 11, 0xFF8A8A92);
    }

    private static String keyName(net.minecraft.client.option.KeyBinding k) {
        return k == null ? "?" : k.getBoundKeyLocalizedText().getString();
    }

    /** Soft coloured glow along the top/bottom edges while a shield or pact is active. */
    private static void drawScreenGlow(DrawContext ctx, int sw, int sh, HexPath path, long ms) {
        int glow = 0;
        int rgb = 0;
        if (ClientState.shieldTicks > 0) {
            glow = 1;
            rgb = HexPath.SCARLET.color;
        } else if (ClientState.pactTicks > 0) {
            glow = 1;
            rgb = HexPath.AGATHA.color;
        }
        if (glow == 0) return;
        float pulse = 0.5f + 0.5f * (float) Math.sin(ms / 300.0);
        int a = 40 + (int) (30 * pulse);
        int h = 28;
        ctx.fillGradient(0, 0, sw, h, argb(a, rgb), argb(0, rgb));
        ctx.fillGradient(0, sh - h, sw, sh, argb(0, rgb), argb(a, rgb));
    }
}
