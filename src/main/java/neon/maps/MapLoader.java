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

package neon.maps;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import lombok.Setter;
import neon.core.*;
import neon.entities.*;
import neon.entities.components.Enchantment;
import neon.entities.components.Lock;
import neon.maps.model.DungeonModel;
import neon.maps.model.WorldModel;
import neon.resources.*;
import neon.systems.files.FileSystem;
import neon.systems.files.JacksonMapper;
import neon.systems.files.XMLTranslator;
import org.jdom2.*;
import org.jdom2.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;

/**
 * This class loads a map from an xml file.
 *
 * <p>Refactored to support dependency injection for better testability and reduced coupling.
 *
 * @author mdriesen
 */
public class MapLoader {
  private final MapUtils mapUtils;
  private final UIDStore uidStore;
  private final ResourceManager resourceManager;
  private final FileSystem fileSystem;
  private final ZoneFactory zoneFactory;
  @Setter private Player player;

  /** Creates a MapLoader with dependency injection. */
  public MapLoader(
      FileSystem fileSystem,
      UIDStore uidStore,
      ResourceManager resourceManager,
      ZoneFactory zoneFactory,
      Player player) {
    this(fileSystem, uidStore, resourceManager, new MapUtils(), zoneFactory, player);
  }

  /**
   * Creates a MapLoader with dependency injection and custom random source.
   *
   * @param mapUtils the MapUtils instance for random operations
   */
  public MapLoader(
      FileSystem fileSystem,
      UIDStore uidStore,
      ResourceManager resourceManager,
      MapUtils mapUtils,
      ZoneFactory zoneFactory,
      Player player) {
    this.fileSystem = fileSystem;
    this.uidStore = uidStore;
    this.resourceManager = resourceManager;
    this.mapUtils = mapUtils;
    this.zoneFactory = zoneFactory;
    this.player = player;
  }

  /**
   * Returns a map described in an xml file with the given name (instance method).
   *
   * @param path the pathname of a map file
   * @param uid the unique identifier of this map
   * @return the <code>Map</code> described by the map file
   */
  public Map load(@NotNull String[] path, int uid) {
    // For now, use JDOM to determine type, then build models
    // In the future, FileSystem can provide InputStream directly

    Document doc = fileSystem.getFile(new XMLTranslator(), path);
    Element root = doc.getRootElement();

    if (root.getName().equals("world")) {

      return loadWorld(root, uid);
    } else {
      return loadDungeon(root, uid);
    }
  }

  private World loadWorld(Element root, int uid) {
    try {
      // Convert JDOM Element to XML string
      XMLOutputter outputter = new XMLOutputter();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      outputter.output(root, out);

      // Parse with Jackson
      JacksonMapper mapper = new JacksonMapper();
      ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
      WorldModel model = mapper.fromXml(input, WorldModel.class);

      // Build World from model
      return buildWorldFromModel(model, uid);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load world map", e);
    }
  }

  private Dungeon loadDungeon(Element root, int uid) {
    try {
      // Convert JDOM Element to XML string
      XMLOutputter outputter = new XMLOutputter();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      outputter.output(root, out);

      // Parse with Jackson
      JacksonMapper mapper = new JacksonMapper();
      ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
      DungeonModel model = mapper.fromXml(input, DungeonModel.class);

      // Build Dungeon from model
      return buildDungeonFromModel(model, uid);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load dungeon map", e);
    }
  }

  public Dungeon loadThemedDungeon(String name, String dungeonTheme, int uid) {
    Dungeon map = new Dungeon(name, uid, zoneFactory);
    RDungeonTheme theme = (RDungeonTheme) resourceManager.getResource(dungeonTheme, "theme");

    int minZ = theme.min;
    int maxZ = theme.max;
    float branch = theme.branching;
    String[] types = theme.zones.split(";");

    int[] zones = new int[mapUtils.random(minZ, maxZ)];
    int z = zones.length - 1;
    while (z > -1) {
      int t = mapUtils.random(0, types.length - 1);
      zones[z] = 1;
      RZoneTheme rzt = (RZoneTheme) resourceManager.getResource(types[t], "theme");
      map.addZone(z, "zone " + z, rzt);
      z--;
    }

    zones[0] = 0;
    for (z = 1; z < zones.length; z++) {
      // connect to already visited zone
      int to = mapUtils.random((int) Math.max(0, z - branch), z - 1);
      map.addConnection(z, to);
      zones[z] = 0;
    }

    return map;
  }

