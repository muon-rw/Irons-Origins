package dev.muon.irons_origins.mixin;

import dev.muon.irons_origins.spell.CastFinishCallbacks;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerRecasts.class, remap = false)
public class PlayerRecastsMixin {

    @Shadow
    @Final
    private ServerPlayer serverPlayer;

    @Inject(method = "triggerRecastComplete", at = @At("TAIL"))
    private void irons_origins$runFinishActionsAfterRecasts(RecastInstance recastInstance, RecastResult recastResult, CallbackInfo ci) {
        if (serverPlayer != null) {
            CastFinishCallbacks.onRecastSequenceEnd(serverPlayer, recastInstance.getSpellId());
        }
    }
}
