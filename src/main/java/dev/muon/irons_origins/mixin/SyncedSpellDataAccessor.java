package dev.muon.irons_origins.mixin;

import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SyncedSpellData.class, remap = false)
public interface SyncedSpellDataAccessor {
    @Accessor("livingEntity")
    @Nullable
    LivingEntity irons_origins$getLivingEntity();
}
