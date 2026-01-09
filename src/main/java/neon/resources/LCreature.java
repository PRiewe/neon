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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement(localName = "list")
public class LCreature extends RCreature {
  public HashMap<String, Integer> creatures = new HashMap<String, Integer>();

  /** Inner class for Jackson XML parsing */
  public static class CreatureEntry {
    @JacksonXmlProperty(isAttribute = true)
    public String id;

    @JacksonXmlProperty(isAttribute = true, localName = "l")
    public int level;
  }

  // No-arg constructor for Jackson deserialization
  public LCreature() {
    super();
  }

  public LCreature(String id, String... path) {
    super(id, path);
  }

  // Keep JDOM constructor for backward compatibility during migration
  public LCreature(Element e, String... path) {
    super(e.getAttributeValue("id"), path);
    for (Element c : e.getChildren()) {
      creatures.put(c.getAttributeValue("id"), Integer.parseInt(c.getAttributeValue("l")));
    }
  }

  /** Jackson setter for creature entries - converts list to HashMap */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "creature")
  public void setCreatureList(java.util.List<CreatureEntry> creatureList) {
    if (creatureList != null) {
      for (CreatureEntry entry : creatureList) {
        creatures.put(entry.id, entry.level);
      }
    }
  }

  /** Jackson getter for creature entries - converts HashMap to list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "creature")
  public java.util.List<CreatureEntry> getCreatureList() {
    java.util.List<CreatureEntry> list = new java.util.ArrayList<>();
    for (java.util.Map.Entry<String, Integer> entry : creatures.entrySet()) {
      CreatureEntry ce = new CreatureEntry();
      ce.id = entry.getKey();
      ce.level = entry.getValue();
      list.add(ce);
    }
    return list;
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
      throw new RuntimeException("Failed to serialize LCreature to Element", e);
    }
  }
}
