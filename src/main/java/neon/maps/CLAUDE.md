# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Package Overview

The `neon.maps` package handles all map management, zone loading, and procedural dungeon generation for the Neon roguelike engine.

## Architecture

### Hierarchical Map Structure
```
Atlas (map cache/manager)
  └── Map (interface)
       ├── World (single-zone outdoor maps)
       └── Dungeon (multi-zone indoor maps)
            └── Zone (playable area with regions, creatures, items)
                 └── Region (terrain tiles with properties)
```

### Key Classes

- **Atlas**: Central manager for all loaded maps. Uses MVStore for disk-backed caching. Handles zone transitions when players enter doors.
- **Zone**: Represents a playable area containing regions (terrain), creatures, and items. Can be procedurally generated or loaded from XML.
- **Region**: A rectangular area with terrain type, rendering layer, and optional scripts.
- **MapLoader**: Loads maps from XML files, supports both static and themed (procedural) dungeons.

### Tile Constants (MapUtils)
Generator algorithms use these integer constants for tile types:
- `WALL (0)`, `FLOOR (1)`, `DOOR (2)`, `DOOR_CLOSED (3)`, `DOOR_LOCKED (4)`
- `CORRIDOR (5)`, `WALL_ROOM (6)`, `ENTRY (7)`, `CORNER (8)`, `TEMP (9)`

## Procedural Generation

### DungeonGenerator Flow
1. `generateTiles()` creates base terrain layout using algorithm from theme
2. `makeTerrain()` converts tile integers to terrain string IDs
3. `generateFeatures()` adds lakes, rivers, patches based on theme
4. `generateEngineContent()` converts terrain strings to Region/Entity objects
5. `generate()` places doors, connections, and quest objects

### Generator Types (neon.maps.generators)
- **MazeGenerator**: Maze patterns, squashed mazes for cave-like layouts
- **CaveGenerator**: Open cave generation with cellular automata
- **RoomGenerator**: Individual room shapes (rectangular, polygonal, cave-like)
- **ComplexGenerator**: BSP dungeons, sparse dungeons, packed room dungeons
- **TownGenerator**: Town layouts
- **WildernessGenerator**: Outdoor terrain with forests, ridges, swamps
- **BlocksGenerator**: Utility for placing rectangular features

### Theme Resources
- `RZoneTheme`: Defines zone generation parameters (dimensions, floor/wall tiles, features, creatures, items)
- `RDungeonTheme`: Defines dungeon structure (min/max zones, branching factor, zone types)
- `RRegionTheme`: Defines region randomization for wilderness areas

## Service Interfaces (neon.maps.services)

Dependency injection interfaces to decouple from Engine singleton:

| Interface | Purpose | Engine Adapter |
|-----------|---------|----------------|
| `EntityStore` | Entity CRUD + UID generation | `EngineEntityStore` |
| `ResourceProvider` | Resource lookup | `EngineResourceProvider` |
| `PhysicsManager` | Physics registration | `EnginePhysicsManager` |
| `QuestProvider` | Quest object requests | `EngineQuestProvider` |

**Pattern**: Constructors with `@Deprecated` annotation use Engine singleton for backward compatibility. New code should use constructors accepting service interfaces.

## Zone Activation

When switching zones:
1. `Atlas.setCurrentZone()` updates current zone index
2. `ZoneActivator.activateZone()` clears physics and registers new entities
3. If entering a random dungeon zone, `DungeonGenerator.generate()` creates content

## Map File Format

Maps are XML files in `darkness/maps/`. Two root element types:
- `<world>`: Single-zone outdoor map
- `<dungeon>`: Multi-zone indoor map with `<level>` children

Themed dungeons use `theme="..."` attribute to reference `RDungeonTheme` resources instead of explicit zone definitions.
