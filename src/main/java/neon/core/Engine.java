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

import java.awt.*;
import java.io.IOException;
import java.util.EventObject;
import javax.script.*;
import lombok.extern.slf4j.Slf4j;
import neon.core.event.*;
import neon.core.handlers.CombatHandler;
import neon.core.handlers.DeathHandler;
import neon.core.handlers.InventoryHandler;
import neon.core.handlers.MagicHandler;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.maps.AtlasPosition;
import neon.maps.services.EnginePhysicsManager;
import neon.maps.services.PhysicsManager;
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

  // GameContext provides instance-based access to all services
  // TODO: migrate all static accessors to use context, then remove static state
  private static DefaultGameContext context;

  // initialized by engine
  private static Context engine;
  private static org.graalvm.polyglot.Engine polyengine;
  private static FileSystem files; // virtual file system
  private static PhysicsSystem physics; // the physics engine
  private static PhysicsManager physicsManager;
  private static QuestTracker quests;
  private static MBassador<EventObject> bus; // event bus
  private static ResourceManager resources;

  private static TaskQueue queue;
  private final Configuration config;

  // set externally
  private static Game game;

  /** Initializes the engine. */
  public Engine(Port port) throws IOException {
    instance = this;
    context = new DefaultGameContext();

    // set up engine components
    bus = port.getBus();
    // Create a custom Engine with desired options or settings
    polyengine =
        org.graalvm.polyglot.Engine.newBuilder("js")
            // Example: configure an engine-level option
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    // Create a Context using that engine
    engine =
        Context.newBuilder("js")
            .engine(polyengine)
            .allowHostAccess(HostAccess.ALL)
            // allows access to all Java classes
            .allowHostClassLookup(className -> true)
            // Configure context-level options (e.g., host access)
            .allowAllAccess(true)
            .build();

    files = new FileSystem();
    physics = new PhysicsSystem();
    physicsManager = new EnginePhysicsManager(physics);
    queue = new TaskQueue();

    // create a resourcemanager to keep track of all the resources
    resources = new ResourceManager();
    // we use an IniBuilder to add all resources to the manager
    new IniBuilder("neon.ini.xml", files, queue).build(resources);

    // set up remaining engine components
    quests = new QuestTracker();
    config = new Configuration(resources);

    // Initialize the GameContext with all engine systems
    context.setResources(resources);
    context.setQuestTracker(quests);
    context.setPhysicsEngine(physics);
    context.setScriptEngine(engine);
    context.setBus(bus);
    context.setFileSystem(files);
    context.setQueue(queue);
    context.setEngine(this);
  }

  /** This method is the run method of the gamethread. It sets up the event system. */
  public void run() {
    EventAdapter adapter = new EventAdapter(quests);
    bus.subscribe(queue);
    bus.subscribe(new CombatHandler());
    bus.subscribe(new DeathHandler());
    bus.subscribe(new InventoryHandler());
    bus.subscribe(adapter);
    bus.subscribe(quests);
    bus.subscribe(new GameLoader(context, config));
    bus.subscribe(new GameSaver(queue));
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
    try {

      return engine.eval("js", script);
    } catch (Exception e) {
      System.err.println(e);
      return null; // not very good
    }
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
    if (game != null) {
      return game.getPlayer();
    } else return null;
  }

  /**
   * @return the quest tracker
   * @deprecated Use {@link GameContext#getQuestTracker()} instead
   */
  @Deprecated
  public static QuestTracker getQuestTracker() {
    return quests;
  }

  /**
   * @return the timer
   * @deprecated Use {@link GameContext#getTimer()} instead
   */
  @Deprecated
  public static Timer getTimer() {
    return game.getTimer();
  }

  @Deprecated
  public static TaskQueue getTaskQueue() {
    return queue;
  }

  /**
   * @return the virtual filesystem of the engine
   */
  public static FileSystem getFileSystem() {
    // Note: FileSystem is not part of GameContext as it's an engine-internal system
    return files;
  }

  /**
   * @return the physics engine
   * @deprecated Use {@link GameContext#getPhysicsEngine()} instead
   */
  @Deprecated
  public static PhysicsSystem getPhysicsEngine() {
    return physics;
  }

  @Deprecated
  public static PhysicsManager getPhysicsManager() {
    return physicsManager;
  }

  /**
   * @return the script engine
   * @deprecated Use {@link GameContext#getScriptEngine()} instead
   */
  @Deprecated
  public static Context getScriptEngine() {
    return engine;
  }

  /**
   * @return the entity store
   * @deprecated Use {@link GameContext#getStore()} instead
   */
  @Deprecated
  public static UIDStore getStore() {
    return game.getStore();
  }

  /**
   * @return the resource manager
   * @deprecated Use {@link GameContext#getResources()} instead
   */
  @Deprecated
  public static ResourceManager getResources() {
    return resources;
  }

  /**
   * @return the atlas
   * @deprecated Use {@link GameContext#getAtlas()} instead
   */
  @Deprecated
  public static Atlas getAtlas() {
    return game.getAtlas();
  }

  @Deprecated
  public static AtlasPosition getAtlasPosition() {
    return game.getAtlasPosition();
  }

  public TaskQueue getQueue() {
    return queue;
  }

  /**
   * Returns the GameContext, which provides instance-based access to all game services. Use this
   * instead of the deprecated static accessor methods.
   *
   * @return the game context
   */
  public GameContext getContext() {
    return context;
  }

  /**
   * Returns the static GameContext instance. This is a transitional method to allow gradual
   * migration from static accessors.
   *
   * @return the game context
   */
  public static GameContext getStaticContext() {
    return context;
  }

  /** Starts a new game. */
  public void startGame(Game game) {
    System.out.printf("Engine.startGame() start game %s%n", game);
    Engine.game = game;
    context.setGame(game);

    // set up missing systems
    bus.subscribe(new MagicHandler(queue, game));

    // register player
    Player player = game.getPlayer();
    engine.getBindings("js").putMember("journal", player.getJournal());
    engine.getBindings("js").putMember("player", player);
    engine.getBindings("js").putMember("PC", player);
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
