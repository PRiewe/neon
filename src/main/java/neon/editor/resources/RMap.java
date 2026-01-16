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
import lombok.extern.slf4j.Slf4j;
import neon.editor.DataStore;
import neon.editor.maps.*;
import neon.maps.model.DungeonModel;
import neon.maps.model.WorldModel;
import neon.resources.RData;
import neon.resources.RDungeonTheme;
import neon.resources.RScript;
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
@Slf4j
public class RMap extends RData {
  // id van map = path
  public static final boolean DUNGEON = true;
  public HashMap<Integer, RZone> zones = new HashMap<Integer, RZone>();
  public RDungeonTheme theme;
  public short uid;
  private final boolean type;
  private ArrayList<Integer> uids;

  private final DataStore dataStore;

  // for already existing maps during loadMod
  public RMap(String id, Element properties, DataStore dataStore, String... path) {
    super(id, path);
    uid = Short.parseShort(properties.getChild("header").getAttributeValue("uid"));

    this.dataStore = dataStore;
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
          zones.put(
              Integer.parseInt(zone.getAttributeValue("l")),
              new RZone(zone, this, dataStore, path));
        }
      }
    } else {
      zones.put(0, new RZone(properties, this, dataStore, path));
    }
  }

  // for new maps to be created
  public RMap(short uid, String mod, MapDialog.Properties props, DataStore dataStore) {
    super(props.getID(), mod);
    this.uid = uid;
    type = props.isDungeon();
    this.dataStore = dataStore;
    name = props.getName();

    if (!props.isDungeon()) { // always set zone and base region for outdoor
      Element region = new Element("region");
      region.setAttribute("x", "0");
      region.setAttribute("y", "0");
      region.setAttribute("w", Integer.toString(props.getWidth()));
      region.setAttribute("h", Integer.toString(props.getHeight()));
      region.setAttribute("text", props.getTerrain());
      region.setAttribute("l", "0");
      Instance ri = new IRegion(region, dataStore);
      RZone zone = new RZone(name, mod, ri, this, dataStore);
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
    root.addContent(header);
    if (type == DUNGEON) {
      for (Integer level : zones.keySet()) {
        root.addContent(zones.get(level).toElement().setAttribute("l", level.toString()));
      }
    } else {
      RZone zone = zones.get(0);
      Element creatures = new Element("creatures");
      Element items = new Element("items");
      Element doors = new Element("doors");
      Element containers = new Element("containers");
      Element regions = new Element("regions");
      for (Renderable r : zone.getScene().getElements()) {
        Instance i = (Instance) r;
        Element element = i.toElement();
        element.detach();
        switch (element.getName()) {
          case "region" -> regions.addContent(element);
          case "creature" -> creatures.addContent(element);
          case "item" -> items.addContent(element);
          case "door" -> doors.addContent(element);
          case "container" -> containers.addContent(element);
          default -> log.warn("Unknown element {} with content {}", element.getName(), element);
        }
      }
      root.addContent(creatures);
      root.addContent(items);
      root.addContent(doors);
      root.addContent(containers);
      root.addContent(regions);
    }

    return root;
  }

  /**
   * Converts this map to a WorldModel for Jackson XML serialization.
   *
   * @return WorldModel representation
   */
  public WorldModel toWorldModel() {
    WorldModel model = new WorldModel();

    // Header
    model.header = new WorldModel.Header();
    model.header.uid = uid;
    model.header.name = name;

    RZone zone = zones.get(0);
    for (Renderable r : zone.getScene().getElements()) {
      Instance instance = (Instance) r;

      switch (instance) {
        case IRegion iRegion -> model.regions.add(convertRegion(iRegion));
        case IPerson iPerson -> model.creatures.add(convertCreature(iPerson));
        case IDoor iDoor -> model.doors.add(convertDoor(iDoor));
        case IContainer iContainer -> model.containers.add(convertContainer(iContainer));
        case IObject obj -> model.items.add(convertItem(obj));
        case null, default -> {}
      }
    }

    return model;
  }

  /**
   * Converts this map to a DungeonModel for Jackson XML serialization.
   *
   * @return DungeonModel representation
   */
  public DungeonModel toDungeonModel() {
    DungeonModel model = new DungeonModel();

    // Header
    model.header = new WorldModel.Header();
    model.header.uid = uid;
    model.header.name = name;

    // Levels
    for (Integer levelNum : zones.keySet()) {
      RZone zone = zones.get(levelNum);
      DungeonModel.Level level = new DungeonModel.Level();
      level.l = levelNum;
      level.name = zone.name;

      if (zone.theme != null) {
        level.theme = zone.theme.id;
        // Theme-based zones don't have explicit content
      } else {
        // Explicit zone content
        for (Renderable r : zone.getScene().getElements()) {
          Instance instance = (Instance) r;

          switch (instance) {
            case IRegion iRegion -> level.regions.add(convertRegion(iRegion));
            case IDoor iDoor -> level.doors.add(convertDoor(iDoor));
            case IContainer iContainer -> level.containers.add(convertContainer(iContainer));
            case IPerson iPerson -> level.creatures.add(convertCreature((IPerson) iPerson));
            case IObject iObject -> level.items.add(convertItem(iObject));
            default -> throw new IllegalStateException("Unexpected value: " + instance);
          }
        }
      }

      model.levels.add(level);
    }

    return model;
  }

  private WorldModel.RegionData convertRegion(IRegion region) {
    WorldModel.RegionData data = new WorldModel.RegionData();
    data.x = region.x;
    data.y = region.y;
    data.w = region.width;
    data.h = region.height;
    data.l = (byte) region.z;
    data.text = region.resource.id;
    if (region.theme != null) {
      data.random = region.theme.id;
    }
    if (region.label != null && !region.label.isEmpty()) {
      data.label = region.label;
    }
    for (RScript script : region.scripts) {
      WorldModel.RegionData.ScriptReference scriptRef = new WorldModel.RegionData.ScriptReference();
      scriptRef.id = script.id;
      data.scripts.add(scriptRef);
    }
    return data;
  }

  private WorldModel.CreaturePlacement convertCreature(IPerson person) {
    WorldModel.CreaturePlacement cp = new WorldModel.CreaturePlacement();
    cp.x = person.x;
    cp.y = person.y;
    cp.id = person.resource.id;
    cp.uid = person.uid;
    return cp;
  }

  private WorldModel.ItemPlacement convertItem(IObject obj) {
    WorldModel.ItemPlacement ip = new WorldModel.ItemPlacement();
    ip.x = obj.x;
    ip.y = obj.y;
    ip.id = obj.resource.id;
    ip.uid = obj.uid;
    return ip;
  }

  private WorldModel.DoorPlacement convertDoor(IDoor door) {
    WorldModel.DoorPlacement dp = new WorldModel.DoorPlacement();
    dp.x = door.x;
    dp.y = door.y;
    dp.id = door.resource.id;
    dp.uid = door.uid;

    if (door.state != null) {
      dp.state = door.state.toString().toLowerCase();
    }
    if (door.lock > 0) {
      dp.lock = door.lock;
    }
    if (door.key != null) {
      dp.key = door.key.id;
    }
    if (door.trap > 0) {
      dp.trap = door.trap;
    }
    if (door.spell != null) {
      dp.spell = door.spell.id;
    }

    // Destination
    if (door.destMap != null || door.destTheme != null) {
      dp.destination = new WorldModel.DoorPlacement.Destination();
      if (door.destTheme != null) {
        dp.destination.theme = door.destTheme.id;
      } else {
        if (door.destPos != null) {
          dp.destination.x = door.destPos.x;
          dp.destination.y = door.destPos.y;
        }
        if (door.destZone != null && door.destMap != null) {
          dp.destination.z = door.destMap.getZone(door.destZone);
        }
        if (door.destMap != null) {
          dp.destination.map = (int) door.destMap.uid;
        }
      }
      if (door.text != null && !door.text.isEmpty()) {
        dp.destination.sign = door.text;
      }
    }

    return dp;
  }

  private WorldModel.ContainerPlacement convertContainer(IContainer container) {
    WorldModel.ContainerPlacement cp = new WorldModel.ContainerPlacement();
    cp.x = container.x;
    cp.y = container.y;
    cp.id = container.resource.id;
    cp.uid = container.uid;

    if (container.lock > 0) {
      cp.lock = container.lock;
    }
    if (container.key != null) {
      cp.key = container.key.id;
    }
    if (container.trap > 0) {
      cp.trap = container.trap;
    }
    if (container.spell != null) {
      cp.spell = container.spell.id;
    }

    // Contents
    for (IObject item : container.contents) {
      WorldModel.ContainerPlacement.ContainerItem ci =
          new WorldModel.ContainerPlacement.ContainerItem();
      ci.id = item.resource.id;
      ci.uid = item.uid;
      cp.contents.add(ci);
    }

    return cp;
  }

  public void load() {
    if (uids == null) { // avoid loading map twice
      uids = new ArrayList<Integer>();
      try {
        String file = dataStore.getMod(path[0]).getPath()[0];
        // Load XML directly without XMLTranslator
        java.io.InputStream stream = dataStore.getFileSystem().getStream(file, "maps", id + ".xml");
        if (stream != null) {
          org.jdom2.input.SAXBuilder builder = new org.jdom2.input.SAXBuilder();
          org.jdom2.Document doc = builder.build(stream);
          stream.close();
          Element root = doc.getRootElement();

          if (root.getName().equals("world")) {
            uids.addAll(zones.get(0).load(root));
          } else if (root.getName().equals("dungeon")) {
            for (Element level : root.getChildren("level")) {
              uids.addAll(zones.get(Integer.parseInt(level.getAttributeValue("l"))).load(level));
            }
          } else {
            System.out.println("fout in EditableMap.load(" + id + ")");
          }
        }
      } catch (Exception e) {
        log.error("load", e);
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
