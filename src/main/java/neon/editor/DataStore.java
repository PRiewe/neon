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

package neon.editor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import neon.editor.resources.RMap;
import neon.resources.*;
import neon.resources.quest.RQuest;
import neon.systems.files.FileSystem;
import neon.systems.files.StringTranslator;
import org.jdom2.Document;
import org.jdom2.Element;

@Slf4j
public class DataStore {
  @Getter private final HashMap<String, RScript> scripts = new HashMap<String, RScript>();
  @Getter private final Multimap<String, String> events = ArrayListMultimap.create();
  private final HashMap<String, RMod> mods = new HashMap<String, RMod>();
  @Getter private final ResourceManager resourceManager;
  @Getter private final FileSystem fileSystem;
  @Getter private RMod active;
  private final neon.systems.files.JacksonMapper jacksonMapper =
      new neon.systems.files.JacksonMapper();

  public DataStore(ResourceManager resourceManager, FileSystem fileSystem) {
    this.resourceManager = resourceManager;
    this.fileSystem = fileSystem;
  }

  /**
   * Bridge method for loading XML files as JDOM Documents without using XMLTranslator.
   *
   * <p>This method provides backward compatibility for editor code that expects JDOM Documents
   * while eliminating the deprecated XMLTranslator. The InputStream is read directly and parsed
   * with JDOM SAXBuilder.
   *
   * @param path the path components to the XML file
   * @return JDOM Document, or null if file doesn't exist
   */
  private Document loadAsDocument(String... path) {
    try {
      java.io.InputStream stream = fileSystem.getStream(path);
      if (stream == null) {
        return null;
      }
      org.jdom2.input.SAXBuilder builder = new org.jdom2.input.SAXBuilder();
      Document doc = builder.build(stream);
      stream.close();
      return doc;
    } catch (Exception e) {
      log.error("Failed to load XML as Document: {}", java.util.Arrays.toString(path), e);
      return null;
    }
  }

  public RMod getMod(String id) {
    return mods.get(id);
  }

  /**
   * Loads all data from a mod.
   *
   * <p>NOTE: Uses bridge method loadAsDocument() to load XML files with JDOM for backward
   * compatibility with JDOM-based resource constructors. XMLTranslator has been eliminated. Full
   * migration to Jackson constructors deferred to future phase when JDOM constructors are removed
   * from resource classes.
   *
   * @param root the mod path
   * @param active whether this is the active mod
   * @param extension whether the mod is an extension
   */
  public void loadData(String root, boolean active, boolean extension) {
    RMod mod = new RMod(loadInfo(root, "main.xml"), loadCC(root, "cc.xml"), root);
    if (active) {
      this.active = mod;
    }

    loadScripts(mod, root, "scripts");
    loadEvents(mod, root, "events.xml");
    loadQuests(mod, root, "quests");
    loadMagic(mod, root, "spells.xml");
    loadItems(mod, root, "objects", "items.xml");
    loadMagic(mod, root, "objects", "alchemy.xml");
    loadFactions(mod, root, "factions.xml");
    loadMagic(mod, root, "signs.xml");
    loadMagic(mod, root, "tattoos.xml");
    loadTerrain(mod, root, "terrain.xml");
    loadItems(mod, root, "objects", "crafting.xml");
    loadThemes(mod, root, "themes", "zones.xml");
    loadThemes(mod, root, "themes", "regions.xml");
    loadThemes(mod, root, "themes", "dungeons.xml");
    loadMaps(mod, root, "maps"); // maps must be loaded after themes
    loadCreatures(mod, root, "objects", "monsters.xml");
    loadCreatures(mod, root, "objects", "npc.xml");

    mods.put(mod.get("id"), mod);
  }

  private void loadEvents(RMod mod, String... file) {
    try {
      Document doc = loadAsDocument(file);
      if (doc != null) {
        for (Element event : doc.getRootElement().getChildren()) {
          events.put(event.getAttributeValue("script"), event.getAttributeValue("tick"));
        }
      }
    } catch (NullPointerException e) {
      log.error("loadEvents", e);
    }
  }

  private void loadScripts(RMod mod, String... file) {
    String[] path = new String[file.length + 1];
    try {
      for (String id : fileSystem.listFiles(file)) {
        System.arraycopy(file, 0, path, 0, file.length);
        id = id.substring(id.lastIndexOf("/") + 1);
        id = id.substring(id.lastIndexOf(File.separator) + 1);
        path[file.length] = id;
        String script = fileSystem.getFile(new StringTranslator(), path);
        id = id.replace(".js", "");
        scripts.put(id, new RScript(id, script, mod.get("id")));
      }
    } catch (NullPointerException e) {
      log.error("loadScripts", e);
    }
  }

