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

import java.io.IOException;
import java.util.EventObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import neon.core.event.*;
import neon.core.handlers.CombatHandler;
import neon.core.handlers.DeathHandler;
import neon.core.handlers.InventoryHandler;
import neon.core.handlers.MagicHandler;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.narrative.EventAdapter;
import neon.narrative.QuestTracker;
import neon.resources.ResourceManager;
import neon.resources.builder.IniBuilder;
import neon.systems.files.FileSystem;
import neon.systems.io.Port;
import neon.systems.physics.PhysicsSystem;
import neon.systems.timing.Timer;
import net.engio.mbassy.bus.MBassador;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

/**
 * The engine class is the core of the neon roguelike engine. It keeps track of all game elements.
 *
 * @author mdriesen
 */
@Slf4j
public class Engine implements Runnable {
  // Singleton instance for backward compatibility during migration
  private static Engine instance;

  @Getter private static GameStore gameStore;

  @Getter private static GameServices gameServices;
  // GameContext provides instance-based access to all services
  // TODO: migrate all static accessors to use context, then remove static state
  @Getter @Setter private static DefaultUIEngineContext gameEngineState;

  // initialized by engine
  private static ScriptEngine scriptEngine;

  private static FileSystem files; // virtual file system
  private static PhysicsSystem physics; // the physics engine
  private static QuestTracker quests;
  private static MBassador<EventObject> bus; // event bus
  private static ResourceManager resources;

  @Getter private final TaskQueue taskQueue;
  private final Configuration config;

  // set externally
  private static Game game;

  public static ScriptEngine createScriptEngine() {
    // Create a custom Engine with desired options or settings
    org.graalvm.polyglot.Engine polyengine =
        org.graalvm.polyglot.Engine.newBuilder("js")
            // Example: configure an engine-level option
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    // Create a Context using that engine
    Context engine =
        Context.newBuilder("js")
            .engine(polyengine)
            .allowHostAccess(HostAccess.ALL)
            // allows access to all Java classes
            .allowHostClassLookup(className -> true)
            // Configure context-level options (e.g., host access)
            .allowAllAccess(true)
            .build();
    return new ScriptEngine(engine);
  }

  /** Initializes the engine. */
  public Engine(Port port) throws IOException {
    instance = this;
    // set up engine components
    bus = port.getBus();
    files = new FileSystem();
    scriptEngine = createScriptEngine();
    physics = new PhysicsSystem();
    gameServices = new GameServices(physics, scriptEngine);

    taskQueue = new TaskQueue(scriptEngine);
    // create a resourcemanager to keep track of all the resources
    resources = new ResourceManager();
    gameStore = new GameStore(files, resources);
    // we use an IniBuilder to add all resources to the manager
    new IniBuilder("neon.ini.xml", files, taskQueue).build(resources);
    quests = new QuestTracker(gameStore, gameServices);
    // set up remaining engine components
    config = new Configuration(resources);
    gameEngineState =
        new DefaultUIEngineContext(gameStore, new QuestTracker(gameStore, gameServices), taskQueue);
    gameEngineState.setGameServices(gameServices);
    gameEngineState.setBus(bus);
  }

  /** This method is the run method of the gamethread. It sets up the event system. */
  public void run() {
    EventAdapter adapter = new EventAdapter(quests);
    bus.subscribe(taskQueue);
    bus.subscribe(new CombatHandler(gameStore.getUidStore()));
    bus.subscribe(new DeathHandler(gameStore, gameServices));
    bus.subscribe(new InventoryHandler());
    bus.subscribe(adapter);
    bus.subscribe(quests);
    bus.subscribe(
        new GameLoader(config, gameStore, gameServices, taskQueue, this, gameEngineState));
    bus.subscribe(new GameSaver(taskQueue));
  }

  /**
   * Convenience method to post an event to the event bus.
   *
   * @param message
   */
  public static void post(EventObject message) {
    bus.publishAsync(message);
  }

  /*
   * all script stuff
   */
  /**
   * Executes a script.
   *
   * @param script the script to execute
   * @return the result of the script
   * @deprecated Use {@link GameContext#execute(String)} instead
   */
  @Deprecated
  public static Object execute(String script) {
    return scriptEngine.execute(script);
  }

  /*
   * all getters
   */
  /**
   * @return the player
   * @deprecated Use {@link GameContext#getPlayer()} instead
   */
  @Deprecated
  public static Player getPlayer() {
    if (gameStore != null) {
      return gameStore.getPlayer();
    } else return null;
  }

  @Deprecated
  public static Atlas getAtlas() {
    if (gameEngineState != null) {
      return gameEngineState.getAtlas();
    } else return null;
  }

  /**
   * @return the quest tracker
   * @deprecated Use {@link GameContext#getQuestTracker()} instead
   */
  @Deprecated
  public static QuestTracker getQuestTracker() {
    return gameEngineState.getQuestTracker();
  }

  /**
   * @return the timer
   * @deprecated Use {@link GameContext#getTimer()} instead
   */
  @Deprecated
  public static Timer getTimer() {
    return game.getTimer();
  }

  /**
   * @return the virtual filesystem of the engine
   */
  @Deprecated
  public static FileSystem getFileSystem() {
    // Note: FileSystem is not part of GameContext as it's an engine-internal system
    return gameStore.getFileSystem();
  }

  /**
   * @return the physics engine
   * @deprecated Use {@link GameContext#getPhysicsEngine()} instead
   */
  @Deprecated
  public static PhysicsSystem getPhysicsEngine() {
    return gameServices.physicsEngine();
  }

  /**
   * @return the script engine
   * @deprecated Use {@link GameContext#getScriptEngine()} instead
   */
  @Deprecated
  public static ScriptEngine getScriptEngine() {
    return scriptEngine;
  }

  /**
   * @return the entity store
   * @deprecated Use {@link GameContext#getStore()} instead
   */
  @Deprecated
  public static UIDStore getStore() {
    return gameStore.getUidStore();
  }

  /**
   * @return the resource manager
   * @deprecated Use {@link GameContext#getResources()} instead
   */
  @Deprecated
  public static ResourceManager getResources() {
    return gameStore.getResourceManager();
  }

  /**
   * Returns the GameContext, which provides instance-based access to all game services. Use this
   * instead of the deprecated static accessor methods.
   *
   * @return the game context
   */
  public GameContext getGameEngineState() {
    return gameEngineState;
  }

  /** Starts a new game. */
  public void startGame(Game game) {
    System.out.printf("Engine.startGame() start game %s%n", game);
    Engine.game = game;
    gameEngineState.setGame(game);

    // set up missing systems
    bus.subscribe(new MagicHandler(gameEngineState));

    // register player
    Player player = game.getPlayer();
    scriptEngine.context().getBindings("js").putMember("journal", player.getJournal());
    scriptEngine.context().getBindings("js").putMember("player", player);
    scriptEngine.context().getBindings("js").putMember("PC", player);
    System.out.println("Engine.startGame() exit");
  }

  /**
   * Quit the game.
   *
   * @deprecated Use {@link GameContext#quit()} instead
   */
  @Deprecated
  public static void quit() {
    System.exit(0);
  }
}
