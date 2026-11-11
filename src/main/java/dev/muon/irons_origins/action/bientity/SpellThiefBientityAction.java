package dev.muon.irons_origins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.PlayerMagic;
import dev.muon.irons_origins.spell.RecentSpellCastCache;
import dev.muon.irons_origins.spell.SpellCastInterruptMode;
import dev.muon.irons_origins.spell.SpellCastOptions;
import dev.muon.irons_origins.spell.SpellCasting;
import dev.muon.irons_origins.spell.SpellTarget;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

/**
 * The actor steals a spell from the target and casts it back at them. If the target is casting, we cancel that cast
 * and steal it; otherwise we steal their most recently completed cast. Iron's {@code CounterSpellEvent} can veto the
 * theft.
 */
public final class SpellThiefBientityAction implements ActionType<BiEntityCtx, SpellThiefBientityAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("trigger_target_cooldown", true).forGetter(Configuration::triggerTargetCooldown),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_mana_cost").forGetter(Configuration::targetManaCost),
            SpellCastOptions.MAP_CODEC.forGetter(Configuration::castOptions)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration config, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof LivingEntity caster) || !(ctx.target() instanceof LivingEntity victim)
                || ctx.level().isClientSide()) {
            return;
        }
        if (NeoForge.EVENT_BUS.post(new CounterSpellEvent(caster, victim)).isCanceled()) {
            return;
        }
        // Checked before touching the victim, so a busy thief leaves them alone.
        if (config.castOptions().interruptMode() == SpellCastInterruptMode.FAIL && MagicData.getPlayerMagicData(caster).isCasting()) {
            if (caster instanceof ServerPlayer player) {
                SpellCasting.sendBusyMessage(player);
            }
            return;
        }

        MagicData victimData = MagicData.getPlayerMagicData(victim);
        AbstractSpell stolen;
        int stolenLevel;
        if (victimData.isCasting()) {
            stolen = victimData.getCastingSpell().getSpell();
            if (stolen == SpellRegistry.none()) {
                return;
            }
            stolenLevel = victimData.getCastingSpellLevel();
            if (victim instanceof ServerPlayer victimPlayer) {
                Utils.serverSideCancelCast(victimPlayer, config.triggerTargetCooldown());
            } else if (victim instanceof IMagicEntity magicVictim) {
                magicVictim.cancelCast();
            } else {
                victimData.resetCastingState();
            }
        } else {
            Optional<RecentSpellCastCache.ResolvedLastCast> last = RecentSpellCastCache.getLastCast(victim, ctx.level());
            if (last.isEmpty()) {
                return;
            }
            stolen = last.get().spell();
            stolenLevel = last.get().level();
        }

        if (config.triggerTargetCooldown()) {
            PlayerMagic.addCooldown(victim, stolen);
        }
        config.targetManaCost().ifPresent(cost -> {
            if (victim instanceof ServerPlayer victimPlayer) {
                PlayerMagic.drainMana(victimPlayer, cost);
            }
        });

        SpellCasting.cast(caster, stolen, stolenLevel, new SpellTarget.Entity(victim), config.castOptions(), null);
    }

    public record Configuration(boolean triggerTargetCooldown, Optional<Integer> targetManaCost, SpellCastOptions castOptions) {
    }
}