  /**
   * Builds a World from a WorldModel (Jackson-parsed structure).
   *
   * @param model the WorldModel from Jackson XML parsing
   * @param uid the unique identifier for this map
   * @return the constructed World
   */
  private World buildWorldFromModel(WorldModel model, int uid) {
    World world = new World(model.header.name, uid, zoneFactory);
    Zone zone = world.getZone(0); // outdoor maps have only zone 0
    ItemFactory itemFactory = new ItemFactory(resourceManager);
    CreatureFactory creatureFactory = new CreatureFactory(resourceManager, uidStore, player);
    // Add regions
    for (WorldModel.RegionData regionData : model.regions) {
      Region region = buildRegionFromModel(regionData);
      zone.addRegion(region);
    }

    // Add creatures
    for (WorldModel.CreaturePlacement cp : model.creatures) {
      long creatureUID = UIDStore.getObjectUID(uid, cp.uid);
      Creature creature = creatureFactory.getCreature(cp.id, cp.x, cp.y, creatureUID);
      uidStore.addEntity(creature);
      zone.addCreature(creature);
    }

    // Add items (simple items)
    for (WorldModel.ItemPlacement ip : model.items.items) {
      long itemUID = UIDStore.getObjectUID(uid, ip.uid);
      Item item = itemFactory.getItem(ip.id, ip.x, ip.y, itemUID);
      uidStore.addEntity(item);
      zone.addItem(item);
    }

    // Add doors
    for (WorldModel.DoorPlacement dp : model.items.doors) {
      long doorUID = UIDStore.getObjectUID(uid, dp.uid);
      Door door = buildDoorFromModel(dp, uid, doorUID, itemFactory);
      uidStore.addEntity(door);
      zone.addItem(door);
    }

    // Add containers
    for (WorldModel.ContainerPlacement cp : model.items.containers) {
      long containerUID = UIDStore.getObjectUID(uid, cp.uid);
      Container container = buildContainerFromModel(cp, uid, containerUID, itemFactory);
      uidStore.addEntity(container);
      zone.addItem(container);
    }

    return world;
  }

  /**
   * Builds a Dungeon from a DungeonModel (Jackson-parsed structure).
   *
   * @param model the DungeonModel from Jackson XML parsing
   * @param uid the unique identifier for this map
   * @return the constructed Dungeon
   */
  private Dungeon buildDungeonFromModel(DungeonModel model, int uid) {
    // Check for themed dungeon
    if (model.header.theme != null) {
      return loadThemedDungeon(model.header.name, model.header.theme, uid);
    }
    ItemFactory itemFactory = new ItemFactory(resourceManager);
    CreatureFactory creatureFactory = new CreatureFactory(resourceManager, uidStore, player);
    Dungeon dungeon = new Dungeon(model.header.name, uid, zoneFactory);

    for (DungeonModel.Level levelData : model.levels) {
      int level = levelData.l;
      String name = levelData.name;

      if (levelData.theme != null) {
        // Themed zone - add theme reference
        RZoneTheme theme = (RZoneTheme) resourceManager.getResource(levelData.theme, "theme");
        dungeon.addZone(level, name, theme);

        if (levelData.out != null) {
          String[] connections = levelData.out.split(",");
          for (String connection : connections) {
            dungeon.addConnection(level, Integer.parseInt(connection.trim()));
          }
        }
      } else {
        // Explicit zone - load all content
        dungeon.addZone(level, name);
        Zone zone = dungeon.getZone(level);

        // Add regions
        for (WorldModel.RegionData regionData : levelData.regions) {
          zone.addRegion(buildRegionFromModel(regionData));
        }

        // Add creatures
        for (WorldModel.CreaturePlacement cp : levelData.creatures) {
          long creatureUID = UIDStore.getObjectUID(uid, cp.uid);
          Creature creature = creatureFactory.getCreature(cp.id, cp.x, cp.y, creatureUID);
          uidStore.addEntity(creature);
          zone.addCreature(creature);
        }

        // Add items
        for (WorldModel.ItemPlacement ip : levelData.items.items) {
          long itemUID = UIDStore.getObjectUID(uid, ip.uid);
          Item item = itemFactory.getItem(ip.id, ip.x, ip.y, itemUID);
          uidStore.addEntity(item);
          zone.addItem(item);
        }

        // Add doors
        for (WorldModel.DoorPlacement dp : levelData.items.doors) {
          long doorUID = UIDStore.getObjectUID(uid, dp.uid);
          Door door = buildDoorFromModel(dp, uid, doorUID, itemFactory);
          uidStore.addEntity(door);
          zone.addItem(door);
        }

        // Add containers
        for (WorldModel.ContainerPlacement cp : levelData.items.containers) {
          long containerUID = UIDStore.getObjectUID(uid, cp.uid);
          Container container = buildContainerFromModel(cp, uid, containerUID, itemFactory);
          uidStore.addEntity(container);
          zone.addItem(container);
        }
      }
    }

    return dungeon;
  }

