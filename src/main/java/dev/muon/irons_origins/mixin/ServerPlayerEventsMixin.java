package dev.muon.irons_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Iron's blocks item use while casting. Power-driven casts shouldn't take the player's hands, so we let item use
 * through during {@link CastSource#COMMAND} casts.
 */
@Mixin(value = ServerPlayerEvents.class, remap = false)
public class ServerPlayerEventsMixin {

    @ModifyExpressionValue(
            method = "onUseItem",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;isCasting()Z")
    )
    private static boolean irons_origins$allowItemUseDuringCommandCast(boolean isCasting, PlayerInteractEvent.RightClickItem event) {
        return isCasting && MagicData.getPlayerMagicData(event.getEntity()).getCastSource() != CastSource.COMMAND;
    }
}
