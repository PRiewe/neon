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

import java.awt.Rectangle;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import neon.core.event.LoadEvent;
import neon.core.event.MagicTask;
import neon.core.event.ScriptAction;
import neon.core.event.TaskQueue;
import neon.core.handlers.InventoryHandler;
import neon.core.handlers.SkillHandler;
import neon.entities.*;
import neon.entities.components.Stats;
import neon.entities.property.Ability;
import neon.entities.property.Feat;
import neon.entities.property.Gender;
import neon.entities.property.Skill;
import neon.magic.Effect;
import neon.magic.Spell;
import neon.magic.SpellFactory;
import neon.maps.*;
import neon.resources.CGame;
import neon.resources.RCreature;
import neon.resources.RMod;
import neon.resources.RSign;
import neon.resources.RSpell.SpellType;
import neon.systems.files.FileUtils;
import neon.systems.files.XMLTranslator;
import neon.util.mapstorage.MapStore;
import neon.util.mapstorage.MapStoreMVStoreAdapter;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;
import org.h2.mvstore.MVStore;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

@Listener(references = References.Strong)
@Slf4j
public class GameLoader {
  private final Engine engine;
  private final TaskQueue queue;
  private final Configuration config;
  private final GameStore gameStore;
  private final GameServices gameServices;
  private final GameContext gameContext;
  private final EntityFactory entityFactory;
  private final MapLoader mapLoader;
  private final SpellFactory spellFactory;
  private final InventoryHandler inventoryHandler;

  public GameLoader(
      Configuration config,
      GameStore gameStore,
      GameServices gameServices,
      TaskQueue taskQueue,
      Engine engine,
      GameContext gameContext) {
    this.gameStore = gameStore;
    this.gameServices = gameServices;
    this.gameContext = gameContext;
    this.entityFactory = new EntityFactory(gameContext);
    this.inventoryHandler = new InventoryHandler(gameContext);
    this.config = config;
    this.engine = engine;
    queue = taskQueue;
    spellFactory = new SpellFactory(gameContext.getResourceManageer());

    mapLoader = new MapLoader(gameContext);
  }

  @Handler
  public void loadGame(LoadEvent le) {
    log.trace("loadGame from {}: {}", le.getSource(), le);
    // load game
    switch (le.getMode()) {
      case LOAD:
        loadGame(le.getSaveName());
        // indicate that loading is complete
        Engine.post(new LoadEvent(this));
        break;
      case NEW:
        try {
          initGame(le.race, le.name, le.gender, le.specialisation, le.profession, le.sign);
        } catch (RuntimeException re) {
          log.error("Fatal", re);
        }
        // indicate that loading is complete
        Engine.post(new LoadEvent(this));
        break;
      default:
        break;
    }
  }

  /**
   * Creates a new game using the supplied data.
   *
   * @param race
   * @param name
   * @param gender
   * @param spec
   * @param profession
   * @param sign
   */
  public void initGame(
      String race,
      String name,
      Gender gender,
      Player.Specialisation spec,
      String profession,
      RSign sign) {
    try {
      log.debug("Engine.initGame() start");

      // initialize player

      ItemFactory itemFactory = new ItemFactory(gameContext.getResourceManageer());
      RCreature species =
          new RCreature(((RCreature) gameStore.getResourceManager().getResource(race)).toElement());
      Player player = new Player(species, name, gender, spec, profession, gameStore.getUidStore());
      player.species.text = "@";
      MapStore atlasMapStore =
          new MapStoreMVStoreAdapter(MVStore.open(gameStore.getFileSystem().getFullPath("atlas")));
      Atlas atlas =
          new Atlas(
              gameStore,
              atlasMapStore,
              gameContext.getQuestTracker(),
              new ZoneActivator(gameContext.getPhysicsEngine(), gameContext),
              gameContext.getZoneFactory(),
              new MapLoader(new MapUtils(), gameContext),
              gameContext);

      engine.startGame(new Game(gameStore, gameContext, atlas));
      gameStore.getUidStore().initialize(gameContext);
      gameStore.setPlayer(player);
      setSign(player, sign);
      for (Skill skill : Skill.values()) {
        SkillHandler.checkFeat(skill, player);
      }

      // initialize maps
      initMaps();

      CGame game = (CGame) gameStore.getResourceManager().getResource("game", "config");

      // starting items
      for (String i : game.getStartingItems()) {
        Item item = entityFactory.getItem(i, gameStore.getUidStore().createNewEntityUID());
        gameStore.getUidStore().addEntity(item);
        inventoryHandler.addItem(player, item.getUID());
      }
      // starting spells
      for (String i : game.getStartingSpells()) {
        player.getMagicComponent().addSpell(spellFactory.getSpell(i));
      }

      // position player
      Rectangle bounds = player.getShapeComponent();
      bounds.setLocation(game.getStartPosition().x, game.getStartPosition().y);
      Map map =
          gameContext.getAtlas().getMap(gameStore.getUidStore().getMapUID(game.getStartMap()));
      gameServices.scriptEngine().getBindings().putMember("map", map);
      gameContext.getAtlas().setCurrentMap(map);
      gameContext.getAtlas().setCurrentZone(game.getStartZone());
    } catch (RuntimeException re) {
      log.error("Error during initGame", re);
    }
    log.debug("Engine.initGame() exit");
  }

