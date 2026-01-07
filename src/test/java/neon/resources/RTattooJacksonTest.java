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
import neon.entities.property.Ability;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RTattoo resources. */
public class RTattooJacksonTest {

  @Test
  public void testSimpleTattooParsing() throws IOException {
    String xml =
        "<tattoo id=\"dark_vision\" name=\"Dark Vision Tattoo\" ability=\"darkvision\" size=\"5\" cost=\"100\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RTattoo tattoo = mapper.fromXml(input, RTattoo.class);

    assertNotNull(tattoo);
    assertEquals("dark_vision", tattoo.id);
    assertEquals("Dark Vision Tattoo", tattoo.name);
    assertEquals(Ability.DARKVISION, tattoo.ability);
    assertEquals(5, tattoo.magnitude);
    assertEquals(100, tattoo.cost);
  }

  @Test
  public void testTattooWithoutName() throws IOException {
    // Name defaults to id if not specified
    String xml = "<tattoo id=\"fire_resist\" ability=\"fire_resistance\" size=\"3\" cost=\"50\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RTattoo tattoo = mapper.fromXml(input, RTattoo.class);

    assertNotNull(tattoo);
    assertEquals("fire_resist", tattoo.id);
    assertEquals(Ability.FIRE_RESISTANCE, tattoo.ability);
    assertEquals(3, tattoo.magnitude);
    assertEquals(50, tattoo.cost);
  }

  @Test
  public void testCaseInsensitiveAbility() throws IOException {
    // Jackson should handle case-insensitive enum parsing
    String xml = "<tattoo id=\"test\" ability=\"COLD_RESISTANCE\" size=\"2\" cost=\"75\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RTattoo tattoo = mapper.fromXml(input, RTattoo.class);

    assertNotNull(tattoo);
    assertEquals(Ability.COLD_RESISTANCE, tattoo.ability);
  }

  @Test
  public void testToElementUsesJackson() {
    RTattoo tattoo = new RTattoo("test_tattoo");
    tattoo.name = "Test Tattoo";
    tattoo.ability = Ability.SPELL_ABSORPTION;
    tattoo.magnitude = 4;
    tattoo.cost = 200;

    // Call toElement() which uses Jackson internally
    org.jdom2.Element element = tattoo.toElement();

    // Verify JDOM Element
    assertEquals("tattoo", element.getName());
    assertEquals("test_tattoo", element.getAttributeValue("id"));
    assertEquals("Test Tattoo", element.getAttributeValue("name"));
    assertEquals("SPELL_ABSORPTION", element.getAttributeValue("ability"));
    assertEquals("4", element.getAttributeValue("size"));
    assertEquals("200", element.getAttributeValue("cost"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    // Create tattoo, serialize, deserialize, compare
    RTattoo original = new RTattoo("fast_heal");
    original.name = "Fast Healing";
    original.ability = Ability.FAST_HEALING;
    original.magnitude = 10;
    original.cost = 500;

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RTattoo deserialized = mapper.fromXml(input, RTattoo.class);

    assertEquals(original.id, deserialized.id);
    assertEquals(original.name, deserialized.name);
    assertEquals(original.ability, deserialized.ability);
    assertEquals(original.magnitude, deserialized.magnitude);
    assertEquals(original.cost, deserialized.cost);
  }
}
