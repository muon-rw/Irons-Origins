package dev.muon.irons_origins.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

/**
 * Cast pipeline settings shared by every action and power that casts a spell. The codec is flattened into the owning
 * JSON object, so these read as top-level fields.
 *
 * @param castTime         overrides the spell's cast time, in ticks; {@code 0} resolves the spell immediately
 * @param manaCost         mana charged on cast (players only). Iron's charges nothing for these casts by default
 * @param continuousCost   charge {@code manaCost} every {@code costInterval} ticks for as long as the cast lasts
 * @param triggerCooldown  put the spell on Iron's cooldown, which also locks the copy in the player's spellbook
 * @param playerAnimations play Iron's cast start and finish animations
 * @param castingSlowdown  apply Iron's movement slowdown while casting
 */
public record SpellCastOptions(
        Optional<Integer> castTime,
        Optional<Integer> manaCost,
        boolean continuousCost,
        int costInterval,
        SpellCastInterruptMode interruptMode,
        boolean triggerCooldown,
        boolean playerAnimations,
        boolean castingSlowdown
) {

    public static final MapCodec<SpellCastOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cast_time").forGetter(SpellCastOptions::castTime),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("mana_cost").forGetter(SpellCastOptions::manaCost),
            Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(SpellCastOptions::continuousCost),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("cost_interval", 20).forGetter(SpellCastOptions::costInterval),
            SpellCastInterruptMode.CODEC.optionalFieldOf("interrupt_mode", SpellCastInterruptMode.CANCEL).forGetter(SpellCastOptions::interruptMode),
            Codec.BOOL.optionalFieldOf("trigger_cooldown", false).forGetter(SpellCastOptions::triggerCooldown),
            Codec.BOOL.optionalFieldOf("player_animations", true).forGetter(SpellCastOptions::playerAnimations),
            Codec.BOOL.optionalFieldOf("casting_slowdown", true).forGetter(SpellCastOptions::castingSlowdown)
    ).apply(instance, SpellCastOptions::new));

    public static final SpellCastOptions DEFAULT = new SpellCastOptions(Optional.empty(), Optional.empty(), false, 20,
            SpellCastInterruptMode.CANCEL, false, true, true);
}
