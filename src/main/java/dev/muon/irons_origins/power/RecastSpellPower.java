package dev.muon.irons_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.SpellCastFilter;
import dev.muon.irons_origins.spell.SpellCastOptions;
import dev.muon.irons_origins.spell.SpellCasting;
import dev.muon.irons_origins.spell.SpellTarget;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

/**
 * After one of the player's casts finishes, casts the same spell at the same level again after {@code delay_ticks}
 * ticks, through the {@code cast_spell} pipeline. {@code entity_action} runs on the player when the recast starts.
 */
public class RecastSpellPower extends PowerType<RecastSpellPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SpellCastFilter.CODEC.optionalFieldOf("cast_conditions", SpellCastFilter.any()).forGetter(Configuration::castConditions),
            Codec.BOOL.optionalFieldOf("skip_if_spell_has_recasts", true).forGetter(Configuration::skipIfSpellHasRecasts),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("delay_ticks", 5).forGetter(Configuration::delayTicks),
            Codec.DOUBLE.optionalFieldOf("raycast_distance", SpellTarget.DEFAULT_RAYCAST_DISTANCE).forGetter(Configuration::raycastDistance),
            SpellCastOptions.MAP_CODEC.forGetter(Configuration::castOptions),
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Configuration::entityAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static void onCastComplete(AbstractSpell spell, ServerPlayer caster, int spellLevel, CastSource castSource, CastType castType) {
        PowerLookup.forEach(caster, IronsOriginsPowers.RECAST_SPELL, Configuration.class, config -> {
            if (!config.castConditions().matches(spell, castSource, castType)) {
                return;
            }
            if (config.skipIfSpellHasRecasts() && spell.getRecastCount(spellLevel, caster) > 0) {
                return;
            }
            SpellCasting.castLater(caster, spell, spellLevel, new SpellTarget.Raycast(config.raycastDistance()), config.castOptions(),
                    (recaster, consumedRecast) -> config.entityAction().ifPresent(action -> action.run(new EntityCtx(recaster, recaster.level()))),
                    config.delayTicks());
        });
    }

    public record Configuration(
            SpellCastFilter castConditions,
            boolean skipIfSpellHasRecasts,
            int delayTicks,
            double raycastDistance,
            SpellCastOptions castOptions,
            Optional<EntityAction> entityAction
    ) {
    }
}
