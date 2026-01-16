/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
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
import java.awt.Rectangle;
import java.util.*;
import lombok.Getter;
import neon.entities.Creature;
import neon.entities.Item;
import neon.entities.UIDStore;
import neon.resources.RZoneTheme;
import neon.resources.ResourceManager;
import neon.ui.graphics.*;
import neon.util.spatial.*;
import org.jetbrains.annotations.NotNull;

public class Zone /* implements Externalizable */ {
  private static final ZComparator comparator = new ZComparator();

  @Getter private final String name;
  @Getter private final int map;
  @Getter private final int index;
  @Getter private RZoneTheme theme;

  private final HashMap<Point, Integer> lights = new HashMap<>();
  private final SimpleIndex<Long> creatures = new SimpleIndex<>();
  private final GridIndex<Long> items = new GridIndex<>();
  private final RTree<Region> regions;
  private final RTree<Long> top = new RTree<>(100, 40);
  private final UIDStore uidStore;
  private final ResourceManager resourceManager;

  /**
   * Initializes a new zone.
   *
   * @param name the zone name
   * @param map the map UID
   * @param index the zone index
   */
  public Zone(
      String name,
      int map,
      int index,
      UIDStore uidStore,
      ResourceManager resourceManager,
      RTree<Region> tree) {
    this.map = map;
    this.name = name;
    this.index = index;
    this.uidStore = uidStore;
    this.resourceManager = resourceManager;
    this.regions = tree;
  }

  /**
   * Initializes a new zone with a theme.
   *
   * @param name the zone name
   * @param map the map UID
   * @param theme the zone theme
   * @param index the zone index
   */
  public Zone(
      String name,
      int map,
      RZoneTheme theme,
      int index,
      UIDStore uidStore,
      ResourceManager resourceManager,
      RTree<Region> tree) {
    this(name, map, index, uidStore, resourceManager, tree);
    this.theme = theme;
  }

  /**
   * @param bounds
   * @return all renderables within the given bounds
   */
  public Collection<Renderable> getRenderables(Rectangle bounds) {
    ArrayList<Renderable> elements = new ArrayList<Renderable>();
    for (long uid : creatures.getElements(bounds)) {
      elements.add(uidStore.getEntity(uid).getRenderComponent());
    }
    for (long uid : items.getElements(bounds)) {
      elements.add(uidStore.getEntity(uid).getRenderComponent());
    }
    //		for(Region r : regions.getElements(bounds)) {
    elements.addAll(regions.getElements(bounds));
    //		}
    for (long uid : top.getElements(bounds)) {
      elements.add(uidStore.getEntity(uid).getRenderComponent());
    }
    return elements;
  }

  /**
   * @return whether this is a randomly generated zone
   */
  public boolean isRandom() {
    return theme != null;
  }

