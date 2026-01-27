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
import neon.core.Engine;
import neon.core.UIEngineContext;
import neon.systems.io.LocalPort;
import neon.ui.Client;

/**
 * The main class of the neon roguelike engine.
 *
 * @author mdriesen
 */
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

    // create engine and ui
    Engine engine = new Engine(sPort);
    UIEngineContext context = engine.getGameEngineState();
    Client client = new Client(cPort, version, context);

    // custom look and feels are sometimes stricter than normal ones, apparently
    // the main problem is that parts of the ui are created outside the swing thread.
    // Therefore, everything must be on the event-dispatch thread.
    javax.swing.SwingUtilities.invokeLater(client);
    engine.run();
  }
}
