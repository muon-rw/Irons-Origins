package dev.muon.irons_origins.network;

import dev.muon.irons_origins.IronsOrigins;
import dev.muon.irons_origins.client.CastAnimationSuppression;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Replaces Iron's {@code OnCastFinishedPacket} for casts with {@code player_animations: false}.
 */
public record CastFinishedWithoutAnimationPacket(UUID casterId, String spellId, boolean cancelled) implements CustomPacketPayload {

    public static final Type<CastFinishedWithoutAnimationPacket> TYPE = new Type<>(IronsOrigins.loc("cast_finished_without_animation"));
    public static final StreamCodec<ByteBuf, CastFinishedWithoutAnimationPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CastFinishedWithoutAnimationPacket::casterId,
            ByteBufCodecs.STRING_UTF8, CastFinishedWithoutAnimationPacket::spellId,
            ByteBufCodecs.BOOL, CastFinishedWithoutAnimationPacket::cancelled,
            CastFinishedWithoutAnimationPacket::new);

    public static void handle(CastFinishedWithoutAnimationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> CastAnimationSuppression.run(() ->
                ClientSpellCastHelper.handleClientBoundOnCastFinished(packet.casterId, packet.spellId, packet.cancelled)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
