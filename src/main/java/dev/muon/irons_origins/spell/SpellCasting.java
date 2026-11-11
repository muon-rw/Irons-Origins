package dev.muon.irons_origins.spell;

import dev.muon.irons_origins.IronsOrigins;
import dev.muon.irons_origins.network.CastStartedWithoutAnimationPacket;
import dev.muon.irons_origins.network.CastWithoutSlowdownPacket;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.network.casting.CastErrorPacket;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.spells.TargetedTargetAreaCastData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-side Iron's cast pipeline for every spell we cast (always {@link CastSource#COMMAND}).
 * <p>
 * Iron's keeps one cast per entity. When an action asks us to cast while the caster is resolving a spell (because the
 * action ran from that spell's damage, healing, or completion), starting our cast would overwrite the resolving one,
 * and Iron's cleanup for that spell would then wipe ours. We queue such casts in {@link PendingSpellCasts} for the end
 * of the tick instead, and treat the queued cast as started.
 */
public final class SpellCasting {

    // Iron's uses this slot id for /cast-style casts in MagicData.initiateCast and UpdateCastingStatePacket.
    private static final String COMMAND_CASTING_SLOT = "command";

    // Server thread only.
    private static final Set<LivingEntity> RESOLVING = Collections.newSetFromMap(new IdentityHashMap<>());

    // Server thread only. Player casts we started, by spell id, until Iron's completes or cancels them. Weak keys drop
    // a disconnected player's entry.
    private static final Map<ServerPlayer, Map<String, SpellCastOptions>> OUR_CASTS = new WeakHashMap<>();

    private static final Map<UUID, ContinuousCost> CONTINUOUS_COSTS = new HashMap<>();

    private static final ThreadLocal<Boolean> IGNORING_LEARNING = ThreadLocal.withInitial(() -> false);

    private SpellCasting() {
    }

    @FunctionalInterface
    public interface CastStartedCallback {
        /**
         * @param consumedRecast whether the cast used up an open Iron's recast rather than starting a new sequence
         */
        void onCastStarted(LivingEntity caster, boolean consumedRecast);
    }

    /**
     * Casts {@code spell} now, or at the end of the tick if the caster is resolving a spell.
     *
     * @param onStarted runs once the cast actually starts; not at all if it fails
     * @return {@code false} if the cast failed immediately
     */
    public static boolean cast(LivingEntity caster, AbstractSpell spell, int spellLevel, SpellTarget target,
                               SpellCastOptions options, @Nullable CastStartedCallback onStarted) {
        if (caster.level().isClientSide()) {
            return false;
        }
        if (RESOLVING.contains(caster)) {
            PendingSpellCasts.add(caster, spell, spellLevel, target, options, onStarted, 0);
            return true;
        }
        return castNow(caster, spell, spellLevel, target, options, onStarted);
    }

    public static void castLater(LivingEntity caster, AbstractSpell spell, int spellLevel, SpellTarget target,
                                 SpellCastOptions options, @Nullable CastStartedCallback onStarted, int delayTicks) {
        if (!caster.level().isClientSide()) {
            PendingSpellCasts.add(caster, spell, spellLevel, target, options, onStarted, delayTicks);
        }
    }

