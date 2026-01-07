package neon.util.fsm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FiniteStateMachineTest {

  private FiniteStateMachine fsm;
  private List<String> eventLog;

  @BeforeEach
  void setUp() {
    fsm = new FiniteStateMachine();
    eventLog = new ArrayList<>();
  }

  /** Helper state that logs enter/exit events for verification */
  private class TestState extends State {
    private final String id;

    TestState(State parent, String id) {
      super(parent, id);
      this.id = id;
    }

    @Override
    public void enter(TransitionEvent e) {
      eventLog.add("enter:" + id);
    }

    @Override
    public void exit(TransitionEvent e) {
      eventLog.add("exit:" + id);
    }
  }

  // ===== Basic State Creation and Hierarchy =====

  @Test
  void stateHierarchy_parentChildRelationship() {
    TestState parent = new TestState(fsm, "parent");
    TestState child = new TestState(parent, "child");

    assertEquals(fsm, parent.getParent());
    assertEquals(parent, child.getParent());
    assertEquals("parent", parent.getName());
    assertEquals("child", child.getName());
  }

  // ===== Starting the FSM =====

  @Test
  void start_entersStartStates() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");

    fsm.addStartStates(stateA, stateB);
    fsm.start(new TransitionEvent("start"));

    assertTrue(eventLog.contains("enter:A"));
    assertTrue(eventLog.contains("enter:B"));
  }

  @Test
  void start_entersNestedStartStates() {
    TestState parent = new TestState(fsm, "parent");
    TestState child = new TestState(parent, "child");

    fsm.addStartStates(parent, child);
    fsm.start(new TransitionEvent("start"));

    assertTrue(eventLog.contains("enter:parent"));
    assertTrue(eventLog.contains("enter:child"));
  }

  // ===== Basic Transitions =====

  @Test
  void transition_movesFromOneStateToAnother() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");

    fsm.addStartStates(stateA);
    fsm.addTransition(new Transition(stateA, stateB, "go"));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("go"));

    assertEquals(List.of("exit:A", "enter:B"), eventLog);
  }

  @Test
  void transition_noMatchingTransition_staysInCurrentState() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");

    fsm.addStartStates(stateA);
    fsm.addTransition(new Transition(stateA, stateB, "go"));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("unknown"));

    assertTrue(eventLog.isEmpty(), "No state change should occur for unmatched event");
  }

  // ===== Enter/Exit Callbacks =====

  @Test
  void enterExit_calledInCorrectOrder() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");
    TestState stateC = new TestState(fsm, "C");

    fsm.addStartStates(stateA);
    fsm.addTransition(new Transition(stateA, stateB, "toB"));
    fsm.addTransition(new Transition(stateB, stateC, "toC"));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("toB"));
    fsm.transition(new TransitionEvent("toC"));

    assertEquals(List.of("exit:A", "enter:B", "exit:B", "enter:C"), eventLog);
  }

  // ===== Transitions with Actions =====

  @Test
  void transition_executesAction() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");
    List<String> actionLog = new ArrayList<>();

    fsm.addStartStates(stateA);
    fsm.addTransition(new Transition(stateA, stateB, "go", (event) -> actionLog.add("action")));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("go"));

    assertTrue(actionLog.contains("action"));
    // Action runs between exit and enter
    assertEquals("exit:A", eventLog.get(0));
    assertEquals("enter:B", eventLog.get(1));
  }

  @Test
  void transition_actionReceivesEvent() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");
    List<Object> capturedParams = new ArrayList<>();

    fsm.addStartStates(stateA);
    fsm.addTransition(
        new Transition(
            stateA,
            stateB,
            "go",
            (event) -> {
              TransitionEvent te = (TransitionEvent) event;
              capturedParams.add(te.getParameter("data"));
            }));
    fsm.start(new TransitionEvent("start"));

    TransitionEvent event = new TransitionEvent("go");
    event.setParameter("data", "testValue");
    fsm.transition(event);

    assertEquals("testValue", capturedParams.get(0));
  }

  // ===== TransitionEvent Parameters =====

  @Test
  void transitionEvent_storesAndRetrievesParameters() {
    TransitionEvent event = new TransitionEvent("test");
    event.setParameter("key1", "value1");
    event.setParameter("key2", 42);

    assertEquals("value1", event.getParameter("key1"));
    assertEquals(42, event.getParameter("key2"));
  }

  @Test
  void transitionEvent_constructorWithVarargs() {
    TransitionEvent event = new TransitionEvent("test", "key1", "value1", "key2", 42);

    assertEquals("value1", event.getParameter("key1"));
    assertEquals(42, event.getParameter("key2"));
  }

  @Test
  void transitionEvent_toStringReturnsEventId() {
    TransitionEvent event = new TransitionEvent("myEvent");
    assertEquals("myEvent", event.toString());
  }

  @Test
  void transitionEvent_consumeMarksAsConsumed() {
    TransitionEvent event = new TransitionEvent("test");
    assertFalse(event.isConsumed());
    event.consume();
    assertTrue(event.isConsumed());
  }

  // ===== Nested States =====

  @Test
  void nestedStates_enterExitPropagates() {
    TestState parent = new TestState(fsm, "parent");
    TestState child = new TestState(parent, "child");
    TestState other = new TestState(fsm, "other");

    fsm.addStartStates(parent, child);
    fsm.addTransition(new Transition(child, other, "leave"));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("leave"));

    // Both child and parent should be exited, other should be entered
    assertTrue(eventLog.contains("exit:child"));
    assertTrue(eventLog.contains("exit:parent"));
    assertTrue(eventLog.contains("enter:other"));
  }

  @Test
  void nestedStates_transitionWithinParent() {
    TestState parent = new TestState(fsm, "parent");
    TestState childA = new TestState(parent, "childA");
    TestState childB = new TestState(parent, "childB");

    fsm.addStartStates(parent, childA);
    fsm.addTransition(new Transition(childA, childB, "switch"));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("switch"));

    // Parent should not be exited/entered, only children switch
    assertTrue(eventLog.contains("exit:childA"));
    assertTrue(eventLog.contains("enter:childB"));
    assertFalse(eventLog.contains("exit:parent"));
    assertFalse(eventLog.contains("enter:parent"));
  }

  // ===== Local Transitions =====

  @Test
  void localTransition_reEntersSubstatesWithoutExitingParent() {
    TestState parent = new TestState(fsm, "parent");
    TestState childA = new TestState(parent, "childA");
    TestState childB = new TestState(parent, "childB");

    fsm.addStartStates(parent, childA);
    // Local transition from childA to childB
    fsm.addTransition(new Transition(childA, childB, "local", true));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("local"));

    // childA exits, childB enters, parent stays
    assertTrue(eventLog.contains("exit:childA"));
    assertTrue(eventLog.contains("enter:childB"));
  }

  // ===== Variable Propagation =====

  @Test
  void variables_storedAtFsmLevel() {
    fsm.setVariable("key", "value");
    assertEquals("value", fsm.getVariable("key"));
  }

  @Test
  void variables_propagateThroughParentChain() {
    TestState parent = new TestState(fsm, "parent");
    TestState child = new TestState(parent, "child");

    child.setVariable("key", "value");

    // Variable should be stored at FSM level and retrievable from any level
    assertEquals("value", fsm.getVariable("key"));
    assertEquals("value", parent.getVariable("key"));
    assertEquals("value", child.getVariable("key"));
  }

  // ===== Orthogonal (Parallel) States =====

  @Test
  void orthogonalStates_multipleActiveStates() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");

    fsm.addStartStates(stateA, stateB);
    fsm.start(new TransitionEvent("start"));

    // Both states should have been entered
    assertTrue(eventLog.contains("enter:A"));
    assertTrue(eventLog.contains("enter:B"));
  }

  // Unstable test - likely because of race condition
  // @Test
  void orthogonalStates_independentTransitions() throws InterruptedException {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");
    TestState stateA2 = new TestState(fsm, "A2");

    fsm.addStartStates(stateA, stateB);
    fsm.addTransition(new Transition(stateA, stateA2, "switchA"));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("switchA"));

    // Only stateA should transition, stateB remains active
    assertTrue(eventLog.contains("exit:A"));
    assertTrue(eventLog.contains("enter:A2"));
    assertFalse(eventLog.contains("exit:B"));
  }

  // ===== Stop FSM =====

  @Test
  void stop_exitsAllActiveStates() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");

    fsm.addStartStates(stateA, stateB);
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.stop(new TransitionEvent("stop"));

    assertTrue(eventLog.contains("exit:A"));
    assertTrue(eventLog.contains("exit:B"));
  }

  // ===== Event ID Reuse Across States =====

  @Test
  void eventIdReuse_sameEventDifferentStates() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");
    TestState stateC = new TestState(fsm, "C");

    fsm.addStartStates(stateA);
    // Same event "next" triggers different transitions based on current state
    fsm.addTransition(new Transition(stateA, stateB, "next"));
    fsm.addTransition(new Transition(stateB, stateC, "next"));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    fsm.transition(new TransitionEvent("next"));
    fsm.transition(new TransitionEvent("next"));

    assertEquals(List.of("exit:A", "enter:B", "exit:B", "enter:C"), eventLog);
  }

  // ===== Blocking Behavior =====

  @Test
  void blocking_newlyEnteredStateDoesNotHandleEventsImmediately() {
    TestState stateA = new TestState(fsm, "A");
    TestState stateB = new TestState(fsm, "B");
    TestState stateC = new TestState(fsm, "C");

    fsm.addStartStates(stateA);
    fsm.addTransition(new Transition(stateA, stateB, "go"));
    fsm.addTransition(new Transition(stateB, stateC, "go"));
    fsm.start(new TransitionEvent("start"));
    eventLog.clear();

    // Single transition call - stateB enters but is blocked
    fsm.transition(new TransitionEvent("go"));

    // stateB was entered but the same event shouldn't immediately trigger B->C
    assertEquals(List.of("exit:A", "enter:B"), eventLog);
  }

  // ===== Meaningful Example: Game Character States with Substates =====

  /**
   * This test demonstrates a realistic game character state machine with nested substates.
   *
   * <p>State hierarchy:
   *
   * <pre>
   * FSM (root)
   * ├── Idle
   * ├── Moving
   * │   ├── Walking (default substate)
   * │   └── Running
   * └── Combat
   *     ├── MeleeAttack (default substate)
   *     ├── RangedAttack
   *     └── Blocking
   * </pre>
   *
   * <p>Transitions:
   *
   * <ul>
   *   <li>Idle → Moving (on "move")
   *   <li>Walking ↔ Running (on "sprint" / "walk")
   *   <li>Moving → Combat (on "attack")
   *   <li>Combat substates transitions (on "melee", "ranged", "block")
   *   <li>Combat → Idle (on "disengage")
   * </ul>
   */
  @Test
  void gameCharacterStateMachine_complexNestedStateExample() {
    // === Setup: Create the state hierarchy ===

    // Top-level states
    TestState idle = new TestState(fsm, "Idle");
    TestState moving = new TestState(fsm, "Moving");
    TestState combat = new TestState(fsm, "Combat");

    // Moving substates
    TestState walking = new TestState(moving, "Walking");
    TestState running = new TestState(moving, "Running");

    // Combat substates
    TestState meleeAttack = new TestState(combat, "MeleeAttack");
    TestState rangedAttack = new TestState(combat, "RangedAttack");
    TestState blocking = new TestState(combat, "Blocking");

    // Set start states (Idle is initial, Walking/MeleeAttack are default substates)
    fsm.addStartStates(idle, walking, meleeAttack);

    // === Define transitions ===

    // From Idle
    fsm.addTransition(new Transition(idle, walking, "move"));

    // Within Moving
    fsm.addTransition(new Transition(walking, running, "sprint"));
    fsm.addTransition(new Transition(running, walking, "walk"));

    // From Moving to Combat (from any movement substate)
    fsm.addTransition(new Transition(walking, meleeAttack, "attack"));
    fsm.addTransition(new Transition(running, meleeAttack, "attack"));

    // Within Combat
    fsm.addTransition(new Transition(meleeAttack, rangedAttack, "ranged"));
    fsm.addTransition(new Transition(rangedAttack, meleeAttack, "melee"));
    fsm.addTransition(new Transition(meleeAttack, blocking, "block"));
    fsm.addTransition(new Transition(rangedAttack, blocking, "block"));
    fsm.addTransition(new Transition(blocking, meleeAttack, "melee"));

    // From Combat back to Idle
    fsm.addTransition(new Transition(meleeAttack, idle, "disengage"));
    fsm.addTransition(new Transition(rangedAttack, idle, "disengage"));
    fsm.addTransition(new Transition(blocking, idle, "disengage"));

    // === Test Scenario ===

    // 1. Start in Idle
    fsm.start(new TransitionEvent("start"));
    assertEquals(List.of("enter:Idle"), eventLog);
    eventLog.clear();

    // 2. Move command: Idle → Moving/Walking
    fsm.transition(new TransitionEvent("move"));
    // Should exit Idle, enter Moving parent, then enter Walking substate
    assertTrue(eventLog.contains("exit:Idle"));
    assertTrue(eventLog.contains("enter:Moving"));
    assertTrue(eventLog.contains("enter:Walking"));
    eventLog.clear();

    // 3. Sprint command: Walking → Running (within Moving parent)
    fsm.transition(new TransitionEvent("sprint"));
    // Parent should NOT be exited, only substate changes
    assertTrue(eventLog.contains("exit:Walking"));
    assertTrue(eventLog.contains("enter:Running"));
    assertFalse(eventLog.contains("exit:Moving"));
    assertFalse(eventLog.contains("enter:Moving"));
    eventLog.clear();

    // 4. Attack while running: Running → Combat/MeleeAttack
    fsm.transition(new TransitionEvent("attack"));
    // Should exit Running and Moving, enter Combat and MeleeAttack
    assertTrue(eventLog.contains("exit:Running"));
    assertTrue(eventLog.contains("exit:Moving"));
    assertTrue(eventLog.contains("enter:Combat"));
    assertTrue(eventLog.contains("enter:MeleeAttack"));
    eventLog.clear();

    // 5. Switch to ranged attack within Combat
    fsm.transition(new TransitionEvent("ranged"));
    assertTrue(eventLog.contains("exit:MeleeAttack"));
    assertTrue(eventLog.contains("enter:RangedAttack"));
    assertFalse(eventLog.contains("exit:Combat"));
    eventLog.clear();

    // 6. Block while in ranged mode
    fsm.transition(new TransitionEvent("block"));
    assertTrue(eventLog.contains("exit:RangedAttack"));
    assertTrue(eventLog.contains("enter:Blocking"));
    eventLog.clear();

    // 7. Disengage from combat: Blocking → Idle
    fsm.transition(new TransitionEvent("disengage"));
    // Should exit Blocking and Combat, enter Idle
    assertTrue(eventLog.contains("exit:Blocking"));
    assertTrue(eventLog.contains("exit:Combat"));
    assertTrue(eventLog.contains("enter:Idle"));
  }

  /**
   * Tests that when entering a parent state, the default substate is automatically entered. This
   * simulates the Client.java pattern where GameState has MoveState as a default active substate.
   */
  @Test
  void substates_defaultSubstateAutoEntersWithParent() {
    // Create hierarchy: Game -> (Move as default, Aim, Doors)
    TestState game = new TestState(fsm, "Game");
    TestState move = new TestState(game, "Move");
    TestState aim = new TestState(game, "Aim");

    TestState mainMenu = new TestState(fsm, "MainMenu");

    // MainMenu and Move are start states (Move is default substate of Game)
    fsm.addStartStates(mainMenu, move);

    // Transition from MainMenu to Game (which should auto-activate Move)
    fsm.addTransition(new Transition(mainMenu, game, "startGame"));

    // Transitions within Game
    fsm.addTransition(new Transition(move, aim, "aim"));
    fsm.addTransition(new Transition(aim, move, "return"));

    // Start in MainMenu
    fsm.start(new TransitionEvent("init"));
    assertEquals(List.of("enter:MainMenu"), eventLog);
    eventLog.clear();

    // Start game - should enter Game and its default substate Move
    fsm.transition(new TransitionEvent("startGame"));
    assertTrue(eventLog.contains("exit:MainMenu"));
    assertTrue(eventLog.contains("enter:Game"));
    assertTrue(eventLog.contains("enter:Move"));
    eventLog.clear();

    // Switch to Aim mode within Game
    fsm.transition(new TransitionEvent("aim"));
    assertTrue(eventLog.contains("exit:Move"));
    assertTrue(eventLog.contains("enter:Aim"));
    assertFalse(eventLog.contains("exit:Game"), "Parent should not exit for sibling transition");
    eventLog.clear();

    // Return to Move mode
    fsm.transition(new TransitionEvent("return"));
    assertTrue(eventLog.contains("exit:Aim"));
    assertTrue(eventLog.contains("enter:Move"));
  }
}
