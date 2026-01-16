# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Editor Package Overview

The `neon.editor` package provides a Swing-based visual editor for creating and modifying game content (mods, maps, quests, NPCs, items, etc.) for the Neon roguelike engine.

## Architecture

### Core Components

- **Editor**: Main entry point and Swing application frame. Manages the overall UI layout including menu bar, toolbars, terrain/object/resource panels, and the map editing area.
- **DataStore**: Central data repository that loads and manages all game resources (scripts, events, quests, spells, items, factions, terrain, themes, maps, creatures). Resources are loaded from XML files via `loadData()`.
- **ModFiler**: Handles loading and saving of game modules (mods). Manages both master modules and extension modules, including dependency resolution for extensions.

### Resource Hierarchy

The editor defines its own resource types in `neon.editor.resources`:
- **Instance**: Abstract base class for placed objects in maps (extends `Renderable`)
- **IPerson**, **IContainer**, **IDoor**, **IRegion**: Specific instance types for map placement
- **RMap**, **RZone**, **RFaction**: Editor-specific resource wrappers

### Editor Subsystems

**ObjectEditor Pattern** (`neon.editor.editors`):
All resource editors extend the abstract `ObjectEditor` class which provides:
- Standard dialog structure with Ok/Cancel/Apply buttons
- `load()` / `save()` abstract methods for data binding
- Common utilities like `getMaskFormatter()` for input formatting

Specialized editors: `CreatureEditor`, `SpellEditor`, `QuestEditor`, `ItemEditor`, `WeaponEditor`, `ArmorEditor`, `TerrainEditor`, `FactionEditor`, `NPCEditor`, etc.

**Map Editing** (`neon.editor.maps`):
- **MapEditor**: Central map editing controller with terrain painting and object placement
- **ZoneEditor**: Zone configuration and dungeon generation settings
- **MapDialog**, **LevelDialog**: Dialogs for map/level creation
- **EditablePane**: The actual map rendering and editing surface
- **UndoAction**: Undo/redo support for map edits

**Quest/Dialog Editing**:
- **QuestEditor**: Quest creation with conditions, variables, and dialog entries
- **DialogEditor**: Table-based dialog topic editor with preconditions and actions
- **ScriptEditor**: JavaScript script editing interface

### Data Flow

1. **Loading**: `ModFiler.load()` → mounts mod path → `DataStore.loadData()` → populates `ResourceManager`
2. **Editing**: User interacts with specialized editors → data held in editor fields
3. **Saving**: `ObjectEditor.save()` → `XMLBuilder.getResourceDoc()` → `ModFiler.save()` → XML files

### Key Integration Points

- **ResourceManager**: Shared with game engine (`Editor.resources` static field)
- **FileSystem**: Shared file system for reading/writing mod data (`Editor.files`)
- Uses JDOM2 for XML serialization via `XMLBuilder`. Maps use Jackson XML serialization via JacksonMapper.
- Bridge methods in DataStore and ModFiler provide JDOM loading without XMLTranslator (which has been removed)

## Adding New Resource Editors

1. Create a new editor class extending `ObjectEditor` in `neon.editor.editors`
2. Implement `load()` to populate UI from resource data
3. Implement `save()` to persist UI state back to the resource
4. Register the editor in `Editor.initResources()` via the resource tree listener

## Module Types

- **Master modules**: Standalone mods with `<master>` element in `main.xml`
- **Extension modules**: Depend on master modules; `ModFiler.isExtension()` checks for master dependencies
