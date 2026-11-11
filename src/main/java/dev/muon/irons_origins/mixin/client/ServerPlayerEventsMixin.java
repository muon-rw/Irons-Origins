package dev.muon.irons_origins.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/**
 * Client half of {@link dev.muon.irons_origins.mixin.ServerPlayerEventsMixin}: Iron's also cancels the right-click on
 * the client, from a lambda it passes to {@code MinecraftInstanceHelper.ifPlayerPresent}.
 */
@Mixin(value = ServerPlayerEvents.class, remap = false)
public class ServerPlayerEventsMixin {

    @WrapOperation(
            method = "onUseItem",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/util/MinecraftInstanceHelper;ifPlayerPresent(Ljava/util/function/Consumer;)V")
    )
    private static void irons_origins$skipClientCancelDuringCommandCast(Consumer<?> consumer, Operation<Void> original) {
        if (ClientMagicData.isCasting()
                && ClientMagicDataAccessor.irons_origins$getPlayerMagicData().getCastSource() == CastSource.COMMAND) {
            return;
        }
        original.call(consumer);
    }
}
