# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## UI Package Overview

The `neon.ui` package implements the client-side user interface for the Neon roguelike engine. It is completely decoupled from the game engine through a port-based communication system.

## Architecture

### Client-Server Separation
- `Client` runs on the UI thread and communicates with `Engine` via `LocalPort`
- Uses MBassador event bus for receiving events from the engine
- Never directly accesses game state - all interactions happen through events

### State Machine Pattern
The UI uses a `FiniteStateMachine` to manage distinct interaction modes:

**Primary States:**
- `MainMenuState` - Entry point, game loading
- `GameState` - Main gameplay, handles turn events and physics
- `InventoryState` - Player inventory management
- `ContainerState` - Looting containers
- `JournalState` - Quest log and character stats
- `DialogState` - NPC conversations and services

**Sub-States (nested under GameState):**
- `MoveState` - Standard movement mode
- `AimState` - Targeting for ranged attacks/spells
- `DoorState` - Door interaction
- `LockState` - Lock picking
- `BumpState` - Collision handling with NPCs/objects

State transitions are triggered by `TransitionEvent` with string-based transition names (e.g., "start", "inventory", "dialog", "return").

### Graphics System (`neon.ui.graphics`)

**Rendering Pipeline:**
- `JVectorPane` - Main rendering component, handles zoom and camera
- `Scene` - Contains all renderable elements organized in `Layer`s
- `Layer` - Groups renderables by depth with spatial indexing
- `Renderable` - Interface for all drawable elements

**Shape Hierarchy (`neon.ui.graphics.shapes`):**
- `JVShape` - Abstract base extending `RenderComponent`
- Concrete shapes: `JVRectangle`, `JVEllipse`, `JVText`, `JVCompoundShape`

**Selection System:**
- `VectorSelectionListener` and `VectorSelectionEvent` for interactive selection
- `SelectionFilter` for filtering selectable elements

### Dialog System (`neon.ui.dialog`)
Modal dialogs for specific interactions:
- Trading: `TradeDialog`, `SpellTradeDialog`
- Crafting: `CrafterDialog`, `PotionDialog`, `SpellMakerDialog`, `EnchantDialog`
- Services: `TrainingDialog`, `RepairDialog`, `ChargeDialog`, `TattooDialog`, `RentalDialog`
- Utility: `NewGameDialog`, `LoadGameDialog`, `MapDialog`, `BookDialog`, `TravelDialog`, `OptionDialog`

### Console System (`neon.ui.console`)
JavaScript debug console:
- `JConsole` - Interactive console with command history
- `ConsoleInputStream`/`ConsoleOutputStream` - Stream redirection
- `CommandHistory` - Command recall functionality

## Key Patterns

### Event Handling
States implement `KeyListener` for keyboard input and post events to the bus:
```java
bus.post(new TransitionEvent("inventory"));
bus.post(new TurnEvent());
```

### UI Updates
The `BusAdapter` inner class in `Client` subscribes to engine events:
- `UpdateEvent` - Triggers UI refresh
- `MessageEvent` - Displays notifications
- `LoadEvent` - Handles game load completion
- `MagicEvent.Result` - Shows spell casting feedback

### Panel Architecture
- `GamePanel` - Main game view with stats overlay, HUD toggle, zoom controls
- `MapPanel` - World/area map display
- `DescriptionPanel` - Item/entity descriptions
- `InventoryCellRenderer` - Custom list rendering for inventory items

## Adding New Features

### New UI State
1. Create class extending appropriate base (implement `KeyListener`)
2. Add fields for `bus` (event bus) and `ui` (UserInterface)
3. Implement `enter()` and `exit()` for state lifecycle
4. Register transitions in `Client.initFSM()`

### New Dialog
1. Create class in `neon.ui.dialog`
2. Implement `show()` method that creates and displays JDialog
3. Handle keyboard/mouse input
4. Post events to engine via bus when actions complete

### New Renderable
1. Implement `Renderable` interface or extend `JVShape`
2. Implement `paint(Graphics2D, float zoom, boolean selected)`
3. Provide bounds via `getBounds()`
