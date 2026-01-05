# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Magic Package Overview

The `neon.magic` package implements the spell and effect system for the Neon roguelike engine. It uses a strategy pattern where each effect type has its own handler for processing spell mechanics.

## Core Classes

### Effect (Enum)
Defines all magic effects in the game. Each effect belongs to one of six magic schools (matching the `Skill` enum):
- **Alteration**: levitate, open, lock, disarm, leech effects
- **Restoration**: restore health/mana, cure effects
- **Illusion**: paralyze, blind, burden, calm, charm, silence
- **Conjuration**: elemental shields
- **Destruction**: drain effects, damage effects, elemental damage

Every effect has a **duration type**:
- `INSTANT` (0): One-time effect applied immediately
- `REPEAT` (1): Applied every round for multiple rounds
- `TIMED` (2): Constant effect that wears off after a duration

### Spell (Class)
Runtime instance of a spell being cast. Contains:
- `effect`: The Effect being applied
- `magnitude`: Strength of the spell (modified by caster)
- `caster` / `target`: Entity references
- `type`: SpellType (spell, disease, poison, curse, potion, scroll, enchantment)
- `script`: Optional JavaScript script to execute

### EffectHandler (Interface)
Strategy interface for processing spell effects. Each Effect has an associated handler. Methods:
- `addEffect(Spell)`: Apply initial spell effect
- `repeatEffect(Spell)`: Apply repeating effect each round
- `removeEffect(Spell)`: Clean up when spell expires
- `isWeaponEnchantment()` / `isClothingEnchantment()` / `onItem()`: Enchantment compatibility

### Handler Implementations

| Handler | Purpose | Effects |
|---------|---------|---------|
| `DamageHandler` | Direct damage (health, mana, elemental) | DAMAGE_*, FIRE/FROST/SHOCK_DAMAGE |
| `DrainHandler` | Temporary resource drain | DRAIN_HEALTH, DRAIN_MANA |
| `DrainStatHandler` | Stat drain (str, int, etc.) | DRAIN_STRENGTH, etc. |
| `DrainSkillHandler` | Skill drain | DRAIN_* skills |
| `RestoreHandler` | Restore health/mana | RESTORE_HEALTH, RESTORE_MANA |
| `CureHandler` | Remove ailments | CURE_*, LIFT_CURSE |
| `ShieldHandler` | Elemental resistance | *_SHIELD effects |
| `LockHandler` | Door/container manipulation | OPEN, LOCK, DISARM |
| `LeechHandler` | Transfer resources | LEECH_HEALTH, LEECH_MANA |
| `DefaultHandler` | No-op handler for scripted effects | SCRIPTED, status effects |

### SpellFactory (Static Factory)
Creates spell resources from IDs:
- `getSpell(id)`: Returns `RSpell`, handles leveled spell lists (`LSpell`)
- `getEnchantment(id)`: Returns `RSpell.Enchantment`

### MagicUtils (Static Utilities)
- `check(creature, spell)`: Skill check for casting
- `getMana(spell)`: Calculate mana cost based on size, range, duration
- `getLevel(spell)`: Spell level derived from mana cost
- `getCost(spell)`: Gold cost (for merchants)
- `cure(target, type)`: Remove all spells of given SpellType
- `equip(creature, item)` / `unequip(...)`: Apply/remove enchantment effects

## Integration with Engine

The `MagicHandler` in `neon.core.handlers` orchestrates spell casting:
1. Validates mana, range, skill, target
2. Creates `Spell` instance from `RSpell` resource
3. Calls the Effect's `EffectHandler.addEffect()`
4. For timed/repeating spells, adds to creature's active spell list
5. Returns result code (OK, MANA, SKILL, RANGE, etc.)

## Adding New Effects

1. Add enum constant to `Effect.java` with school, mana cost, name, handler, and duration type
2. If needed, create a new handler implementing `EffectHandler`
3. Define spell resources in XML (`darkness/spells.xml`)

## Key Patterns

- **Strategy Pattern**: Effects delegate behavior to handlers
- **Immutable Effects**: Effect enum is static; Spell instances hold runtime state
- **Resource Separation**: `RSpell` (resource definition) vs `Spell` (runtime instance)
