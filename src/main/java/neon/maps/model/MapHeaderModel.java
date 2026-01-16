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

package neon.maps.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Lightweight Jackson model for extracting only the header from map XML files.
 *
 * <p>This class is used when only the map UID is needed, avoiding parsing the entire map file which
 * can contain thousands of entities.
 *
 * <p>Example:
 *
 * <pre>{@code
 * <world>
 *   <header uid="1">
 *     <name>Aneirin</name>
 *   </header>
 *   ...
 * </world>
 * }</pre>
 *
 * @author mdriesen
 */
public class MapHeaderModel {

  /** The map header containing UID and name. */
  @JacksonXmlProperty(localName = "header")
  public Header header;

  /** Map header information. */
  public static class Header {
    /** The unique identifier for this map. */
    @JacksonXmlProperty(isAttribute = true)
    public short uid;

    /** The display name of the map (optional). */
    @JacksonXmlProperty(localName = "name")
    public String name;
  }
}