  /**
   * Builds a Region from RegionData model.
   *
   * @param regionData the region data from model
   * @return the constructed Region
   */
  private Region buildRegionFromModel(WorldModel.RegionData regionData) {
    RTerrain terrain = (RTerrain) resourceManager.getResource(regionData.text, "terrain");
    RRegionTheme theme = (RRegionTheme) resourceManager.getResource(regionData.random, "theme");

    Region region =
        new Region(
            regionData.text,
            regionData.x,
            regionData.y,
            regionData.w,
            regionData.h,
            theme,
            regionData.l,
            terrain);

    region.setLabel(regionData.label);

    for (WorldModel.RegionData.ScriptReference script : regionData.scripts) {
      region.addScript(script.id, false);
    }

    return region;
  }

  /**
   * Builds a Door from DoorPlacement model.
   *
   * @param doorData the door placement data
   * @param mapUID the map UID
   * @param doorUID the door entity UID
   * @return the constructed Door
   */
  private Door buildDoorFromModel(
      WorldModel.DoorPlacement doorData, int mapUID, long doorUID, ItemFactory itemFactory) {
    Door door = (Door) itemFactory.getItem(doorData.id, doorData.x, doorData.y, doorUID);

    // Lock
    if (doorData.lock != null) {
      door.lock.setLockDC(doorData.lock);
    }

    // Key
    if (doorData.key != null) {
      RItem key = (RItem) resourceManager.getResource(doorData.key);
      door.lock.setKey(key);
    }

    // State
    if (doorData.state != null) {
      if (doorData.state.equals("locked")) {
        if (doorData.lock != null && doorData.lock > 0) {
          door.lock.setState(Lock.LOCKED);
        } else {
          door.lock.setState(Lock.CLOSED);
        }
      } else if (doorData.state.equals("closed")) {
        door.lock.setState(Lock.CLOSED);
      }
    }

    // Trap
    if (doorData.trap != null) {
      door.trap.setTrapDC(doorData.trap);
    }

    // Spell
    if (doorData.spell != null) {
      RSpell.Enchantment enchantment =
          (RSpell.Enchantment) resourceManager.getResource(doorData.spell, "magic");
      door.setMagicComponent(new Enchantment(enchantment, 0, door.getUID()));
    }

    // Destination
    if (doorData.destination != null) {
      WorldModel.DoorPlacement.Destination dest = doorData.destination;
      Point destPos = null;
      int destLevel = 0;
      int destMapUID = 0;

      if (dest.x != null && dest.y != null) {
        destPos = new Point(dest.x, dest.y);
      }
      if (dest.z != null) {
        destLevel = dest.z;
      }
      if (dest.map != null) {
        destMapUID = (mapUID & 0xFFFF0000) + dest.map;
      }

      door.portal.setDestination(destPos, destLevel, destMapUID);
      door.portal.setDestTheme(dest.theme);
      door.setSign(dest.sign);
    }

    return door;
  }

