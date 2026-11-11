package dev.muon.irons_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.PlayerMagic;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.server.level.ServerPlayer;

public final class ReduceSpellCooldownsAction implements ActionType<EntityCtx, ReduceSpellCooldownsAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("ticks").forGetter(Configuration::ticks)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration config, EntityCtx ctx) {
        if (ctx.entity() instanceof ServerPlayer player) {
            PlayerMagic.reduceCooldowns(player, config.ticks());
        }
    }

    public record Configuration(int ticks) {
    }
}
