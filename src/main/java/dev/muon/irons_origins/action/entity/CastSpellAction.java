package dev.muon.irons_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.CastFinishCallbacks;
import dev.muon.irons_origins.spell.SpellCastOptions;
import dev.muon.irons_origins.spell.SpellCasting;
import dev.muon.irons_origins.spell.SpellSelection;
import dev.muon.irons_origins.spell.SpellTarget;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Casts a spell from the entity in the direction it looks. {@code success_action} runs when the cast starts,
 * {@code finish_action} once the cast and any Iron's recast sequence it opened are over. Neither runs when the cast
 * only used up an open recast.
 */
public final class CastSpellAction implements ActionType<EntityCtx, CastSpellAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SpellSelection.FIELD_CODEC.forGetter(Configuration::spell),
            Codec.DOUBLE.optionalFieldOf("raycast_distance", SpellTarget.DEFAULT_RAYCAST_DISTANCE).forGetter(Configuration::raycastDistance),
            SpellCastOptions.MAP_CODEC.forGetter(Configuration::castOptions),
            LoggedOptionalField.of("success_action", EntityAction.CODEC).forGetter(Configuration::successAction),
            LoggedOptionalField.of("finish_action", EntityAction.CODEC).forGetter(Configuration::finishAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration config, EntityCtx ctx) {
        if (!(ctx.entity() instanceof LivingEntity caster) || ctx.level().isClientSide()) {
            return;
        }
        SpellSelection.ResolvedSpell resolved = config.spell().resolve(ctx.level().random);
        if (resolved == null) {
            return;
        }
        AbstractSpell spell = resolved.spell();
        SpellCasting.cast(caster, spell, resolved.level(), new SpellTarget.Raycast(config.raycastDistance()), config.castOptions(),
                (actor, consumedRecast) -> {
                    if (consumedRecast) {
                        return;
                    }
                    config.successAction().ifPresent(action -> action.run(new EntityCtx(actor, actor.level())));
                    config.finishAction().ifPresent(action -> {
                        if (actor instanceof ServerPlayer player) {
                            CastFinishCallbacks.runWhenFinished(player, spell, p -> action.run(new EntityCtx(p, p.level())));
                        } else {
                            action.run(new EntityCtx(actor, actor.level()));
                        }
                    });
                });
    }

    public record Configuration(
            SpellSelection spell,
            double raycastDistance,
            SpellCastOptions castOptions,
            Optional<EntityAction> successAction,
            Optional<EntityAction> finishAction
    ) {
    }
}