  private void setSign(Player player, RSign sign) {
    player.setSign(sign.id);
    for (String power : sign.powers) {
      player.getMagicComponent().addSpell(spellFactory.getSpell(power));
    }
    for (Ability ability : sign.abilities.keySet()) {
      player.getCharacteristicsComponent().addAbility(ability, sign.abilities.get(ability));
    }
  }

  /*
   * Loads a saved game.
   *
   * @param save	the name of the saved game
   */
  private void loadGame(String save) {
    config.setProperty("save", save);

    Document doc = new Document();
    try {
      FileInputStream in = new FileInputStream("saves/" + save + "/save.xml");
      doc = new SAXBuilder().build(in);
      in.close();
    } catch (IOException e) {
      System.out.println("IOException in loadGame");
    } catch (JDOMException e) {
      System.out.println("JDOMException in loadGame");
    }
    Element root = doc.getRootElement();

    // copy save map to temp
    Path savePath = Paths.get("saves", save);
    Path tempPath = Paths.get("temp");
    FileUtils.copy(savePath, tempPath);

    // initialize maps
    initMaps();

    // set time correctly (using setTime(), otherwise listeners would be called)
    gameContext
        .getTimer()
        .setTime(Integer.parseInt(root.getChild("timer").getAttributeValue("ticks")));

    // create player
    loadPlayer(root.getChild("player"));

    // events
    loadEvents(root.getChild("events"));

    // quests
    Element journal = root.getChild("journal");
    Player player = gameContext.getPlayer();
    if (player != null) {
      for (Element e : journal.getChildren()) {
        gameContext.getPlayer().getJournal().addQuest(e.getAttributeValue("id"), e.getText());
        gameContext
            .getPlayer()
            .getJournal()
            .updateQuest(e.getAttributeValue("id"), Integer.parseInt(e.getAttributeValue("stage")));
      }
    } else {
      System.out.println("Skipping journal update");
    }
  }

  private void loadEvents(Element events) {
    // normal tasks
    for (Element event : events.getChildren("task")) {
      String description = event.getAttributeValue("desc");
      if (event.getAttribute("script") != null) {
        String script = event.getAttributeValue("script");
        queue.add(description, new ScriptAction(script));
      }
    }

    // timed tasks
    for (Element event : events.getChildren("timer")) {
      String[] ticks = event.getAttributeValue("tick").split(":");
      int start = Integer.parseInt(ticks[0]);
      int period = Integer.parseInt(ticks[1]);
      int stop = Integer.parseInt(ticks[2]);

      switch (event.getAttributeValue("task")) {
        case "script":
          queue.add(event.getAttributeValue("script"), start, period, stop);
          break;
        case "magic":
          Effect effect = Effect.valueOf(event.getAttributeValue("effect").toUpperCase());
          float magnitude = Float.parseFloat(event.getAttributeValue("magnitude"));
          String script = event.getAttributeValue("script");
          SpellType type = SpellType.valueOf(event.getAttributeValue("type").toUpperCase());
          Entity caster = null;
          if (event.getAttribute("caster") != null) {
            caster =
                gameStore
                    .getUidStore()
                    .getEntity(Long.parseLong(event.getAttributeValue("caster")));
          }
          Entity target = null;
          if (event.getAttribute("target") != null) {
            target =
                gameStore
                    .getUidStore()
                    .getEntity(Long.parseLong(event.getAttributeValue("target")));
          }
          Spell spell = new Spell(target, caster, effect, magnitude, script, type);
          queue.add(new MagicTask(spell, stop), start, stop, period);
          break;
      }
    }
  }

