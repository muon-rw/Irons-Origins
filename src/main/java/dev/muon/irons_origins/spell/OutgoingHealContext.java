package dev.muon.irons_origins.spell;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Applies the caster's {@code apoli:modify_healing} to Iron's spell heals on other entities. Apoli only modifies
 * healing on the recipient, so we compute the caster-modified amount when Iron's posts {@code SpellHealEvent} and
 * substitute it when the target's {@code heal} runs.
 */
public final class OutgoingHealContext {

    private static final ThreadLocal<Deque<Pending>> PENDING = ThreadLocal.withInitial(ArrayDeque::new);

    private record Pending(LivingEntity target, float modifiedAmount) {
    }

    private OutgoingHealContext() {
    }

    public static void pushOutgoingHeal(LivingEntity target, float modifiedHealAmount) {
        PENDING.get().addLast(new Pending(target, modifiedHealAmount));
    }

    public static float consumeFor(LivingEntity healed, float healAmount) {
        for (Iterator<Pending> it = PENDING.get().iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (pending.target == healed) {
                it.remove();
                return pending.modifiedAmount;
            }
        }
        return healAmount;
    }
}
