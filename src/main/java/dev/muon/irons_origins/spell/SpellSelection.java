package dev.muon.irons_origins.spell;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_origins.IronsOrigins;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Picks an Iron's spell and cast level for the {@code cast_spell} actions. JSON accepts either the shorthand
 * {@code "spell": "id"} with an optional sibling {@code "power_level"}, or an object with exactly one of
 * {@code spell}, {@code spells}, {@code spell_tag}, or {@code spell_school}.
 */
public final class SpellSelection {

    /**
     * How to pick the base spell level when {@code min_rarity}/{@code max_rarity} are set.
     */
    public enum LevelMode implements StringRepresentable {
        LOWEST("lowest"),
        HIGHEST("highest"),
        RANDOM("random");

        public static final Codec<LevelMode> CODEC = StringRepresentable.fromEnum(LevelMode::values);

        private final String serializedName;

        LevelMode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    private static final Codec<SpellSelection> OBJECT_CODEC = RecordCodecBuilder.<SpellSelection>create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("spell").forGetter(s -> s.singleSpell),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("spells").forGetter(s -> s.spellList),
            ResourceLocation.CODEC.optionalFieldOf("spell_tag").forGetter(s -> s.spellTag),
            ResourceLocation.CODEC.optionalFieldOf("spell_school").forGetter(s -> s.spellSchool),
            SpellRarity.CODEC.optionalFieldOf("min_rarity").forGetter(s -> s.minRarity),
            SpellRarity.CODEC.optionalFieldOf("max_rarity").forGetter(s -> s.maxRarity),
            LevelMode.CODEC.optionalFieldOf("level_mode", LevelMode.LOWEST).forGetter(s -> s.levelMode),
            Codec.INT.optionalFieldOf("power_level", 1).forGetter(s -> s.powerLevel),
            Codec.INT.optionalFieldOf("level_bonus", 0).forGetter(s -> s.levelBonus)
    ).apply(instance, (spell, spells, tag, school, minRarity, maxRarity, levelMode, powerLevel, levelBonus) ->
            new SpellSelection(spell, spells.map(List::copyOf), tag, school, minRarity, maxRarity, levelMode, powerLevel, levelBonus, false)
    )).comapFlatMap(SpellSelection::validateExactlyOneSource, Function.identity());

    /**
     * The {@code spell} field plus its optional shorthand sibling {@code power_level}, for embedding in an action's
     * codec.
     */
    public static final MapCodec<SpellSelection> FIELD_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(ResourceLocation.CODEC, OBJECT_CODEC).fieldOf("spell").forGetter(SpellSelection::encodeSpellField),
            Codec.INT.optionalFieldOf("power_level").forGetter(SpellSelection::shorthandPowerLevel)
    ).apply(instance, (spellField, powerLevel) -> spellField.map(id -> shorthand(id, powerLevel.orElse(1)), Function.identity())));

    public record ResolvedSpell(AbstractSpell spell, int level) {
    }

    private final Optional<ResourceLocation> singleSpell;
    private final Optional<List<ResourceLocation>> spellList;
    private final Optional<ResourceLocation> spellTag;
    private final Optional<ResourceLocation> spellSchool;
    private final Optional<SpellRarity> minRarity;
    private final Optional<SpellRarity> maxRarity;
    private final LevelMode levelMode;
    private final int powerLevel;
    private final int levelBonus;
    private final boolean isShorthand;

    private SpellSelection(
            Optional<ResourceLocation> singleSpell,
            Optional<List<ResourceLocation>> spellList,
            Optional<ResourceLocation> spellTag,
            Optional<ResourceLocation> spellSchool,
            Optional<SpellRarity> minRarity,
            Optional<SpellRarity> maxRarity,
            LevelMode levelMode,
            int powerLevel,
            int levelBonus,
            boolean isShorthand
    ) {
        this.singleSpell = singleSpell;
        this.spellList = spellList;
        this.spellTag = spellTag;
        this.spellSchool = spellSchool;
        this.minRarity = minRarity;
        this.maxRarity = maxRarity;
        this.levelMode = levelMode;
        this.powerLevel = powerLevel;
        this.levelBonus = levelBonus;
        this.isShorthand = isShorthand;
    }

    public static SpellSelection shorthand(ResourceLocation spellId, int powerLevel) {
        return new SpellSelection(Optional.of(spellId), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), LevelMode.LOWEST, powerLevel, 0, true);
    }

    private static DataResult<SpellSelection> validateExactlyOneSource(SpellSelection s) {
        int sources = 0;
        if (s.singleSpell.isPresent()) sources++;
        if (s.spellList.isPresent() && !s.spellList.get().isEmpty()) sources++;
        if (s.spellTag.isPresent()) sources++;
        if (s.spellSchool.isPresent()) sources++;
        int found = sources;
        if (found != 1) {
            return DataResult.error(() -> "Spell selection must set exactly one of: spell, spells, spell_tag, spell_school (found " + found + ")");
        }
        return DataResult.success(s);
    }

    private Either<ResourceLocation, SpellSelection> encodeSpellField() {
        return isShorthand && singleSpell.isPresent() ? Either.left(singleSpell.get()) : Either.right(this);
    }

    private Optional<Integer> shorthandPowerLevel() {
        return isShorthand && powerLevel != 1 ? Optional.of(powerLevel) : Optional.empty();
    }

    @Nullable
    public ResolvedSpell resolve(RandomSource random) {
        List<AbstractSpell> candidates = gatherCandidates();
        if (candidates.isEmpty()) {
            IronsOrigins.LOGGER.warn("SpellSelection: no candidate spells for {}", this);
            return null;
        }

        boolean rarityFilter = minRarity.isPresent() || maxRarity.isPresent();
        SpellRarity min = minRarity.orElse(SpellRarity.COMMON);
        SpellRarity max = maxRarity.orElse(SpellRarity.LEGENDARY);
        if (min.getValue() > max.getValue()) {
            IronsOrigins.LOGGER.warn("SpellSelection: min_rarity {} is above max_rarity {}", min, max);
            return null;
        }

        List<ResolvedSpell> viable = new ArrayList<>();
        for (AbstractSpell spell : candidates) {
            if (!rarityFilter) {
                viable.add(new ResolvedSpell(spell, powerLevel));
                continue;
            }
            List<Integer> levels = levelsInRarityWindow(spell, min, max);
            if (!levels.isEmpty()) {
                viable.add(new ResolvedSpell(spell, pickLevel(levels, random)));
            }
        }
        if (viable.isEmpty()) {
            IronsOrigins.LOGGER.warn("SpellSelection: no spells survived the rarity filter for {}", this);
            return null;
        }

        ResolvedSpell picked = viable.get(random.nextInt(viable.size()));
        // No upper clamp: like Iron's own spell level bonuses, these casts may exceed a spell's configured max.
        int level = Math.max(picked.level() + levelBonus, picked.spell().getMinLevel());
        return new ResolvedSpell(picked.spell(), level);
    }

    private List<AbstractSpell> gatherCandidates() {
        if (singleSpell.isPresent()) {
            AbstractSpell spell = SpellRegistry.getSpell(SpellIds.normalize(singleSpell.get()));
            return isValid(spell) ? List.of(spell) : List.of();
        }
        if (spellList.isPresent()) {
            return spellList.get().stream()
                    .map(id -> SpellRegistry.getSpell(SpellIds.normalize(id)))
                    .filter(SpellSelection::isValid)
                    .toList();
        }
        if (spellTag.isPresent()) {
            TagKey<AbstractSpell> tag = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, SpellIds.normalize(spellTag.get()));
            return SpellRegistry.REGISTRY.getTag(tag)
                    .map(set -> set.stream().map(Holder::value).filter(SpellSelection::isValid).toList())
                    .orElseGet(() -> {
                        IronsOrigins.LOGGER.warn("SpellSelection: unknown or unloaded spell tag {}", spellTag.get());
                        return List.of();
                    });
        }
        if (spellSchool.isPresent()) {
            SchoolType school = SchoolRegistry.getSchool(SpellIds.normalize(spellSchool.get()));
            return school == null ? List.of() : SpellRegistry.getSpellsForSchool(school).stream().filter(SpellSelection::isValid).toList();
        }
        return List.of();
    }

    private static List<Integer> levelsInRarityWindow(AbstractSpell spell, SpellRarity min, SpellRarity max) {
        List<Integer> levels = new ArrayList<>();
        for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
            int rarity = spell.getRarity(level).getValue();
            if (rarity >= min.getValue() && rarity <= max.getValue()) {
                levels.add(level);
            }
        }
        return levels;
    }

    private int pickLevel(List<Integer> levels, RandomSource random) {
        return switch (levelMode) {
            case LOWEST -> levels.getFirst();
            case HIGHEST -> levels.getLast();
            case RANDOM -> levels.get(random.nextInt(levels.size()));
        };
    }

    private static boolean isValid(@Nullable AbstractSpell spell) {
        return spell != null && spell != SpellRegistry.none();
    }

    @Override
    public String toString() {
        return "SpellSelection{spell=" + singleSpell + ", spells=" + spellList + ", spellTag=" + spellTag
                + ", spellSchool=" + spellSchool + ", minRarity=" + minRarity + ", maxRarity=" + maxRarity
                + ", levelMode=" + levelMode + ", powerLevel=" + powerLevel + ", levelBonus=" + levelBonus + '}';
    }
}
