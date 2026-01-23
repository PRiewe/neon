/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
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

package neon;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import neon.core.Engine;
import neon.core.GameContext;
import neon.fx.ui.FXClientLauncher;
import neon.resources.CClient;
import neon.systems.io.LocalPort;
import neon.ui.Client;

/**
 * The main class of the neon roguelike engine.
 *
 * @author mdriesen
 */
@Slf4j
public class Main {
  private static final String version = "0.4.2"; // current version

  /**
   * The application's main method. This method creates an {@code Engine} and a {@code Client}
   * instance and connects them.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) throws IOException {
    // create and connect ports
    LocalPort cPort = new LocalPort("Client");
    LocalPort sPort = new LocalPort("Server");
    cPort.connect(sPort);
    sPort.connect(cPort);

    // create engine
    Engine engine = new Engine(sPort);
    GameContext context = engine.getContext();

    // Load configuration to determine UI mode
    CClient config = (CClient) context.getResources().getResource("client", "config");
    String uiMode = config.getUiMode();
    log.info("Starting Neon with UI mode: {}", uiMode);

    // Launch appropriate UI
    if ("javafx".equalsIgnoreCase(uiMode)) {
      log.info("Launching JavaFX UI");
      FXClientLauncher.initialize(cPort, context, version);
      FXClientLauncher.launchFX();
    } else {
      log.info("Launching Swing UI");
      Client client = new Client(cPort, version, context);
      // custom look and feels are sometimes stricter than normal ones, apparently
      // the main problem is that parts of the ui are created outside the swing thread.
      // Therefore, everything must be on the event-dispatch thread.
      javax.swing.SwingUtilities.invokeLater(client);
    }

    engine.run();
  }
}
