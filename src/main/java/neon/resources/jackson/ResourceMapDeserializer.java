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

package neon.resources.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Custom Jackson deserializer for HashMap&lt;String, Integer&gt; from XML child elements.
 *
 * <p>Handles XML structures like:
 *
 * <pre>{@code
 * <list id="goblin_tribe">
 *   <creature id="goblin" l="1" />
 *   <creature id="goblin_warrior" l="3" />
 * </list>
 * }</pre>
 *
 * <p>The element name (creature, item, spell) is configurable via constructor parameter.
 *
 * @author mdriesen
 */
public class ResourceMapDeserializer extends StdDeserializer<HashMap<String, Integer>> {

  private final String elementName;

  public ResourceMapDeserializer(String elementName) {
    super(HashMap.class);
    this.elementName = elementName;
  }

  /** Default constructor for creatures (used by LCreature) */
  public ResourceMapDeserializer() {
    this("creature");
  }

  @Override
  public HashMap<String, Integer> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    HashMap<String, Integer> map = new HashMap<>();

    JsonNode node = p.getCodec().readTree(p);

    // Handle array of elements
    if (node.isArray()) {
      Iterator<JsonNode> elements = node.elements();
      while (elements.hasNext()) {
        JsonNode element = elements.next();
        String id = element.get("id").asText();
        int level = element.get("l").asInt();
        map.put(id, level);
      }
    } else if (node.isObject()) {
      // Handle single element case
      String id = node.get("id").asText();
      int level = node.get("l").asInt();
      map.put(id, level);
    }

    return map;
  }
}