  private void loadPlayer(Element playerData) {
    // create player
    RCreature species =
        (RCreature)
            gameStore.getResourceManager().getResource(playerData.getAttributeValue("race"));
    Player player =
        new Player(
            new RCreature(species.toElement()),
            playerData.getAttributeValue("name"),
            Gender.valueOf(playerData.getAttributeValue("gender").toUpperCase()),
            Player.Specialisation.valueOf(playerData.getAttributeValue("spec")),
            playerData.getAttributeValue("prof"),
            gameStore.getUidStore());
    MapStore atlasMapStore =
        new MapStoreMVStoreAdapter(MVStore.open(gameStore.getFileSystem().getFullPath("atlas")));

    Atlas atlas =
        new Atlas(
            gameStore,
            atlasMapStore,
            gameContext.getQuestTracker(),
            new ZoneActivator(gameContext.getPhysicsEngine(), gameContext),
            gameContext.getZoneFactory(),
            new MapLoader(new MapUtils(), gameContext),
            gameContext);
    gameStore.setPlayer(player);
    engine.startGame(new Game(gameStore, gameContext, atlas));
    Rectangle bounds = player.getShapeComponent();
    bounds.setLocation(
        Integer.parseInt(playerData.getAttributeValue("x")),
        Integer.parseInt(playerData.getAttributeValue("y")));
    player.setSign(playerData.getAttributeValue("sign"));
    player.species.text = "@";

    // start map
    int mapUID = Integer.parseInt(playerData.getAttributeValue("map"));
    gameContext.getAtlas().setCurrentMap(gameContext.getAtlas().getMap(mapUID));
    int level = Integer.parseInt(playerData.getAttributeValue("l"));
    gameContext.getAtlas().setCurrentZone(level);

    // stats
    Stats stats = player.getStatsComponent();
    stats.addStr(
        Integer.parseInt(playerData.getChild("stats").getAttributeValue("str")) - stats.getStr());
    stats.addCon(
        Integer.parseInt(playerData.getChild("stats").getAttributeValue("con")) - stats.getCon());
    stats.addDex(
        Integer.parseInt(playerData.getChild("stats").getAttributeValue("dex")) - stats.getDex());
    stats.addInt(
        Integer.parseInt(playerData.getChild("stats").getAttributeValue("int")) - stats.getInt());
    stats.addWis(
        Integer.parseInt(playerData.getChild("stats").getAttributeValue("wis")) - stats.getWis());
    stats.addCha(
        Integer.parseInt(playerData.getChild("stats").getAttributeValue("cha")) - stats.getCha());

    // skills
    for (Attribute skill : playerData.getChild("skills").getAttributes()) {
      player.setSkill(Skill.valueOf(skill.getName()), Integer.parseInt(skill.getValue()));
    }

    // items
    for (Element e : playerData.getChildren("item")) {
      long uid = Long.parseLong(e.getAttributeValue("uid"));
      player.getInventoryComponent().addItem(uid);
    }

    // spells
    for (Element e : playerData.getChildren("spell")) {
      player.getMagicComponent().addSpell(spellFactory.getSpell(e.getText()));
    }

    // feats
    for (Element e : playerData.getChildren("feat")) {
      player.getCharacteristicsComponent().addFeat(Feat.valueOf(e.getText()));
    }

    // money
    player.getInventoryComponent().addMoney(Integer.parseInt(playerData.getChildText("money")));
  }

  private void initMaps() {
    // put mods and maps in uidstore
    for (RMod mod : gameStore.getResources().getResources(RMod.class)) {
      if (!gameStore.getStore().isModUIDLoaded(mod.id)) {
        gameStore.getStore().addMod(mod.id);
      }
      for (String[] path : mod.getMaps())
        try { // maps are in twowaymap, and are therefore not stored in cache
          Element map =
              gameStore.getFileSystem().getFile(new XMLTranslator(), path).getRootElement();
          short mapUID = Short.parseShort(map.getChild("header").getAttributeValue("uid"));
          int uid = UIDStore.getMapUID(gameStore.getUidStore().getModUID(path[0]), mapUID);
          gameStore.getUidStore().addMap(uid, path);
        } catch (Exception e) {
          log.info("Map error in mod {}", path[0], e);
        }
    }
  }
}
