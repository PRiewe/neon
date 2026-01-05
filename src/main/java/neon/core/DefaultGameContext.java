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

package neon.core;

import java.util.EventObject;
import lombok.Setter;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.narrative.QuestTracker;
import neon.resources.ResourceManager;
import neon.systems.physics.PhysicsSystem;
import neon.systems.timing.Timer;
import net.engio.mbassy.bus.MBassador;
import org.graalvm.polyglot.Context;

/**
 * Default implementation of {@link GameContext} that holds references to all game services and
 * state. This class is instantiated by the Engine and provides instance-based access to services
 * that were previously accessed via static methods.
 *
 * <p>Some fields (player, atlas, store, timer) are set when a game is started, while others
 * (resources, quests, physics, scriptEngine) are set during engine initialization.
 *
 * @author mdriesen
 */
public class DefaultGameContext implements GameContext {

  // Engine-level systems (set during engine initialization)
  @Setter private ResourceManager resources;
  @Setter private QuestTracker questTracker;
  @Setter private PhysicsSystem physicsEngine;
  @Setter private Context scriptEngine;
  @Setter private MBassador<EventObject> bus;

  // Game-level state (set when a game starts)
  @Setter private Game game;

  @Override
  public Player getPlayer() {
    return game != null ? game.getPlayer() : null;
  }

  @Override
  public Atlas getAtlas() {
    return game != null ? game.getAtlas() : null;
  }

  @Override
  public UIDStore getStore() {
    return game != null ? game.getStore() : null;
  }

  @Override
  public Timer getTimer() {
    return game != null ? game.getTimer() : null;
  }

  @Override
  public ResourceManager getResources() {
    return resources;
  }

  @Override
  public QuestTracker getQuestTracker() {
    return questTracker;
  }

  @Override
  public PhysicsSystem getPhysicsEngine() {
    return physicsEngine;
  }

  @Override
  public Context getScriptEngine() {
    return scriptEngine;
  }

  @Override
  public Object execute(String script) {
    try {
      return scriptEngine.eval("js", script);
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  @Override
  public void quit() {
    System.exit(0);
  }

  @Override
  public void post(EventObject event) {
    bus.publishAsync(event);
  }
}
