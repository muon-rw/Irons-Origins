package dev.muon.irons_origins.condition;

import dev.muon.irons_origins.IronsOrigins;
import dev.muon.irons_origins.condition.entity.HasSpellRecastCondition;
import dev.muon.irons_origins.condition.entity.IsRootedCondition;
import dev.muon.irons_origins.condition.entity.ManaCondition;
import dev.muon.irons_origins.condition.item.CastingItemCondition;
import dev.overgrown.apoli.condition.ConditionTypes;
import net.minecraft.resources.ResourceLocation;

public final class IronsOriginsConditions {
    public static final ResourceLocation MANA = IronsOrigins.loc("mana");
    public static final ResourceLocation HAS_SPELL_RECAST = IronsOrigins.loc("has_spell_recast");
    public static final ResourceLocation IS_ROOTED = IronsOrigins.loc("is_rooted");
    public static final ResourceLocation IS_CASTING_ITEM = IronsOrigins.loc("is_casting_item");

    private IronsOriginsConditions() {
    }

    public static void register() {
        ConditionTypes.ENTITY.register(MANA, new ManaCondition());
        ConditionTypes.ENTITY.register(HAS_SPELL_RECAST, new HasSpellRecastCondition());
        ConditionTypes.ENTITY.register(IS_ROOTED, new IsRootedCondition());

        ConditionTypes.ITEM.register(IS_CASTING_ITEM, new CastingItemCondition());
    }
}
