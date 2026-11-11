package dev.muon.irons_origins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Teleport casts its cast data to {@link TeleportSpell.TeleportData} unchecked. Casts that skip Teleport's own precast
 * (power casts, force-completes) can leave other cast data behind, so we hand Teleport {@code null} instead, which it
 * treats as "compute the destination now".
 */
@Mixin(value = TeleportSpell.class, remap = false)
public class TeleportSpellMixin {

    @WrapOperation(method = "onCast", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;getAdditionalCastData()Lio/redspace/ironsspellbooks/api/spells/ICastData;"))
    private ICastData irons_origins$ignoreForeignCastData(MagicData magicData, Operation<ICastData> original) {
        ICastData data = original.call(magicData);
        return data instanceof TeleportSpell.TeleportData ? data : null;
    }
}
