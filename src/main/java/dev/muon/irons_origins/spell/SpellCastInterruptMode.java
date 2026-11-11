package dev.muon.irons_origins.spell;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * What a cast action does when the caster is already casting.
 */
public enum SpellCastInterruptMode implements StringRepresentable {
    /**
     * Cancel the current cast. Players go through Iron's {@code Utils.serverSideCancelCast}; non-players have no
     * cancel path, so their current cast is force-completed instead.
     */
    CANCEL("cancel"),
    /**
     * Resolve the current cast immediately. Continuous casts can't be force-completed, so the new cast is skipped.
     */
    FORCE_COMPLETE("force_complete"),
    /**
     * Leave the current cast alone and skip the new one.
     */
    FAIL("fail");

    public static final Codec<SpellCastInterruptMode> CODEC = StringRepresentable.fromEnum(SpellCastInterruptMode::values);

    private final String serializedName;

    SpellCastInterruptMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
