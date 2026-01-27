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
import java.util.ArrayList;
import java.util.List;
import neon.maps.model.WorldModel.*; // Reuse inner classes from WorldModel

/**
 * Jackson model for dungeon map XML structure.
 *
 * <p>This class represents the parsed XML structure of a dungeon map file. It is designed to
 * separate XML parsing (Jackson's responsibility) from game object construction (MapLoader's
 * responsibility).
 *
 * @author priewe
 */
@JacksonXmlRootElement(localName = "dungeon")
public class DungeonModel {

  @JacksonXmlProperty(localName = "header")
  public Header header;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "level")
  public List<Level> levels = new ArrayList<>();

  /** Dungeon level with creatures, items, and regions */
  public static class Level {
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String name;

    @JacksonXmlProperty(isAttribute = true, localName = "l")
    public int l; // level number

    @JacksonXmlProperty(isAttribute = true, localName = "theme")
    public String theme; // optional theme for generation

    @JacksonXmlProperty(isAttribute = true, localName = "out")
    public String out; // comma-separated connections to other levels

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
  }
}
