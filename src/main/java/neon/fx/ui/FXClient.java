/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2024 - Maarten Driesen
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.fx.ui;

import java.util.EventObject;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;
import neon.core.event.LoadEvent;
import neon.core.event.MagicEvent;
import neon.core.event.MessageEvent;
import neon.core.event.UpdateEvent;
import neon.core.handlers.MagicHandler;
import neon.entities.Player;
import neon.fx.ui.states.FXGameState;
import neon.fx.ui.states.FXMainMenuState;
import neon.resources.CClient;
import neon.systems.io.LocalPort;
import neon.util.fsm.FiniteStateMachine;
import neon.util.fsm.Transition;
import neon.util.fsm.TransitionEvent;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

/**
 * JavaFX equivalent of Client. Manages the UI, FSM, and event bus integration.
 *
 * @author mdriesen
 */
@Slf4j
public class FXClient {
  private FXUserInterface ui;
  private final FiniteStateMachine fsm;
  private final MBassador<EventObject> bus;
  private final String version;
  private final GameContext context;
  private final Stage primaryStage;

  public FXClient(LocalPort port, String version, GameContext context, Stage primaryStage) {
    this.bus = port.getBus();
    this.version = version;
    this.context = context;
    this.primaryStage = primaryStage;
    this.fsm = new FiniteStateMachine();
    bus.subscribe(new BusAdapter());
  }

  public void run() {
    initUI();
    initFSM();
  }

  private void initUI() {
    CClient client = (CClient) context.getResources().getResource("client", "config");
    ui = new FXUserInterface(client.getTitle(), primaryStage);
    ui.show();
  }

  private void initFSM() {
    // Main menu state
    FXMainMenuState main = new FXMainMenuState(fsm, bus, ui, version, context);

    // Game state
    FXGameState game = new FXGameState(fsm, bus, ui, context);
    bus.subscribe(game);

    // TODO: Add more states (inventory, journal, dialog, etc.) in future iterations

    // Set start state
    fsm.addStartStates(main);

    // Transitions
    fsm.addTransition(new Transition(main, game, "start"));
    // fsm.addTransition(new Transition(game, main, "menu"));
    // fsm.addTransition(new Transition(game, inventory, "inventory"));
    // fsm.addTransition(new Transition(game, journal, "journal"));

    // Start the FSM
    fsm.start(new TransitionEvent("start"));
  }

  @Listener(references = References.Strong)
  private class BusAdapter {
    @Handler
    public void transition(TransitionEvent te) {
      log.trace("transition {}", te);
      fsm.transition(te);
    }

    @Handler
    public void update(UpdateEvent ue) {
      log.trace("update {}", ue);
      ui.update();
    }

    @Handler
    public void message(MessageEvent me) {
      log.trace("message {}", me);
      ui.showMessage(me.toString(), me.getDuration());
    }

    @Handler
    public void load(LoadEvent le) {
      log.trace("load {}", le);
      if (le.getMode() == LoadEvent.Mode.DONE) {
        fsm.transition(new TransitionEvent("start"));
      }
    }

    @Handler
    public void result(MagicEvent.Result me) {
      log.trace("result {}", me);
      if (me.getCaster() instanceof Player) {
        switch (me.getResult()) {
          case MagicHandler.MANA:
            ui.showMessage("Not enough mana to cast this spell.", 1);
            break;
          case MagicHandler.RANGE:
            ui.showMessage("Target out of range.", 1);
            break;
          case MagicHandler.NONE:
            ui.showMessage("No spell equiped.", 1);
            break;
          case MagicHandler.SKILL:
            ui.showMessage("Casting failed.", 1);
            break;
          case MagicHandler.OK:
            ui.showMessage("Spell cast.", 1);
            break;
          case MagicHandler.NULL:
            ui.showMessage("No target selected.", 1);
            break;
          case MagicHandler.LEVEL:
            ui.showMessage("Spell is too difficult to cast.", 1);
            break;
          case MagicHandler.SILENCED:
            ui.showMessage("You are silenced", 1);
            break;
          case MagicHandler.INTERVAL:
            ui.showMessage("Can't cast this power yet.", 1);
            break;
        }
      }
    }
  }
}
