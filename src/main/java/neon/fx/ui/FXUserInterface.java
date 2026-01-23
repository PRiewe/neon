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

import java.util.concurrent.CountDownLatch;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * JavaFX equivalent of UserInterface. Manages the main window (Stage) and provides utilities for
 * displaying messages and dialogs.
 *
 * @author mdriesen
 */
@Slf4j
public class FXUserInterface {
  private final Stage window;
  private Scene scene;

  /**
   * Initialize the user interface with the given title.
   *
   * @param title the window title
   * @param primaryStage the primary JavaFX stage
   */
  public FXUserInterface(String title, Stage primaryStage) {
    this.window = primaryStage;
    window.setTitle("Neon: " + title);
    window.setMinWidth(800);
    window.setMinHeight(480);
    window.setWidth(1024);
    window.setHeight(600);

    // Create initial empty scene
    scene = new Scene(new BorderPane(), 1024, 600);
    window.setScene(scene);
  }

  /** Shows the main window. */
  public void show() {
    Platform.runLater(
        () -> {
          window.show();
          window.requestFocus();
          window.centerOnScreen();
        });
  }

  /**
   * Shows a Region (panel) by making it the root of the scene.
   *
   * @param panel the Region to show
   */
  public void showPanel(Region panel) {
    Platform.runLater(
        () -> {
          scene.setRoot(panel);
          panel.requestFocus();
        });
  }

  /**
   * Shows a message on screen for the specified duration.
   *
   * @param message the message to show
   * @param seconds the duration in seconds
   */
  public void showMessage(String message, int seconds) {
    Platform.runLater(
        () -> {
          Label label = new Label(message);
          label.setStyle(
              "-fx-background-color: rgba(0, 0, 0, 0.8); "
                  + "-fx-text-fill: white; "
                  + "-fx-padding: 10; "
                  + "-fx-border-color: gray; "
                  + "-fx-border-width: 1;");

          Stage messageStage = new Stage(StageStyle.UNDECORATED);
          messageStage.initModality(Modality.NONE);
          messageStage.initOwner(window);
          messageStage.setScene(new Scene(label));
          messageStage.setAlwaysOnTop(true);

          // Center below main window
          double x = window.getX() + (window.getWidth() - 300) / 2;
          double y = window.getY() + window.getHeight() * 0.8;
          messageStage.setX(x);
          messageStage.setY(y);

          messageStage.show();

          // Auto-hide after duration
          PauseTransition delay = new PauseTransition(Duration.seconds(seconds));
          delay.setOnFinished(e -> messageStage.close());
          delay.play();
        });
  }

  /**
   * Shows a yes/no question dialog.
   *
   * @param question the question to ask
   * @return true if "Yes" was selected, false otherwise
   */
  public boolean showQuestion(String question) {
    // Note: This needs to be called on FX thread and block for result
    final boolean[] result = {false};
    final CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

    Platform.runLater(
        () -> {
          Stage dialog = new Stage(StageStyle.DECORATED);
          dialog.initModality(Modality.APPLICATION_MODAL);
          dialog.initOwner(window);
          dialog.setTitle("Question");

          VBox content = new VBox(15);
          content.setPadding(new Insets(20));
          content.setAlignment(Pos.CENTER);

          Label label = new Label(question);
          label.setWrapText(true);

          VBox buttons = new VBox(10);
          buttons.setAlignment(Pos.CENTER);

          Button yesButton = new Button("Yes");
          yesButton.setMinWidth(80);
          yesButton.setOnAction(
              e -> {
                result[0] = true;
                dialog.close();
                latch.countDown();
              });

          Button noButton = new Button("No");
          noButton.setMinWidth(80);
          noButton.setOnAction(
              e -> {
                result[0] = false;
                dialog.close();
                latch.countDown();
              });

          buttons.getChildren().addAll(yesButton, noButton);
          content.getChildren().addAll(label, buttons);

          Scene dialogScene = new Scene(content, 300, 150);
          dialog.setScene(dialogScene);
          dialog.showAndWait();

          latch.countDown(); // In case dialog is closed without button click
        });

    try {
      latch.await();
    } catch (InterruptedException e) {
      log.error("Question dialog interrupted", e);
      Thread.currentThread().interrupt();
    }

    return result[0];
  }

  /** Triggers a repaint of the current scene. */
  public void update() {
    Platform.runLater(
        () -> {
          if (scene != null && scene.getRoot() != null) {
            scene.getRoot().requestLayout();
          }
        });
  }

  /**
   * Gets the primary stage.
   *
   * @return the Stage
   */
  public Stage getWindow() {
    return window;
  }
}
