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

package neon.editor.resources;

import java.util.*;
import neon.editor.DataStore;
import neon.editor.maps.*;
import neon.resources.RData;
import neon.resources.RDungeonTheme;
import neon.systems.files.XMLTranslator;
import neon.ui.graphics.Renderable;
import org.jdom2.*;

/*
 * life cycle of a map:
 * 	A. existing map:
 * 		1. load map
 * 		2. load zones
 * 	B. new map:
 * 		a. dungeon
 * 		b. random dungeon
 * 		c. outdoor
 */
public class RMap extends RData {
  // id van map = path
  public static final boolean DUNGEON = true;
  public HashMap<Integer, RZone> zones = new HashMap<Integer, RZone>();
  public RDungeonTheme theme;
  public short uid;
  private boolean type;
  private ArrayList<Integer> uids;
  private final DataStore dataStore;
  private final List<String> outs = new ArrayList<>();

  // for already existing maps during loadMod
  public RMap(DataStore dataStore, String id, Element properties, String... path) {
    super(id, path);
    this.dataStore = dataStore;
    uid = Short.parseShort(properties.getChild("header").getAttributeValue("uid"));
    name = properties.getChild("header").getChildText("name");
    type = properties.getName().equals("dungeon");

    if (type == DUNGEON) {
      if (properties.getChild("header").getAttribute("theme") != null) {
        theme =
            (RDungeonTheme)
                dataStore
                    .getResourceManager()
                    .getResource(properties.getChild("header").getAttributeValue("theme"), "theme");
      } else {
        for (Element zone : properties.getChildren("level")) {
          RZone newZone = new RZone(dataStore.getResourceManager(), zone, this, path);
          zones.put(Integer.parseInt(zone.getAttributeValue("l")), newZone);
        }
      }
    } else {
      zones.put(0, new RZone(dataStore.getResourceManager(), properties, this, path));
    }
  }

  // for new maps to be created
  public RMap(DataStore dataStore, short uid, String mod, MapDialog.Properties props) {
    super(props.getID(), mod);
    this.dataStore = dataStore;
    this.uid = uid;
    type = props.isDungeon();
    name = props.getName();

    if (!props.isDungeon()) { // always set zone and base region for outdoor
      Element region = new Element("region");
      region.setAttribute("x", "0");
      region.setAttribute("y", "0");
      region.setAttribute("w", Integer.toString(props.getWidth()));
      region.setAttribute("h", Integer.toString(props.getHeight()));
      region.setAttribute("text", props.getTerrain());
      region.setAttribute("l", "0");
      Instance ri = new IRegion(dataStore, region);
      RZone zone = new RZone(name, mod, ri, this);
      zones.put(0, zone);
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isDungeon() {
    return type;
  }

  public RZone getZone(int index) {
    if (zones.isEmpty()) {
      load();
    }
    return zones.get(index);
  }

  public int getZone(RZone zone) {
    if (zones.isEmpty()) {
      load();
    }

    for (Integer i : zones.keySet()) {
      if (zones.get(i) == zone) {
        return i;
      }
    }
    return 0;
  }

  public short getUID() {
    return uid;
  }

  public String toString() {
    return name;
  }

  // also remove objects from tree if needed!!!
  public void removeObjectUID(int uid) {
    uids.remove((Integer) uid); // because remove(int) removes the int'th value
  }

  // don't forget to add objects to the tree!!!
  public int createUID(Element e) {
    int hash = e.hashCode();
    while (uids.contains(hash)) {
      hash++;
    }
    uids.add(hash);
    return hash;
  }

  public Element toElement() {
    System.out.println("save map: " + name);
    Element root = new Element(isDungeon() ? "dungeon" : "world");
    Element header = new Element("header");
    header.setAttribute("uid", Short.toString(uid));
    header.addContent(new Element("name").setText(name));
    if (theme != null) {
      header.setAttribute("theme", theme.id);
    }
    root.addContent(header);
    if (type == DUNGEON) {
      for (Integer level : zones.keySet()) {
        root.addContent(zones.get(level).toElement().setAttribute("l", level.toString()));
      }
    } else {
      RZone zone = zones.get(0);
      Element creatures = new Element("creatures");
      Element items = new Element("items");
      Element regions = new Element("regions");
      for (Renderable r : zone.getScene().getElements()) {
        Instance i = (Instance) r;
        Element element = i.toElement();
        element.detach();
        switch (element.getName()) {
          case "region" -> regions.addContent(element);
          case "creature" -> creatures.addContent(element);
          case "item", "door", "container" -> items.addContent(element);
        }
      }
      if (!creatures.getChildren().isEmpty()) {
        root.addContent(creatures);
      }
      if (!items.getChildren().isEmpty()) {
        root.addContent(items);
      }
      if (!regions.getChildren().isEmpty()) {
        root.addContent(regions);
      }
    }

    return root;
  }

  public void load() {
    if (uids == null) { // avoid loading map twice
      uids = new ArrayList<Integer>();
      try {
        String file = dataStore.getMod(path[0]).getPath()[0];
        Element root =
            dataStore
                .getFiles()
                .getFile(new XMLTranslator(), file, "maps", id + ".xml")
                .getRootElement();

        if (root.getName().equals("world")) {
          uids.addAll(zones.get(0).load(dataStore.getRZoneFactory(), root));
        } else if (root.getName().equals("dungeon")) {
          for (Element level : root.getChildren("level")) {
            uids.addAll(
                zones
                    .get(Integer.parseInt(level.getAttributeValue("l")))
                    .load(dataStore.getRZoneFactory(), level));
          }
        } else {
          System.out.println("fout in EditableMap.load(" + id + ")");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * This method removes a zone from this map.
   *
   * @param level the zone to remove
   */
  public void removeZone(int level) {
    for (Renderable r : zones.get(level).getScene().getElements()) {
      Instance instance = (Instance) r;
      if (instance instanceof IObject) { // remove uids
        uids.remove(Integer.parseInt(instance.toElement().getAttributeValue("uid")));
        if (instance.toElement().getName().equals("container")) { // remove container contents
          for (Element e : instance.toElement().getChildren()) {
            uids.remove(Integer.parseInt(e.getAttributeValue("uid")));
          }
        }
      }
    }
    zones.remove(level);
  }
}
