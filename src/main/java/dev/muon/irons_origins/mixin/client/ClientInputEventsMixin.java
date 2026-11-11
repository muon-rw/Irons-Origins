package dev.muon.irons_origins.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Iron's blocks attack and use input while casting. Power-driven casts shouldn't take the player's hands, so we let
 * input through during {@link CastSource#COMMAND} casts.
 */
@Mixin(value = ClientInputEvents.class, remap = false)
public class ClientInputEventsMixin {

    @ModifyExpressionValue(
            method = "onUseInput",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;isCasting()Z")
    )
    private static boolean irons_origins$allowInputDuringCommandCast(boolean isCasting) {
        return isCasting && ClientMagicDataAccessor.irons_origins$getPlayerMagicData().getCastSource() != CastSource.COMMAND;
    }
}
