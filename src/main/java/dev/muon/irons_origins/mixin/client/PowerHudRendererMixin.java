package dev.muon.irons_origins.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.irons_origins.power.ActionOnSpellDamagePower;
import dev.overgrown.apoli.client.PowerHudRenderer;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Apoli resolves hud_render through a closed instanceof chain over its own power configs, so we add ours here or
// their bars never draw.
@Mixin(value = PowerHudRenderer.class, remap = false)
public class PowerHudRendererMixin {

    @ModifyReturnValue(method = "hudRenderOf", at = @At("RETURN"))
    private static @Nullable HudRender irons_origins$addOurHudRenders(@Nullable HudRender original, PowerType<?> type, Object config) {
        if (original == null && config instanceof ActionOnSpellDamagePower.Configuration cfg) {
            return cfg.hudRender();
        }
        return original;
    }
}
