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

package neon.fx.ui.states;

import java.util.EventObject;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;
import neon.core.event.TurnEvent;
import neon.entities.Player;
import neon.fx.ui.FXGamePanel;
import neon.fx.ui.FXUserInterface;
import neon.resources.CClient;
import neon.util.fsm.State;
import neon.util.fsm.TransitionEvent;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

/**
 * JavaFX game state. Handles game rendering and player input.
 *
 * <p>NOTE: This is a minimal POC implementation. Full game functionality requires implementing
 * rendering of all entities from the atlas, physics integration, and complete input handling.
 *
 * @author mdriesen
 */
@Slf4j
public class FXGameState extends State {
  private Player player;
  private final FXGamePanel gamePanel;
  private final CClient keys;
  private final MBassador<EventObject> bus;
  private final FXUserInterface ui;
  private final GameContext context;

  public FXGameState(
      State parent, MBassador<EventObject> bus, FXUserInterface ui, GameContext context) {
    super(parent, "game state");
    this.bus = bus;
    this.ui = ui;
    this.context = context;
    this.keys = (CClient) context.getResources().getResource("client", "config");
    this.gamePanel = new FXGamePanel(1024, 600);

    // Set up keyboard handler
    gamePanel.setOnKeyPressed(this::handleKeyPress);

    bus.subscribe(new EventHandler());
  }

  @Override
  public void enter(TransitionEvent e) {
    log.info("Entering game state");
    ui.showPanel(gamePanel);

    if ("start".equals(e.toString())) {
      player = context.getPlayer();
      if (player != null) {
        // Initial repaint to show the game world
        gamePanel.repaint(context);
        gamePanel.updateStats(player);
        gamePanel.addMessage("Game started. Welcome to Neon!");
        log.info("Player initialized: {}", player.getName());

        // Start turn event
        bus.publishAsync(new TurnEvent(context.getTimer().getTime(), true));
      } else {
        gamePanel.addMessage("ERROR: Player not loaded");
        log.error("Player is null when entering game state");
      }
    }

    // Start rendering
    gamePanel.getVectorPane().startRendering();

    Platform.runLater(() -> gamePanel.requestFocus());
  }

  @Override
  public void exit(TransitionEvent t) {
    log.info("Exiting game state");
    gamePanel.getVectorPane().stopRendering();
  }

  private void handleKeyPress(KeyEvent event) {
    KeyCode code = event.getCode();

    // Zoom controls
    if (code == KeyCode.PLUS || code == KeyCode.EQUALS) {
      float currentZoom = gamePanel.getVectorPane().getZoom();
      gamePanel.getVectorPane().setZoom(currentZoom + 0.1f);
      gamePanel.getVectorPane().render();
      return;
    } else if (code == KeyCode.MINUS) {
      float currentZoom = gamePanel.getVectorPane().getZoom();
      gamePanel.getVectorPane().setZoom(currentZoom - 0.1f);
      gamePanel.getVectorPane().render();
      return;
    }

    // HUD toggle
    if (code == KeyCode.H) {
      gamePanel.setStatsVisible(!gamePanel.getChildren().contains(gamePanel));
      return;
    }

    // Movement keys (placeholder - would need to post movement events)
    if (isMovementKey(code)) {
      gamePanel.addMessage("Movement not yet implemented in POC");
      log.debug("Movement key pressed: {}", code);
      // TODO: Post movement/turn events to engine
      // bus.post(new TurnEvent(...));
      return;
    }

    // State transitions
    if (code == KeyCode.I) {
      log.info("Inventory requested");
      gamePanel.addMessage("Inventory state not yet implemented in POC");
      // bus.post(new TransitionEvent("inventory"));
    } else if (code == KeyCode.J) {
      log.info("Journal requested");
      gamePanel.addMessage("Journal state not yet implemented in POC");
      // bus.post(new TransitionEvent("journal"));
    } else if (code == KeyCode.M) {
      log.info("Map requested");
      gamePanel.addMessage("Map dialog not yet implemented in POC");
    } else if (code == KeyCode.ESCAPE) {
      log.info("Escape pressed - would return to main menu");
      gamePanel.addMessage("ESC pressed - full implementation would return to menu");
    }
  }

  private boolean isMovementKey(KeyCode code) {
    // Check for movement keys based on keyboard layout
    return code == KeyCode.W
        || code == KeyCode.A
        || code == KeyCode.S
        || code == KeyCode.D
        || code == KeyCode.Q
        || code == KeyCode.E
        || code == KeyCode.Z
        || code == KeyCode.C
        || code == KeyCode.UP
        || code == KeyCode.DOWN
        || code == KeyCode.LEFT
        || code == KeyCode.RIGHT
        || code == KeyCode.NUMPAD1
        || code == KeyCode.NUMPAD2
        || code == KeyCode.NUMPAD3
        || code == KeyCode.NUMPAD4
        || code == KeyCode.NUMPAD5
        || code == KeyCode.NUMPAD6
        || code == KeyCode.NUMPAD7
        || code == KeyCode.NUMPAD8
        || code == KeyCode.NUMPAD9;
  }

  @Listener(references = References.Strong)
  private class EventHandler {
    @Handler
    public void onTurn(TurnEvent event) {
      log.trace("Turn event received");
      Platform.runLater(
          () -> {
            if (player != null) {
              // Repaint the game world (queries zone, converts renderables, updates camera)
              gamePanel.repaint(context);
              // Update stats HUD
              gamePanel.updateStats(player);
            }
          });
    }
  }
}
