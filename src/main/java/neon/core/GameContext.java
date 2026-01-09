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
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.narrative.QuestTracker;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
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

  // ========== Game State Accessors ==========

  /**
   * Returns the player entity.
   *
   * @return the player, or null if no game is active
   */
  Player getPlayer();

  /**
   * Returns the atlas (map/world manager).
   *
   * @return the atlas
   */
  Atlas getAtlas();

  /**
   * Returns the entity store.
   *
   * @return the UID store containing all game entities
   */
  UIDStore getStore();

  /**
   * Returns the game timer.
   *
   * @return the timer
   */
  Timer getTimer();

  // ========== System Accessors ==========

  /**
   * Returns the resource manager.
   *
   * @return the resource manager
   */
  ResourceManager getResources();

  /**
   * Returns the file system.
   *
   * @return the file system for accessing game data files
   */
  FileSystem getFileSystem();

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

  /**
   * Returns the physics engine.
   *
   * @return the physics system
   */
  PhysicsSystem getPhysicsEngine();

  /**
   * Returns the script engine (GraalVM Polyglot context).
   *
   * @return the script engine context
   */
  Context getScriptEngine();

  // ========== Actions ==========

  /**
   * Executes a JavaScript script.
   *
   * @param script the script to execute
   * @return the result of the script execution
   */
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
