package dev.muon.irons_origins.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.muon.irons_origins.network.CastFinishedWithoutAnimationPacket;
import dev.muon.irons_origins.power.ActionOnSpellCastPower;
import dev.muon.irons_origins.power.RecastSpellPower;
import dev.muon.irons_origins.spell.CastFinishCallbacks;
import dev.muon.irons_origins.spell.RecentSpellCastCache;
import dev.muon.irons_origins.spell.SpellCastOptions;
import dev.muon.irons_origins.spell.SpellCasting;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {

    @WrapMethod(method = "castSpell")
    private void irons_origins$markCasterResolving(Level level, int spellLevel, ServerPlayer serverPlayer,
                                                   CastSource castSource, boolean triggerCooldown, Operation<Void> original) {
        SpellCasting.resolve(serverPlayer, () -> original.call(level, spellLevel, serverPlayer, castSource, triggerCooldown));
    }

    @Inject(method = "onServerCastComplete", at = @At("HEAD"))
    private void irons_origins$untrackOurCast(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData,
                                              boolean cancelled, CallbackInfo ci,
                                              @Share("ourCast") LocalRef<SpellCastOptions> ourCast) {
        if (entity instanceof ServerPlayer player) {
            ourCast.set(SpellCasting.onCastEnd(player, (AbstractSpell) (Object) this));
        }
    }

    @ModifyArg(method = "onServerCastComplete",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/PacketDistributor;sendToPlayersTrackingEntityAndSelf(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"),
            index = 1)
    private CustomPacketPayload irons_origins$skipFinishAnimation(CustomPacketPayload onCastFinished,
                                                                 @Local(argsOnly = true) LivingEntity entity,
                                                                 @Local(argsOnly = true) boolean cancelled,
                                                                 @Share("ourCast") LocalRef<SpellCastOptions> ourCast) {
        SpellCastOptions options = ourCast.get();
        if (options == null || options.playerAnimations()) {
            return onCastFinished;
        }
        return new CastFinishedWithoutAnimationPacket(entity.getUUID(), ((AbstractSpell) (Object) this).getSpellId(), cancelled);
    }

    // Iron's resets the cast source and type during completion, so we read them first.
    @WrapMethod(method = "onServerCastComplete")
    private void irons_origins$runCastCompleteHooks(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData,
                                                    boolean cancelled, Operation<Void> original) {
        CastSource castSource = playerMagicData.getCastSource();
        CastType castType = playerMagicData.getCastType();
        original.call(level, spellLevel, entity, playerMagicData, cancelled);
        if (level.isClientSide()) {
            return;
        }
        AbstractSpell self = (AbstractSpell) (Object) this;
        RecentSpellCastCache.recordCompletedCast(level, entity, self, spellLevel, cancelled);
        if (entity instanceof ServerPlayer player) {
            CastFinishCallbacks.onCastComplete(player, self);
        }
        ActionOnSpellCastPower.onCastComplete(self, entity, castSource, castType);
        if (entity instanceof ServerPlayer player) {
            RecastSpellPower.onCastComplete(self, player, spellLevel, castSource, castType);
        }
    }
}
