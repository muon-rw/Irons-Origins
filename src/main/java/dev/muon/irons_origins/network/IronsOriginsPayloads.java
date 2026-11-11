package dev.muon.irons_origins.network;

import dev.muon.irons_origins.IronsOrigins;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class IronsOriginsPayloads {

    private IronsOriginsPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(IronsOrigins.MODID).versioned("1");
        registrar.playToClient(CastStartedWithoutAnimationPacket.TYPE, CastStartedWithoutAnimationPacket.STREAM_CODEC,
                CastStartedWithoutAnimationPacket::handle);
        registrar.playToClient(CastFinishedWithoutAnimationPacket.TYPE, CastFinishedWithoutAnimationPacket.STREAM_CODEC,
                CastFinishedWithoutAnimationPacket::handle);
        registrar.playToClient(CastWithoutSlowdownPacket.TYPE, CastWithoutSlowdownPacket.STREAM_CODEC,
                CastWithoutSlowdownPacket::handle);
    }
}
