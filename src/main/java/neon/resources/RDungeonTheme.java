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

package neon.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.ByteArrayInputStream;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement(localName = "dungeon")
public class RDungeonTheme extends RData {
  @JacksonXmlProperty(isAttribute = true)
  public int min;

  @JacksonXmlProperty(isAttribute = true)
  public int max;

  @JacksonXmlProperty(isAttribute = true, localName = "b")
  public int branching;

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public String zones;

  // No-arg constructor for Jackson deserialization
  public RDungeonTheme() {
    super("unknown");
  }

  public RDungeonTheme(String id, String... path) {
    super(id, path);
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RDungeonTheme(Element props, String... path) {
    super(props.getAttributeValue("id"), path);
    min = Integer.parseInt(props.getAttributeValue("min"));
    max = Integer.parseInt(props.getAttributeValue("max"));
    branching = Integer.parseInt(props.getAttributeValue("b"));
    zones = props.getAttributeValue("zones");
  }

  /**
   * Creates a JDOM Element from this resource using Jackson serialization.
   *
   * @return JDOM Element representation
   */
  public Element toElement() {
    try {
      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(this).toString();
      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RDungeonTheme to Element", e);
    }
  }
}
