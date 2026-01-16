/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Peter Riewe
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

package neon.maps.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson model for world map XML structure.
 *
 * <p>This class represents the parsed XML structure of a world map file. It is designed to separate
 * XML parsing (Jackson's responsibility) from game object construction (MapLoader's
 * responsibility).
 *
 * @author priewe
 */
@JacksonXmlRootElement(localName = "world")
public class WorldModel {

  @JacksonXmlProperty(localName = "header")
  public Header header;

  @JacksonXmlElementWrapper(localName = "creatures")
  @JacksonXmlProperty(localName = "creature")
  public List<CreaturePlacement> creatures = new ArrayList<>();

  @JacksonXmlElementWrapper(localName = "items")
  @JacksonXmlProperty(localName = "item")
  public List<ItemPlacement> items = new ArrayList<>();

  @JacksonXmlElementWrapper(localName = "doors")
  @JacksonXmlProperty(localName = "door")
  public List<DoorPlacement> doors = new ArrayList<>();

  @JacksonXmlElementWrapper(localName = "containers")
  @JacksonXmlProperty(localName = "container")
  public List<ContainerPlacement> containers = new ArrayList<>();

  @JacksonXmlElementWrapper(localName = "regions")
  @JacksonXmlProperty(localName = "region")
  public List<RegionData> regions = new ArrayList<>();


  /** Map header with name and UID */
  public static class Header implements Serializable {
    @JacksonXmlProperty(isAttribute = true, localName = "uid")
    public int uid;

    @JacksonXmlProperty(isAttribute = true, localName = "theme")
    public String theme; // optional for themed dungeons

    @JacksonXmlProperty(localName = "name")
    public String name;
  }

  /** Creature placement in the world */
  public static class CreaturePlacement implements Serializable {
    @JacksonXmlProperty(isAttribute = true, localName = "x")
    public int x;

    @JacksonXmlProperty(isAttribute = true, localName = "y")
    public int y;

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    public String id;

    @JacksonXmlProperty(isAttribute = true, localName = "uid")
    public int uid;
  }

  /** Base class for item placement (can be item, door, or container) */
  public static class ItemPlacement implements Serializable {
    @JacksonXmlProperty(isAttribute = true, localName = "x")
    public int x;

    @JacksonXmlProperty(isAttribute = true, localName = "y")
    public int y;

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    public String id;

    @JacksonXmlProperty(isAttribute = true, localName = "uid")
    public int uid;
  }

  /** Door placement with destination and state */
  public static class DoorPlacement extends ItemPlacement {
    @JacksonXmlProperty(isAttribute = true, localName = "state")
    public String state; // open, closed, locked

    @JacksonXmlProperty(isAttribute = true, localName = "lock")
    public Integer lock; // lock difficulty (optional)

    @JacksonXmlProperty(isAttribute = true, localName = "key")
    public String key; // key item ID (optional)

    @JacksonXmlProperty(isAttribute = true, localName = "trap")
    public Integer trap; // trap difficulty (optional)

    @JacksonXmlProperty(isAttribute = true, localName = "spell")
    public String spell; // spell ID for trapped door (optional)

    @JacksonXmlProperty(localName = "dest")
    public Destination destination;

    /** Door destination */
    public static class Destination implements Serializable {
      @JacksonXmlProperty(isAttribute = true, localName = "x")
      public Integer x;

      @JacksonXmlProperty(isAttribute = true, localName = "y")
      public Integer y;

      @JacksonXmlProperty(isAttribute = true, localName = "z")
      public Integer z; // level/zone

      @JacksonXmlProperty(isAttribute = true, localName = "map")
      public Integer map; // map UID

      @JacksonXmlProperty(isAttribute = true, localName = "theme")
      public String theme; // themed dungeon

      @JacksonXmlProperty(isAttribute = true, localName = "sign")
      public String sign; // destination label
    }
  }

  /** Container placement with contents */
  public static class ContainerPlacement extends ItemPlacement {
    @JacksonXmlProperty(isAttribute = true, localName = "lock")
    public Integer lock; // lock difficulty (optional)

    @JacksonXmlProperty(isAttribute = true, localName = "key")
    public String key; // key item ID (optional)

    @JacksonXmlProperty(isAttribute = true, localName = "trap")
    public Integer trap; // trap difficulty (optional)

    @JacksonXmlProperty(isAttribute = true, localName = "spell")
    public String spell; // spell ID for trapped container (optional)

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    public List<ContainerItem> contents = new ArrayList<>();

    /** Item inside a container */
    public static class ContainerItem {
      @JacksonXmlProperty(isAttribute = true, localName = "id")
      public String id;

      @JacksonXmlProperty(isAttribute = true, localName = "uid")
      public int uid;
    }
  }

  /** Region data for terrain generation */
  public static class RegionData implements Serializable {
    @JacksonXmlProperty(isAttribute = true, localName = "x")
    public int x;

    @JacksonXmlProperty(isAttribute = true, localName = "y")
    public int y;

    @JacksonXmlProperty(isAttribute = true, localName = "w")
    public int w; // width

    @JacksonXmlProperty(isAttribute = true, localName = "h")
    public int h; // height

    @JacksonXmlProperty(isAttribute = true, localName = "l")
    public byte l; // layer/order

    @JacksonXmlProperty(isAttribute = true, localName = "text")
    public String text; // terrain texture ID

    @JacksonXmlProperty(isAttribute = true, localName = "random")
    public String random; // theme ID for random generation

    @JacksonXmlProperty(isAttribute = true, localName = "label")
    public String label; // optional label

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "script")
    public List<ScriptReference> scripts = new ArrayList<>();

    /** Script reference */
    public static class ScriptReference implements Serializable {
      @JacksonXmlProperty(isAttribute = true, localName = "id")
      public String id;
    }
  }
}
