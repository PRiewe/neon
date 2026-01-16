/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Maarten Driesen
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

package neon.resources.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson model for character creation configuration XML structure (cc.xml).
 *
 * <p>This class represents the parsed XML structure of a character creation configuration file.
 *
 * <p>Example:
 *
 * <pre>{@code
 * <root>
 *   <race>dwarf</race>
 *   <race>dark elf</race>
 *   <map path="world" x="34020" y="15170" z="0" />
 *   <item>wool pants</item>
 *   <spell>heal</spell>
 * </root>
 * }</pre>
 *
 * @author mdriesen
 */
@JacksonXmlRootElement(localName = "root")
public class CharacterCreationModel {

  /** List of available races for character creation. */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "race")
  public List<String> races = new ArrayList<>();

  /** Starting location for new characters. */
  @JacksonXmlProperty(localName = "map")
  public MapLocation map;

  /** List of starting items. */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "item")
  public List<String> items = new ArrayList<>();

  /** List of starting spells. */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "spell")
  public List<String> spells = new ArrayList<>();

  /** Starting map location configuration. */
  public static class MapLocation {
    @JacksonXmlProperty(isAttribute = true)
    public String path;

    @JacksonXmlProperty(isAttribute = true)
    public int x;

    @JacksonXmlProperty(isAttribute = true)
    public int y;

    @JacksonXmlProperty(isAttribute = true)
    public int z;
  }
}
