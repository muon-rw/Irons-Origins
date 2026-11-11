package dev.muon.irons_origins.action.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.CastFinishCallbacks;
import dev.muon.irons_origins.spell.SpellCastOptions;
import dev.muon.irons_origins.spell.SpellCasting;
import dev.muon.irons_origins.spell.SpellSelection;
import dev.muon.irons_origins.spell.SpellTarget;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Casts a spell from the actor at the target. During the cast we point the actor's reported facing at the target and
 * bind the target into the spell's target data, so look-aimed and entity-targeted spells both hit it.
 */
public final class CastSpellBientityAction implements ActionType<BiEntityCtx, CastSpellBientityAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SpellSelection.FIELD_CODEC.forGetter(Configuration::spell),
            SpellCastOptions.MAP_CODEC.forGetter(Configuration::castOptions),
            LoggedOptionalField.of("success_action", BiEntityAction.CODEC).forGetter(Configuration::successAction),
            LoggedOptionalField.of("finish_action", BiEntityAction.CODEC).forGetter(Configuration::finishAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration config, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof LivingEntity caster) || !(ctx.target() instanceof LivingEntity target)
                || ctx.level().isClientSide()) {
            return;
        }
        SpellSelection.ResolvedSpell resolved = config.spell().resolve(ctx.level().random);
        if (resolved == null) {
            return;
        }
        AbstractSpell spell = resolved.spell();
        SpellCasting.cast(caster, spell, resolved.level(), new SpellTarget.Entity(target), config.castOptions(),
                (actor, consumedRecast) -> {
                    if (consumedRecast) {
                        return;
                    }
                    config.successAction().ifPresent(action -> action.run(new BiEntityCtx(actor, target, actor.level())));
                    config.finishAction().ifPresent(action -> {
                        if (actor instanceof ServerPlayer player) {
                            // The player entity changes on End-exit respawn; follow it if they targeted themselves.
                            CastFinishCallbacks.runWhenFinished(player, spell, p -> action.run(
                                    new BiEntityCtx(p, target == player ? p : target, p.level())));
                        } else {
                            action.run(new BiEntityCtx(actor, target, actor.level()));
                        }
                    });
                });
    }

    public record Configuration(
            SpellSelection spell,
            SpellCastOptions castOptions,
            Optional<BiEntityAction> successAction,
            Optional<BiEntityAction> finishAction
    ) {
    }
}
