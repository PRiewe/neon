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

package neon.resources.builder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
// import neon.core.Engine;
import neon.core.event.TaskQueue;
import neon.resources.*;
import neon.resources.quest.RQuest;
import neon.systems.files.FileSystem;
import neon.systems.files.JacksonMapper;
import neon.systems.files.StringTranslator;

@Slf4j
public class ModLoader {
  private String path;
  private final TaskQueue queue;
  private final FileSystem files;
  private final ResourceManager resourceManager;
  private final JacksonMapper jacksonMapper;

  public ModLoader(String mod, TaskQueue queue, FileSystem files, ResourceManager resources) {
    this.queue = queue;
    this.files = files;
    this.resourceManager = resources;
    this.jacksonMapper = new JacksonMapper();
    try {
      path = files.mount(mod);
    } catch (IOException e) {
      log.error("IOException during construction", e);
    }
  }

  public RMod loadMod(CGame game, CClient client) {
    // load main.xml using Jackson
    RMod.MainXml mainXml = files.getFile(jacksonMapper, RMod.MainXml.class, path, "main.xml");

    // load cc.xml using Jackson
    RMod.CCXml ccXml = null;
    if (files.exists(path, "cc.xml")) {
      ccXml = files.getFile(jacksonMapper, RMod.CCXml.class, path, "cc.xml");
    }

    // Use Jackson constructor
    RMod rmod = new RMod(mainXml, ccXml, path);
    rmod.addMaps(initMaps(path, "maps"));

    initMain(client, mainXml);
    if (rmod.isExtension()) {
      ResourceManager resources = resourceManager;
      if (!resources.hasResource(mainXml.master, "mods")) {
        log.error("Extension master not found: {}.", path);
      }
    }

    // terrain
    if (files.exists(path, "terrain.xml")) {
      initTerrain(path, "terrain.xml");
    }

    // books
    if (files.listFiles(path, "books") != null) {
      initBooks(path, "books"); // load before items, otherwise book won't find its text
    }

    // items
    initItems(path, "objects", "items.xml"); // items
    initItems(path, "objects", "crafting.xml"); // crafting

    // themes (after terrain and items, because themes contain terrain and items)
    initThemes(path, "themes", "dungeons.xml"); // dungeons
    initThemes(path, "themes", "zones.xml"); // zones
    initThemes(path, "themes", "regions.xml"); // regions

    // creatures
    initCreatures(path, "objects", "monsters.xml"); // species
    initCreatures(path, "objects", "npc.xml"); // people

    // scripts
    if (files.listFiles(path, "scripts") != null) {
      initScripts(path, "scripts");
    }

    // events
    if (files.exists(path, "events.xml")) {
      initTasks(path, "events.xml");
    }

    // character creation
    if (files.exists(path, "cc.xml")) {
      initCC(game, path, "cc.xml");
    }

    // random quests
    if (files.listFiles(path, "quests") != null) {
      initQuests(path, "quests");
    }

    // magic
    initMagic(path, "spells.xml"); // spells
    initMagic(path, "objects", "alchemy.xml"); // alchemy
    initMagic(path, "signs.xml"); // birth signs
    initMagic(path, "tattoos.xml"); // tattoos

    return rmod;
  }

  private void initMain(CClient client, RMod.MainXml info) {
    if (info.title != null) {
      client.setTitle(info.title);
    }
    if (info.currency != null) {
      if (info.currency.big != null) {
        client.setBig(info.currency.big);
      }
      if (info.currency.small != null) {
        client.setSmall(info.currency.small);
      }
    }
  }

