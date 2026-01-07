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

package neon.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import neon.entities.property.Ability;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RSign resources. */
public class RSignJacksonTest {

  @Test
  public void testSimpleSignParsing() throws IOException {
    String xml =
        "<sign id=\"s_alraun\" name=\"alraun\">"
            + "<ability id=\"spell_resistance\" size=\"20\" />"
            + "<power id=\"heal_p\" />"
            + "</sign>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSign sign = mapper.fromXml(input, RSign.class);

    assertNotNull(sign);
    assertEquals("s_alraun", sign.id);
    assertEquals("alraun", sign.name);

    // Check legacy fields were populated
    assertEquals(1, sign.powers.size());
    assertEquals("heal_p", sign.powers.get(0));

    assertEquals(1, sign.abilities.size());
    assertTrue(sign.abilities.containsKey(Ability.SPELL_RESISTANCE));
    assertEquals(20, sign.abilities.get(Ability.SPELL_RESISTANCE));
  }

  @Test
  public void testSignWithMultiplePowersAndAbilities() throws IOException {
    String xml =
        "<sign id=\"s_wolf\" name=\"wolf\">"
            + "<power id=\"power1\" />"
            + "<power id=\"power2\" />"
            + "<power id=\"power3\" />"
            + "<ability id=\"fire_resistance\" size=\"5\" />"
            + "<ability id=\"cold_resistance\" size=\"3\" />"
            + "</sign>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSign sign = mapper.fromXml(input, RSign.class);

    assertNotNull(sign);
    assertEquals("s_wolf", sign.id);

    // Check powers
    assertEquals(3, sign.powers.size());
    assertTrue(sign.powers.contains("power1"));
    assertTrue(sign.powers.contains("power2"));
    assertTrue(sign.powers.contains("power3"));

    // Check abilities
    assertEquals(2, sign.abilities.size());
    assertEquals(5, sign.abilities.get(Ability.FIRE_RESISTANCE));
    assertEquals(3, sign.abilities.get(Ability.COLD_RESISTANCE));
  }

  @Test
  public void testEmptySign() throws IOException {
    String xml = "<sign id=\"s_empty\" name=\"empty\"></sign>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSign sign = mapper.fromXml(input, RSign.class);

    assertNotNull(sign);
    assertEquals("s_empty", sign.id);
    assertEquals("empty", sign.name);
    assertTrue(sign.powers.isEmpty());
    assertTrue(sign.abilities.isEmpty());
  }

  @Test
  public void testCaseInsensitiveEnums() throws IOException {
    // Test that "spell_resistance" (lowercase with underscore) maps to SPELL_RESISTANCE enum
    String xml =
        "<sign id=\"s_test\">"
            + "<ability id=\"spell_resistance\" size=\"10\" />"
            + "<ability id=\"darkvision\" size=\"15\" />"
            + "</sign>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSign sign = mapper.fromXml(input, RSign.class);

    assertNotNull(sign);
    assertEquals(2, sign.abilities.size());
    assertTrue(sign.abilities.containsKey(Ability.SPELL_RESISTANCE));
    assertTrue(sign.abilities.containsKey(Ability.DARKVISION));
  }
}
