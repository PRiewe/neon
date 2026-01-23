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
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;
import neon.systems.io.LocalPort;

/**
 * Bootstrap class for launching the JavaFX UI. Handles JavaFX Application lifecycle integration
 * with the existing Main.java structure.
 *
 * @author mdriesen
 */
@Slf4j
public class FXClientLauncher extends Application {
  private static LocalPort cPort;
  private static GameContext context;
  private static String version;
  private static final CountDownLatch latch = new CountDownLatch(1);
  private static FXClient fxClient;

  /**
   * Initialize the launcher with required dependencies before calling launchFX().
   *
   * @param port the client port for communication
   * @param ctx the game context
   * @param ver the version string
   */
  public static void initialize(LocalPort port, GameContext ctx, String ver) {
    cPort = port;
    context = ctx;
    version = ver;
  }

  /**
   * Launch the JavaFX application. This method starts JavaFX on a separate thread and waits for
   * initialization to complete.
   */
  public static void launchFX() {
    new Thread(
            () -> {
              try {
                Application.launch(FXClientLauncher.class);
              } catch (Exception e) {
                log.error("Failed to launch JavaFX application", e);
              }
            })
        .start();

    try {
      latch.await(); // Wait for JavaFX to initialize
    } catch (InterruptedException e) {
      log.error("JavaFX initialization interrupted", e);
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void start(Stage primaryStage) {
    try {
      log.info("Starting JavaFX client");
      fxClient = new FXClient(cPort, version, context, primaryStage);
      fxClient.run();
      latch.countDown(); // Signal that initialization is complete
    } catch (Exception e) {
      log.error("Failed to start FXClient", e);
      latch.countDown(); // Signal even on failure
    }
  }

  @Override
  public void stop() {
    log.info("Stopping JavaFX client");
    // Cleanup if needed
  }
}
