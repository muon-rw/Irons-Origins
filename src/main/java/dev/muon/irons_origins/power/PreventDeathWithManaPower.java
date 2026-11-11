package dev.muon.irons_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.spell.PlayerMagic;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Optional;

/**
 * When lethal damage would kill the player, they survive at half a heart by paying
 * {@code lethal damage * mana_multiplier} mana. If they can't pay and stay above 0 mana, they die as usual.
 */
public class PreventDeathWithManaPower extends PowerType<PreventDeathWithManaPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Configuration::entityAction),
            LoggedOptionalField.strict("damage_condition", DamageCondition.CODEC).forGetter(Configuration::damageCondition),
            Codec.FLOAT.optionalFieldOf("mana_multiplier", 4.0F).forGetter(Configuration::manaMultiplier)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static boolean tryPreventDeath(ServerPlayer player, DamageSource source, float lethalDamage) {
        if (lethalDamage <= 0.0F) {
            return false;
        }
        DamageCtx damageCtx = new DamageCtx(source, player, player.level(), lethalDamage);
        for (Configuration config : PowerLookup.active(player, IronsOriginsPowers.PREVENT_DEATH_WITH_MANA, Configuration.class)) {
            if (config.damageCondition().isEmpty() || config.damageCondition().get().test(damageCtx)) {
                return payAndSurvive(player, config, lethalDamage);
            }
        }
        return false;
    }

    private static boolean payAndSurvive(ServerPlayer player, Configuration config, float lethalDamage) {
        float cost = lethalDamage * config.manaMultiplier();
        if (cost <= 0.0F) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            if (MagicData.getPlayerMagicData(player).getMana() <= cost) {
                return false;
            }
            PlayerMagic.drainMana(player, cost);
        }
        player.setHealth(1.0F);
        config.entityAction().ifPresent(action -> action.run(new EntityCtx(player, player.level())));
        return true;
    }

    public record Configuration(Optional<EntityAction> entityAction, Optional<DamageCondition> damageCondition, float manaMultiplier) {
    }
}
