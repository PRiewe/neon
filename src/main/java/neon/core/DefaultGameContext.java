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
import neon.core.event.TaskQueue;
import neon.entities.Player;
import neon.maps.AtlasPosition;
import neon.maps.services.PhysicsManager;
import neon.narrative.QuestTracker;
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
  @Setter private QuestTracker questTracker;
  @Setter private PhysicsSystem physicsEngine;
  @Setter private PhysicsManager physicsManager;
  @Setter private Context scriptEngine;
  @Setter private MBassador<EventObject> bus;
  @Setter private GameStores gamesStores;
  @Setter private TaskQueue queue;
  @Setter private Engine engine;

  // Game-level state (set when a game starts)
  @Setter private Game game;

  @Override
  public Player getPlayer() {
    return game != null ? game.getPlayer() : null;
  }

  @Override
  public AtlasPosition getAtlasPosition() {
    return game != null ? game.getAtlasPosition() : null;
  }

  @Override
  public Timer getTimer() {
    return game != null ? game.getTimer() : null;
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
  public PhysicsManager getPhysicsManager() {
    return physicsManager;
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

  @Override
  public TaskQueue getQueue() {
    return queue;
  }

  @Override
  public void startGame(Game game) {
    // Delegate to Engine's startGame implementation
    // Engine is responsible for registering handlers and setting up script bindings
    if (engine != null) {
      engine.startGame(game);
    } else {
      // Fallback for tests that don't have an Engine instance
      setGame(game);
    }
  }
}
