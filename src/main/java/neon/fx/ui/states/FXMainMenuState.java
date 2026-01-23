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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;
import neon.fx.ui.FXUserInterface;
import neon.resources.CClient;
import neon.util.fsm.State;
import neon.util.fsm.TransitionEvent;
import net.engio.mbassy.bus.MBassador;

/**
 * JavaFX main menu state. Displays menu with New Game, Load Game, Options, and Quit buttons.
 *
 * @author mdriesen
 */
@Slf4j
public class FXMainMenuState extends State {
  private final BorderPane mainPanel;
  private final MBassador<EventObject> bus;
  private final FXUserInterface ui;
  private final GameContext context;
  private final String version;

  public FXMainMenuState(
      State parent,
      MBassador<EventObject> bus,
      FXUserInterface ui,
      String version,
      GameContext context) {
    super(parent, "main menu");
    this.bus = bus;
    this.ui = ui;
    this.context = context;
    this.version = version;

    // Create main panel
    mainPanel = new BorderPane();
    mainPanel.setStyle("-fx-background-color: #2c2c2c;");

    CClient config = (CClient) context.getResources().getResource("client", "config");

    // Title
    Label title = new Label(config.getTitle());
    title.setStyle("-fx-font-size: 36; -fx-text-fill: white; -fx-font-weight: bold;");

    // Buttons
    VBox buttonBox = new VBox(15);
    buttonBox.setAlignment(Pos.CENTER);
    buttonBox.setPadding(new Insets(70, 120, 70, 120));

    String newGameText = config.getString("$newGame");
    Button newGameButton = createMenuButton(newGameText);
    newGameButton.setOnAction(e -> handleNewGame());

    String loadGameText = config.getString("$loadGame");
    Button loadGameButton = createMenuButton(loadGameText);
    loadGameButton.setOnAction(e -> handleLoadGame());

    String optionsText = config.getString("$options");
    Button optionsButton = createMenuButton(optionsText);
    optionsButton.setOnAction(e -> handleOptions());

    String quitText = config.getString("$quit");
    Button quitButton = createMenuButton(quitText);
    quitButton.setOnAction(e -> handleQuit());

    Label contact = new Label("http://sourceforge.net/projects/neon/");
    contact.setStyle("-fx-text-fill: lightblue; -fx-underline: true;");
    contact.setOnMouseClicked(e -> openWebsite());

    buttonBox
        .getChildren()
        .addAll(title, newGameButton, loadGameButton, optionsButton, quitButton, contact);

    // Version label
    Label versionLabel = new Label("release " + version);
    versionLabel.setStyle("-fx-text-fill: gray;");
    versionLabel.setPadding(new Insets(10));

    mainPanel.setCenter(buttonBox);
    mainPanel.setBottom(versionLabel);
    BorderPane.setAlignment(versionLabel, Pos.CENTER);

    // Add keyboard shortcuts
    mainPanel.setOnKeyPressed(this::handleKeyPress);
  }

  private Button createMenuButton(String text) {
    Button button = new Button(text);
    button.setMinWidth(200);
    button.setStyle(
        "-fx-font-size: 14; "
            + "-fx-background-color: #4c4c4c; "
            + "-fx-text-fill: white; "
            + "-fx-padding: 10 20 10 20;");
    button.setOnMouseEntered(
        e ->
            button.setStyle(
                "-fx-font-size: 14; -fx-background-color: #6c6c6c; -fx-text-fill: white; -fx-padding: 10 20 10 20;"));
    button.setOnMouseExited(
        e ->
            button.setStyle(
                "-fx-font-size: 14; -fx-background-color: #4c4c4c; -fx-text-fill: white; -fx-padding: 10 20 10 20;"));
    return button;
  }

  private void handleKeyPress(KeyEvent event) {
    if (event.getCode() == KeyCode.N) {
      handleNewGame();
    } else if (event.getCode() == KeyCode.L) {
      handleLoadGame();
    } else if (event.getCode() == KeyCode.O) {
      handleOptions();
    } else if (event.getCode() == KeyCode.Q || event.getCode() == KeyCode.ESCAPE) {
      handleQuit();
    }
  }

  private void handleNewGame() {
    log.info("New game requested");
    // TODO: Implement NewGameDialog equivalent for full functionality
    // For POC, we'll attempt to start the game if data is already loaded
    ui.showMessage("Starting game (simplified POC version)...", 2);
    try {
      // Transition to game state
      // Note: In full implementation, this would first show NewGameDialog
      // to let user select character, game module, etc.
      bus.post(new TransitionEvent("start"));
    } catch (Exception e) {
      log.error("Failed to start game", e);
      ui.showMessage("Error starting game: " + e.getMessage(), 3);
    }
  }

  private void handleLoadGame() {
    log.info("Load game requested");
    // TODO: Implement LoadGameDialog equivalent
    ui.showMessage("Load Game functionality not yet implemented in JavaFX POC", 2);
  }

  private void handleOptions() {
    log.info("Options requested");
    // TODO: Implement OptionDialog equivalent
    ui.showMessage("Options functionality not yet implemented in JavaFX POC", 2);
  }

  private void handleQuit() {
    log.info("Quit requested");
    Platform.exit();
    System.exit(0);
  }

  private void openWebsite() {
    try {
      java.awt.Desktop.getDesktop()
          .browse(new java.net.URI("http://sourceforge.net/projects/neon"));
    } catch (Exception e) {
      log.error("Failed to open website", e);
    }
  }

  @Override
  public void enter(TransitionEvent t) {
    log.debug("Entering main menu state");
    ui.showPanel(mainPanel);
    Platform.runLater(() -> mainPanel.requestFocus());
  }

  @Override
  public void exit(TransitionEvent t) {
    log.debug("Exiting main menu state");
  }
}
