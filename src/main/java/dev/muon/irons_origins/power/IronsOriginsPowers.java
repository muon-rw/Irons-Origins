package dev.muon.irons_origins.power;

import dev.muon.irons_origins.IronsOrigins;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;

public final class IronsOriginsPowers {
    public static final ResourceLocation ACTION_ON_SPELL_CAST = IronsOrigins.loc("action_on_spell_cast");
    public static final ResourceLocation ACTION_ON_SPELL_DAMAGE = IronsOrigins.loc("action_on_spell_damage");
    public static final ResourceLocation LEARNED_SPELLS = IronsOrigins.loc("learned_spells");
    public static final ResourceLocation PREVENT_DEATH_WITH_MANA = IronsOrigins.loc("prevent_death_with_mana");
    public static final ResourceLocation RECAST_SPELL = IronsOrigins.loc("recast_spell");
    public static final ResourceLocation SPELL_IMMUNITY = IronsOrigins.loc("spell_immunity");

    private IronsOriginsPowers() {
    }

    public static void register() {
        PowerTypeRegistry.register(ACTION_ON_SPELL_CAST, new ActionOnSpellCastPower());
        PowerTypeRegistry.register(ACTION_ON_SPELL_DAMAGE, new ActionOnSpellDamagePower());
        PowerTypeRegistry.register(LEARNED_SPELLS, new LearnedSpellsPower());
        PowerTypeRegistry.register(PREVENT_DEATH_WITH_MANA, new PreventDeathWithManaPower());
        PowerTypeRegistry.register(RECAST_SPELL, new RecastSpellPower());
        PowerTypeRegistry.register(SPELL_IMMUNITY, new SpellImmunityPower());
    }
}
