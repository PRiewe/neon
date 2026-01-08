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
public class LItem extends RItem {
  public HashMap<String, Integer> items = new HashMap<String, Integer>();

  /** Inner class for Jackson XML parsing */
  public static class ItemEntry {
    @JacksonXmlProperty(isAttribute = true)
    public String id;

    @JacksonXmlProperty(isAttribute = true, localName = "l")
    public int level;
  }

  // No-arg constructor for Jackson deserialization
  public LItem() {
    super();
  }

  public LItem(String id, String... path) {
    super(id, Type.item, path);
  }

  // Keep JDOM constructor for backward compatibility during migration
  public LItem(Element e, String... path) {
    super(e.getAttributeValue("id"), Type.item, path);
    for (Element c : e.getChildren()) {
      items.put(c.getAttributeValue("id"), Integer.parseInt(c.getAttributeValue("l")));
    }
  }

  /** Jackson setter for item entries - converts list to HashMap */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "item")
  public void setItemList(java.util.List<ItemEntry> itemList) {
    if (itemList != null) {
      for (ItemEntry entry : itemList) {
        items.put(entry.id, entry.level);
      }
    }
  }

  /** Jackson getter for item entries - converts HashMap to list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "item")
  public java.util.List<ItemEntry> getItemList() {
    java.util.List<ItemEntry> list = new java.util.ArrayList<>();
    for (java.util.Map.Entry<String, Integer> entry : items.entrySet()) {
      ItemEntry ie = new ItemEntry();
      ie.id = entry.getKey();
      ie.level = entry.getValue();
      list.add(ie);
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
      throw new RuntimeException("Failed to serialize LItem to Element", e);
    }
  }
}
