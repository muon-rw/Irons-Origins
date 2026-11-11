# Iron's Origins

Apoli power, action, and condition types for Iron's Spells 'n Spellbooks, for NeoForge 1.21.1 using Overgrown's Origins/Apoli. 

This mod ships no powers of its own. See [`docs/examples`](docs/examples) for sample powers you can copy into a datapack.

Wherever a spell or school id appears, a bare id like `"fireball"` resolves under `irons_spellbooks`.

## Documentation
*This section will be moved into a more accessible format soon*


## Spell entries

`learned_spells` and `spell_immunity` take an `entries` list of strings. Each entry matches:

| Form            | Matches                      |
|-----------------|------------------------------|
| `"spell_id"`    | that spell                   |
| `"#spell_tag"`  | every spell in the spell tag |
| `"@school_id"`  | every spell in the school    |

## Casting spells

Every action and power below that casts a spell goes through the same pipeline, and accepts these optional fields:

| Field               | Default  | Description                                                                                                                         |
|---------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------|
| `cast_time`         | spell's  | Cast time in ticks. `0` resolves the spell immediately. Instant spells default to `0`.                                              |
| `mana_cost`         | none     | Mana charged when the cast starts (players only). Without it, the cast is free. Recasts are always free, as in Iron's.               |
| `continuous_cost`   | `false`  | Charge `mana_cost` again every `cost_interval` ticks while the cast lasts. The cast is cancelled when the player runs out.           |
| `cost_interval`     | `20`     | Ticks between continuous charges.                                                                                                   |
| `interrupt_mode`    | `cancel` | What to do when the caster is already casting: `cancel` the current cast, `force_complete` it, or `fail` (skip the new cast).        |
| `trigger_cooldown`  | `false`  | Put the spell on Iron's cooldown. Off by default, since the power usually owns the cooldown; Iron's would also lock the spellbook copy. |
| `player_animations` | `true`   | Play Iron's cast start and finish animations.                                                                                       |
| `casting_slowdown`  | `true`   | Apply Iron's movement slowdown while casting.                                                                                       |

All of these casts use Iron's `command` cast source, skip Iron's learning requirement, and leave the player free to attack and use items while the cast runs.

Casting the spell that is already being cast cancels it, so a key-bound `cast_spell` toggles a long spell off. `force_complete` can't resolve a continuous spell early, so it skips the new cast instead.

If an action asks for a cast while the caster is in the middle of resolving another spell (for example, from `action_on_spell_damage`), we start the cast at the end of the tick, after Iron's has finished with the first spell.

### Spell selection

The `spell` field of `cast_spell` takes either a spell id with an optional sibling `power_level`:

```json
{ "type": "irons_origins:cast_spell", "spell": "fireball", "power_level": 3 }
```

or an object with exactly one of `spell`, `spells`, `spell_tag`, or `spell_school`. We pick one of the matching spells at random.

| Field          | Default  | Description                                                                                     |
|----------------|----------|-------------------------------------------------------------------------------------------------|
| `power_level`  | `1`      | Spell level, unless a rarity window is set.                                                     |
| `min_rarity`   | none     | With `max_rarity`, limits candidates to spells with a level in this rarity window.              |
| `max_rarity`   | none     |                                                                                                 |
| `level_mode`   | `lowest` | Which level in the rarity window to cast at: `lowest`, `highest`, or `random`.                  |
| `level_bonus`  | `0`      | Added to the picked level. Like Iron's own level bonuses, this may exceed the spell's max level. |

### Cast filters

`action_on_spell_cast`, `recast_spell`, and `action_on_spell_damage` take a filter object. Every field present must match.

| Field                          | Description                                                                                                |
|--------------------------------|------------------------------------------------------------------------------------------------------------|
| `spell` / `spells`             | Spell id, or a list of spell ids.                                                                          |
| `spell_tag`                    | Spell tag id.                                                                                              |
| `spell_school`                 | School id.                                                                                                 |
| `cast_type` / `cast_types`     | `instant`, `long`, `continuous`, `none`.                                                                   |
| `cast_source` / `cast_sources` | `spellbook`, `scroll`, `sword`, `mob`, `command`, `none`. Defaults to everything except `command`, so casts from these powers don't retrigger them. Include `command` only when the action is chance-gated, or the power will loop. |

`action_on_spell_damage` ignores cast type and source.

## Entity actions

### `irons_origins:cast_spell`

Casts a spell from the entity in the direction it looks. If the spell targets an entity, we bind the entity the caster looks at.

