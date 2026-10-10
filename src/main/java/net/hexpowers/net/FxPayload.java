package net.hexpowers.net;

import net.hexpowers.HexPowers;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: draw one magic visual. */
public record FxPayload(int type, int style, double x, double y, double z,
                        float vx, float vy, float vz,
                        int life, int delay, float size, int attach) implements CustomPayload {

    public static final CustomPayload.Id<FxPayload> ID = new CustomPayload.Id<>(HexPowers.id("fx"));
    public static final PacketCodec<RegistryByteBuf, FxPayload> CODEC = new PacketCodec<>() {
        @Override
        public FxPayload decode(RegistryByteBuf b) {
            return new FxPayload(b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readInt());
        }

        @Override
        public void encode(RegistryByteBuf b, FxPayload v) {
            b.writeVarInt(v.type());
            b.writeVarInt(v.style());
            b.writeDouble(v.x());
            b.writeDouble(v.y());
            b.writeDouble(v.z());
            b.writeFloat(v.vx());
            b.writeFloat(v.vy());
            b.writeFloat(v.vz());
            b.writeVarInt(v.life());
            b.writeVarInt(v.delay());
            b.writeFloat(v.size());
            b.writeInt(v.attach());
        }
    };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
