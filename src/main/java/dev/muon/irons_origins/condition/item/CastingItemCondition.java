package dev.muon.irons_origins.condition.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import io.redspace.ironsspellbooks.item.CastingItem;

/**
 * True for Iron's casting implements: staffs and wands that cast from the spell wheel.
 */
public final class CastingItemCondition implements ConditionType<ItemCtx, EmptyCfg> {

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg config, ItemCtx ctx) {
        return ctx.stack().getItem() instanceof CastingItem;
    }
}
