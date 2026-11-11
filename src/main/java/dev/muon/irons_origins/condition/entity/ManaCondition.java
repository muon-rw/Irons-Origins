package dev.muon.irons_origins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.world.entity.player.Player;

/**
 * Compares the player's current mana. Non-players have 0 mana.
 */
public final class ManaCondition implements ConditionType<EntityCtx, ManaCondition.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Configuration::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Configuration::compareTo)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public boolean test(Configuration config, EntityCtx ctx) {
        int mana = ctx.raw() instanceof Player player ? (int) MagicData.getPlayerMagicData(player).getMana() : 0;
        return config.comparison().compare(mana, config.compareTo());
    }

    public record Configuration(Comparison comparison, int compareTo) {
    }
}
