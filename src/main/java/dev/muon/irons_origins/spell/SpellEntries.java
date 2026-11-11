package dev.muon.irons_origins.spell;

import com.mojang.serialization.Codec;
import dev.muon.irons_origins.IronsOrigins;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A list of spell matchers in compact string form: {@code "spell_id"}, {@code "#spell_tag"}, or {@code "@school_id"}.
 * Ids without a namespace resolve under {@code irons_spellbooks}.
 */
public record SpellEntries(List<String> entries) {

    public static final Codec<SpellEntries> CODEC = Codec.STRING.listOf().xmap(SpellEntries::new, SpellEntries::entries);

    private static final Set<String> WARNED = Collections.synchronizedSet(new HashSet<>());

    public SpellEntries {
        entries = List.copyOf(entries);
    }

    public boolean matches(AbstractSpell spell) {
        for (String entry : entries) {
            boolean matched;
            if (entry.startsWith("#")) {
                matched = matchesTag(spell, entry.substring(1));
            } else if (entry.startsWith("@")) {
                matched = matchesSchool(spell, entry.substring(1));
            } else {
                matched = matchesSpell(spell, entry);
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesTag(AbstractSpell spell, String rawId) {
        ResourceLocation id = parse(rawId);
        if (id == null) {
            warnOnce("Invalid spell tag id: " + rawId);
            return false;
        }
        TagKey<AbstractSpell> tag = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, id);
        return SpellRegistry.REGISTRY.wrapAsHolder(spell).is(tag);
    }

    private static boolean matchesSchool(AbstractSpell spell, String rawId) {
        ResourceLocation id = parse(rawId);
        if (id == null) {
            warnOnce("Invalid school id: " + rawId);
            return false;
        }
        SchoolType school = SchoolRegistry.getSchool(id);
        if (school == null) {
            warnOnce("Unknown school: " + rawId);
            return false;
        }
        return spell.getSchoolType() == school;
    }

    private static boolean matchesSpell(AbstractSpell spell, String rawId) {
        ResourceLocation id = parse(rawId);
        if (id == null) {
            warnOnce("Invalid spell id: " + rawId);
            return false;
        }
        AbstractSpell target = SpellRegistry.getSpell(id);
        if (target == SpellRegistry.none()) {
            warnOnce("Unknown spell: " + rawId);
            return false;
        }
        return spell == target;
    }

    @Nullable
    private static ResourceLocation parse(String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        return id == null ? null : SpellIds.normalize(id);
    }

    private static void warnOnce(String message) {
        if (WARNED.add(message)) {
            IronsOrigins.LOGGER.warn("[SpellEntries] {}", message);
        }
    }
}