  private Element loadInfo(String... file) {
    Element info;
    try {
      Document doc = loadAsDocument(file);
      if (doc != null) {
        info = doc.getRootElement();
        info.detach();
      } else {
        throw new NullPointerException("File does not exist");
      }
    } catch (NullPointerException e) { // file does not exist
      info = new Element("master");
      info.setAttribute("id", file[0]);
      info.addContent(new Element("title"));
      info.addContent(new Element("currency"));
    }
    return info;
  }

  private Element loadCC(String... file) {
    Element cc;
    try {
      Document doc = loadAsDocument(file);
      if (doc != null) {
        cc = doc.getRootElement();
        cc.detach();
      } else {
        throw new NullPointerException("File does not exist");
      }
    } catch (NullPointerException e) { // file does not exist
      cc = new Element("root");
      cc.addContent(new Element("races"));
      cc.addContent(new Element("items"));
      cc.addContent(new Element("spells"));
      cc.addContent(new Element("map"));
    }
    return cc;
  }

  private void loadMaps(RMod mod, String... file) {
    String[] path = new String[file.length + 1];
    try {
      for (String s : fileSystem.listFiles(file)) {
        try {
          System.arraycopy(file, 0, path, 0, file.length);
          // both substrings must be included for jars
          s = s.substring(s.lastIndexOf("/") + 1);
          s = s.substring(s.lastIndexOf(File.separator) + 1);
          path[file.length] = s;
          Document doc = loadAsDocument(path);
          if (doc != null) {
            Element map = doc.getRootElement();
            resourceManager.addResource(
                new RMap(s.replace(".xml", ""), map, this, mod.get("id")), "maps");
          }
        } catch (RuntimeException re) {
          log.error("Failed to load map {}", path, re);
        }
      }
    } catch (NullPointerException e) {
      log.error("loadMaps", e);
    }
  }

  private void loadQuests(RMod mod, String... file) {
    String[] path = new String[file.length + 1];
    try {
      Collection<String> files = fileSystem.listFiles(file);
      for (String quest : files) {
        System.arraycopy(file, 0, path, 0, file.length);
        quest = quest.substring(quest.lastIndexOf("/") + 1);
        quest = quest.substring(quest.lastIndexOf(File.separator) + 1);
        path[file.length] = quest;

        java.io.InputStream stream = fileSystem.getStream(path);
        if (stream != null) {
          String id = quest.replace(".xml", "");
          RQuest rquest = jacksonMapper.fromXml(stream, RQuest.class);
          if (rquest != null) {
            // Set ID from filename (RQuest uses filename as ID like JDOM path did)
            try {
              java.lang.reflect.Field idField =
                  neon.resources.Resource.class.getDeclaredField("id");
              idField.setAccessible(true);
              idField.set(rquest, id);
            } catch (Exception e) {
              log.error("Failed to set quest ID", e);
            }
            resourceManager.addResource(rquest, "quest");
          }
          stream.close();
        }
      }
    } catch (Exception e) {
      log.error("Failed to load quests", e);
    }
  }

  private void loadMagic(RMod mod, String... path) {
    try {
      java.io.InputStream stream = fileSystem.getStream(path);
      if (stream == null) {
        return;
      }

      jacksonMapper.parseMultiTypeXml(
          stream,
          (elementName, elementXml) -> {
            switch (elementName) {
              case "sign" -> {
                RSign sign = jacksonMapper.fromXml(elementXml, RSign.class);
                if (sign != null) resourceManager.addResource(sign, "magic");
              }
              case "tattoo" -> {
                RTattoo tattoo = jacksonMapper.fromXml(elementXml, RTattoo.class);
                if (tattoo != null) resourceManager.addResource(tattoo, "magic");
              }
              case "recipe" -> {
                RRecipe recipe = jacksonMapper.fromXml(elementXml, RRecipe.class);
                if (recipe != null) resourceManager.addResource(recipe, "magic");
              }
              case "list" -> {
                LSpell list = jacksonMapper.fromXml(elementXml, LSpell.class);
                if (list != null) resourceManager.addResource(list, "magic");
              }
              case "power" -> {
                RSpell.Power power = jacksonMapper.fromXml(elementXml, RSpell.Power.class);
                if (power != null) resourceManager.addResource(power, "magic");
              }
              case "enchant" -> {
                RSpell.Enchantment enchant =
                    jacksonMapper.fromXml(elementXml, RSpell.Enchantment.class);
                if (enchant != null) resourceManager.addResource(enchant, "magic");
              }
              default -> {
                RSpell spell = jacksonMapper.fromXml(elementXml, RSpell.class);
                if (spell != null) resourceManager.addResource(spell, "magic");
              }
            }
          });
    } catch (Exception e) {
      log.error("Failed to load magic from " + java.util.Arrays.toString(path), e);
    }
  }

