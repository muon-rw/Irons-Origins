package dev.muon.irons_origins.action.entity;

import com.mojang.serialization.MapCodec;
import dev.muon.irons_origins.spell.PlayerMagic;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.server.level.ServerPlayer;

public final class ResetSpellCooldownsAction implements ActionType<EntityCtx, EmptyCfg> {

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg config, EntityCtx ctx) {
        if (ctx.entity() instanceof ServerPlayer player) {
            PlayerMagic.clearCooldowns(player);
        }
    }
}
