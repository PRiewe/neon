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
import lombok.Getter;
import lombok.Setter;
import neon.core.event.TaskQueue;
import neon.core.event.TaskSubmission;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.maps.ZoneFactory;
import neon.maps.services.ResourceProvider;
import neon.narrative.QuestTracker;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.systems.physics.PhysicsSystem;
import neon.systems.timing.Timer;
import neon.util.mapstorage.MapStore;
import neon.util.mapstorage.MapStoreMVStoreAdapter;
import net.engio.mbassy.bus.MBassador;
import org.h2.mvstore.MVStore;

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
public class DefaultUIEngineContext implements GameContext {

  // Engine-level systems (set during engine initialization)
  private final GameStore gameStore;
  @Setter private GameServices gameServices;
  private final QuestTracker questTracker;

  private final TaskQueue taskQueue;
  @Setter private MBassador<EventObject> bus;
  private final ZoneFactory zoneFactory;
  // Game-level state (set when a game starts)
  @Setter private Game game;
  @Getter private final String zoneMapStoreFileName;

  public DefaultUIEngineContext(
      GameStore gameStore, QuestTracker questTracker, TaskQueue taskQueue) {
    this.gameStore = gameStore;
    this.questTracker = questTracker;
    this.taskQueue = taskQueue;
    zoneMapStoreFileName = gameStore.getFileSystem().getFullPath("zomes");
    MapStore zoneMapStore = new MapStoreMVStoreAdapter(MVStore.open(zoneMapStoreFileName));
    zoneFactory =
        new ZoneFactory(zoneMapStore, gameStore.getUidStore(), gameStore.getResourceManager());
  }

  @Override
  public Player getPlayer() {
    return gameStore != null ? gameStore.getPlayer() : null;
  }

  @Override
  public Atlas getAtlas() {
    return game != null ? game.getAtlas() : null;
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
  public TaskSubmission getTaskSubmissionQueue() {
    return taskQueue;
  }

  @Override
  public ResourceProvider getResources() {
    return gameStore.getResources();
  }

  @Override
  public ResourceManager getResourceManageer() {
    return gameStore.getResourceManageer();
  }

  @Override
  public UIDStore getStore() {
    return gameStore.getUidStore();
  }

  @Override
  public FileSystem getFileSystem() {
    return gameStore.getFileSystem();
  }

  @Override
  public ScriptEngine getScriptEngine() {
    return gameServices.scriptEngine();
  }

  @Override
  public Object execute(String script) {
    return gameServices.scriptEngine().execute(script);
  }

  @Override
  public void quit() {
    System.exit(0);
  }

  /**
   * Posts an event to the event bus asynchronously.
   *
   * @param event the event to post
   */
  @Override
  public void post(EventObject event) {
    bus.post(event);
  }

  @Override
  public PhysicsSystem getPhysicsEngine() {
    return gameServices.physicsEngine();
  }

  @Override
  public ZoneFactory getZoneFactory() {
    return zoneFactory;
  }
}