| Field              | Default | Description                                                                                                  |
|--------------------|---------|--------------------------------------------------------------------------------------------------------------|
| `spell`            |         | [Spell selection](#spell-selection).                                                                         |
| `raycast_distance` | `64`    | How far to look for an entity target.                                                                        |
| `success_action`   | none    | Entity action, run when the cast starts.                                                                     |
| `finish_action`    | none    | Entity action, run once the cast and any recast sequence it opened are over.                                 |
| ...                |         | [Cast pipeline fields](#casting-spells).                                                                     |

Neither follow-up action runs when the cast only used up an open recast.

### `irons_origins:restore_mana`

Restores `amount` mana, up to the player's max.

### `irons_origins:reset_spell_cooldowns`

Clears all of the player's Iron's spell cooldowns.

### `irons_origins:reduce_spell_cooldowns`

Reduces each of the player's active spell cooldowns by `ticks`.

## Bi-entity actions

### `irons_origins:cast_spell`

Casts a spell from the actor at the target. For the duration of the cast, we point the actor's facing at the target and bind the target as the spell's target, so look-aimed projectiles and entity-targeted spells both hit it. Takes the same fields as the entity version, minus `raycast_distance`; `success_action` and `finish_action` are bi-entity actions.

### `irons_origins:spell_thief`

The actor steals a spell from the target and casts it back at them. If the target is casting, we cancel that cast and steal it; otherwise we steal the last spell they finished casting in the past 5 minutes. Iron's `CounterSpellEvent` can veto the theft.

| Field                     | Default | Description                                                  |
|---------------------------|---------|--------------------------------------------------------------|
| `trigger_target_cooldown` | `true`  | Put the stolen spell on cooldown for the target.             |
| `target_mana_cost`        | none    | Mana drained from the target (players only).                 |
| ...                       |         | [Cast pipeline fields](#casting-spells), for the actor's cast. |

With `interrupt_mode: fail`, a busy actor leaves the target untouched.

## Entity conditions

| Type                             | Fields                       | True when                                                    |
|----------------------------------|------------------------------|--------------------------------------------------------------|
| `irons_origins:mana`             | `comparison`, `compare_to`   | The player's current mana compares true. Non-players have 0. |
| `irons_origins:has_spell_recast` | `spell`                      | The player has an open recast for the spell (server only).   |
| `irons_origins:is_rooted`        |                              | Iron's Root spell holds the entity.                          |

## Item conditions

| Type                            | True when                                      |
|---------------------------------|------------------------------------------------|
| `irons_origins:is_casting_item` | The item is an Iron's staff or other casting implement. |

## Power types

### `irons_origins:action_on_spell_cast`

Runs `entity_action` on the caster after one of their casts ends server-side, including casts the player released or cancelled early. Filtered by `cast_conditions` ([cast filter](#cast-filters)).

### `irons_origins:recast_spell`

After one of the player's casts ends, casts the same spell at the same level again.

| Field                       | Default | Description                                                           |
|-----------------------------|---------|-----------------------------------------------------------------------|
| `cast_conditions`           | any     | [Cast filter](#cast-filters).                                         |
| `skip_if_spell_has_recasts` | `true`  | Skip spells that open their own Iron's recasts.                       |
| `delay_ticks`               | `5`     | Ticks to wait. `0` recasts at the end of the current tick.            |
| `raycast_distance`          | `64`    | As in `cast_spell`.                                                   |
| `entity_action`             | none    | Run on the player when the recast starts.                             |
| ...                         |         | [Cast pipeline fields](#casting-spells).                              |

### `irons_origins:action_on_spell_damage`

Runs `bientity_action` (actor: the player whose spell dealt the damage, target: the entity hurt) after Iron's spell damage reaches the target's health.

| Field                | Default | Description                                                     |
|----------------------|---------|-----------------------------------------------------------------|
| `bientity_action`    |         | Bi-entity action.                                               |
| `bientity_condition` | none    | Bi-entity condition.                                            |
| `spell_conditions`   | any     | [Cast filter](#cast-filters); only the spell fields apply.      |
| `cooldown`           | `1`     | Ticks, or an Apoli expression.                                  |
| `hud_render`         | hidden  | Apoli HUD render for the cooldown.                              |

The action can't trigger the power again, and we clear the target's damage immunity frames while it runs, so bonus damage in the action lands.

### `irons_origins:learned_spells`

While active, Iron's treats the [`entries`](#spell-entries) as learned, for spells that require learning (such as the eldritch school). The player's stored learned spells are left alone, so the effect ends with the power.

### `irons_origins:spell_immunity`

Makes the entity an invalid target for the [`entries`](#spell-entries) wherever Iron's picks a spell target through its standard targeting helper (Root, Ray of Sickness, and similar). Those casts fail with "No valid target".

### `irons_origins:prevent_death_with_mana`

When lethal damage would kill the player, they survive at half a heart by paying `lethal damage * mana_multiplier` mana. If paying would leave them at 0 mana or less, they die as usual.

| Field              | Default | Description                                  |
|--------------------|---------|----------------------------------------------|
| `mana_multiplier`  | `4.0`   |                                              |
| `damage_condition` | none    | Only prevent deaths from matching damage.    |
| `entity_action`    | none    | Run on the player after they survive.        |

## Other integration

We apply the caster's `apoli:modify_healing` to Iron's heals cast on other entities. Apoli on its own only applies the recipient's.
