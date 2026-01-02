# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Neon is a roguelike game engine written in Java 1.7, consisting of:
- **neon**: The main game engine and editor
- **darkness**: A sample game built with the engine

## Build and Development Commands

### Building the Project
```bash
mvn clean compile
```

### Installing Dependencies
The project uses Maven for dependency management. Some dependencies (like JTexGen) require special setup:
```bash
./setup.sh
```

### Running Tests
```bash
mvn test
```

### Code Formatting
```bash
mvn com.spotify.fmt:fmt-maven-plugin:format
```

## Architecture

### Client-Server Communication
The engine uses a **LocalPort-based communication system** to decouple the game engine from the UI:
- `Engine` (server-side) handles game logic and runs on a separate thread
- `Client` (UI-side) manages user interface and rendering
- Communication happens through `LocalPort` instances that connect bidirectionally
- Messages are passed as events through the ports

Entry point in `Main.java`:
```java
LocalPort cPort = new LocalPort();
LocalPort sPort = new LocalPort();
cPort.connect(sPort);  // Bidirectional connection
sPort.connect(cPort);

Engine engine = new Engine(sPort);
Client client = new Client(cPort, version);
```

### Event System
The engine uses **MBassador** event bus for event-driven architecture:
- Event handlers are annotated with `@Handler`
- Events are posted to the bus and distributed to all registered handlers
- Key event types: `TurnEvent`, `CombatEvent`, `MagicEvent`, `SkillEvent`, `LoadEvent`, `SaveEvent`, `UpdateEvent`, `MessageEvent`
- Events flow through `TaskQueue` for deferred execution
- The event system integrates with the quest/narrative system through `EventAdapter`

### Component-Based Entity System
Entities use a **component-based architecture**:
- Base class: `Entity` with various components
- Components include: `RenderComponent`, `PhysicsComponent`, `Inventory`, `Stats`, `HealthComponent`, `ScriptComponent`, `ShapeComponent`, `Characteristics`, etc.
- Entity types: `Creature`, `Player`, `Item`, `Door`, `Container`, `Weapon`, `Armor`, `Clothing`
- Specialized creatures: `Hominid`, `Dragon`, `Daemon`, `Construct`
- Entity serialization handled by specialized serializers in `neon.entities.serialization`

### Core Systems
The `Engine` orchestrates several subsystems:
- **FileSystem** (`neon.systems.files`): Manages file I/O with XML/INI translators
- **PhysicsSystem** (`neon.systems.physics`): 2D physics using Phys2d library
- **ScriptInterface** (`neon.core.ScriptInterface`): JavaScript scripting support
- **QuestTracker** (`neon.narrative`): Quest and dialog management
- **ResourceManager** (`neon.resources`): Loads and manages game resources
- **Atlas** (`neon.maps`): Map and world management

### Resource System
Resources are defined in XML files and managed by `ResourceManager`:
- Resource types start with `R` prefix: `RCreature`, `RItem`, `RSpell`, `RQuest`, `RTerrain`, `RWeapon`, etc.
- Leveled resources start with `L` prefix: `LCreature`, `LItem`, `LSpell`
- Game/Client configuration: `CGame`, `CClient`, `CServer`
- Resources are built using the builder pattern (`neon.resources.builder`)
- Resource data stored in `darkness/` directory as XML files

### Map System
Maps use a hierarchical structure:
- **World** → **Region** → **Zone** → **Map**
- Dungeon generation: Multiple generator types (Maze, Room, Cave, Complex, Town, Wilderness)
- Spatial indexing: Multiple implementations (RTree, QuadTree, GridIndex) in `neon.util.spatial`
- Map loading/saving handled by `MapLoader`

### Magic System
Magic effects are handled through specialized handlers:
- Base: `EffectHandler` interface with concrete implementations
- Effect handlers: `DamageHandler`, `ShieldHandler`, `DrainHandler`, `RestoreHandler`, `CureHandler`, `LockHandler`, etc.
- Spells created by `SpellFactory`
- Magic events dispatched through `MagicHandler` which routes to appropriate effect handlers

### AI System
NPC behavior controlled by AI classes:
- `AI` interface with implementations: `BasicAI`, `ScheduleAI`, `GuardAI`
- `Behaviour` system for modular AI behaviors (e.g., `HuntBehaviour`)
- Pathfinding: `PathFinder` with A* implementation
- AI created by `AIFactory`

### Editor
The editor (`neon.editor.Editor`) provides tools for:
- Resource editing: Specialized editors for each resource type in `neon.editor.editors`
- Map editing: Visual map editor with terrain painting and object placement
- Quest/dialog editing: `QuestEditor`, `DialogEditor`
- Script editing: `ScriptEditor` with JavaScript support

## Key Patterns and Conventions

### Naming Conventions
- **Resources**: `R` prefix (e.g., `RCreature`, `RSpell`)
- **Leveled Resources**: `L` prefix (e.g., `LCreature`, `LSpell`)
- **Configuration**: `C` prefix (e.g., `CGame`, `CClient`)
- **Instances**: `I` prefix in editor resources (e.g., `IPerson`, `IContainer`)

### Event Flow
1. User action triggers UI state change
2. UI posts event to client port
3. Engine receives event through server port
4. Engine processes event, updates game state
5. Engine posts result events back through port
6. Client receives events and updates UI

### Adding New Features
When adding features that involve game state changes:
1. Define events in `neon.core.event`
2. Create handler in `neon.core.handlers` with `@Handler` methods
3. Register handler with the event bus in `Engine` constructor
4. Add UI state/dialog in `neon.ui.states` or `neon.ui.dialog`
5. Create resource types in `neon.resources` if needed
6. Add serialization logic if persisting new data

## Dependencies
Key external libraries:
- **Guava**: Enhanced Java collections
- **JDBM**: Disk-backed collections
- **JDOM 2**: XML reading/writing
- **JTexGen**: Procedural texture generation
- **MBassador**: Fast event bus
- **Phys2d**: 2D physics engine
- **TinyLAF**: Look-and-feel

## Data Files
Game content stored in `darkness/` directory:
- Maps: `darkness/maps/*.xml`
- Quests: `darkness/quests/*.xml`
- Resource definitions: `darkness/*.xml` (spells, factions, terrain, etc.)
- Scripts: `darkness/scripts/`