  private void initQuests(String... file) {
    try {
      for (String s : files.listFiles(file)) {
        s = s.substring(s.lastIndexOf("/") + 1);
        String quest = s.substring(s.lastIndexOf(File.separator) + 1);

        // Skip non-XML files
        if (!quest.toLowerCase().endsWith(".xml")) {
          continue;
        }

        try (InputStream stream = files.getStream(path, "quests", quest)) {
          if (stream == null) {
            log.warn("Quest file {} not found, skipping", quest);
            continue;
          }

          RQuest resource = deserialize(stream, quest);
          resourceManager.addResource(resource, "quest");
        } catch (IOException e) {
          log.error("Error loading quest file {} in mod {} due to {}", quest, path, e.toString());
        } catch (Exception e) {
          log.error(
              "Error deserializing quest file {} in mod {} due to {}", quest, path, e.toString());
        }
      }
    } catch (Exception e) { // happens with .svn directory or other file system errors
      log.error("Error accessing quests directory in mod {}", path);
    }
  }

  /** Deserialize quest XML from InputStream. Quests use the quest filename as their ID. */
  private RQuest deserialize(InputStream stream, String questFileName) throws IOException {
    RQuest result = jacksonMapper.fromXml(stream, RQuest.class);
    if (result == null) {
      throw new RuntimeException("Failed to deserialize quest: " + questFileName);
    }
    // Set the quest ID from filename (matches JDOM behavior)
    // Use reflection since id is final in Resource base class
    try {
      java.lang.reflect.Field idField = neon.resources.Resource.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(result, questFileName);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set quest ID: " + questFileName, e);
    }
    return result;
  }

  private void initBooks(String... file) {
    try {
      for (String s : files.listFiles(file)) {
        s = s.substring(s.lastIndexOf("/") + 1);
        String id = s.substring(s.lastIndexOf(File.separator) + 1);
        Resource book = new RText(id, files, path, "books", id);
        resourceManager.addResource(book, "text");
      }
    } catch (Exception e) {
      log.info("No books in mod {}", path);
    }
  }

  private ArrayList<String[]> initMaps(String... file) {
    ArrayList<String[]> maps = new ArrayList<String[]>();
    for (String s : files.listFiles(file)) {
      /* workaround with separators to get jar or folder files:
       * both substrings must be present when dealing with jars
       */
      s = s.substring(s.lastIndexOf("/") + 1);
      s = s.substring(s.lastIndexOf(File.separator) + 1);
      String[] map = {path, "maps", s};
      maps.add(map);
    }
    return maps;
  }

  private void initCreatures(String... file) {
    if (!files.exists(file)) return;

    parseMultiElementFile(
        file,
        (elementName, elementXml) -> {
          switch (elementName) {
            case "npc" -> {
              RPerson person = deserialize(elementXml, RPerson.class);
              resourceManager.addResource(person);
            }
            case "list" -> {
              LCreature list = deserialize(elementXml, LCreature.class);
              resourceManager.addResource(list);
            }
            default -> {
              RCreature creature = deserialize(elementXml, RCreature.class);
              resourceManager.addResource(creature);
            }
          }
        });
  }

  private void initItems(String... file) {
    if (!files.exists(file)) return;

    parseMultiElementFile(
        file,
        (elementName, elementXml) -> {
          switch (elementName) {
            case "book", "scroll" -> {
              RItem.Text text = deserialize(elementXml, RItem.Text.class);
              resourceManager.addResource(text);
            }
            case "weapon" -> {
              RWeapon weapon = deserialize(elementXml, RWeapon.class);
              resourceManager.addResource(weapon);
            }
            case "craft" -> {
              RCraft craft = deserialize(elementXml, RCraft.class);
              resourceManager.addResource(craft);
            }
            case "door" -> {
              RItem.Door door = deserialize(elementXml, RItem.Door.class);
              resourceManager.addResource(door);
            }
            case "potion" -> {
              RItem.Potion potion = deserialize(elementXml, RItem.Potion.class);
              resourceManager.addResource(potion);
            }
            case "container" -> {
              RItem.Container container = deserialize(elementXml, RItem.Container.class);
              resourceManager.addResource(container);
            }
            case "list" -> {
              LItem list = deserialize(elementXml, LItem.class);
              resourceManager.addResource(list);
            }
            case "armor", "clothing" -> {
              RClothing clothing = deserialize(elementXml, RClothing.class);
              resourceManager.addResource(clothing);
            }
            default -> {
              RItem item = deserialize(elementXml, RItem.class);
              resourceManager.addResource(item);
            }
          }
        });
  }

