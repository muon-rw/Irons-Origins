package dev.muon.irons_origins.spell;

import net.minecraft.world.entity.LivingEntity;

public sealed interface SpellTarget {

    double DEFAULT_RAYCAST_DISTANCE = 64.0;

    /**
     * Aim where the caster looks. If the spell targets an entity, we bind whatever entity the caster's look ray hits
     * within {@code distance}.
     */
    record Raycast(double distance) implements SpellTarget {
    }

    /**
     * Aim at {@code target}: the caster's reported facing points at it for the cast, and the spell's target data is
     * bound to it.
     */
    record Entity(LivingEntity target) implements SpellTarget {
    }
}
