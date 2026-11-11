package dev.muon.irons_origins.action;

import dev.muon.irons_origins.IronsOrigins;
import dev.muon.irons_origins.action.bientity.CastSpellBientityAction;
import dev.muon.irons_origins.action.bientity.SpellThiefBientityAction;
import dev.muon.irons_origins.action.entity.CastSpellAction;
import dev.muon.irons_origins.action.entity.ReduceSpellCooldownsAction;
import dev.muon.irons_origins.action.entity.ResetSpellCooldownsAction;
import dev.muon.irons_origins.action.entity.RestoreManaAction;
import dev.overgrown.apoli.action.ActionTypes;
import net.minecraft.resources.ResourceLocation;

public final class IronsOriginsActions {
    public static final ResourceLocation CAST_SPELL = IronsOrigins.loc("cast_spell");
    public static final ResourceLocation RESTORE_MANA = IronsOrigins.loc("restore_mana");
    public static final ResourceLocation RESET_SPELL_COOLDOWNS = IronsOrigins.loc("reset_spell_cooldowns");
    public static final ResourceLocation REDUCE_SPELL_COOLDOWNS = IronsOrigins.loc("reduce_spell_cooldowns");
    public static final ResourceLocation SPELL_THIEF = IronsOrigins.loc("spell_thief");

    private IronsOriginsActions() {
    }

    public static void register() {
        ActionTypes.ENTITY.register(CAST_SPELL, new CastSpellAction());
        ActionTypes.ENTITY.register(RESTORE_MANA, new RestoreManaAction());
        ActionTypes.ENTITY.register(RESET_SPELL_COOLDOWNS, new ResetSpellCooldownsAction());
        ActionTypes.ENTITY.register(REDUCE_SPELL_COOLDOWNS, new ReduceSpellCooldownsAction());

        ActionTypes.BI_ENTITY.register(CAST_SPELL, new CastSpellBientityAction());
        ActionTypes.BI_ENTITY.register(SPELL_THIEF, new SpellThiefBientityAction());
    }
}
