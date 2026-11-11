package dev.muon.irons_origins.event;

import dev.muon.irons_origins.IronsOrigins;
import dev.muon.irons_origins.power.PreventDeathWithManaPower;
import dev.muon.irons_origins.spell.CastFinishCallbacks;
import dev.muon.irons_origins.spell.PendingSpellCasts;
import dev.muon.irons_origins.spell.RecentSpellCastCache;
import dev.muon.irons_origins.spell.SpellCastOptions;
import dev.muon.irons_origins.spell.SpellCasting;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = IronsOrigins.MODID)
public final class IronsOriginsEvents {

    private static final int CACHE_PRUNE_INTERVAL_TICKS = 100;
    private static int ticksSinceCachePrune;

    private static final Map<ServerPlayer, Float> LAST_DAMAGE = Collections.synchronizedMap(new WeakHashMap<>());

    private IronsOriginsEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++ticksSinceCachePrune >= CACHE_PRUNE_INTERVAL_TICKS) {
            ticksSinceCachePrune = 0;
            RecentSpellCastCache.maintenancePrune(event.getServer());
        }
        PendingSpellCasts.castDue(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpellCasting.tickContinuousCost(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RecentSpellCastCache.clear();
        PendingSpellCasts.clear();
        SpellCasting.clear();
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        RecentSpellCastCache.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            CastFinishCallbacks.runAllPending(player);
        }
    }

    // Players also leave a level when changing dimension, so we only evict non-players here.
    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            RecentSpellCastCache.remove(living.getUUID());
        }
    }

    // Unless the cast asked for one, powers own the cooldown of the spells they cast. An Iron's cooldown would also lock
    // the player's spellbook copy of the spell, and stop spells such as Tremor Spike from opening recasts on the next
    // cast.
    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpellCastOptions options = SpellCasting.ourCastOptions(player, event.getSpell());
            if (options != null && !options.triggerCooldown()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void recordLethalDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_DAMAGE.put(player, event.getNewDamage());
        }
    }

    // HIGHEST, because Hardcore Revival cancels the death at HIGH to knock the player out, and the bus skips later
    // listeners once an event is cancelled.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDeathWithMana(LivingDeathEvent event) {
        RecentSpellCastCache.remove(event.getEntity().getUUID());
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Float damage = LAST_DAMAGE.remove(player);
        if (damage != null && PreventDeathWithManaPower.tryPreventDeath(player, event.getSource(), damage)) {
            event.setCanceled(true);
        }
    }
}
