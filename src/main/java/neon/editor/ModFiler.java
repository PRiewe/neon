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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import neon.editor.resources.*;
import neon.maps.model.DungeonModel;
import neon.maps.model.WorldModel;
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
import neon.systems.files.JacksonMapper;
import neon.systems.files.StringTranslator;
import neon.systems.files.XMLTranslator;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class ModFiler {
  private final FileSystem files;
  private final DataStore dataStore;
  private final Editor editor;
  private final JFrame frame;

  public ModFiler(JFrame frame, FileSystem files, DataStore dataStore, Editor editor) {
    this.frame = frame;
    this.files = files;
    this.dataStore = dataStore;
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
                  dataStore.loadData(mod.getText(), false, false);
                } else {
                  files.removePath(mod.getText());
                }
              }
            }
          }
        }

        frame.setTitle("Neon Editor: " + path);
        dataStore.loadData(path, active, isExtension(path));
        editor.mapEditor.loadMaps(dataStore.getResourceManager().getResources(RMap.class), path);
        editor.enableEditing(file.isDirectory());
        frame.pack();
      }
    } catch (IOException e1) {
      JOptionPane.showMessageDialog(frame, "Selected file is not a valid mod.");
    }
  }

  public void save() {
    JacksonXmlBuilder builder = new JacksonXmlBuilder(dataStore);
    RMod active = dataStore.getActive();
    saveFile(new Document(dataStore.getActive().getMainElement()), "main.xml");
    saveFile(new Document(dataStore.getActive().getCCElement()), "cc.xml");
    saveFile(
        builder.getResourceDoc(
            dataStore.getResourceManager().getResources(RItem.class), "items", active),
        "objects",
        "items.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RFaction.class), "factions", active),
        "factions.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RRecipe.class), "recipes", active),
        "objects",
        "alchemy.xml");
    saveFile(builder.getEventsDoc(), "events.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RPerson.class), "people", active),
        "objects",
        "npc.xml");
    saveFile(
        builder.getResourceDoc(
            dataStore.getResourceManager().getResources(RCreature.class), "monsters", active),
        "objects",
        "monsters.xml");
    saveFile(
        builder.getResourceDoc(
            dataStore.getResourceManager().getResources(RSpell.class), "spells", active),
        "spells.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RTerrain.class), "terrain", active),
        "terrain.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RCraft.class), "items", active),
        "objects",
        "crafting.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RSign.class), "signs", active),
        "signs.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RTattoo.class), "tattoos", active),
        "tattoos.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RZoneTheme.class), "themes", active),
        "themes",
        "zones.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RDungeonTheme.class), "themes", active),
        "themes",
        "dungeons.xml");
    saveFile(
        builder.getListDoc(
            dataStore.getResourceManager().getResources(RRegionTheme.class), "themes", active),
        "themes",
        "regions.xml");
    saveMaps();
    saveQuests();
    saveScripts();
  }

  /**
   * Saves all maps using Jackson XML serialization.
   *
   * <p>NOTE (Phase 6 - Partial Migration): Maps use Jackson via toWorldModel()/toDungeonModel()
   * (migrated in Phase 2D). Other resources still use toElement() bridge and XMLTranslator. Full
   * migration of resource saving to Jackson deferred to Phase 7.
   */
  private void saveMaps() {
    // Delete maps that no longer exist
    for (String name : files.listFiles(dataStore.getActive().getPath()[0], "maps")) {
      String map =
          name.substring(name.lastIndexOf(File.separator) + 1, name.length() - 4); // -4 for ".xml"
      if (dataStore.getResourceManager().getResource(map, "maps") == null) {
        files.delete(name);
      }
    }

    // Save maps using Jackson serialization
    JacksonMapper mapper = new JacksonMapper();
    for (RMap map : editor.mapEditor.getActiveMaps()) {
      try {
        ByteArrayOutputStream out;
        if (map.isDungeon()) {
          DungeonModel model = map.toDungeonModel();
          out = mapper.toXml(model);
        } else {
          WorldModel model = map.toWorldModel();
          out = mapper.toXml(model);
        }
        // Convert ByteArrayOutputStream to String for saveFile
        String xml = out.toString("UTF-8");
        saveFile(xml, "maps", map.id + ".xml");
      } catch (Exception e) {
        System.err.println("Failed to save map: " + map.id);
        e.printStackTrace();
      }
    }
  }

  private void saveQuests() {
    for (String name : files.listFiles(dataStore.getActive().getPath()[0], "quests")) {
      String quest =
          name.substring(name.lastIndexOf(File.separator) + 1, name.length() - 4); // -4 for ".xml"
      if (dataStore.getResourceManager().getResource(quest, "quest") == null) {
        files.delete(name);
      }
    }
    for (RQuest quest : dataStore.getResourceManager().getResources(RQuest.class)) {
      saveFile(new Document(quest.toElement()), "quests", quest.id + ".xml");
    }
  }

  private void saveScripts() {
    for (String name : files.listFiles(dataStore.getActive().getPath()[0], "scripts")) {
      String script =
          name.substring(name.lastIndexOf(File.separator) + 1, name.length() - 3); // -3 for ".js"
      if (!dataStore.getScripts().containsKey(script)) {
        files.delete(name);
      }
    }
    for (RScript script : dataStore.getScripts().values()) {
      saveFile(script.script, "scripts", script.id + ".js");
    }
  }

  private void saveFile(String text, String... file) {
    String[] fullPath = new String[file.length + 1];
    System.arraycopy(file, 0, fullPath, 1, file.length);
    fullPath[0] = dataStore.getActive().getPath()[0];
    files.saveFile(text, new StringTranslator(), fullPath);
  }

  private void saveFile(Document doc, String... file) {
    String[] fullPath = new String[file.length + 1];
    System.arraycopy(file, 0, fullPath, 1, file.length);
    fullPath[0] = dataStore.getActive().getPath()[0];
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
