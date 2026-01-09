/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2024 - Maarten Driesen
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
import java.util.EnumMap;
import java.util.Map;
import neon.entities.property.Ability;

/**
 * Custom Jackson serializer for EnumMap&lt;Ability, Integer&gt; to XML like: {@code <ability
 * id="spell_resistance" size="20" />}
 *
 * @author mdriesen
 */
public class AbilityMapSerializer extends StdSerializer<EnumMap<Ability, Integer>> {

  public AbilityMapSerializer() {
    super((Class<EnumMap<Ability, Integer>>) (Class<?>) EnumMap.class);
  }

  @Override
  public void serialize(
      EnumMap<Ability, Integer> map, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    for (Map.Entry<Ability, Integer> entry : map.entrySet()) {
      if (entry.getValue() > 0) {
        gen.writeStartObject();
        gen.writeStringField("id", entry.getKey().name());
        gen.writeNumberField("size", entry.getValue());
        gen.writeEndObject();
      }
    }
  }
}
