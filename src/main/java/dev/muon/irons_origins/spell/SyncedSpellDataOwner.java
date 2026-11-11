package dev.muon.irons_origins.spell;

import dev.muon.irons_origins.client.SyncedSpellDataClientOwner;
import dev.muon.irons_origins.mixin.SyncedSpellDataAccessor;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps the client-only owner lookup out of {@code SyncedSpellDataMixin}: Mixin resolves every class a mixin
 * references while preparing it, so a dedicated server would fail on the client helper before any dist check ran.
 * Plain classes link lazily.
 */
public final class SyncedSpellDataOwner {

    private SyncedSpellDataOwner() {
    }

    @Nullable
    public static Player resolve(SyncedSpellData data) {
        if (((SyncedSpellDataAccessor) data).irons_origins$getLivingEntity() instanceof Player player) {
            return player;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return SyncedSpellDataClientOwner.resolve(data);
        }
        return null;
    }
}
