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
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import neon.entities.property.Skill;

/**
 * Custom Jackson deserializer for EnumMap&lt;Skill, Float&gt; from XML attributes like: {@code
 * <skills axe="10" sword="5" />}
 *
 * <p>The attribute names correspond to Skill enum values (case-insensitive), and the values are
 * floats.
 *
 * @author mdriesen
 */
public class SkillMapDeserializer extends StdDeserializer<EnumMap<Skill, Float>> {

  public SkillMapDeserializer() {
    super(EnumMap.class);
  }

  @Override
  public EnumMap<Skill, Float> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    EnumMap<Skill, Float> map = new EnumMap<>(Skill.class);

    // Initialize all skills to 0.0f
    for (Skill skill : Skill.values()) {
      map.put(skill, 0f);
    }

    JsonNode node = p.getCodec().readTree(p);

    // Iterate over all fields in the node (these are XML attributes)
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String skillName = field.getKey();
      float skillValue = field.getValue().floatValue();

      try {
        // Skill enum values are uppercase (AXE, SWORD, etc.)
        Skill skill = Skill.valueOf(skillName.toUpperCase());
        map.put(skill, skillValue);
      } catch (IllegalArgumentException e) {
        // Unknown skill, skip it
      }
    }

    return map;
  }
}
