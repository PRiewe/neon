# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Neon is a roguelike game engine written in Java 21, consisting of:
- **neon**: The main game engine and editor
- **darkness**: A sample game built with the engine

The project has been migrated from Java 1.7 to Java 21, with key changes including:
- Upgrading from Nashorn to GraalVM for JavaScript scripting
- Migrating from MapDB/JDBM to H2 MVStore for persistent storage
- Adopting Google Java Format for code formatting

## Build and Development Commands

### Building the Project
```bash
mvn clean compile
```

### Packaging
Create an executable JAR with all dependencies:
```bash
mvn package
```
This creates `target/neon-<version>-jar-with-dependencies.jar` which can be run with:
```bash
java -jar target/neon-<version>-jar-with-dependencies.jar
```

### Running the Application
After packaging, the JAR can be run directly. The application will start with a main menu where you can:
- Start a new game using the "darkness" sample game
- Load a saved game
- Open the editor to create/modify game content

The main entry point is `neon.Main`, which initializes the Engine and Client with bidirectional LocalPort communication.

### Running Tests
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ClassName

# Run a specific test method
mvn test -Dtest=ClassName#methodName
```

### Code Formatting
The project uses Google Java Format via the `fmt-maven-plugin`:
```bash
# Format all source files (runs automatically during build)
mvn fmt:format

# Check formatting without making changes
mvn fmt:check
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
- Key event types: `TurnEvent`, `CombatEvent`, `MagicEvent`, `SkillEvent`, `LoadEvent`, `SaveEvent`, `UpdateEvent`, `MessageEvent`, `DeathEvent`, `StoreEvent`
- Events flow through `TaskQueue` for deferred execution
- The event system integrates with the quest/narrative system through `EventAdapter`

Handler classes in `neon.core.handlers`:
- **TurnHandler**: Processes game turns and creature actions
- **CombatHandler**: Manages combat resolution
- **MagicHandler**: Routes magic effects to appropriate effect handlers
- **InventoryHandler**: Manages item transactions
- **SkillHandler**: Handles skill checks and applications
- **DeathHandler**: Processes entity death
- **MotionHandler**: Handles entity movement

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
- **ScriptInterface** (`neon.core.ScriptInterface`): JavaScript scripting support using GraalVM Polyglot
- **QuestTracker** (`neon.narrative`): Quest and dialog management
- **ResourceManager** (`neon.resources`): Loads and manages game resources
- **Atlas** (`neon.maps`): Map and world management
- **TaskQueue** (`neon.core.TaskQueue`): Deferred execution of events
- **UIDStore** (`neon.entities.UIDStore`): Central registry for all game entities

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

### UI System
The client uses a state-based UI architecture (`neon.ui.states`):
- **GameState**: Main game loop and rendering
- **MainMenuState**: Main menu navigation
- **DialogState**: NPC conversation and quest dialogs
- **InventoryState**: Player inventory management
- **ContainerState**: Container and loot management
- **JournalState**: Quest journal and character stats
- **AimState**: Targeting for ranged attacks and spells
- **MoveState**, **BumpState**, **DoorState**, **LockState**: Various interaction states

UI states handle user input and post events to the client port for the engine to process.

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
- **MapDb**: Disk-backed collections
- **JDBM**: Java database for persistent collections
- **JDOM 2**: XML reading/writing
- **JTexGen**: Procedural texture generation
- **MBassador**: Fast event bus for event-driven architecture
- **Phys2d**: 2D physics engine
- **TinyLAF**: Look-and-feel
- **GraalVM Polyglot/JS**: JavaScript scripting engine (replaces Nashorn)

## Data Files
Game content stored in `darkness/` directory:
- Maps: `darkness/maps/*.xml`
- Quests: `darkness/quests/*.xml`
- Resource definitions: `darkness/*.xml` (spells, factions, terrain, etc.)
- Scripts: `darkness/scripts/` (JavaScript files executed by GraalVM)

Configuration stored in `neon.ini.xml` at the project root.

## Scripting System
The engine uses **GraalVM Polyglot** for JavaScript scripting:
- Script engine initialized in `Engine` constructor with host access enabled
- Scripts can access Java classes and engine components through `ScriptInterface`
- Script context provides access to entities via `get(uid)` and player via `getPlayer()`
- Scripts are loaded and executed through `Engine.execute()`
- Located in `darkness/scripts/` directory

## Java 21 Migration Notes
The project has been migrated from Java 1.7 to Java 21:
- **Scripting engine**: Migrated from Nashorn (removed in Java 15+) to GraalVM Polyglot
- **Compiler**: Now uses Java 21 source/target with `maven.compiler.source/target` set to 21
- **Storage**: Migrated from MapDB/JDBM to H2 MVStore for persistent collections
- **Formatting**: Google Java Format automatically applied during build
- Some legacy code may still exist from the Java 1.7 era and should be modernized as needed

## Testing
The project uses JUnit 5 (Jupiter) for testing:
- Test files are located in `src/test/java/`
- Uses Mockito for mocking (version 5.11.0)
- XMLUnit for XML comparison testing
- Integration tests exist for critical systems like serialization (`DataStoreIntegrationTest`)
