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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import neon.core.event.LoadEvent;
import neon.core.event.MagicTask;
import neon.core.event.ScriptAction;
import neon.core.event.TaskQueue;
import neon.core.handlers.InventoryHandler;
import neon.core.handlers.SkillHandler;
import neon.core.model.SaveGameModel;
import neon.entities.Entity;
import neon.entities.EntityFactory;
import neon.entities.Item;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.entities.components.Stats;
import neon.entities.property.Ability;
import neon.entities.property.Feat;
import neon.entities.property.Gender;
import neon.entities.property.Skill;
import neon.magic.Effect;
import neon.magic.Spell;
import neon.magic.SpellFactory;
import neon.maps.Atlas;
import neon.maps.Map;
import neon.maps.MapLoader;
import neon.maps.MapUtils;
import neon.maps.services.GameContextResourceProvider;
import neon.resources.CGame;
import neon.resources.RCreature;
import neon.resources.RMod;
import neon.resources.RSign;
import neon.resources.RSpell.SpellType;
import neon.systems.files.FileUtils;
import neon.systems.files.JacksonMapper;
import neon.systems.files.XMLTranslator;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;
import org.jdom2.Element;

@Listener(references = References.Strong)
@Slf4j
public class GameLoader {
  private GameContext context;
  private TaskQueue queue;
  private Configuration config;
  private GameContextResourceProvider resourceProvider;
  private MapLoader mapLoader;
  @Getter @Setter private int worldMapUID;

  public GameLoader(GameContext context, Configuration config) {
    this.context = context;
    this.config = config;
    queue = context.getQueue();
    resourceProvider = new GameContextResourceProvider(context);
    mapLoader = new MapLoader(context.getStore(), resourceProvider, new MapUtils());
  }

  @Handler
  public void loadGame(LoadEvent le) {
    log.trace("loadGame from {}: {}", le.getSource(), le);
    // load game
    switch (le.getMode()) {
      case LOAD:
        loadGame(le.getSaveName());
        // indicate that loading is complete
        context.post(new LoadEvent(this));
        break;
      case NEW:
        try {
          initGame(le.race, le.name, le.gender, le.specialisation, le.profession, le.sign);
        } catch (RuntimeException re) {
          System.out.println(re);
          re.fillInStackTrace().printStackTrace();
        }
        // indicate that loading is complete
        context.post(new LoadEvent(this));
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
      RCreature species = ((RCreature) context.getResources().getResource(race)).clone();
      Player player = new Player(species, name, gender, spec, profession);
      player.species.text = "@";
      context.startGame(new Game(player, context.getFileSystem()));
      setSign(player, sign);
      for (Skill skill : Skill.values()) {
        SkillHandler.checkFeat(skill, player);
      }

      // initialize maps
      initMaps();

      CGame game = (CGame) context.getResources().getResource("game", "config");

      // starting items
      for (String i : game.getStartingItems()) {
        Item item = EntityFactory.getItem(i, context.getStore().createNewEntityUID());
        context.getStore().addEntity(item);
        InventoryHandler.addItem(player, item.getUID());
      }
      // starting spells
      for (String i : game.getStartingSpells()) {
        player.getMagicComponent().addSpell(SpellFactory.getSpell(i));
      }

      // position player
      Rectangle bounds = player.getShapeComponent();
      bounds.setLocation(game.getStartPosition().x, game.getStartPosition().y);
      Atlas atlas = context.getAtlas();
      UIDStore store = context.getStore();
      String[] startMap = game.getStartMap();

      Map map = atlas.getMap(store.getMapUID(startMap));
      context.getScriptEngine().getBindings("js").putMember("map", map);
      context.getAtlas().setMap(map);
      context.getAtlas().setCurrentZone(game.getStartZone());
    } catch (RuntimeException re) {
      log.error("Error during initGame", re);
    }
    log.debug("Engine.initGame() exit");
  }

