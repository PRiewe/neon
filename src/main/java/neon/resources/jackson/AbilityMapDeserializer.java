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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.EnumMap;
import neon.entities.property.Ability;

/**
 * Custom Jackson deserializer for EnumMap&lt;Ability, Integer&gt; from XML like: {@code <ability
 * id="spell_resistance" size="20" />}
 *
 * @author mdriesen
 */
public class AbilityMapDeserializer extends StdDeserializer<EnumMap<Ability, Integer>> {

  public AbilityMapDeserializer() {
    super(EnumMap.class);
  }

  @Override
  public EnumMap<Ability, Integer> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    EnumMap<Ability, Integer> map = new EnumMap<>(Ability.class);
    JsonNode node = p.getCodec().readTree(p);

    // Handle both single element and array of elements
    if (node.isArray()) {
      for (JsonNode abilityNode : node) {
        parseAbility(abilityNode, map);
      }
    } else {
      parseAbility(node, map);
    }

    return map;
  }

  private void parseAbility(JsonNode node, EnumMap<Ability, Integer> map) {
    JsonNode idNode = node.get("id");
    JsonNode sizeNode = node.get("size");

    if (idNode != null && sizeNode != null) {
      String abilityName = idNode.asText();
      int size = sizeNode.asInt();

      try {
        Ability ability = Ability.valueOf(abilityName.toUpperCase());
        map.put(ability, size);
      } catch (IllegalArgumentException e) {
        // Unknown ability, skip it
      }
    }
  }
}
