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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Jackson serializer for HashMap&lt;String, Integer&gt; to XML child elements.
 *
 * <p>Serializes to XML structures like:
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
public class ResourceMapSerializer extends StdSerializer<HashMap<String, Integer>> {

  private final String elementName;

  public ResourceMapSerializer(String elementName) {
    super((Class<HashMap<String, Integer>>) (Class<?>) HashMap.class);
    this.elementName = elementName;
  }

  /** Default constructor for creatures (used by LCreature) */
  public ResourceMapSerializer() {
    this("creature");
  }

  @Override
  public void serialize(
      HashMap<String, Integer> map, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartArray();
    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      gen.writeStartObject();
      gen.writeStringField("id", entry.getKey());
      gen.writeNumberField("l", entry.getValue());
      gen.writeEndObject();
    }
    gen.writeEndArray();
  }
}
