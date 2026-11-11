package dev.muon.irons_origins.spell;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PendingSpellCasts {

    // Server thread only. Static state outlives an integrated server, so we clear it when the server stops.
    private static final List<PendingCast> PENDING = new ArrayList<>();

    private PendingSpellCasts() {
    }

    static void add(LivingEntity caster, AbstractSpell spell, int spellLevel, SpellTarget target, SpellCastOptions options,
                    @Nullable SpellCasting.CastStartedCallback onStarted, int delayTicks) {
        int dueTick = ((ServerLevel) caster.level()).getServer().getTickCount() + Math.max(delayTicks, 0);
        PENDING.add(new PendingCast(caster, spell, spellLevel, target, options, onStarted, dueTick));
    }

    /**
     * Runs after every level has ticked, so a cast queued mid-resolution with no delay still starts this tick, after
     * Iron's has finished cleaning up the spell that triggered it.
     */
    public static void castDue(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        int tick = server.getTickCount();
        List<PendingCast> due = new ArrayList<>();
        // Collect first: a cast can queue another one, which would break an open iterator.
        PENDING.removeIf(cast -> {
            if (cast.dueTick <= tick) {
                due.add(cast);
                return true;
            }
            return false;
        });
        for (PendingCast cast : due) {
            LivingEntity caster = cast.currentCaster(server);
            if (caster == null || !caster.isAlive()) {
                continue;
            }
            SpellTarget target = cast.target;
            if (target instanceof SpellTarget.Entity entity
                    && (!entity.target().isAlive() || entity.target().level() != caster.level())) {
                continue;
            }
            SpellCasting.castNow(caster, cast.spell, cast.spellLevel, target, cast.options, cast.onStarted);
        }
    }

    public static void clear() {
        PENDING.clear();
    }

    private record PendingCast(LivingEntity caster, AbstractSpell spell, int spellLevel, SpellTarget target,
                               SpellCastOptions options, @Nullable SpellCasting.CastStartedCallback onStarted,
                               int dueTick) {

        // Vanilla replaces the player entity on respawn, so we look the caster up again.
        @Nullable
        LivingEntity currentCaster(MinecraftServer server) {
            if (caster instanceof ServerPlayer player) {
                return server.getPlayerList().getPlayer(player.getUUID());
            }
            return caster.isRemoved() ? null : caster;
        }
    }
}