    static boolean castNow(LivingEntity caster, AbstractSpell spell, int spellLevel, SpellTarget target,
                           SpellCastOptions options, @Nullable CastStartedCallback onStarted) {
        Level level = caster.level();
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        if (!makeRoomForCast(caster, level, magicData, spell, options.interruptMode())) {
            return false;
        }
        magicData = MagicData.getPlayerMagicData(caster);

        LivingEntity aimTarget = target instanceof SpellTarget.Entity entity ? entity.target() : null;
        LivingEntity bindTarget = switch (target) {
            case SpellTarget.Entity entity -> entity.target();
            case SpellTarget.Raycast raycast -> SpellRaycast.findLookTarget(caster, raycast.distance());
        };

        boolean consumedRecast = magicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId());
        boolean started;
        if (aimTarget != null) {
            BientitySpellCastAim.push(caster, aimTarget);
        }
        try {
            if (caster instanceof ServerPlayer player) {
                started = castForPlayer(player, spell, spellLevel, magicData, bindTarget, aimTarget != null, options);
            } else if (caster instanceof IMagicEntity magicEntity) {
                // Iron's mob casting; target binding and aim aren't supported on this path.
                magicEntity.initiateCastSpell(spell, spellLevel);
                started = true;
            } else if (spell.checkPreCastConditions(level, spellLevel, caster, magicData)) {
                bindTarget(caster, bindTarget, aimTarget != null, magicData, spell);
                spell.onCast(level, spellLevel, caster, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(level, spellLevel, caster, magicData, false);
                started = true;
            } else {
                started = false;
            }
        } finally {
            if (aimTarget != null) {
                BientitySpellCastAim.pop();
            }
        }

        if (started && onStarted != null) {
            onStarted.onCastStarted(caster, consumedRecast);
        }
        return started;
    }

    private static boolean castForPlayer(ServerPlayer player, AbstractSpell spell, int spellLevel, MagicData magicData,
                                         @Nullable LivingEntity bindTarget, boolean targeted, SpellCastOptions options) {
        IGNORING_LEARNING.set(true);
        try {
            return startPlayerCast(player, spell, spellLevel, magicData, bindTarget, targeted, options);
        } finally {
            IGNORING_LEARNING.set(false);
        }
    }

    private static boolean startPlayerCast(ServerPlayer player, AbstractSpell spell, int spellLevel, MagicData magicData,
                                           @Nullable LivingEntity bindTarget, boolean targeted, SpellCastOptions options) {
        CastResult castResult = spell.canBeCastedBy(spellLevel, CastSource.COMMAND, magicData, player);
        if (castResult.message != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }
        if (!castResult.isSuccess()) {
            return false;
        }

        Level level = player.level();
        if (!spell.checkPreCastConditions(level, spellLevel, player, magicData)) {
            return false;
        }
        SpellPreCastEvent preCast = new SpellPreCastEvent(player, spell.getSpellId(), spellLevel, spell.getSchoolType(), CastSource.COMMAND);
        if (NeoForge.EVENT_BUS.post(preCast).isCanceled()) {
            return false;
        }

        // Recasts are free, as in Iron's.
        boolean charges = options.manaCost().isPresent() && !player.getAbilities().instabuild
                && !magicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId());
        if (charges) {
            int manaCost = options.manaCost().get();
            if (magicData.getMana() < manaCost) {
                PacketDistributor.sendToPlayer(player, new CastErrorPacket(CastErrorPacket.ErrorType.MANA, spell));
                return false;
            }
            PlayerMagic.setManaWithEvent(player, magicData, magicData.getMana() - manaCost);
        }

        if (player.isUsingItem()) {
            player.stopUsingItem();
        }

        int castTime;
        if (options.castTime().isPresent()) {
            castTime = options.castTime().get();
        } else if (spell.getCastType() == CastType.INSTANT) {
            castTime = 0;
        } else {
            castTime = spell.getEffectiveCastTime(spellLevel, player);
        }

        if (charges && options.continuousCost()) {
            CONTINUOUS_COSTS.put(player.getUUID(), new ContinuousCost(options.manaCost().get(), options.costInterval()));
        }

        magicData.initiateCast(spell, spellLevel, castTime, CastSource.COMMAND, COMMAND_CASTING_SLOT);
        magicData.setPlayerCastingItem(ItemStack.EMPTY);
        OUR_CASTS.computeIfAbsent(player, p -> new HashMap<>()).put(spell.getSpellId(), options);

        spell.onServerPreCast(level, spellLevel, player, magicData);
        bindTarget(player, bindTarget, targeted, magicData, spell);

