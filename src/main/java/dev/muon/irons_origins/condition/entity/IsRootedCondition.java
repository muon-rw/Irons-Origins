package dev.muon.irons_origins.condition.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;

/**
 * True while Iron's Root spell holds the entity.
 */
public final class IsRootedCondition implements ConditionType<EntityCtx, EmptyCfg> {

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg config, EntityCtx ctx) {
        return ctx.raw().getVehicle() instanceof RootEntity;
    }
}