  private void loadCreatures(RMod mod, String... path) {
    try {
      java.io.InputStream stream = fileSystem.getStream(path);
      if (stream == null) {
        return;
      }

      jacksonMapper.parseMultiTypeXml(
          stream,
          (elementName, elementXml) -> {
            switch (elementName) {
              case "list" -> {
                LCreature list = jacksonMapper.fromXml(elementXml, LCreature.class);
                if (list != null) resourceManager.addResource(list);
              }
              case "npc" -> {
                RPerson npc = jacksonMapper.fromXml(elementXml, RPerson.class);
                if (npc != null) resourceManager.addResource(npc);
              }
              case "group" -> {} // Groups are ignored
              default -> {
                RCreature creature = jacksonMapper.fromXml(elementXml, RCreature.class);
                if (creature != null) resourceManager.addResource(creature);
              }
            }
          });
    } catch (Exception e) {
      log.error("Failed to load creatures from " + java.util.Arrays.toString(path), e);
    }
  }

  private void loadFactions(RMod mod, String... path) {
    try {
      java.io.InputStream stream = fileSystem.getStream(path);
      if (stream == null) {
        return;
      }

      jacksonMapper.parseMultiTypeXml(
          stream,
          (elementName, elementXml) -> {
            neon.editor.resources.RFaction faction =
                jacksonMapper.fromXml(elementXml, neon.editor.resources.RFaction.class);
            if (faction != null) resourceManager.addResource(faction, "faction");
          });
    } catch (Exception e) {
      log.error("Failed to load factions from " + java.util.Arrays.toString(path), e);
    }
  }

  private void loadTerrain(RMod mod, String... path) {
    try {
      java.io.InputStream stream = fileSystem.getStream(path);
      if (stream == null) {
        return;
      }

      jacksonMapper.parseMultiTypeXml(
          stream,
          (elementName, elementXml) -> {
            RTerrain terrain = jacksonMapper.fromXml(elementXml, RTerrain.class);
            if (terrain != null) resourceManager.addResource(terrain, "terrain");
          });
    } catch (Exception e) {
      log.error("Failed to load terrain from " + java.util.Arrays.toString(path), e);
    }
  }

  private void loadItems(RMod mod, String... path) {
    try {
      java.io.InputStream stream = fileSystem.getStream(path);
      if (stream == null) {
        return;
      }

      jacksonMapper.parseMultiTypeXml(
          stream,
          (elementName, elementXml) -> {
            switch (elementName) {
              case "list" -> {
                LItem list = jacksonMapper.fromXml(elementXml, LItem.class);
                if (list != null) resourceManager.addResource(list);
              }
              case "book", "scroll" -> {
                RItem.Text text = jacksonMapper.fromXml(elementXml, RItem.Text.class);
                if (text != null) resourceManager.addResource(text);
              }
              case "armor", "clothing" -> {
                RClothing clothing = jacksonMapper.fromXml(elementXml, RClothing.class);
                if (clothing != null) resourceManager.addResource(clothing);
              }
              case "weapon" -> {
                RWeapon weapon = jacksonMapper.fromXml(elementXml, RWeapon.class);
                if (weapon != null) resourceManager.addResource(weapon);
              }
              case "craft" -> {
                RCraft craft = jacksonMapper.fromXml(elementXml, RCraft.class);
                if (craft != null) resourceManager.addResource(craft);
              }
              case "door" -> {
                RItem.Door door = jacksonMapper.fromXml(elementXml, RItem.Door.class);
                if (door != null) resourceManager.addResource(door);
              }
              case "potion" -> {
                RItem.Potion potion = jacksonMapper.fromXml(elementXml, RItem.Potion.class);
                if (potion != null) resourceManager.addResource(potion);
              }
              case "container" -> {
                RItem.Container container =
                    jacksonMapper.fromXml(elementXml, RItem.Container.class);
                if (container != null) resourceManager.addResource(container);
              }
              default -> {
                RItem item = jacksonMapper.fromXml(elementXml, RItem.class);
                if (item != null) resourceManager.addResource(item);
              }
            }
          });
    } catch (Exception e) {
      log.error("Failed to load items from " + java.util.Arrays.toString(path), e);
    }
  }

  private void loadThemes(RMod mod, String... path) {
    try {
      java.io.InputStream stream = fileSystem.getStream(path);
      if (stream == null) {
        return;
      }

      jacksonMapper.parseMultiTypeXml(
          stream,
          (elementName, elementXml) -> {
            switch (elementName) {
              case "dungeon" -> {
                RDungeonTheme dungeon = jacksonMapper.fromXml(elementXml, RDungeonTheme.class);
                if (dungeon != null) resourceManager.addResource(dungeon, "theme");
              }
              case "region" -> {
                RRegionTheme region = jacksonMapper.fromXml(elementXml, RRegionTheme.class);
                if (region != null) resourceManager.addResource(region, "theme");
              }
              case "zone" -> {
                RZoneTheme zone = jacksonMapper.fromXml(elementXml, RZoneTheme.class);
                if (zone != null) resourceManager.addResource(zone, "theme");
              }
            }
          });
    } catch (Exception e) {
      log.error("Failed to load themes from " + java.util.Arrays.toString(path), e);
    }
  }
}