  /**
   * Builds a Container from ContainerPlacement model.
   *
   * @param containerData the container placement data
   * @param mapUID the map UID
   * @param containerUID the container entity UID
   * @return the constructed Container
   */
  private Container buildContainerFromModel(
      WorldModel.ContainerPlacement containerData,
      int mapUID,
      long containerUID,
      ItemFactory itemFactory) {
    Container container =
        (Container)
            itemFactory.getItem(containerData.id, containerData.x, containerData.y, containerUID);

    // Lock
    if (containerData.lock != null) {
      container.lock.setLockDC(containerData.lock);
      container.lock.setState(Lock.LOCKED);
    }

    // Key
    if (containerData.key != null) {
      RItem key = (RItem) resourceManager.getResource(containerData.key);
      container.lock.setKey(key);
    }

    // Trap
    if (containerData.trap != null) {
      container.trap.setTrapDC(containerData.trap);
    }

    // Spell
    if (containerData.spell != null) {
      RSpell.Enchantment enchantment =
          (RSpell.Enchantment) resourceManager.getResource(containerData.spell, "magic");
      container.setMagicComponent(new Enchantment(enchantment, 0, container.getUID()));
    }

    // Contents
    if (!containerData.contents.isEmpty()) {
      for (WorldModel.ContainerPlacement.ContainerItem contentData : containerData.contents) {
        long contentUID = UIDStore.getObjectUID(mapUID, contentData.uid);
        uidStore.addEntity(itemFactory.getItem(contentData.id, contentUID));
        container.addItem(contentUID);
      }
    } else {
      // Default items from resource definition
      for (String itemId : ((RItem.Container) container.resource).contents) {
        Item item = itemFactory.getItem(itemId, uidStore.createNewEntityUID());
        uidStore.addEntity(item);
        container.addItem(item.getUID());
      }
    }

    return container;
  }

  private void loadZone(
      Element root,
      Map map,
      int l,
      int uid,
      ItemFactory itemFactory,
      CreatureFactory creatureFactory) {
    for (Element region : root.getChild("regions").getChildren()) { // load regions
      map.getZone(l).addRegion(loadRegion(region));
    }
    if (root.getChild("creatures") != null) { // load creatures
      for (Element c : root.getChild("creatures").getChildren()) {
        String species = c.getAttributeValue("id");
        int x = Integer.parseInt(c.getAttributeValue("x"));
        int y = Integer.parseInt(c.getAttributeValue("y"));
        long creatureUID = UIDStore.getObjectUID(uid, Integer.parseInt(c.getAttributeValue("uid")));
        Creature creature = creatureFactory.getCreature(species, x, y, creatureUID);
        uidStore.addEntity(creature);
        map.getZone(l).addCreature(creature);
      }
    }
    if (root.getChild("items") != null) { // load items
      for (Element i : root.getChild("items").getChildren()) {
        long itemUID = UIDStore.getObjectUID(uid, Integer.parseInt(i.getAttributeValue("uid")));
        String id = i.getAttributeValue("id");
        int x = Integer.parseInt(i.getAttributeValue("x"));
        int y = Integer.parseInt(i.getAttributeValue("y"));
        Item item = null;
        if (i.getName().equals("container")) {
          item =
              loadContainer(
                  i, id, x, y, itemUID, uid, itemFactory); // because containers are complicated
        } else if (i.getName().equals("door")) {
          item =
              loadDoor(i, id, x, y, itemUID, uid, itemFactory); // because doors are complicated too
        } else {
          item = itemFactory.getItem(id, x, y, itemUID);
        }
        map.getZone(l).addItem(item);
        uidStore.addEntity(item);
      }
    }
  }

