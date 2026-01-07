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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.ByteArrayInputStream;
import neon.entities.property.Ability;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement(localName = "tattoo")
public class RTattoo extends RData {
  @JacksonXmlProperty(isAttribute = true)
  public Ability ability;

  @JacksonXmlProperty(isAttribute = true, localName = "size")
  public int magnitude;

  @JacksonXmlProperty(isAttribute = true)
  public int cost;

  // No-arg constructor for Jackson deserialization
  public RTattoo() {
    super("unknown");
  }

  public RTattoo(String id, String... path) {
    super(id, path);
    name = id;
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RTattoo(Element tattoo, String... path) {
    super(tattoo, path);
    ability = Ability.valueOf(tattoo.getAttributeValue("ability").toUpperCase());
    magnitude = Integer.parseInt(tattoo.getAttributeValue("size"));
    cost = Integer.parseInt(tattoo.getAttributeValue("cost"));
    if (tattoo.getAttribute("name") != null) {
      name = tattoo.getAttributeValue("name");
    } else {
      name = id;
    }
  }

  /**
   * Creates a JDOM Element from this resource using Jackson serialization.
   *
   * @return JDOM Element representation
   */
  @Override
  public Element toElement() {
    try {
      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(this).toString();
      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RTattoo to Element", e);
    }
  }
}
