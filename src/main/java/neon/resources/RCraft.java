/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
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

@JacksonXmlRootElement(localName = "craft")
public class RCraft extends RData {
  @JacksonXmlProperty(isAttribute = true)
  public String raw;

  @JacksonXmlProperty(isAttribute = true)
  public int amount;

  @JacksonXmlProperty(isAttribute = true)
  public int cost;

  @JacksonXmlProperty(isAttribute = true, localName = "result")
  @JsonProperty(required = false)
  private String resultName; // Maps to 'name' field in parent

  // No-arg constructor for Jackson deserialization
  public RCraft() {
    super("unknown");
  }

  /**
   * Sync result name to parent name field (called by Jackson after deserialization).
   *
   * @param resultName the result name
   */
  public void setResult(String resultName) {
    this.resultName = resultName;
    this.name = resultName;
  }

  /**
   * Get result name for serialization.
   *
   * @return result name
   */
  public String getResult() {
    return name;
  }

  public RCraft(String id, RItem item, String... path) {
    super(Double.toString(Math.random()), path);
    name = item.id;
    raw = item.id;
  }

  public String toString() {
    return name;
  }

  public RCraft(RCraft procedure) {
    super(procedure.id, procedure.path);
    raw = procedure.raw;
    name = procedure.name;
    amount = procedure.amount;
    cost = procedure.cost;
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RCraft(Element properties, String... path) {
    super(properties.getAttributeValue("id"), path);
    name = properties.getAttributeValue("result");
    raw = properties.getAttributeValue("raw");
    amount = Integer.parseInt(properties.getAttributeValue("amount"));
    cost = Integer.parseInt(properties.getAttributeValue("cost"));
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
      throw new RuntimeException("Failed to serialize RCraft to Element", e);
    }
  }
}
