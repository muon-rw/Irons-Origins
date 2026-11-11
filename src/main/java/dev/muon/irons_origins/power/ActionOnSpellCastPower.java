package dev.muon.irons_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.SpellCastFilter;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Runs an entity action on the caster after one of their casts finishes server-side. Cancelled casts don't count.
 */
public class ActionOnSpellCastPower extends PowerType<ActionOnSpellCastPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Configuration::entityAction),
            SpellCastFilter.CODEC.optionalFieldOf("cast_conditions", SpellCastFilter.any()).forGetter(Configuration::castConditions)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static void onCastComplete(AbstractSpell spell, LivingEntity caster, CastSource castSource, CastType castType) {
        PowerLookup.forEach(caster, IronsOriginsPowers.ACTION_ON_SPELL_CAST, Configuration.class, config -> {
            if (config.castConditions().matches(spell, castSource, castType)) {
                config.entityAction().run(new EntityCtx(caster, caster.level()));
            }
        });
    }

    public record Configuration(EntityAction entityAction, SpellCastFilter castConditions) {
    }
}