  /*
   * this is going to get messy, with a whole if-then-else heap
   */
  private Door loadDoor(
      Element door, String id, int x, int y, long itemUID, int mapUID, ItemFactory itemFactory) {
    Door d = (Door) itemFactory.getItem(id, x, y, itemUID);

    // lock difficulty
    int lock = 0;
    if (door.getAttribute("lock") != null) {
      lock = Integer.parseInt(door.getAttributeValue("lock"));
      d.lock.setLockDC(lock);
    }
    // key
    if (door.getAttribute("key") != null) {
      RItem key = (RItem) resourceManager.getResource(door.getAttributeValue("key"));
      d.lock.setKey(key);
    }
    // state of the door (open, closed or locked)
    if (door.getAttributeValue("state").equals("locked")) {
      if (lock > 0) {
        d.lock.setState(Lock.LOCKED);
      } else { // if there's no lock, change state to closed
        d.lock.setState(Lock.CLOSED);
      }
    } else if (door.getAttributeValue("state").equals("closed")) {
      d.lock.setState(Lock.CLOSED);
    }

    // trap
    int trap = 0;
    if (door.getAttribute("trap") != null) {
      trap = Integer.parseInt(door.getAttributeValue("trap"));
      d.trap.setTrapDC(trap);
    }
    // spell
    if (door.getAttribute("spell") != null) {
      String spell = door.getAttributeValue("spell");
      RSpell.Enchantment enchantment =
          (RSpell.Enchantment) resourceManager.getResource(spell, "magic");
      d.setMagicComponent(new Enchantment(enchantment, 0, d.getUID()));
    }

    // destination of the door
    Element dest = door.getChild("dest");
    Point destPos = null;
    int destLevel = 0;
    int destMapUID = 0;
    String theme = null;
    String sign = null;
    if (door.getChild("dest") != null) {
      int destX = -1;
      int destY = -1;
      if (dest.getAttribute("x") != null) {
        destX = Integer.parseInt(dest.getAttributeValue("x"));
      }
      if (dest.getAttribute("y") != null) {
        destY = Integer.parseInt(dest.getAttributeValue("y"));
      }
      if (destX > -1 && destY > -1) {
        destPos = new Point(destX, destY);
      }
      if (dest.getAttributeValue("z") != null) {
        destLevel = Integer.parseInt(dest.getAttributeValue("z"));
      }
      if (dest.getAttributeValue("map") != null) {
        destMapUID = (mapUID & 0xFFFF0000) + Integer.parseInt(dest.getAttributeValue("map"));
      }
      theme = dest.getAttributeValue("theme");
      sign = dest.getAttributeValue("sign");
    }

    if (dest != null) {
      d.portal.setDestination(destPos, destLevel, destMapUID);
    }
    d.portal.setDestTheme(theme);
    d.setSign(sign);
    return d;
  }

  private Container loadContainer(
      Element container,
      String id,
      int x,
      int y,
      long itemUID,
      int mapUID,
      ItemFactory itemFactory) {
    Container cont = (Container) itemFactory.getItem(id, x, y, itemUID);

    // lock difficulty
    if (container.getAttribute("lock") != null) {
      int lock = Integer.parseInt(container.getAttributeValue("lock"));
      cont.lock.setLockDC(lock);
      cont.lock.setState(Lock.LOCKED);
    }
    // key
    RItem key = null;
    if (container.getAttribute("key") != null) {
      key = (RItem) resourceManager.getResource(container.getAttributeValue("key"));
      cont.lock.setKey(key);
    }

    // trap
    int trap = 0;
    if (container.getAttribute("trap") != null) {
      trap = Integer.parseInt(container.getAttributeValue("trap"));
      cont.trap.setTrapDC(trap);
    }
    // spell
    if (container.getAttribute("spell") != null) {
      String spell = container.getAttributeValue("spell");
      RSpell.Enchantment enchantment =
          (RSpell.Enchantment) resourceManager.getResource(spell, "magic");
      cont.setMagicComponent(new Enchantment(enchantment, 0, cont.getUID()));
    }

    if (!container.getChildren("item").isEmpty()) { // if items in map file
      for (Element e : container.getChildren("item")) {
        long contentUID =
            UIDStore.getObjectUID(mapUID, Integer.parseInt(e.getAttributeValue("uid")));
        uidStore.addEntity(itemFactory.getItem(e.getAttributeValue("id"), contentUID));
        cont.addItem(contentUID);
      }
    } else { // otherwise default items
      for (String s : ((RItem.Container) cont.resource).contents) {
        Item i = itemFactory.getItem(s, uidStore.createNewEntityUID());
        uidStore.addEntity(i);
        cont.addItem(i.getUID());
      }
    }

    return cont;
  }

  private Region loadRegion(Element element) {
    int x = Integer.parseInt(element.getAttributeValue("x"));
    int y = Integer.parseInt(element.getAttributeValue("y"));
    int w = Integer.parseInt(element.getAttributeValue("w"));
    int h = Integer.parseInt(element.getAttributeValue("h"));
    byte order = Byte.parseByte(element.getAttributeValue("l"));

    String text = element.getAttributeValue("text");
    RRegionTheme theme =
        (RRegionTheme) resourceManager.getResource(element.getAttributeValue("random"), "theme");

    RTerrain rt = (RTerrain) resourceManager.getResource(text, "terrain");
    Region r = new Region(text, x, y, w, h, theme, order, rt);
    r.setLabel(element.getAttributeValue("label"));
    for (Element e : element.getChildren("script")) {
      r.addScript(e.getAttributeValue("id"), false);
    }

    return r;
  }
}
