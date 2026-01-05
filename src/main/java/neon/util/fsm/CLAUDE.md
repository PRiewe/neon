# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Package Overview

The `neon.util.fsm` package implements a hierarchical finite state machine (FSM) supporting:
- **Nested states**: States can have parent states forming a tree hierarchy
- **Orthogonal (parallel) states**: Multiple states can be active simultaneously
- **Local transitions**: Transitions that don't exit the containing state

## Architecture

### Class Hierarchy

```
State (base class)
  └── FiniteStateMachine (extends State, manages the machine)
```

### Core Components

| Class | Purpose |
|-------|---------|
| `FiniteStateMachine` | Main controller. Manages current states, transitions, and shared variables. Itself extends `State` so it's the root of the state tree. |
| `State` | Base class for all states. Override `enter(TransitionEvent)` and `exit(TransitionEvent)` for state-specific behavior. Delegates variable storage to parent via `getVariable`/`setVariable`. |
| `Transition` | Defines a transition from one state to another, triggered by a condition string. Can include an `Action` to execute and a `local` flag for local transitions. |
| `TransitionEvent` | Event that triggers transitions. Has a string ID (matched against transition conditions) and optional key-value parameters. |
| `Action` | Functional interface with `run(EventObject)` - executed when a transition occurs. |

### State Lifecycle

1. States form a tree rooted at `FiniteStateMachine`
2. `fsm.start(event)` activates initial states (set via `addStartStates()`)
3. `fsm.transition(event)` processes state changes based on registered transitions
4. On transition, the FSM:
   - Finds common ancestor between current and next states
   - Calls `exit()` on states being left (including substates)
   - Executes transition `Action` if present
   - Calls `enter()` on states being entered (including substates)
5. Newly entered states are "blocked" until the current transition cycle completes

### Key Implementation Details

- Transitions are keyed by `eventID + fromState` allowing event ID reuse across states
- `ConcurrentSkipListSet` maintains active states with custom comparator based on hierarchy depth
- Variables propagate through parent chain: child states delegate to `parent.getVariable()`

## Usage Pattern

From `Client.java`:

```java
// Create FSM and states (states get parent in constructor)
FiniteStateMachine fsm = new FiniteStateMachine();
GameState game = new GameState(fsm, bus, ui);  // parent = fsm
MoveState move = new MoveState(game, bus);     // parent = game (nested)

// Register start states
fsm.addStartStates(main, move);

// Define transitions (from, to, condition)
fsm.addTransition(new Transition(main, game, "start"));
fsm.addTransition(new Transition(game, inventory, "inventory"));
fsm.addTransition(new Transition(inventory, game, "cancel"));

// Start the machine
fsm.start(new TransitionEvent("start"));

// Trigger transitions via event bus
bus.publishAsync(new TransitionEvent("inventory"));
```

### Creating States

Extend `State` and override lifecycle methods:

```java
public class MyState extends State {
    public MyState(State parent, ...) {
        super(parent, "state-name");
    }
    
    @Override
    public void enter(TransitionEvent e) {
        // Called when entering this state
        // Access params: e.getParameter("key")
    }
    
    @Override
    public void exit(TransitionEvent e) {
        // Called when leaving this state
    }
}
```

### Transition with Action

```java
fsm.addTransition(new Transition(stateA, stateB, "trigger", 
    (event) -> { /* action code */ }));
```

### Local Transitions

Local transitions re-enter substates without exiting the containing state:

```java
fsm.addTransition(new Transition(stateA, stateB, "trigger", true)); // local=true
```
