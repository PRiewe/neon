# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Package Overview

The `neon.resources` package contains all game resource definitions - static data templates loaded from XML files that define creatures, items, spells, terrain, and other game content.

## Architecture

### Resource Hierarchy

All resources extend the abstract `Resource` base class which provides:
- `id`: Unique string identifier
- `path`: VFS location array
- Abstract `load()`/`unload()` methods for lazy loading

### Naming Conventions

| Prefix | Purpose | Examples |
|--------|---------|----------|
| `R` | Resource definition (template) | `RCreature`, `RSpell`, `RItem`, `RWeapon` |
| `L` | Leveled resource (level-gated spawn lists) | `LCreature`, `LItem`, `LSpell` |
| `C` | Configuration resource | `CGame`, `CClient`, `CServer` |

### Leveled Resources

`LCreature`, `LItem`, `LSpell` extend their base `R*` class and contain a `HashMap<String, Integer>` mapping resource IDs to minimum levels. Used for level-appropriate spawning.

### ResourceManager

Central registry using `Map<Class, Map<String, Resource>>` structure:
- `getResource(Class, String id)`: Retrieve by type and ID
- `getResources(Class)`: Get all resources of a type
- `addResource(Resource)` / `removeResource(Resource)`: Modify registry

### Builder System (`neon.resources.builder`)

Loads resources from XML using the builder pattern:

| Builder | Purpose |
|---------|---------|
| `ModLoader` | Loads complete mods, delegates to specialized init methods |
| `SingleBuilder` | Builds individual resources from XML elements |
| `IniBuilder` | Builds configuration from INI-style XML |

`ModLoader` methods correspond to resource types: `initCreatures()`, `initItems()`, `initMagic()`, `initQuests()`, etc.

### Quest Subpackage (`neon.resources.quest`)

Defines quest structure (not runtime tracking which is in `neon.narrative`):
- `RQuest`: Quest definition with stages and conversations
- `Stage`: Quest stage with objectives
- `Conversation`: NPC dialog structure
- `Topic`: Individual dialog topics

## XML Serialization

Resources implement `toElement()` returning JDOM `Element` for XML output. Constructor typically accepts `Element e, String... path` for loading from XML.

## Adding New Resource Types

1. Create `R<Name>` class extending `Resource`
2. Add constructor accepting `Element` for XML parsing
3. Add `toElement()` method for serialization
4. Add initialization method in `ModLoader` (e.g., `initNewType()`)
5. Register in appropriate XML files under `darkness/`
