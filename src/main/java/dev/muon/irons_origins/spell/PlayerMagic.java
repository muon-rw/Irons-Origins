package dev.muon.irons_origins.spell;

import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server-side mana and cooldown changes that post Iron's events and sync to the client.
 */
public final class PlayerMagic {

    private PlayerMagic() {
    }

    public static void setManaWithEvent(ServerPlayer player, MagicData magicData, float newMana) {
        ChangeManaEvent event = new ChangeManaEvent(player, magicData, magicData.getMana(), newMana);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return;
        }
        magicData.setMana(event.getNewMana());
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
    }

    public static void drainMana(ServerPlayer player, float amount) {
        if (amount <= 0 || player.getAbilities().instabuild) {
            return;
        }
        MagicData magicData = MagicData.getPlayerMagicData(player);
        setManaWithEvent(player, magicData, Math.max(0f, magicData.getMana() - amount));
    }

    public static void restoreMana(ServerPlayer player, float amount) {
        if (amount <= 0) {
            return;
        }
        MagicData magicData = MagicData.getPlayerMagicData(player);
        float max = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
        float current = magicData.getMana();
        float newMana = Math.min(max, current + amount);
        if (newMana > current) {
            setManaWithEvent(player, magicData, newMana);
        }
    }

    public static void clearCooldowns(ServerPlayer player) {
        PlayerCooldowns cooldowns = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        cooldowns.clearCooldowns();
        cooldowns.syncToPlayer(player);
    }

    /**
     * Uses {@link PlayerCooldowns#decrementCooldown} so expiry matches Iron's own tick.
     */
    public static void reduceCooldowns(ServerPlayer player, int ticks) {
        PlayerCooldowns cooldowns = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        if (ticks <= 0 || !cooldowns.hasCooldownsActive()) {
            return;
        }
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, CooldownInstance> entry : cooldowns.getSpellCooldowns().entrySet()) {
            if (cooldowns.decrementCooldown(entry.getValue(), ticks)) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(cooldowns.getSpellCooldowns()::remove);
        cooldowns.syncToPlayer(player);
    }

    /**
     * Players go through {@code MagicManager#addCooldown} for its events and sync. Non-players get the same effective
     * cooldown without a sync.
     */
    public static void addCooldown(LivingEntity entity, AbstractSpell spell) {
        if (entity instanceof ServerPlayer player) {
            MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, CastSource.COMMAND);
            return;
        }
        double reduction = entity.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION);
        int ticks = (int) (spell.getSpellCooldown() * (2 - Utils.softCapFormula(reduction)));
        if (ticks > 0) {
            MagicData.getPlayerMagicData(entity).getPlayerCooldowns().addCooldown(spell, ticks);
        }
    }
}
