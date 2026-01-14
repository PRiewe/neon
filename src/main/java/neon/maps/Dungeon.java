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

import java.nio.ByteBuffer;
import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import neon.maps.mvstore.MVUtils;
import neon.maps.mvstore.ZoneType;
import neon.resources.RZoneTheme;
import neon.util.Graph;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

/**
 * A dungeon. It can contain several interconnected zones.
 *
 * @author mdriesen
 */
public class Dungeon implements Map {
  @Setter @Getter private String name;
  private final int uid;
  private final Graph<Zone> zones;
  private final ZoneFactory zoneFactory;

  /**
   * Initialize a dungeon.
   *
   * @param name the name of this dungeon
   * @param uid the unique identifier of this dungeon
   */
  public Dungeon(String name, int uid, ZoneFactory zoneFactory) {
    this(name, uid, new Graph<>(), zoneFactory);
  }

  private Dungeon(String name, int uid, Graph<Zone> zones, ZoneFactory zoneFactory) {
    this.name = name;
    this.uid = uid;
    this.zones = zones;
    this.zoneFactory = zoneFactory;
  }

  public Zone getZone(int i) {
    return zones.getNodeContent(i);
  }

  public int getUID() {
    return uid;
  }

  /** Adds an empty zone to this dungeon. */
  public void addZone(int zone, String name) {
    zones.addNode(zone, zoneFactory.createZone(name, uid, zone));
  }

  /** Adds an empty zone to this dungeon. */
  public void addZone(int zone, String name, RZoneTheme theme) {
    zones.addNode(zone, zoneFactory.createZoneWithTheme(name, uid, zone, theme));
  }

  public Collection<Zone> getZones() {
    return zones.getNodes();
  }

  public String getZoneName(int zone) {
    return zones.getNodeContent(zone).getName();
  }

  /**
   * Adds a connection between two zones in this dungeon.
   *
   * @param from
   * @param to
   */
  public void addConnection(int from, int to) {
    zones.addConnection(from, to, true);
  }

  /**
   * @param from
   * @return all connections originating from the given zone
   */
  public Collection<Integer> getConnections(int from) {
    return zones.getConnections(from);
  }

  public static class DungeonDataType extends BasicDataType<Dungeon> {

    private final ZoneFactory zoneFactory;
    private final ZoneType zoneDataType;

    public DungeonDataType(ZoneFactory zoneFactory) {
      this.zoneFactory = zoneFactory;
      zoneDataType = new ZoneType(zoneFactory);
    }

    @Override
    public int getMemory(Dungeon obj) {
      return obj.name.length() + 16 * obj.zones.getNodes().size();
    }

    @Override
    public void write(WriteBuffer buff, Dungeon obj) {
      MVUtils.writeString(buff, obj.name);
      buff.putInt(obj.uid);
      int graphSize = obj.zones.getGraphContent().size();
      buff.putInt(graphSize);

      for (var entry : obj.zones.getGraphContent()) {
        buff.putInt(entry.getKey());
        var node = entry.getValue();
        zoneDataType.write(buff, node.getContent());

        var conns = node.getConnections();
        buff.putInt(conns.size());
        for (Integer conn : conns) {
          buff.putInt(conn);
        }
      }
    }

    @Override
    public Dungeon read(ByteBuffer buff) {
      String name = MVUtils.readString(buff);

      int uid = buff.getInt();
      Graph<Zone> zones = new Graph<>();
      int graphSize = buff.getInt();
      for (int i = 0; i < graphSize; i++) {
        int index = buff.getInt();
        Zone zone = zoneDataType.read(buff);
        zones.addNode(index, zone);
        Graph.Node<Zone> node = zones.getNode(index);
        int numConnections = buff.getInt();
        for (int j = 0; j < numConnections; j++) {
          int connection = buff.getInt();
          node.addConnection(connection);
        }
      }
      return new Dungeon(name, uid, zones, zoneFactory);
    }

    /**
     * Create storage object of array type to hold values
     *
     * @param size number of values to hold
     * @return storage object
     */
    @Override
    public Dungeon[] createStorage(int size) {
      return new Dungeon[size];
    }
  }
}
