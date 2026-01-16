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
import neon.editor.resources.RFaction;
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
        Document doc = loadAsDocument(path);
        if (doc != null) {
          Element root = doc.getRootElement();
          String id = quest.replace(".xml", "");
          resourceManager.addResource(new RQuest(id, root, mod.get("id")), "quest");
        }
      }
    } catch (NullPointerException e) {
    }
  }

  private void loadMagic(RMod mod, String... path) {
    try {
      Document doc = loadAsDocument(path);
      if (doc != null) {
        for (Element e : doc.getRootElement().getChildren()) {
          switch (e.getName()) {
            case "sign" -> resourceManager.addResource(new RSign(e, mod.get("id")), "magic");
            case "tattoo" -> resourceManager.addResource(new RTattoo(e, mod.get("id")), "magic");
            case "recipe" -> resourceManager.addResource(new RRecipe(e, mod.get("id")), "magic");
            case "list" -> resourceManager.addResource(new LSpell(e, mod.get("id")), "magic");
            case "power" ->
                resourceManager.addResource(new RSpell.Power(e, mod.get("id")), "magic");
            case "enchant" ->
                resourceManager.addResource(new RSpell.Enchantment(e, mod.get("id")), "magic");
            default -> resourceManager.addResource(new RSpell(e, mod.get("id")), "magic");
          }
        }
      }
    } catch (NullPointerException e) {
    }
  }

  private void loadCreatures(RMod mod, String... path) {
    try {
      Document doc = loadAsDocument(path);
      if (doc != null) {
        for (Element e : doc.getRootElement().getChildren()) {
          switch (e.getName()) {
            case "list" -> resourceManager.addResource(new LCreature(e, mod.get("id")));
            case "npc" -> resourceManager.addResource(new RPerson(e, mod.get("id")));
            case "group" -> {}
            default -> resourceManager.addResource(new RCreature(e, mod.get("id")));
          }
        }
      }
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }

  private void loadFactions(RMod mod, String... path) {
    try {
      Document doc = loadAsDocument(path);
      if (doc != null) {
        for (Element e : doc.getRootElement().getChildren()) {
          resourceManager.addResource(new RFaction(e, mod.get("id")), "faction");
        }
      }
    } catch (NullPointerException e) {
    }
  }

  private void loadTerrain(RMod mod, String... path) {
    try {
      Document doc = loadAsDocument(path);
      if (doc != null) {
        for (Element e : doc.getRootElement().getChildren()) {
          resourceManager.addResource(new RTerrain(e, mod.get("id")), "terrain");
        }
      }
    } catch (NullPointerException e) {
    }
  }

  private void loadItems(RMod mod, String... path) {
    try {
      Document doc = loadAsDocument(path);
      if (doc != null) {
        for (Element e : doc.getRootElement().getChildren()) {
          switch (e.getName()) {
            case "list" -> resourceManager.addResource(new LItem(e, mod.get("id")));
            case "book", "scroll" -> resourceManager.addResource(new RItem.Text(e, mod.get("id")));
            case "armor", "clothing" ->
                resourceManager.addResource(new RClothing(e, mod.get("id")));
            case "weapon" -> resourceManager.addResource(new RWeapon(e, mod.get("id")));
            case "craft" -> resourceManager.addResource(new RCraft(e, mod.get("id")));
            case "door" -> resourceManager.addResource(new RItem.Door(e, mod.get("id")));
            case "potion" -> resourceManager.addResource(new RItem.Potion(e, mod.get("id")));
            case "container" -> resourceManager.addResource(new RItem.Container(e, mod.get("id")));
            default -> resourceManager.addResource(new RItem(e, mod.get("id")));
          }
        }
      }
    } catch (NullPointerException e) {
    }
  }

  private void loadThemes(RMod mod, String... path) {
    try {
      Document doc = loadAsDocument(path);
      if (doc != null) {
        for (Element e : doc.getRootElement().getChildren()) {
          switch (e.getName()) {
            case "dungeon" ->
                resourceManager.addResource(new RDungeonTheme(e, mod.get("id")), "theme");
            case "region" ->
                resourceManager.addResource(new RRegionTheme(e, mod.get("id")), "theme");
            case "zone" -> resourceManager.addResource(new RZoneTheme(e, mod.get("id")), "theme");
          }
        }
      }
    } catch (NullPointerException e) {
    }
  }
}
