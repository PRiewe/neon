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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;
import neon.editor.maps.MapEditor;
import neon.editor.resources.*;
import neon.resources.RCraft;
import neon.resources.RCreature;
import neon.resources.RDungeonTheme;
import neon.resources.RItem;
import neon.resources.RMod;
import neon.resources.RPerson;
import neon.resources.RRecipe;
import neon.resources.RRegionTheme;
import neon.resources.RScript;
import neon.resources.RSign;
import neon.resources.RSpell;
import neon.resources.RTattoo;
import neon.resources.RTerrain;
import neon.resources.RZoneTheme;
import neon.resources.quest.RQuest;
import neon.systems.files.FileSystem;
import neon.systems.files.StringTranslator;
import neon.systems.files.XMLTranslator;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

@Slf4j
public class ModFiler {
  private final FileSystem files;
  private final DataStore store;
  private final Editor editor;
  private final JFrame frame;

  public ModFiler(JFrame frame, FileSystem files, DataStore store, Editor editor) {
    this.frame = frame;
    this.files = files;
    this.store = store;
    this.editor = editor;
  }

  void loadMod() {
    // hacky way to make the filechooser start in the game dir
    JFileChooser chooser = new JFileChooser(new File("neon.ini.xml"));
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    chooser.setDialogTitle("Choose module");
    if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
      load(chooser.getSelectedFile(), true);
    }
  }

  void load(File file, boolean active) {
    String path = file.getPath();
    try {
      path = files.mount(path);
      if (!isMod(path)) { // check if this is a mod
        JOptionPane.showMessageDialog(frame, "Selected file is not a valid mod.");
        files.removePath(path);
      } else {
        if (isExtension(path)) { // if extension: load all masters
          Document doc = files.getFile(new XMLTranslator(), path, "main.xml");
          for (Object master : doc.getRootElement().getChildren("master")) {
            String id = ((Element) master).getText();
            Document ini = new Document();
            try { // check in neon.ini.xml which mods exist
              FileInputStream in = new FileInputStream("neon.ini.xml");
              ini = new SAXBuilder().build(in);
              in.close();
            } catch (JDOMException e) {
            }

            // check if there is a mod with the correct id
            for (Element mod : ini.getRootElement().getChild("files").getChildren()) {
              if (!mod.getText().equals(path)) { // make sure current mod is not loaded again
                System.out.println(mod.getText() + ", " + path);
                files.mount(mod.getText());
                Document d = files.getFile(new XMLTranslator(), mod.getText(), "main.xml");
                if (d.getRootElement().getAttributeValue("id").equals(id)) {
                  store.loadData(mod.getText(), false, false);
                } else {
                  files.removePath(mod.getText());
                }
              }
            }
          }
        }

        store.loadData(path, active, isExtension(path));
        editor.mapEditor.loadMaps(
            store.getResourceManager().getResources(RMap.class),
            path,
            editor.mapEditor.getMapTree(),
            store);
        editor.enableEditing(file.isDirectory());
        frame.setTitle("Neon Editor: " + path);
        frame.pack();
      }
    } catch (IOException e1) {
      JOptionPane.showMessageDialog(frame, "Selected file is not a valid mod.");
    }
  }

  public static void save(DataStore store, FileSystem files) {
    XMLBuilder builder = new XMLBuilder(store);
    RMod active = store.getActive();
    saveFile(store,files,new Document(store.getActive().getMainElement()), "main.xml");
    saveFile(store,files,new Document(store.getActive().getCCElement()), "cc.xml");
    saveFile(store,files,
        builder.getResourceDoc(
            store.getResourceManager().getResources(RItem.class), "items", active),
        "objects",
        "items.xml");
    saveFile(store,files,
        builder.getListDoc(
            store.getResourceManager().getResources(RFaction.class), "factions", active),
        "factions.xml");
    saveFile(store,files,
        builder.getListDoc(
            store.getResourceManager().getResources(RRecipe.class), "recipes", active),
        "objects",
        "alchemy.xml");
    saveFile(store,files,builder.getEventsDoc(), "events.xml");
    saveFile(store,files,
        builder.getListDoc(
            store.getResourceManager().getResources(RPerson.class), "people", active),
        "objects",
        "npc.xml");
    saveFile(store,files,
        builder.getResourceDoc(
            store.getResourceManager().getResources(RCreature.class), "monsters", active),
        "objects",
        "monsters.xml");
    saveFile(store,files,
        builder.getResourceDoc(
            store.getResourceManager().getResources(RSpell.class), "spells", active),
        "spells.xml");
    saveFile(store,files,
        builder.getListDoc(
            store.getResourceManager().getResources(RTerrain.class), "terrain", active),
        "terrain.xml");
    saveFile(store,files,
        builder.getListDoc(store.getResourceManager().getResources(RCraft.class), "items", active),
        "objects",
        "crafting.xml");
    saveFile(store,files,
        builder.getListDoc(store.getResourceManager().getResources(RSign.class), "signs", active),
        "signs.xml");
    saveFile(store,files,
        builder.getListDoc(
            store.getResourceManager().getResources(RTattoo.class), "tattoos", active),
        "tattoos.xml");
    saveFile(store,files,
        builder.getListDoc(
            store.getResourceManager().getResources(RZoneTheme.class), "themes", active),
        "themes",
        "zones.xml");
    saveFile(store,files,
        builder.getListDoc(
            store.getResourceManager().getResources(RDungeonTheme.class), "themes", active),
        "themes",
        "dungeons.xml");
    saveFile(store,files,
        builder.getListDoc(
            store.getResourceManager().getResources(RRegionTheme.class), "themes", active),
        "themes",
        "regions.xml");
    saveMaps(store,files);
    saveQuests(store,files);
    saveScripts(store,files);
  }

  private static void saveMaps(DataStore store, FileSystem files) {
    for (String name : files.listFiles(store.getActive().getPath()[0], "maps")) {
      String map =
          name.substring(name.lastIndexOf(File.separator) + 1, name.length() - 4); // -4 for ".xml"
      if (store.getResourceManager().getResource(map, "maps") == null) {
        files.delete(name);
      }
    }
    for (RMap map : store.getActiveMaps()) {
      Document doc = new Document().setRootElement(map.toElement());
      saveFile(store,files,doc, "maps", map.id + ".xml");
    }
  }

  private static void saveQuests(DataStore store, FileSystem files) {
    for (String name : files.listFiles(store.getActive().getPath()[0], "quests")) {
      String quest =
          name.substring(name.lastIndexOf(File.separator) + 1, name.length() - 4); // -4 for ".xml"
      if (store.getResourceManager().getResource(quest, "quest") == null) {
        files.delete(name);
      }
    }
    for (RQuest quest : store.getResourceManager().getResources(RQuest.class)) {
      saveFile(store,files,new Document(quest.toElement()), "quests", quest.id + ".xml");
    }
  }

  private static void saveScripts(DataStore store, FileSystem files) {
    for (String name : files.listFiles(store.getActive().getPath()[0], "scripts")) {
      String script =
          name.substring(name.lastIndexOf(File.separator) + 1, name.length() - 3); // -3 for ".js"
      if (!store.getScripts().containsKey(script)) {
        files.delete(name);
      }
    }
    for (RScript script : store.getScripts().values()) {
      saveFile(store,files, script.script, "scripts", script.id + ".js");
    }
  }

  private static void saveFile(DataStore store, FileSystem files,String text, String... file) {
    String[] fullPath = new String[file.length + 1];
    System.arraycopy(file, 0, fullPath, 1, file.length);
    fullPath[0] = store.getActive().getPath()[0];
    files.saveFile(text, new StringTranslator(), fullPath);
  }

  private static void saveFile(DataStore store, FileSystem files, Document doc, String... file) {
    String[] fullPath = new String[file.length + 1];
    System.arraycopy(file, 0, fullPath, 1, file.length);
    fullPath[0] = store.getActive().getPath()[0];
    files.saveFile(doc, new XMLTranslator(), fullPath);
  }

  private boolean isExtension(String path) {
    Document doc = files.getFile(new XMLTranslator(), path, "main.xml");
    return doc.getRootElement().getName().equals("extension");
  }

  private boolean isMod(String path) {
    try { // main.xml must exist and be valid xml
      return files.getFile(new XMLTranslator(), path, "main.xml") != null;
    } catch (NullPointerException e) {
      return false;
    }
  }
}
