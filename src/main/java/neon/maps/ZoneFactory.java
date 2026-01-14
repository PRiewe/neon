/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2024 - Maarten Driesen
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

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import neon.core.GameStores;
import neon.entities.Item;
import neon.entities.UIDStore;
import neon.maps.mvstore.MVUtils;
import neon.maps.mvstore.RegionDataType;
import neon.resources.RZoneTheme;
import neon.util.mapstorage.MapStore;
import neon.util.spatial.RTree;
import org.h2.mvstore.WriteBuffer;

/**
 * Factory for creating Zone instances with proper dependency injection. Eliminates the constructor
 * side effect of accessing Engine.getAtlas().getCache().
 *
 * @author mdriesen
 */
public class ZoneFactory {
  private final MapStore cache;
  private final UIDStore uidStore;
  private final ResourceManager resourceManager;
  private final RegionDataType regionDataType;

  /**
   * Creates a new ZoneFactory with the given cache database.
   *
   * @param cache the MapDB cache database for spatial indices
   */
  public ZoneFactory(MapStore cache) {
    this.cache = cache;
    this.uidStore = uidStore;
    this.resourceManager = resourceManager;
    this.regionDataType = new RegionDataType(resourceManager);
  }

  public ZoneFactory(GameStores gameStore) {
    this(gameStore.getAtlas().getCache(), gameStore.getStore(), gameStore.getResources());
  }

  public Zone createZone(String name, int map, int index) {
    RTree<Region> regions = new RTree<>(100, 40, cache, map + ":" + index, regionDataType);
    return new Zone(name, map, index, uidStore, resourceManager, regions);
  }

  public Zone createZoneWithTheme(String name, int map, int index, RZoneTheme theme) {
    RTree<Region> regions = new RTree<>(100, 40, cache, map + ":" + index, regionDataType);
    return new Zone(name, map, theme, index, uidStore, resourceManager, regions);
  }

  public Zone readZoneFromExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int index = in.readInt();
    int map = in.readInt();
    String name = in.readUTF();
    String t = in.readUTF();
    Zone theZone;
    if (!t.isEmpty()) {
      RZoneTheme theme = (RZoneTheme) resourceManager.getResource(t, "theme");
      theZone = createZoneWithTheme(name, map, index, theme);
    } else {
      theZone = createZone(name, map, index);
    }

    int iSize = in.readInt();
    for (int i = 0; i < iSize; i++) {
      long uid = in.readLong();
      Item item = (Item) uidStore.getEntity(uid);
      theZone.addItem(item);
    }
    int tSize = in.readInt();
    for (int i = 0; i < tSize; i++) {
      long uid = in.readLong();
      Item item = (Item) uidStore.getEntity(uid);
      theZone.addItem(item);
    }

    int cSize = in.readInt();
    for (int i = 0; i < cSize; i++) {
      long uid = in.readLong();
      Rectangle bounds = uidStore.getEntity(uid).getShapeComponent();
      theZone.addCreature(uid, bounds);
    }

    return theZone;
  }

  public Zone readZoneByteBuffer(ByteBuffer in) throws IOException, ClassNotFoundException {
    int index = in.getInt();
    int map = in.getInt();
    String name = MVUtils.readString(in);
    String t = MVUtils.readString(in);
    Zone theZone;
    if (t != null) {
      RZoneTheme theme = (RZoneTheme) resourceManager.getResource(t, "theme");
      theZone = createZoneWithTheme(name, map, index, theme);
    } else {
      theZone = createZone(name, map, index);
    }

    int iSize = in.getInt();
    for (int i = 0; i < iSize; i++) {
      long uid = in.getLong();
      Item item = (Item) uidStore.getEntity(uid);
      theZone.addItem(item);
    }
    int tSize = in.getInt();
    for (int i = 0; i < tSize; i++) {
      long uid = in.getLong();
      Item item = (Item) uidStore.getEntity(uid);
      theZone.addItem(item);
    }

    int cSize = in.getInt();
    for (int i = 0; i < cSize; i++) {
      long uid = in.getLong();
      Rectangle bounds = uidStore.getEntity(uid).getShapeComponent();
      theZone.addCreature(uid, bounds);
    }

    return theZone;
  }

  public void writeZoneToWriteBuffer(WriteBuffer out, Zone zone) throws IOException {
    out.putInt(zone.getIndex());
    out.putInt(zone.getMap());
    MVUtils.writeString(out, zone.getName());

    if (zone.getTheme() != null) {
      MVUtils.writeString(out, zone.getTheme().id);

    } else {
      MVUtils.writeString(out, null);
    }

    // items
    out.putInt(zone.getItems().size());
    for (long l : zone.getItems()) {
      out.putLong(l);
    }
    out.putInt(zone.getTopSize());
    for (long l : zone.getTopElements()) {
      out.putLong(l);
    }

    // creatures
    out.putInt(zone.getCreatures().size());
    for (long l : zone.getCreatures()) {
      out.putLong(l);
    }
  }

  public void writeZoneToExternal(ObjectOutput out, Zone zone) throws IOException {
    out.writeInt(zone.getIndex());
    out.writeInt(zone.getMap());
    out.writeUTF(zone.getName());
    if (zone.getTheme() != null) {
      out.writeUTF(zone.getTheme().id);
    } else {
      out.writeUTF("");
    }

    // items
    out.writeInt(zone.getItems().size());
    for (long l : zone.getItems()) {
      out.writeLong(l);
    }
    out.writeInt(zone.getTopSize());
    for (long l : zone.getTopElements()) {
      out.writeLong(l);
    }

    // creatures
    out.writeInt(zone.getCreatures().size());
    for (long l : zone.getCreatures()) {
      out.writeLong(l);
    }
  }
}