  private void initTerrain(String... file) {
    parseMultiElementFile(
        file,
        (elementName, elementXml) -> {
          RTerrain terrain = deserialize(elementXml, RTerrain.class);
          resourceManager.addResource(terrain, "terrain");
        });
  }

  private void initThemes(String... file) {
    if (!files.exists(file)) return;

    parseMultiElementFile(
        file,
        (elementName, elementXml) -> {
          switch (elementName) {
            case "dungeon" -> {
              RDungeonTheme theme = deserialize(elementXml, RDungeonTheme.class);
              resourceManager.addResource(theme, "theme");
            }
            case "zone" -> {
              RZoneTheme theme = deserialize(elementXml, RZoneTheme.class);
              resourceManager.addResource(theme, "theme");
            }
            case "region" -> {
              RRegionTheme theme = deserialize(elementXml, RRegionTheme.class);
              resourceManager.addResource(theme, "theme");
            }
          }
        });
  }

  private void initMagic(String... file) {
    if (!files.exists(file)) return;

    parseMultiElementFile(
        file,
        (elementName, elementXml) -> {
          switch (elementName) {
            case "sign" -> {
              RSign sign = deserialize(elementXml, RSign.class);
              resourceManager.addResource(sign, "magic");
            }
            case "tattoo" -> {
              RTattoo tattoo = deserialize(elementXml, RTattoo.class);
              resourceManager.addResource(tattoo, "magic");
            }
            case "recipe" -> {
              RRecipe recipe = deserialize(elementXml, RRecipe.class);
              resourceManager.addResource(recipe, "magic");
            }
            case "list" -> {
              LSpell list = deserialize(elementXml, LSpell.class);
              resourceManager.addResource(list, "magic");
            }
            case "power" -> {
              RSpell.Power power = deserialize(elementXml, RSpell.Power.class);
              assignSpellType(power, elementName);
              resourceManager.addResource(power, "magic");
            }
            case "enchant" -> {
              RSpell.Enchantment enchant = deserialize(elementXml, RSpell.Enchantment.class);
              assignSpellType(enchant, elementName);
              resourceManager.addResource(enchant, "magic");
            }
            default -> {
              RSpell spell = deserialize(elementXml, RSpell.class);
              assignSpellType(spell, elementName);
              resourceManager.addResource(spell, "magic");
            }
          }
        });
  }

  private void initScripts(String... file) {
    try {
      for (String s : files.listFiles(file)) {
        s = s.substring(s.lastIndexOf("/") + 1);
        s = s.substring(s.lastIndexOf(File.separator) + 1);
        String[] path = new String[file.length + 1];
        path[file.length] = s;
        System.arraycopy(file, 0, path, 0, file.length);
        RScript script =
            new RScript(s.replaceAll(".js", ""), files.getFile(new StringTranslator(), path));
        resourceManager.addResource(script, "script");
      }
    } catch (Exception e) {
      log.info("No scripts in mod {}", path);
    }
  }

  /*
   * Initializes all character creation data.
   *
   * @param file
   */
  private void initCC(CGame game, String... file) {
    RMod.CCXml cc = files.getFile(jacksonMapper, RMod.CCXml.class, file);
    int x = Integer.parseInt(cc.map.x);
    int y = Integer.parseInt(cc.map.y);
    if (cc.map.z != null) {
      game.setStartZone(Integer.parseInt(cc.map.z));
    }
    game.getStartPosition().setLocation(x, y);
    String[] path = {file[0], "maps", cc.map.path + ".xml"};
    game.setStartMap(path);
    for (String race : cc.races) {
      game.getPlayableRaces().add(race);
    }
    for (String item : cc.items) {
      game.getStartingItems().add(item);
    }
    for (String spell : cc.spells) {
      game.getStartingSpells().add(spell);
    }
  }

