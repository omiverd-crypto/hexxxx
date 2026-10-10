package net.hexpowers.net;

import net.hexpowers.HexPowers;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client -> server: a key was pressed. */
public record CastPayload(int action) implements CustomPayload {
    public static final int CAST = 0;
    public static final int NEXT = 1;
    public static final int PREV = 2;
    public static final int FLIGHT = 3;

    public static final CustomPayload.Id<CastPayload> ID = new CustomPayload.Id<>(HexPowers.id("cast"));
    public static final PacketCodec<RegistryByteBuf, CastPayload> CODEC = new PacketCodec<>() {
        @Override
        public CastPayload decode(RegistryByteBuf buf) {
            return new CastPayload(buf.readVarInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, CastPayload value) {
            buf.writeVarInt(value.action());
        }
    };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