  private void setSign(Player player, RSign sign) {
    player.setSign(sign.id);
    for (String power : sign.powers) {
      player.getMagicComponent().addSpell(SpellFactory.getSpell(power));
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

    SaveGameModel saveModel = null;
    try {
      FileInputStream in = new FileInputStream("saves/" + save + "/save.xml");
      JacksonMapper mapper = new JacksonMapper();
      saveModel = mapper.fromXml(in, SaveGameModel.class);
      in.close();
    } catch (IOException e) {
      System.out.println("IOException in loadGame: " + e.getMessage());
      return;
    } catch (Exception e) {
      System.out.println("Error parsing save file: " + e.getMessage());
      return;
    }

    // copy save map to temp
    Path savePath = Paths.get("saves", save);
    Path tempPath = Paths.get("temp");
    FileUtils.copy(savePath, tempPath);

    // initialize maps
    initMaps();

    // set time correctly (using setTime(), otherwise listeners would be called)
    context.getTimer().setTime(saveModel.timer.ticks);

    // create player
    loadPlayer(saveModel.player);

    // events
    loadEvents(saveModel.events);

    // quests
    Player player = context.getPlayer();
    if (player != null) {
      for (SaveGameModel.QuestEntry quest : saveModel.journal.quests) {
        context.getPlayer().getJournal().addQuest(quest.id, quest.subject);
        context.getPlayer().getJournal().updateQuest(quest.id, quest.stage);
      }
    } else {
      System.out.println("Skipping journal update");
    }
  }

  private void loadEvents(SaveGameModel.EventsData events) {
    // normal tasks
    for (SaveGameModel.TaskEvent event : events.tasks) {
      if (event.script != null) {
        queue.add(event.description, new ScriptAction(event.script));
      }
    }

    // timed tasks
    for (SaveGameModel.TimerEvent event : events.timerEvents) {
      String[] ticks = event.tick.split(":");
      int start = Integer.parseInt(ticks[0]);
      int period = Integer.parseInt(ticks[1]);
      int stop = Integer.parseInt(ticks[2]);

      if (event.taskType == null) {
        continue;
      }

      switch (event.taskType) {
        case "script":
          queue.add(event.script, start, period, stop);
          break;
        case "magic":
          Effect effect = Effect.valueOf(event.effect.toUpperCase());
          float magnitude = event.magnitude;
          String script = event.script;
          SpellType type = SpellType.valueOf(event.spellType.toUpperCase());
          Entity caster = null;
          if (event.caster != null) {
            caster = context.getStore().getEntity(event.caster);
          }
          Entity target = null;
          if (event.target != null) {
            target = context.getStore().getEntity(event.target);
          }
          Spell spell = new Spell(target, caster, effect, magnitude, script, type);
          queue.add(new MagicTask(spell, stop), start, stop, period);
          break;
      }
    }
  }

  private void loadPlayer(SaveGameModel.PlayerSaveData playerData) {
    // create player
    RCreature species = (RCreature) context.getResources().getResource(playerData.race);
    Player player =
        new Player(
            species.clone(),
            playerData.name,
            Gender.valueOf(playerData.gender.toUpperCase()),
            Player.Specialisation.valueOf(playerData.specialisation),
            playerData.profession);
    context.startGame(new Game(player, context.getFileSystem()));
    Rectangle bounds = player.getShapeComponent();
    bounds.setLocation(playerData.x, playerData.y);
    player.setSign(playerData.sign);
    player.species.text = "@";

    // start map
    context.getAtlas().setMap(context.getAtlas().getMap(playerData.map));
    context.getAtlas().setCurrentZone(playerData.level);

    // stats
    Stats stats = player.getStatsComponent();
    stats.addStr(playerData.stats.str - stats.getStr());
    stats.addCon(playerData.stats.con - stats.getCon());
    stats.addDex(playerData.stats.dex - stats.getDex());
    stats.addInt(playerData.stats.int_ - stats.getInt());
    stats.addWis(playerData.stats.wis - stats.getWis());
    stats.addCha(playerData.stats.cha - stats.getCha());

    // skills
    loadSkills(player, playerData.skills);

    // items
    for (SaveGameModel.ItemReference itemRef : playerData.items) {
      player.getInventoryComponent().addItem(itemRef.uid);
    }

    // spells
    for (SaveGameModel.SpellReference spellRef : playerData.spells) {
      player.getMagicComponent().addSpell(SpellFactory.getSpell(spellRef.id));
    }

    // feats
    for (SaveGameModel.FeatReference featRef : playerData.feats) {
      player.getCharacteristicsComponent().addFeat(Feat.valueOf(featRef.name));
    }

    // money
    player.getInventoryComponent().addMoney(playerData.money.value);
  }

  private void loadSkills(Player player, SaveGameModel.SkillsData skills) {
    // Load each skill if it has a value
    if (skills.CREATION != null) player.setSkill(Skill.CREATION, skills.CREATION);
    if (skills.DESTRUCTION != null) player.setSkill(Skill.DESTRUCTION, skills.DESTRUCTION);
    if (skills.RESTORATION != null) player.setSkill(Skill.RESTORATION, skills.RESTORATION);
    if (skills.ALTERATION != null) player.setSkill(Skill.ALTERATION, skills.ALTERATION);
    if (skills.ILLUSION != null) player.setSkill(Skill.ILLUSION, skills.ILLUSION);
    if (skills.ENCHANT != null) player.setSkill(Skill.ENCHANT, skills.ENCHANT);
    if (skills.ALCHEMY != null) player.setSkill(Skill.ALCHEMY, skills.ALCHEMY);
    if (skills.CONJURATION != null) player.setSkill(Skill.CONJURATION, skills.CONJURATION);
    if (skills.ARCHERY != null) player.setSkill(Skill.ARCHERY, skills.ARCHERY);
    if (skills.AXE != null) player.setSkill(Skill.AXE, skills.AXE);
    if (skills.BLUNT != null) player.setSkill(Skill.BLUNT, skills.BLUNT);
    if (skills.BLADE != null) player.setSkill(Skill.BLADE, skills.BLADE);
    if (skills.SPEAR != null) player.setSkill(Skill.SPEAR, skills.SPEAR);
    if (skills.UNARMED != null) player.setSkill(Skill.UNARMED, skills.UNARMED);
    if (skills.CLIMBING != null) player.setSkill(Skill.CLIMBING, skills.CLIMBING);
    if (skills.SWIMMING != null) player.setSkill(Skill.SWIMMING, skills.SWIMMING);
    if (skills.SNEAK != null) player.setSkill(Skill.SNEAK, skills.SNEAK);
    if (skills.HEAVY_ARMOR != null) player.setSkill(Skill.HEAVY_ARMOR, skills.HEAVY_ARMOR);
    if (skills.MEDIUM_ARMOR != null) player.setSkill(Skill.MEDIUM_ARMOR, skills.MEDIUM_ARMOR);
    if (skills.LIGHT_ARMOR != null) player.setSkill(Skill.LIGHT_ARMOR, skills.LIGHT_ARMOR);
    if (skills.DODGING != null) player.setSkill(Skill.DODGING, skills.DODGING);
    if (skills.BLOCK != null) player.setSkill(Skill.BLOCK, skills.BLOCK);
    if (skills.UNARMORED != null) player.setSkill(Skill.UNARMORED, skills.UNARMORED);
    if (skills.MERCANTILE != null) player.setSkill(Skill.MERCANTILE, skills.MERCANTILE);
    if (skills.PICKPOCKET != null) player.setSkill(Skill.PICKPOCKET, skills.PICKPOCKET);
    if (skills.ARMORER != null) player.setSkill(Skill.ARMORER, skills.ARMORER);
    if (skills.LOCKPICKING != null) player.setSkill(Skill.LOCKPICKING, skills.LOCKPICKING);
    if (skills.MEDICAL != null) player.setSkill(Skill.MEDICAL, skills.MEDICAL);
    if (skills.DISABLE != null) player.setSkill(Skill.DISABLE, skills.DISABLE);
    if (skills.SPEECHCRAFT != null) player.setSkill(Skill.SPEECHCRAFT, skills.SPEECHCRAFT);
    if (skills.PERFORM != null) player.setSkill(Skill.PERFORM, skills.PERFORM);
    if (skills.DISGUISE != null) player.setSkill(Skill.DISGUISE, skills.DISGUISE);
    if (skills.RIDING != null) player.setSkill(Skill.RIDING, skills.RIDING);
    if (skills.NONE != null) player.setSkill(Skill.NONE, skills.NONE);
  }

  private void initMaps() {
    // put mods and maps in uidstore
    for (RMod mod : context.getResources().getResources(RMod.class)) {
      if (!context.getStore().isModUIDLoaded(mod.id)) {
        context.getStore().addMod(mod.id);
      }
      for (String[] path : mod.getMaps())
        try { // maps are in twowaymap, and are therefore not stored in cache
          Element map = context.getFileSystem().getFile(new XMLTranslator(), path).getRootElement();
          short mapUID = Short.parseShort(map.getChild("header").getAttributeValue("uid"));
          int uid = UIDStore.getMapUID(context.getStore().getModUID(path[0]), mapUID);
          mapLoader.load(path, mapUID, context.getFileSystem());
          context.getStore().addMap(uid, path);
        } catch (Exception e) {
          log.info("Map error in mod {} : {}", path, e.toString());
        }
    }
  }
}
