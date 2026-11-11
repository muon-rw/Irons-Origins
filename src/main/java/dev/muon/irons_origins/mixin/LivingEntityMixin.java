package dev.muon.irons_origins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.irons_origins.power.ActionOnSpellDamagePower;
import dev.muon.irons_origins.spell.OutgoingHealContext;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float irons_origins$applyCasterModifyHealing(float healAmount) {
        return OutgoingHealContext.consumeFor((LivingEntity) (Object) this, healAmount);
    }

    // After setHealth rather than in LivingDamageEvent.Post, so the action sees the target's new health even when
    // another mod changes the final damage between the two.
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void irons_origins$runActionOnSpellDamage(LivingEntity instance, float health, Operation<Void> original,
                                                     @Local(argsOnly = true) DamageSource damageSource) {
        original.call(instance, health);
        if (damageSource instanceof SpellDamageSource spellDamage) {
            ActionOnSpellDamagePower.afterSpellDamageApplied((LivingEntity) (Object) this, spellDamage);
        }
    }
}
