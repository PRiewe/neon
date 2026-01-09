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
import java.util.EnumMap;
import java.util.Map;
import neon.entities.property.Skill;

/**
 * Custom Jackson serializer for EnumMap&lt;Skill, Float&gt; to XML attributes like: {@code <skills
 * axe="10" sword="5" />}
 *
 * <p>Only includes skills with non-zero values.
 *
 * @author mdriesen
 */
public class SkillMapSerializer extends StdSerializer<EnumMap<Skill, Float>> {

  public SkillMapSerializer() {
    super((Class<EnumMap<Skill, Float>>) (Class<?>) EnumMap.class);
  }

  @Override
  public void serialize(EnumMap<Skill, Float> map, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    for (Map.Entry<Skill, Float> entry : map.entrySet()) {
      if (entry.getValue() > 0) {
        gen.writeNumberField(entry.getKey().name().toLowerCase(), entry.getValue());
      }
    }
    gen.writeEndObject();
  }
}
