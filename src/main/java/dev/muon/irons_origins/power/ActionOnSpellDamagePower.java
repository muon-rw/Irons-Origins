package dev.muon.irons_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.SpellCastFilter;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Runs a bi-entity action (actor: the player who cast the spell, target: the entity hurt) after Iron's spell damage
 * reaches the target's health. Only {@code spell_conditions}' spell fields apply; cast source and type are ignored.
 * <p>
 * The action doesn't trigger this power again, and the target's i-frames from the triggering hit are cleared while it
 * runs, so bonus damage in the action lands.
 */
public class ActionOnSpellDamagePower extends PowerType<ActionOnSpellDamagePower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Expression.INT_OR_EXPR.optionalFieldOf("cooldown", Expression.constant(1)).forGetter(Configuration::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Configuration::hudRender),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Configuration::bientityCondition),
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Configuration::bientityAction),
            SpellCastFilter.CODEC.optionalFieldOf("spell_conditions", SpellCastFilter.any()).forGetter(Configuration::spellConditions)
    ).apply(instance, Configuration::new));

    private static final ThreadLocal<Boolean> RUNNING_ACTION = ThreadLocal.withInitial(() -> false);

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && impl.getAuxInt(powerId).isEmpty()) {
            impl.setAuxInt(powerId, 0);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && !holder.hasPower(powerId)) {
            impl.removeAux(powerId);
        }
    }

    @Override
    public OptionalDouble readResource(ResourceLocation powerId, Configuration cfg, PowerContainer holder) {
        return PowerResources.readDeadline(holder, powerId);
    }

    @Override
    public OptionalDouble writeResource(ResourceLocation powerId, Configuration cfg, PowerContainer holder, double value) {
        return PowerResources.writeDeadline(holder, powerId, value, PowerResources.cooldownTicks(cfg.cooldown(), holder));
    }

    @Override
    public OptionalDouble resourceBound(ResourceLocation powerId, Configuration cfg, PowerContainer holder, boolean max) {
        return OptionalDouble.of(max ? Math.max(PowerResources.cooldownTicks(cfg.cooldown(), holder), 0) : 0);
    }

    public static void afterSpellDamageApplied(LivingEntity target, SpellDamageSource source) {
        if (target.level().isClientSide() || RUNNING_ACTION.get()) {
            return;
        }
        if (!(source.getEntity() instanceof Player attacker) || !(PowerContainer.of(attacker) instanceof PowerContainerImpl impl)) {
            return;
        }
        AbstractSpell spell = source.spell();
        if (spell == null) {
            return;
        }
        long now = attacker.level().getGameTime();
        PowerLookup.forEachEntry(attacker, IronsOriginsPowers.ACTION_ON_SPELL_DAMAGE, Configuration.class, (powerId, config) -> {
            if (!config.spellConditions().matchesSpell(spell) || impl.getAuxIntOr(powerId, 0) > (int) now) {
                return;
            }
            BiEntityCtx ctx = new BiEntityCtx(attacker, target, attacker.level());
            if (config.bientityCondition().isPresent() && !config.bientityCondition().get().test(ctx)) {
                return;
            }
            runAction(target, () -> config.bientityAction().run(ctx));
            impl.setAuxInt(powerId, (int) now + Math.max(PowerResources.cooldownTicks(config.cooldown(), impl), 0));
        });
    }

    private static void runAction(LivingEntity target, Runnable action) {
        int savedIFrames = target.invulnerableTime;
        target.invulnerableTime = 0;
        RUNNING_ACTION.set(true);
        try {
            action.run();
        } finally {
            RUNNING_ACTION.set(false);
            target.invulnerableTime = savedIFrames;
        }
    }

    public record Configuration(
            Expression cooldown,
            HudRender hudRender,
            Optional<BiEntityCondition> bientityCondition,
            BiEntityAction bientityAction,
            SpellCastFilter spellConditions
    ) {
    }
}
