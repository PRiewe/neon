# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Package Overview

The `neon.core` package is the heart of the game engine, containing the central orchestration logic, event system, and game state management.

## Key Classes

### Engine (`Engine.java`)
The central orchestrator class that:
- Initializes all subsystems: FileSystem, PhysicsSystem, ResourceManager, QuestTracker
- Sets up the GraalVM Polyglot JavaScript engine for scripting
- Manages the MBassador event bus for inter-component communication
- Provides static accessors to all major subsystems (getPlayer(), getAtlas(), getStore(), etc.)
- Runs on a dedicated thread via `Runnable.run()` which subscribes all handlers to the event bus

Note: The `Engine` class uses static fields extensively (marked with TODO to remove). When modifying, be aware that state is shared globally.

### Game (`Game.java`)
Holds the active game state:
- `UIDStore` - Central entity registry
- `Player` - Current player entity
- `Timer` - Game time tracking
- `Atlas` - Map/world management

Supports dependency injection constructor for testing: `Game(Player, Atlas, UIDStore)`

### GameLoader (`GameLoader.java`)
Handles game initialization via `@Handler` annotation on `loadGame(LoadEvent)`:
- `LoadEvent.Mode.NEW` → calls `initGame()` to create new game with player creation params
- `LoadEvent.Mode.LOAD` → calls `loadGame(String save)` to restore saved game

Key initialization sequence in `initGame()`:
1. Create Player from RCreature species
2. Call `engine.startGame(new Game(...))`
3. Apply birth sign powers/abilities
4. Initialize maps via `initMaps()`
5. Add starting items/spells from CGame config
6. Position player at start location

### GameSaver (`GameSaver.java`)
Persists game state, handles `SaveEvent` via event bus.

### Configuration (`Configuration.java`)
Property-based configuration with `getProperty()`/`setProperty()` methods.

### ScriptInterface (`ScriptInterface.java`)
Exposes entities to JavaScript scripts via `get(uid)` and `getPlayer()` methods.

## Event System (`neon.core.event`)

### TaskQueue (`TaskQueue.java`)
Deferred execution system for events:
- `add(String description, Runnable task)` - immediate execution
- `add(String script, int start, int period, int stop)` - timed/repeating scripts
- `tick()` - advances timer and executes due tasks
- Used for magic effects, scheduled events, and quest triggers

### Event Types
All extend `EventObject` and are posted via `Engine.post(event)`:

| Event | Purpose |
|-------|---------|
| `TurnEvent` | Game turn progression (has `time` and `start` flag) |
| `CombatEvent` | Combat resolution (attacker, defender, result, type) |
| `MagicEvent` | Spell casting with variants: OnCreature, OnPoint, OnSelf, ItemOnSelf, etc. |
| `SkillEvent` | Skill usage and checks |
| `LoadEvent` | Game loading (NEW or LOAD mode) |
| `SaveEvent` | Game saving |
| `UpdateEvent` | UI refresh triggers |
| `MessageEvent` | Game log messages |
| `DeathEvent` | Entity death |
| `StoreEvent` | Inventory/store transactions |
| `ScriptAction` | Wraps script execution as Runnable |
| `MagicTask` | Timed magic effect execution |

## Handler System (`neon.core.handlers`)

Handlers subscribe to the event bus with `@Handler` annotations. Registered in `Engine.run()`:

| Handler | Responsibility |
|---------|---------------|
| `TurnHandler` | Processes game turns, creature actions, region checks |
| `CombatHandler` | Resolves melee (`fight`), ranged (`shoot`), and thrown (`fling`) combat |
| `MagicHandler` | Routes magic effects to appropriate effect handlers, handles eat/drink |
| `InventoryHandler` | Item transactions and inventory management |
| `SkillHandler` | Skill checks, feat unlocking |
| `DeathHandler` | Entity death processing |
| `MotionHandler` | Entity movement |

### Adding a New Handler
1. Create class in `neon.core.handlers`
2. Add `@Handler` method(s) for specific event types
3. Register in `Engine.run()` via `bus.subscribe(new YourHandler())`

## Event Flow Pattern
```
User Action → UI State → Client Port → Engine receives event
    → Handler processes → Game state updated
    → Engine posts result event → Client Port → UI updates
```

## Common Patterns

### Posting Events
```java
Engine.post(new CombatEvent(attacker, defender));
```

### Handler Method
```java
@Handler
public void handleCombat(CombatEvent event) {
    // process combat
}
```

### Script Execution
```java
Engine.execute("player.heal(10);");  // JavaScript with access to player binding
```

## Testing Notes
- `Game` has a DI constructor for mocking Atlas and UIDStore
- Handlers can be unit tested by constructing events directly
- Use `Engine.getStore().createNewEntityUID()` for test entities
