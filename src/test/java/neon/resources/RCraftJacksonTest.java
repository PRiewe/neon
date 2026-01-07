/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Maarten Driesen
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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RCraft resources. */
public class RCraftJacksonTest {

  @Test
  public void testSimpleCraftParsing() throws IOException {
    String xml =
        "<craft id=\"leather_armor\" result=\"leather_armor\" raw=\"leather\" amount=\"5\" cost=\"10\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RCraft craft = mapper.fromXml(input, RCraft.class);

    assertNotNull(craft);
    assertEquals("leather_armor", craft.id);
    assertEquals("leather_armor", craft.name);
    assertEquals("leather", craft.raw);
    assertEquals(5, craft.amount);
    assertEquals(10, craft.cost);
  }

  @Test
  public void testCraftWithDifferentResult() throws IOException {
    // Result name can differ from id
    String xml =
        "<craft id=\"craft_001\" result=\"iron_sword\" raw=\"iron_ingot\" amount=\"3\" cost=\"25\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RCraft craft = mapper.fromXml(input, RCraft.class);

    assertNotNull(craft);
    assertEquals("craft_001", craft.id);
    assertEquals("iron_sword", craft.name); // result maps to name
    assertEquals("iron_ingot", craft.raw);
    assertEquals(3, craft.amount);
    assertEquals(25, craft.cost);
  }

  @Test
  public void testToElementUsesJackson() {
    // Use copy constructor to create test craft
    RCraft template = new RCraft();
    template.name = "wooden_shield";
    template.raw = "wood_planks";
    template.amount = 10;
    template.cost = 15;
    RCraft craft = new RCraft(template);

    // Call toElement() which uses Jackson internally
    org.jdom2.Element element = craft.toElement();

    // Verify JDOM Element
    assertEquals("craft", element.getName());
    assertNotNull(element.getAttributeValue("id"));
    assertEquals("wooden_shield", element.getAttributeValue("result"));
    assertEquals("wood_planks", element.getAttributeValue("raw"));
    assertEquals("10", element.getAttributeValue("amount"));
    assertEquals("15", element.getAttributeValue("cost"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    // Create craft, serialize, deserialize, compare
    RCraft original = new RCraft();
    original.name = "health_potion";
    original.raw = "herbs";
    original.amount = 2;
    original.cost = 50;

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RCraft deserialized = mapper.fromXml(input, RCraft.class);

    assertEquals(original.id, deserialized.id);
    assertEquals(original.name, deserialized.name);
    assertEquals(original.raw, deserialized.raw);
    assertEquals(original.amount, deserialized.amount);
    assertEquals(original.cost, deserialized.cost);
  }
}
