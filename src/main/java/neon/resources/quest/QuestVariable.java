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

package neon.resources.quest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.ByteArrayInputStream;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 * Represents a quest variable used for dynamic content in quests.
 *
 * <p>Quest variables are placeholders (like $item$, $npc$) that get resolved at runtime to specific
 * game objects. They are stored in the quest XML as elements like:
 *
 * <pre>{@code
 * <item id="dagger,scimitar">item</item>
 * <npc id="trader,merchant">npc</npc>
 * <creature>target</creature>
 * }</pre>
 *
 * @author Peter Riewe
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestVariable {
  /**
   * The variable name/placeholder (e.g., "item", "npc", "target"). This is the text content of the
   * XML element.
   */
  @JacksonXmlText public String name;

  /**
   * The category/type of the variable, determines what kind of object this resolves to. This is the
   * XML element name (e.g., "item", "npc", "creature").
   */
  public transient String category;

  /**
   * Optional comma-separated list of specific IDs this variable can resolve to (e.g.,
   * "dagger,scimitar"). When null, any object of the category can be chosen.
   */
  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public String id;

  /**
   * Optional type filter for items (e.g., "light", "weapon"). Only used for item variables to
   * filter by item type.
   */
  @JacksonXmlProperty(isAttribute = true, localName = "type")
  @JsonProperty(required = false)
  public String typeFilter;

  /** No-arg constructor for Jackson deserialization. */
  public QuestVariable() {}

  /**
   * Creates a quest variable with all fields.
   *
   * @param name Variable placeholder name
   * @param category Variable category (item/npc/creature)
   * @param id Optional comma-separated ID list
   * @param typeFilter Optional type filter
   */
  public QuestVariable(String name, String category, String id, String typeFilter) {
    this.name = name;
    this.category = category;
    this.id = id;
    this.typeFilter = typeFilter;
  }

  /**
   * Converts this QuestVariable to a JDOM Element for backward compatibility.
   *
   * @return JDOM Element representation
   */
  public Element toElement() {
    try {
      // Use Jackson to serialize to XML, then convert to JDOM
      JacksonMapper mapper = new JacksonMapper();

      // Temporarily create a wrapper with the correct element name
      String xml = String.format("<%s", category);
      if (id != null) {
        xml += String.format(" id=\"%s\"", id);
      }
      if (typeFilter != null) {
        xml += String.format(" type=\"%s\"", typeFilter);
      }
      xml += String.format(">%s</%s>", name != null ? name : "", category);

      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize QuestVariable to Element", e);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "QuestVariable{name='%s', category='%s', id='%s', typeFilter='%s'}",
        name, category, id, typeFilter);
  }
}
