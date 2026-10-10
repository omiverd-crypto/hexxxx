package net.hexpowers.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.hexpowers.client.fx.FxManager;
import net.hexpowers.client.fx.FxRenderer;
import net.hexpowers.net.CastPayload;
import net.hexpowers.net.FxPayload;
import net.hexpowers.net.SyncPayload;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class HexPowersClient implements ClientModInitializer {
    private static final String CATEGORY = "key.categories.hexpowers";
    private static KeyBinding castKey;
    private static KeyBinding nextKey;
    private static KeyBinding prevKey;
    private static KeyBinding flightKey;

    @Override
    public void onInitializeClient() {
        castKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hexpowers.cast", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
        nextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hexpowers.next", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
        prevKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hexpowers.prev", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
        flightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hexpowers.flight", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (castKey.wasPressed()) ClientPlayNetworking.send(new CastPayload(CastPayload.CAST));
            while (nextKey.wasPressed()) ClientPlayNetworking.send(new CastPayload(CastPayload.NEXT));
            while (prevKey.wasPressed()) ClientPlayNetworking.send(new CastPayload(CastPayload.PREV));
            while (flightKey.wasPressed()) ClientPlayNetworking.send(new CastPayload(CastPayload.FLIGHT));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> ClientState.apply(payload));
        ClientPlayNetworking.registerGlobalReceiver(FxPayload.ID, (payload, context) -> FxManager.add(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientState.reset();
            FxManager.clear();
        });
        FxRenderer.init();
        HudRenderCallback.EVENT.register(PowerHud::render);
    }

    public static KeyBinding castKey() { return castKey; }
    public static KeyBinding nextKey() { return nextKey; }
    public static KeyBinding prevKey() { return prevKey; }
    public static KeyBinding flightKey() { return flightKey; }
}
