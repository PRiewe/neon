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
import lombok.extern.slf4j.Slf4j;
import neon.core.event.*;
import neon.core.handlers.CombatHandler;
import neon.core.handlers.DeathHandler;
import neon.core.handlers.InventoryHandler;
import neon.core.handlers.MagicHandler;
import neon.entities.Player;
import neon.narrative.EventAdapter;
import neon.narrative.QuestTracker;
import neon.resources.ResourceManager;
import neon.resources.builder.IniBuilder;
import neon.systems.files.FileSystem;
import neon.systems.io.Port;
import neon.systems.physics.PhysicsSystem;
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

  private final GameStore gameStore;

  private final GameServices gameServices;
  // GameContext provides instance-based access to all services
  // TODO: migrate all static accessors to use context, then remove static state
  private final DefaultUIEngineContext gameEngineState;

  // initialized by engine
  private final ScriptEngine scriptEngine;

  private final QuestTracker quests;
  private final MBassador<EventObject> bus; // event bus

  private final TaskQueue taskQueue;
  private final Configuration config;

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
    // Singleton instance for backward compatibility during migration
    Engine instance = this;
    // set up engine components
    bus = port.getBus();
    // virtual file system
    FileSystem files = new FileSystem();
    scriptEngine = createScriptEngine();
    // the physics engine
    PhysicsSystem physics = new PhysicsSystem();
    gameServices = new GameServices(physics, scriptEngine);

    taskQueue = new TaskQueue(scriptEngine);
    // create a resourcemanager to keep track of all the resources
    ResourceManager resources = new ResourceManager();
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
    bus.subscribe(new CombatHandler(gameEngineState));
    bus.subscribe(new DeathHandler(gameStore, gameServices));
    bus.subscribe(new InventoryHandler(gameEngineState));
    bus.subscribe(adapter);
    bus.subscribe(quests);
    GameLoader loader =
        new GameLoader(config, gameStore, gameServices, taskQueue, this, gameEngineState);
    bus.subscribe(loader);
    bus.subscribe(new GameSaver(taskQueue, gameEngineState));
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

    gameEngineState.setGame(game);

    // set up missing systems
    bus.subscribe(new MagicHandler(gameEngineState));

    // register player
    Player player = game.getPlayer();
    scriptEngine.getBindings().putMember("journal", player.getJournal());
    scriptEngine.getBindings().putMember("player", player);
    scriptEngine.getBindings().putMember("PC", player);
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