        PacketDistributor.sendToPlayer(player,
                new UpdateCastingStatePacket(spell.getSpellId(), spellLevel, castTime, CastSource.COMMAND, COMMAND_CASTING_SLOT));
        if (!options.castingSlowdown()) {
            PacketDistributor.sendToPlayer(player, CastWithoutSlowdownPacket.INSTANCE);
        }
        CustomPacketPayload castStarted = options.playerAnimations()
                ? new OnCastStartedPacket(player.getUUID(), spell.getSpellId(), spellLevel)
                : new CastStartedWithoutAnimationPacket(player.getUUID(), spell.getSpellId(), spellLevel);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, castStarted);

        if (castTime == 0) {
            // castSpell rather than onCast: it posts SpellOnCastEvent and consumes pending recasts.
            spell.castSpell(level, spellLevel, player, CastSource.COMMAND, options.triggerCooldown());
            spell.onServerCastComplete(level, spellLevel, player, magicData, false);
        }
        return true;
    }

    /**
     * Applies {@code interruptMode} when the caster is already casting.
     *
     * @return {@code false} if the new cast must not start. Recasting the spell already being cast cancels it, so a
     * cast action bound to a key toggles a long spell off.
     */
    private static boolean makeRoomForCast(LivingEntity caster, Level level, MagicData magicData, AbstractSpell incoming,
                                           SpellCastInterruptMode interruptMode) {
        if (!magicData.isCasting()) {
            return true;
        }
        boolean sameSpell = incoming.getSpellId().equals(magicData.getCastingSpellId());
        switch (interruptMode) {
            case FAIL -> {
                if (caster instanceof ServerPlayer player) {
                    sendBusyMessage(player);
                }
                return false;
            }
            case FORCE_COMPLETE -> {
                if (magicData.getCastType() == CastType.CONTINUOUS) {
                    return false;
                }
                forceCompleteCurrentCast(caster, level, magicData);
            }
            case CANCEL -> cancelCurrentCast(caster, level, magicData);
        }
        return !sameSpell;
    }

    public static void sendBusyMessage(ServerPlayer player) {
        player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("irons_origins.cast.busy").withStyle(ChatFormatting.RED)));
    }

    private static void forceCompleteCurrentCast(LivingEntity caster, Level level, MagicData magicData) {
        AbstractSpell spell = magicData.getCastingSpell().getSpell();
        int spellLevel = magicData.getCastingSpellLevel();
        CastSource source = magicData.getCastSource();
        if (caster instanceof ServerPlayer player) {
            spell.castSpell(level, spellLevel, player, source, true);
            if (source == CastSource.SCROLL) {
                Scroll.attemptRemoveScrollAfterCast(player);
            }
        } else {
            spell.onCast(level, spellLevel, caster, source, magicData);
        }
        spell.onServerCastComplete(level, spellLevel, caster, magicData, false);
        magicData.resetCastingState();
    }

    private static void cancelCurrentCast(LivingEntity caster, Level level, MagicData magicData) {
        if (caster instanceof ServerPlayer player) {
            Utils.serverSideCancelCast(player);
        } else if (caster instanceof IMagicEntity magicEntity) {
            magicEntity.cancelCast();
        } else {
            magicData.getCastingSpell().getSpell()
                    .onServerCastComplete(level, magicData.getCastingSpellLevel(), caster, magicData, true);
        }
        magicData.resetCastingState();
    }

    /**
     * Binds {@code target} into the spell's target data after {@code onServerPreCast}. For raycast casts we only
     * replace a plain {@link TargetEntityCastData}; for targeted casts we also move a
     * {@link TargetedTargetAreaCastData}'s area onto the target. Strict class equality keeps us off other subclasses.
     */
    private static void bindTarget(LivingEntity caster, @Nullable LivingEntity target, boolean targeted,
                                   MagicData magicData, AbstractSpell spell) {
        if (target == null) {
            return;
        }
        ICastData data = magicData.getAdditionalCastData();
        if (data != null && data.getClass() == TargetEntityCastData.class) {
            magicData.setAdditionalCastData(new TargetEntityCastData(target));
            notifyTargeted(caster, target, spell);
        } else if (targeted && data instanceof TargetedTargetAreaCastData targetedArea) {
            TargetedAreaEntity area = targetedArea.getAreaEntity();
            area.setOwner(target);
            area.setPos(target.position());
            magicData.setAdditionalCastData(new TargetedTargetAreaCastData(target, area));
        }
    }

    // Matches the feedback from Iron's Utils.preCastTargetHelper, which skips the targeting sync for instant spells.
    private static void notifyTargeted(LivingEntity caster, LivingEntity target, AbstractSpell spell) {
        if (caster instanceof ServerPlayer player) {
            if (spell.getCastType() != CastType.INSTANT) {
                PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(target, spell));
            }
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.irons_spellbooks.spell_target_success",
                            target.getDisplayName().getString(),
                            spell.getDisplayName(player)).withStyle(ChatFormatting.GREEN)));
        }
        if (target instanceof ServerPlayer targetPlayer) {
            Utils.sendTargetedNotification(targetPlayer, caster, spell);
        }
    }

    /**
     * True while one of our player casts starts; we grant these spells directly, so Iron's learning requirement
     * doesn't apply.
     */
    public static boolean isIgnoringLearning() {
        return IGNORING_LEARNING.get();
    }

    /** Wraps every {@code AbstractSpell#castSpell} (see {@code AbstractSpellMixin}). */
    public static void resolve(ServerPlayer caster, Runnable castSpell) {
        boolean outermost = RESOLVING.add(caster);
        try {
            castSpell.run();
        } finally {
            if (outermost) {
                RESOLVING.remove(caster);
            }
        }
    }

    @Nullable
    public static SpellCastOptions ourCastOptions(ServerPlayer player, AbstractSpell spell) {
        Map<String, SpellCastOptions> casts = OUR_CASTS.get(player);
        return casts == null ? null : casts.get(spell.getSpellId());
    }

    /** Called when {@code spell} completes or is cancelled. */
    @Nullable
    public static SpellCastOptions onCastEnd(ServerPlayer player, AbstractSpell spell) {
        CONTINUOUS_COSTS.remove(player.getUUID());
        Map<String, SpellCastOptions> casts = OUR_CASTS.get(player);
        if (casts == null) {
            return null;
        }
        SpellCastOptions options = casts.remove(spell.getSpellId());
        if (casts.isEmpty()) {
            OUR_CASTS.remove(player);
        }
        return options;
    }

    /**
     * Driven by the player tick rather than {@code AbstractSpell#onServerCastTick}, which spells such as Blaze Storm
     * override without calling super.
     */
    public static void tickContinuousCost(ServerPlayer player) {
        ContinuousCost cost = CONTINUOUS_COSTS.get(player.getUUID());
        if (cost == null) {
            return;
        }
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (!magicData.isCasting() || ++cost.ticksElapsed < cost.interval) {
            return;
        }
        cost.ticksElapsed = 0;
        if (magicData.getMana() >= cost.manaCost) {
            PlayerMagic.setManaWithEvent(player, magicData, magicData.getMana() - cost.manaCost);
        } else {
            IronsOrigins.LOGGER.debug("Out of mana for continuous cast {}; cancelling", magicData.getCastingSpellId());
            Utils.serverSideCancelCast(player);
            CONTINUOUS_COSTS.remove(player.getUUID());
        }
    }

    public static void clear() {
        RESOLVING.clear();
        OUR_CASTS.clear();
        CONTINUOUS_COSTS.clear();
    }

    private static final class ContinuousCost {
        final int manaCost;
        final int interval;
        int ticksElapsed;

        ContinuousCost(int manaCost, int interval) {
            this.manaCost = manaCost;
            this.interval = interval;
        }
    }
}
