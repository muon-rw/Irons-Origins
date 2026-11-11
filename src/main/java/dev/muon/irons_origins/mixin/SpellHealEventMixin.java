package dev.muon.irons_origins.mixin;

import dev.muon.irons_origins.spell.OutgoingHealContext;
import dev.overgrown.apoli.power.builtin.ModifyHealingHandler;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellHealEvent.class, remap = false)
public class SpellHealEventMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void irons_origins$applyCasterModifyHealing(LivingEntity castingEntity, LivingEntity targetEntity, float healAmount,
                                                        SchoolType schoolType, CallbackInfo ci) {
        // Apoli already applies modify_healing when the caster heals themselves.
        if (!castingEntity.level().isClientSide() && castingEntity != targetEntity) {
            OutgoingHealContext.pushOutgoingHeal(targetEntity, ModifyHealingHandler.modify(castingEntity, healAmount));
        }
    }
}
