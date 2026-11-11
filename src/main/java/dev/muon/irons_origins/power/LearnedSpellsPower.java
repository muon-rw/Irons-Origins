package dev.muon.irons_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.SpellEntries;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.player.Player;

/**
 * While active, Iron's treats the matching spells as learned (relevant to spells that require learning, such as the
 * eldritch school). We don't touch the player's stored learned spells, so the effect ends with the power.
 */
public class LearnedSpellsPower extends PowerType<LearnedSpellsPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SpellEntries.CODEC.fieldOf("entries").forGetter(Configuration::entries)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static boolean grants(Player player, AbstractSpell spell) {
        return PowerLookup.anyActive(player, IronsOriginsPowers.LEARNED_SPELLS, Configuration.class,
                config -> config.entries().matches(spell));
    }

    public record Configuration(SpellEntries entries) {
    }
}
