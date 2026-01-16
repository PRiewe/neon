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
import neon.core.event.TaskQueue;
import neon.entities.Player;
import neon.maps.AtlasPosition;
import neon.maps.services.PhysicsManager;
import neon.narrative.QuestTracker;
import neon.systems.physics.PhysicsSystem;
import neon.systems.timing.Timer;
import org.graalvm.polyglot.Context;

/**
 * Interface providing access to game services and state. This interface abstracts away the static
 * global state previously held in the Engine class, allowing for better testability and cleaner
 * dependency injection.
 *
 * @author mdriesen
 */
public interface GameContext {

  AtlasPosition getAtlasPosition();

  Timer getTimer();

  /**
   * Returns the task queue.
   *
   * @return the task queue for deferred execution
   */
  TaskQueue getQueue();

  /**
   * Returns the quest tracker.
   *
   * @return the quest tracker
   */
  QuestTracker getQuestTracker();

  PhysicsSystem getPhysicsEngine();

  PhysicsManager getPhysicsManager();

  Context getScriptEngine();

  Object execute(String script);

  /** Quits the application. */
  void quit();

  /**
   * Posts an event to the event bus asynchronously.
   *
   * @param event the event to post
   */
  void post(EventObject event);

  /**
   * Starts a new game with the provided game instance.
   *
   * @param game the game instance to start
   */
  void startGame(Game game);
}
