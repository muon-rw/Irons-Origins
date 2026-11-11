package dev.muon.irons_origins.condition.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.SpellIds;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * True while the player has an open Iron's recast for the spell. Server-side only.
 */
public final class HasSpellRecastCondition implements ConditionType<EntityCtx, HasSpellRecastCondition.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("spell").forGetter(Configuration::spell)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public boolean test(Configuration config, EntityCtx ctx) {
        return ctx.raw() instanceof ServerPlayer player && MagicData.getPlayerMagicData(player).getPlayerRecasts()
                .hasRecastForSpell(SpellIds.normalize(config.spell()).toString());
    }

    public record Configuration(ResourceLocation spell) {
    }
}