  private void initTasks(String... file) {
    neon.resources.model.EventConfigModel eventsConfig =
        files.getFile(jacksonMapper, neon.resources.model.EventConfigModel.class, file);
    for (neon.resources.model.EventConfigModel.ScheduledEvent event : eventsConfig.events) {
      String[] ticks = event.tick.split(":");
      RScript rs = (RScript) resourceManager.getResource(event.script, "script");
      // TODO shoudlnt' be necessary -- bug in testing frameworks
      if (ticks.length == 1) { // one tick: simply add at that time
        queue.add(rs.script, Integer.parseInt(ticks[0]), 0, 0);
      } else if (ticks.length == 2) { // two ticks
        if (!ticks[0].isEmpty()) {
          ticks[0] = "0";
        }
        if (!ticks[1].isEmpty()) { // if period 0, execute only once
          queue.add(rs.script, Integer.parseInt(ticks[0]), 0, 0);
        } else { // otherwise with period from start
          queue.add(rs.script, Integer.parseInt(ticks[0]), Integer.parseInt(ticks[1]), 0);
        }
      } else if (ticks.length == 3) { // three ticks
        if (!ticks[2].isEmpty()) {
          ticks[2] = "0";
        }
        if (!ticks[1].isEmpty()
            || ticks[1].equals("0")) { // if period 0, execute only at start and end
          queue.add(rs.script, Integer.parseInt(ticks[0]), 0, 0);
          queue.add(rs.script, Integer.parseInt(ticks[2]), 0, 0);
        } else { // otherwise with period from start to stop
          queue.add(
              rs.script,
              Integer.parseInt(ticks[0]),
              Integer.parseInt(ticks[1]),
              Integer.parseInt(ticks[2]));
        }
      }
    }
  }

  /**
   * Parse XML file with multiple heterogeneous child elements. Uses Jackson's parseMultiTypeXml for
   * element-by-element processing. Implements fail-fast error handling - throws RuntimeException on
   * parse failures.
   *
   * @param path the file path components
   * @param elementHandler handler called for each (elementName, elementXml) pair
   */
  private void parseMultiElementFile(String[] path, BiConsumer<String, String> elementHandler) {
    try (InputStream stream = files.getStream(path)) {
      if (stream == null) {
        log.warn("File not found: {}", Arrays.toString(path));
        return;
      }

      jacksonMapper.parseMultiTypeXml(stream, elementHandler::accept);
    } catch (IOException e) {
      log.error("Failed to parse XML file {}", Arrays.toString(path), e);
      throw new RuntimeException("Resource loading failed for " + Arrays.toString(path), e);
    }
  }

  /**
   * Deserialize XML string to resource object using Jackson. Implements fail-fast error handling -
   * throws RuntimeException if deserialization returns null.
   *
   * @param <T> the type of resource to deserialize
   * @param elementXml the XML string for the element
   * @param clazz the class to deserialize to
   * @return the deserialized resource
   */
  private <T> T deserialize(String elementXml, Class<T> clazz) {
    T result = jacksonMapper.fromXml(elementXml, clazz);
    if (result == null) {
      throw new RuntimeException(
          "Failed to deserialize "
              + clazz.getSimpleName()
              + " from XML: "
              + elementXml.substring(0, Math.min(200, elementXml.length())));
    }
    return result;
  }

  /**
   * Set RSpell.type based on element name. Handles spell type assignment after Jackson
   * deserialization.
   *
   * @param spell the spell to configure
   * @param elementName the XML element name (spell, disease, poison, curse, power, enchant)
   */
  private void assignSpellType(RSpell spell, String elementName) {
    spell.type =
        switch (elementName) {
          case "power" -> RSpell.SpellType.POWER;
          case "enchant" -> RSpell.SpellType.ENCHANT;
          case "disease" -> RSpell.SpellType.DISEASE;
          case "poison" -> RSpell.SpellType.POISON;
          case "curse" -> RSpell.SpellType.CURSE;
          default -> RSpell.SpellType.SPELL;
        };
  }
}