  public void fix() {
    theme = null;
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Returns a list of creature in this zone.
   *
   * @return the creatures in this zone
   */
  public Collection<Long> getCreatures() {
    return creatures.getElements();
  }

  /**
   * @param box a rectangle
   * @return all creatures in the given rectangle
   */
  public Collection<Creature> getCreatures(Rectangle box) {
    ArrayList<Creature> list = new ArrayList<Creature>();
    for (long uid : creatures.getElements()) {
      Creature c = (Creature) uidStore.getEntity(uid);
      Rectangle bounds = c.getShapeComponent();
      if (box.contains(bounds.x, bounds.y)) {
        list.add(c);
      }
    }
    return list;
  }

  /**
   * Returns the creature on the requested position.
   *
   * @param p a position
   * @return the creature on the given position, null if there is none
   */
  public Creature getCreature(Point p) {
    for (long uid : creatures.getElements()) {
      Creature c = (Creature) uidStore.getEntity(uid);
      Rectangle bounds = c.getShapeComponent();
      if (p.distance(bounds.x, bounds.y) < 1) {
        return c;
      }
    }
    return null;
  }

  /**
   * Adds a creature to this zone.
   *
   * @param c the creature to add
   */
  public void addCreature(Creature c) {
    Rectangle bounds = c.getShapeComponent();
    creatures.insert(c.getUID(), bounds);
  }

  public void addCreature(long uid, Rectangle bounds) {
    creatures.insert(uid, bounds);
  }

  /**
   * @return the height of this zone
   */
  public int getHeight() {
    return regions.getHeight();
  }

  /**
   * @return the width of this zone
   */
  public int getWidth() {
    return regions.getWidth();
  }

  /**
   * @param window a rectangle
   * @return all regions overlapping with the given window
   */
  public Collection<Region> getRegions(Rectangle window) {
    return regions.getElements(window);
  }

  /**
   * @param point a point
   * @return all regions containing the given point
   */
  public Collection<Region> getRegions(Point point) {
    return regions.getElements(new Rectangle(point.x, point.y, 1, 1));
  }

  /**
   * @param point a position in this zone
   * @return all items on the given position
   */
  public Collection<Long> getItems(Point point) {
    return items.getElements(point);
  }

  /**
   * @param box a rectangle
   * @return all items within the given rectangle
   */
  public Collection<Long> getItems(Rectangle box) {
    return items.getElements(box);
  }

  public Collection<Long> getItems() {
    return items.getElements();
  }

  /**
   * @param p a point
   * @return the highest-order region containing this point
   */
  public Region getRegion(Point p) {
    ArrayList<Region> buffer = new ArrayList<Region>(getRegions(p));
    buffer.sort(comparator);
    return !buffer.isEmpty() ? buffer.getLast() : null;
  }

  /**
   * Adds a region to this map.
   *
   * @param r the region to add
   */
  public void addRegion(Region r) {
    regions.insert(r, r.getBounds());
  }

  /**
   * Removes a region from this map.
   *
   * @param r the region to remove
   */
  public void removeRegion(Region r) {
    regions.remove(r);
  }

  /**
   * @return a <code>Collection</code> with all regions in this map.
   */
  public Collection<Region> getRegions() {
    return regions.getElements();
  }

  public void addItem(@NotNull Item item) {
    Rectangle bounds = item.getShapeComponent();
    if (item.resource.top) {
      top.insert(item.getUID(), bounds);
    } else {
      items.insert(item.getUID(), bounds);
    }
    if (item instanceof Item.Light) {
      Point p = bounds.getLocation();
      if (!lights.containsKey(p)) {
        lights.put(p, 0);
      }
      lights.put(p, lights.get(p) + 1);
    }
  }

  /**
   * Removes a creature from this map.
   *
   * @param uid the uid of the creature to remove
   */
  public void removeCreature(long uid) {
    creatures.remove(uid);
  }

  public void removeItem(Item item) {
    items.remove(item.getUID());
    if (item instanceof Item.Light) {
      Rectangle bounds = item.getShapeComponent();
      Point point = new Point(bounds.x, bounds.y);
      lights.put(point, lights.get(point) - 1);
      if (lights.get(point) < 1) {
        lights.remove(point);
      }
    }
  }

  public int getTopSize() {
    return top.size();
  }

  public Collection<Long> getTopElements() {
    return top.getElements();
  }

  /**
   * @return a hashmap with all lights in this zone
   */
  public HashMap<Point, Integer> getLightMap() {
    return lights;
  }

  public int getEstimatedMemory() {
    return 32
        + (top.getElements().size() + creatures.getElements().size() + items.getElements().size())
            * 8;
  }

  //  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
  //    index = in.readInt();
  //    map = in.readInt();
  //    name = in.readUTF();
  //    String t = in.readUTF();
  //    if (!t.isEmpty()) {
  //      theme = (RZoneTheme) Engine.getResources().getResource(t, "theme");
  //    }
  //
  //    // items
  //    top = new RTree<Long>(100, 40);
  //    items = new GridIndex<Long>();
  //    lights = new HashMap<Point, Integer>();
  //    int iSize = in.readInt();
  //    for (int i = 0; i < iSize; i++) {
  //      long uid = in.readLong();
  //      Item item = (Item) Engine.getStore().getEntity(uid);
  //      addItem(item);
  //    }
  //    int tSize = in.readInt();
  //    for (int i = 0; i < tSize; i++) {
  //      long uid = in.readLong();
  //      Item item = (Item) Engine.getStore().getEntity(uid);
  //      addItem(item);
  //    }
  //
  //    // creatures
  //    creatures = new SimpleIndex<Long>();
  //    int cSize = in.readInt();
  //    for (int i = 0; i < cSize; i++) {
  //      long uid = in.readLong();
  //      Rectangle bounds = Engine.getStore().getEntity(uid).getShapeComponent();
  //      creatures.insert(uid, bounds);
  //    }
  //
  //    // regions
  //    regions = new RTree<Region>(100, 40, Engine.getAtlas().getCache(), map + ":" + index);
  //  }
  //
  //  public void writeExternal(ObjectOutput out) throws IOException {
  //    out.writeInt(index);
  //    out.writeInt(map);
  //    out.writeUTF(name);
  //    if (theme != null) {
  //      out.writeUTF(theme.id);
  //    } else {
  //      out.writeUTF("");
  //    }
  //
  //    // items
  //    out.writeInt(items.getElements().size());
  //    for (long l : items.getElements()) {
  //      out.writeLong(l);
  //    }
  //    out.writeInt(top.getElements().size());
  //    for (long l : top.getElements()) {
  //      out.writeLong(l);
  //    }
  //
  //    // creatures
  //    out.writeInt(creatures.getElements().size());
  //    for (long l : creatures.getElements()) {
  //      out.writeLong(l);
  //    }
  //  }
}
