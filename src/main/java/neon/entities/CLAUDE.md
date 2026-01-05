# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Package Overview

The `neon.entities` package implements a **component-based entity system** for the Neon roguelike engine. All game objects (creatures, items, doors, containers) are entities composed of reusable components.

## Architecture

### Entity Hierarchy

```
Entity (abstract base)
├── Creature
│   ├── Hominid (humanoids, goblins)
│   ├── Dragon
│   ├── Daemon
│   ├── Construct
│   └── Player
├── Item
│   ├── Weapon
│   ├── Armor
│   ├── Clothing
│   ├── Container
│   ├── Door
│   └── Inner classes: Light, Aid, Food, Coin, Potion, Scroll, Book
```

### Component System

`Entity` uses Guava's `ClassToInstanceMap<Component>` to store components. All components implement the `Component` interface (which extends `Serializable`).

**Core components** (in `components/` subpackage):
- `ShapeComponent` - position and bounds (always present as `Entity.bounds`)
- `PhysicsComponent` - physics simulation data
- `RenderComponent` - visual representation (specialized: `CreatureRenderComponent`, `ItemRenderComponent`, `DoorRenderComponent`, `PlayerRenderComponent`)
- `ScriptComponent` - attached JavaScript scripts
- `Inventory` - items and equipped slots
- `Stats` - attribute modifiers (STR, DEX, CON, INT, WIS, CHA, SPD)
- `HealthComponent` - health pool management
- `Characteristics` - base attributes for creatures
- `FactionComponent` - faction memberships
- `Animus` - magic/mana component
- `Enchantment` - magical properties on items
- `Lock` - lockable state for doors/containers
- `Portal` - map transition data
- `Trap` - trap mechanics

### UID System

`UIDStore` is the central registry for all entities and maps. Key concepts:
- **Positive UIDs**: entities from mod resources
- **Negative UIDs**: randomly generated entities
- **DUMMY (0)**: represents non-existent entities
- Uses H2 MVStore for persistence
- Composite UIDs: `getObjectUID(map, object)` combines map and object UIDs; `getMapUID(mod, map)` combines mod and map UIDs

### Entity Creation

`EntityFactory` creates all entities:
- `getItem(id, x, y, uid)` - creates items (handles `LItem` leveled lists)
- `getCreature(id, x, y, uid)` - creates creatures (handles `LCreature` leveled lists and `RPerson` NPCs)
- Uses `AIFactory` to assign AI behaviors to creatures

### Property Enums

The `property/` subpackage contains enums used across the entity system:
- `Attribute` - STR, DEX, CON, INT, WIS, CHA
- `Skill` - character skills with associated attribute
- `Slot` - equipment slots
- `Condition` - status effects
- `Damage` - damage types
- `Trait`, `Feat`, `Ability` - creature capabilities
- `Gender`, `Habitat`, `Subtype` - creature classification

### Serialization

The `serialization/` subpackage handles entity persistence:
- `EntitySerializer` - dispatches to appropriate type-specific serializer
- `ItemSerializer` - item serialization
- `CreatureSerializer` - creature serialization

Entities use `DataInput`/`DataOutput` for binary serialization with a type discriminator prefix ("item" or "creature").

## Key Patterns

### Accessing Components

```java
// Direct accessor methods for common components
creature.getHealthComponent()
creature.getInventoryComponent()
creature.getStatsComponent()

// Generic component access via Entity
entity.getPhysicsComponent()
entity.getRenderComponent()
entity.getScriptComponent()
```

### Adding New Entity Types

1. Extend `Entity`, `Creature`, or `Item` as appropriate
2. Add construction logic in `EntityFactory.getItem()` or `EntityFactory.getCreature()`
3. Add serialization support in the appropriate serializer
4. Define resource type in `neon.resources` if needed

### Adding New Components

1. Implement `Component` interface
2. Add to entity's `components` map in constructor
3. Add accessor method if frequently used
4. Handle in serialization if component needs persistence
