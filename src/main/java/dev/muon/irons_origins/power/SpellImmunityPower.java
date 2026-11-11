package dev.muon.irons_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.SpellEntries;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;

/**
 * Makes the entity an invalid target for the matching spells wherever Iron's picks a target through
 * {@code Utils.preCastTargetHelper} (Root, Ray of Sickness, and similar), so those casts fail with "No valid target".
 */
public class SpellImmunityPower extends PowerType<SpellImmunityPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SpellEntries.CODEC.fieldOf("entries").forGetter(Configuration::entries)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static boolean isImmune(LivingEntity entity, AbstractSpell spell) {
        return PowerLookup.anyActive(entity, IronsOriginsPowers.SPELL_IMMUNITY, Configuration.class,
                config -> config.entries().matches(spell));
    }

    public record Configuration(SpellEntries entries) {
    }
}
