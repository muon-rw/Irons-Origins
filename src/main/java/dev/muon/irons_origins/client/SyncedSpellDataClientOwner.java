package dev.muon.irons_origins.client;

import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only. Only reach this through {@link dev.muon.irons_origins.spell.SyncedSpellDataOwner}, which checks the
 * dist first, so dedicated servers never load it.
 */
public final class SyncedSpellDataClientOwner {

    private SyncedSpellDataClientOwner() {
    }

    @Nullable
    public static Player resolve(SyncedSpellData data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.getEntity(data.getServerPlayerId()) instanceof Player player ? player : null;
    }
}
