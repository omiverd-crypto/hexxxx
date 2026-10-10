package net.hexpowers;

import net.fabricmc.api.ModInitializer;
import net.hexpowers.net.CastPayload;
import net.hexpowers.net.FxPayload;
import net.hexpowers.net.SyncPayload;
import net.hexpowers.power.PowerManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HexPowers implements ModInitializer {
    public static final String MOD_ID = "hexpowers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(CastPayload.ID, CastPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FxPayload.ID, FxPayload.CODEC);
        ModItems.register();
        PowerManager.init();
        HexCommands.register();
        LOGGER.info("Hex Powers loaded: Scarlet Witch, Wiccan and Agatha Harkness are ready.");
    }
}
