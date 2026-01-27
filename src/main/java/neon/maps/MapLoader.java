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
import neon.core.*;
import neon.entities.Container;
import neon.entities.Creature;
import neon.entities.Door;
import neon.entities.EntityFactory;
import neon.entities.Item;
import neon.entities.UIDStore;
import neon.entities.components.Enchantment;
import neon.entities.components.Lock;
import neon.maps.services.EntityStore;
import neon.maps.services.ResourceProvider;
import neon.resources.RDungeonTheme;
import neon.resources.RItem;
import neon.resources.RRegionTheme;
import neon.resources.RSpell;
import neon.resources.RTerrain;
import neon.resources.RZoneTheme;
import neon.systems.files.XMLTranslator;
import org.jdom2.*;

/**
 * This class loads a map from an xml file.
 *
 * <p>Refactored to support dependency injection for better testability and reduced coupling.
 *
 * @author mdriesen
 */
public class MapLoader {
  private final EntityStore entityStore;
  private final ResourceProvider resourceProvider;
  private final MapUtils mapUtils;
  private final GameContext gameContext;
  private final EntityFactory entityFactory;

  /**
   * Creates a MapLoader with dependency injection.
   *
   * @param gameContext
   */
  public MapLoader(GameContext gameContext) {
    this(new MapUtils(), gameContext);
  }

  /**
   * Creates a MapLoader with dependency injection and custom random source.
   *
   * @param mapUtils the MapUtils instance for random operations
   */
  public MapLoader(MapUtils mapUtils, GameContext gameContext) {
    this.entityStore = gameContext.getStore();
    this.resourceProvider = gameContext.getResources();
    this.mapUtils = mapUtils;
    this.gameContext = gameContext;
    this.entityFactory = new EntityFactory(gameContext);
  }

  /**
   * Returns a map described in an xml file with the given name (instance method).
   *
   * @param path the pathname of a map file
   * @param uid the unique identifier of this map
   * @return the <code>Map</code> described by the map file
   */
  public Map loadMap(String[] path, int uid) {

    Document doc = gameContext.getFileSystem().getFile(new XMLTranslator(), path);
    Element root = doc.getRootElement();
    if (root.getName().equals("world")) {
      return loadWorld(root, uid);
    } else {
      return loadDungeon(root, uid);
    }
  }

  /**
   * Loads a dungeon behind a themed door.
   *
   * @param theme the theme ID
   * @return a new Dungeon
   * @deprecated Use instance method {@link #loadThemedDungeon(String, String, int)} instead
   */
  public Dungeon loadDungeon(String theme) {

    return this.loadThemedDungeon(theme, theme, this.entityStore.createNewMapUID());
  }

  private World loadWorld(Element root, int uid) {
    World world = new World(root.getChild("header").getChildText("name"), uid);
    loadZone(root, world, 0, uid); // outdoor has only 1 zone, namely 0
    return world;
  }

  private Dungeon loadDungeon(Element root, int uid) {
    if (root.getChild("header").getAttribute("theme") != null) {
      String name = root.getChild("header").getChildText("name");
      return loadThemedDungeon(name, root.getChild("header").getAttributeValue("theme"), uid);
    }

    Dungeon map = new Dungeon(root.getChild("header").getChildText("name"), uid);

    for (Element l : root.getChildren("level")) {
      int level = Integer.parseInt(l.getAttributeValue("l"));
      String name = l.getAttributeValue("name");
      if (l.getAttribute("theme") != null) {
        RZoneTheme theme =
            (RZoneTheme) resourceProvider.getResource(l.getAttributeValue("theme"), "theme");
        map.addZone(level, name, theme);
        if (l.getAttribute("out") != null) {
          String[] connections = l.getAttributeValue("out").split(",");
          for (String connection : connections) {
            map.addConnection(level, Integer.parseInt(connection));
          }
        }
      } else {
        map.addZone(level, name);
        loadZone(l, map, level, uid);
      }
    }

    return map;
  }

