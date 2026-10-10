package net.hexpowers.net;

import net.hexpowers.HexPowers;
import net.hexpowers.core.Ability;
import net.hexpowers.core.HexPath;
import net.hexpowers.core.PlayerState;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

/** Server -> client: everything the HUD needs. */
public record SyncPayload(int path, float energy, int selected, boolean flying,
                          int shieldTicks, int pactTicks, int[] cooldowns) implements CustomPayload {
    public static final int MAX_ABILITIES = 5;

    public static final CustomPayload.Id<SyncPayload> ID = new CustomPayload.Id<>(HexPowers.id("sync"));
    public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC = new PacketCodec<>() {
        @Override
        public SyncPayload decode(RegistryByteBuf buf) {
            int path = buf.readVarInt();
            float energy = buf.readFloat();
            int selected = buf.readVarInt();
            boolean flying = buf.readBoolean();
            int shield = buf.readVarInt();
            int pact = buf.readVarInt();
            int[] cd = new int[MAX_ABILITIES];
            for (int i = 0; i < MAX_ABILITIES; i++) cd[i] = buf.readVarInt();
            return new SyncPayload(path, energy, selected, flying, shield, pact, cd);
        }

        @Override
        public void encode(RegistryByteBuf buf, SyncPayload v) {
            buf.writeVarInt(v.path());
            buf.writeFloat(v.energy());
            buf.writeVarInt(v.selected());
            buf.writeBoolean(v.flying());
            buf.writeVarInt(v.shieldTicks());
            buf.writeVarInt(v.pactTicks());
            for (int i = 0; i < MAX_ABILITIES; i++) buf.writeVarInt(i < v.cooldowns().length ? v.cooldowns()[i] : 0);
        }
    };

    public static SyncPayload of(PlayerState s) {
        int[] cd = new int[MAX_ABILITIES];
        List<Ability> list = s.path.abilities();
        for (int i = 0; i < list.size() && i < MAX_ABILITIES; i++) cd[i] = s.cooldownLeft(list.get(i));
        int sel = Math.max(0, list.isEmpty() ? 0 : Math.floorMod(s.selected, list.size()));
        return new SyncPayload(s.path.ordinal(), s.energy, sel, s.flying, s.shieldTicks, s.pactTicks, cd);
    }

    public HexPath hexPath() {
        return HexPath.byOrdinal(path);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
