package dev.muon.irons_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.irons_origins.power.LearnedSpellsPower;
import dev.muon.irons_origins.spell.SpellCasting;
import dev.muon.irons_origins.spell.SyncedSpellDataOwner;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SyncedSpellData.class, remap = false)
public class SyncedSpellDataMixin {

    @ModifyReturnValue(method = "isSpellLearned", at = @At("RETURN"))
    private boolean irons_origins$treatGrantedSpellsAsLearned(boolean learned, AbstractSpell spell) {
        if (learned || SpellCasting.isIgnoringLearning()) {
            return true;
        }
        Player owner = SyncedSpellDataOwner.resolve((SyncedSpellData) (Object) this);
        return owner != null && LearnedSpellsPower.grants(owner, spell);
    }
}
