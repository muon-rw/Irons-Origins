package dev.muon.irons_origins.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Optional filters on a finished or damaging spell. Every field present in JSON must match.
 * <p>
 * Without {@code cast_source(s)}, every source except {@code command} matches, so casts from our own
 * {@code cast_spell} actions don't retrigger the power that ran them.
 */
public final class SpellCastFilter {

    private static final Set<CastSource> DEFAULT_SOURCES = allSourcesExceptCommand();

    private static final Codec<CastType> CAST_TYPE_CODEC = lowercaseEnumCodec(CastType.class, "cast type");
    private static final Codec<CastSource> CAST_SOURCE_CODEC = lowercaseEnumCodec(CastSource.class, "cast source");

    private static final Codec<SpellCastFilter> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CAST_TYPE_CODEC.optionalFieldOf("cast_type").forGetter(f -> f.castType),
            Codec.list(CAST_TYPE_CODEC).optionalFieldOf("cast_types").forGetter(f -> f.castTypes),
            CAST_SOURCE_CODEC.optionalFieldOf("cast_source").forGetter(f -> f.castSource),
            Codec.list(CAST_SOURCE_CODEC).optionalFieldOf("cast_sources").forGetter(f -> f.castSources),
            ResourceLocation.CODEC.optionalFieldOf("spell").forGetter(f -> f.spell),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("spells").forGetter(f -> f.spells),
            ResourceLocation.CODEC.optionalFieldOf("spell_tag").forGetter(f -> f.spellTag),
            ResourceLocation.CODEC.optionalFieldOf("spell_school").forGetter(f -> f.spellSchool)
    ).apply(instance, SpellCastFilter::new));

    public static final Codec<SpellCastFilter> CODEC = RAW_CODEC.comapFlatMap(SpellCastFilter::validate, Function.identity());

    private final Optional<CastType> castType;
    private final Optional<List<CastType>> castTypes;
    private final Optional<CastSource> castSource;
    private final Optional<List<CastSource>> castSources;
    private final Optional<ResourceLocation> spell;
    private final Optional<List<ResourceLocation>> spells;
    private final Optional<ResourceLocation> spellTag;
    private final Optional<ResourceLocation> spellSchool;

    private final Set<CastType> effectiveCastTypes;
    private final Set<CastSource> effectiveCastSources;

    private SpellCastFilter(
            Optional<CastType> castType,
            Optional<List<CastType>> castTypes,
            Optional<CastSource> castSource,
            Optional<List<CastSource>> castSources,
            Optional<ResourceLocation> spell,
            Optional<List<ResourceLocation>> spells,
            Optional<ResourceLocation> spellTag,
            Optional<ResourceLocation> spellSchool
    ) {
        this.castType = castType;
        this.castTypes = castTypes;
        this.castSource = castSource;
        this.castSources = castSources;
        this.spell = spell;
        this.spells = spells;
        this.spellTag = spellTag;
        this.spellSchool = spellSchool;

        if (castType.isPresent()) {
            this.effectiveCastTypes = Set.of(castType.get());
        } else if (castTypes.isPresent() && !castTypes.get().isEmpty()) {
            this.effectiveCastTypes = Set.copyOf(castTypes.get());
        } else {
            this.effectiveCastTypes = Set.copyOf(EnumSet.allOf(CastType.class));
        }

        if (castSource.isPresent()) {
            this.effectiveCastSources = Set.of(castSource.get());
        } else if (castSources.isPresent() && !castSources.get().isEmpty()) {
            this.effectiveCastSources = Set.copyOf(castSources.get());
        } else {
            this.effectiveCastSources = DEFAULT_SOURCES;
        }
    }

    public static SpellCastFilter any() {
        return new SpellCastFilter(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public boolean matches(AbstractSpell spellEntity, CastSource source, CastType type) {
        return effectiveCastTypes.contains(type) && effectiveCastSources.contains(source) && matchesSpell(spellEntity);
    }

    /**
     * For contexts without cast metadata, such as spell damage: {@code cast_source(s)} and {@code cast_type(s)} are
     * ignored.
     */
    public boolean matchesSpell(AbstractSpell spellEntity) {
        ResourceLocation id = spellEntity.getSpellResource();
        if (spell.isPresent() && !SpellIds.matches(id, spell.get())) {
            return false;
        }
        if (spells.isPresent() && !spells.get().isEmpty()
                && spells.get().stream().noneMatch(candidate -> SpellIds.matches(id, candidate))) {
            return false;
        }
        if (spellTag.isPresent()) {
            TagKey<AbstractSpell> tag = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, SpellIds.normalize(spellTag.get()));
            if (!SpellRegistry.REGISTRY.wrapAsHolder(spellEntity).is(tag)) {
                return false;
            }
        }
        if (spellSchool.isPresent()) {
            SchoolType school = SchoolRegistry.getSchool(SpellIds.normalize(spellSchool.get()));
            return school != null && spellEntity.getSchoolType() == school;
        }
        return true;
    }

    private static DataResult<SpellCastFilter> validate(SpellCastFilter f) {
        if (f.castType.isPresent() && f.castTypes.isPresent() && !f.castTypes.get().isEmpty()) {
            return DataResult.error(() -> "Set only one of cast_type or cast_types");
        }
        if (f.castSource.isPresent() && f.castSources.isPresent() && !f.castSources.get().isEmpty()) {
            return DataResult.error(() -> "Set only one of cast_source or cast_sources");
        }
        return DataResult.success(f);
    }

    private static Set<CastSource> allSourcesExceptCommand() {
        EnumSet<CastSource> set = EnumSet.allOf(CastSource.class);
        set.remove(CastSource.COMMAND);
        return Set.copyOf(set);
    }

    private static <E extends Enum<E>> Codec<E> lowercaseEnumCodec(Class<E> type, String description) {
        return Codec.STRING.comapFlatMap(
                name -> {
                    try {
                        return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException e) {
                        return DataResult.error(() -> "Unknown " + description + ": " + name);
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT)
        );
    }
}
