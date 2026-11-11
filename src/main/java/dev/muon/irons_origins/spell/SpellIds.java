package dev.muon.irons_origins.spell;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.resources.ResourceLocation;

public final class SpellIds {

    private SpellIds() {
    }

    /**
     * {@link ResourceLocation#CODEC} reads a bare {@code "fireball"} as {@code minecraft:fireball}. Spells and schools
     * never live under {@code minecraft:}, so we read those ids as Iron's.
     */
    public static ResourceLocation normalize(ResourceLocation id) {
        if (ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
            return ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, id.getPath());
        }
        return id;
    }

    public static boolean matches(ResourceLocation a, ResourceLocation b) {
        return normalize(a).equals(normalize(b));
    }
}