  private Dungeon loadThemedDungeon(String name, String dungeon, int uid) {
    Dungeon map = new Dungeon(name, uid);
    RDungeonTheme theme = (RDungeonTheme) resourceProvider.getResource(dungeon, "theme");

    int minZ = theme.min;
    int maxZ = theme.max;
    float branch = theme.branching;
    String[] types = theme.zones.split(";");

    int[] zones = new int[mapUtils.random(minZ, maxZ)];
    int z = zones.length - 1;
    while (z > -1) {
      int t = mapUtils.random(0, types.length - 1);
      zones[z] = 1;
      RZoneTheme rzt = (RZoneTheme) resourceProvider.getResource(types[t], "theme");
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

  private void loadZone(Element root, Map map, int l, int uid) {
    for (Element region : root.getChild("regions").getChildren()) { // load regions
      map.getZone(l).addRegion(loadRegion(region));
    }
    if (root.getChild("creatures") != null) { // load creatures
      for (Element c : root.getChild("creatures").getChildren()) {
        String species = c.getAttributeValue("id");
        int x = Integer.parseInt(c.getAttributeValue("x"));
        int y = Integer.parseInt(c.getAttributeValue("y"));
        long creatureUID = UIDStore.getObjectUID(uid, Integer.parseInt(c.getAttributeValue("uid")));
        Creature creature = entityFactory.getCreature(species, x, y, creatureUID);
        entityStore.addEntity(creature);
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
          item = loadContainer(i, id, x, y, itemUID, uid); // because containers are complicated
        } else if (i.getName().equals("door")) {
          item = loadDoor(i, id, x, y, itemUID, uid); // because doors are complicated too
        } else {
          item = entityFactory.getItem(id, x, y, itemUID);
        }
        map.getZone(l).addItem(item);
        entityStore.addEntity(item);
      }
    }
  }

  /*
   * this is going to get messy, with a whole if-then-else heap
   */
  private Door loadDoor(Element door, String id, int x, int y, long itemUID, int mapUID) {
    Door d = (Door) entityFactory.getItem(id, x, y, itemUID);

    // lock difficulty
    int lock = 0;
    if (door.getAttribute("lock") != null) {
      lock = Integer.parseInt(door.getAttributeValue("lock"));
      d.lock.setLockDC(lock);
    }
    // key
    if (door.getAttribute("key") != null) {
      RItem key = (RItem) resourceProvider.getResource(door.getAttributeValue("key"));
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
          (RSpell.Enchantment) resourceProvider.getResource(spell, "magic");
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
      Element container, String id, int x, int y, long itemUID, int mapUID) {
    Container cont = (Container) entityFactory.getItem(id, x, y, itemUID);

    // lock difficulty
    if (container.getAttribute("lock") != null) {
      int lock = Integer.parseInt(container.getAttributeValue("lock"));
      cont.lock.setLockDC(lock);
      cont.lock.setState(Lock.LOCKED);
    }
    // key
    RItem key = null;
    if (container.getAttribute("key") != null) {
      key = (RItem) resourceProvider.getResource(container.getAttributeValue("key"));
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
          (RSpell.Enchantment) resourceProvider.getResource(spell, "magic");
      cont.setMagicComponent(new Enchantment(enchantment, 0, cont.getUID()));
    }

    if (!container.getChildren("item").isEmpty()) { // if items in map file
      for (Element e : container.getChildren("item")) {
        long contentUID =
            UIDStore.getObjectUID(mapUID, Integer.parseInt(e.getAttributeValue("uid")));
        entityStore.addEntity(entityFactory.getItem(e.getAttributeValue("id"), contentUID));
        cont.addItem(contentUID);
      }
    } else { // otherwise default items
      for (String s : ((RItem.Container) cont.resource).contents) {
        Item i = entityFactory.getItem(s, entityStore.createNewEntityUID());
        entityStore.addEntity(i);
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
        (RRegionTheme) resourceProvider.getResource(element.getAttributeValue("random"), "theme");

    RTerrain rt = (RTerrain) resourceProvider.getResource(text, "terrain");
    Region r = new Region(text, x, y, w, h, theme, order, rt);
    r.setLabel(element.getAttributeValue("label"));
    for (Element e : element.getChildren("script")) {
      r.addScript(e.getAttributeValue("id"), false);
    }

    return r;
  }
}
