# Example powers

These aren't loaded by the mod. To try one, copy it into a datapack under `data/<namespace>/powers/` and add it to an origin.

| File                       | Shows                                                                                       |
|----------------------------|---------------------------------------------------------------------------------------------|
| `acid_orb_cantrip.json`    | A key-bound `cast_spell` with a mana cost, gated on `mana`.                                 |
| `wild_surge.json`          | `action_on_spell_cast` with a chance-gated random spell from a rarity window.               |
| `spellsword_strike.json`   | `action_on_spell_damage` with a cooldown bar, following up with a bi-entity `cast_spell`.   |
| `channeled_beam.json`      | A continuous spell that drains mana every second, without slowdown or animations.           |
| `eldritch_scholar.json`    | `learned_spells` for the eldritch school.                                                   |
| `arcane_ward.json`         | `prevent_death_with_mana` with a follow-up effect.                                          |
| `spell_thief.json`         | A key-bound `spell_thief` on the entity the player looks at.                                |
