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

package neon.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for LSpell resources. */
public class LSpellJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<list id=\"fire_spells\">"
            + "<spell id=\"spark\" l=\"1\" />"
            + "<spell id=\"fireball\" l=\"5\" />"
            + "<spell id=\"inferno\" l=\"10\" />"
            + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    LSpell ls = mapper.fromXml(input, LSpell.class);

    assertNotNull(ls);
    assertEquals("fire_spells", ls.id);
    assertEquals(3, ls.spells.size());
    assertEquals(1, ls.spells.get("spark"));
    assertEquals(5, ls.spells.get("fireball"));
    assertEquals(10, ls.spells.get("inferno"));
  }

  @Test
  public void testSingleSpell() throws IOException {
    String xml =
        "<list id=\"ultimate_spell\">" + "<spell id=\"meteor_storm\" l=\"99\" />" + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    LSpell ls = mapper.fromXml(input, LSpell.class);

    assertNotNull(ls);
    assertEquals("ultimate_spell", ls.id);
    assertEquals(1, ls.spells.size());
    assertEquals(99, ls.spells.get("meteor_storm"));
  }

  @Test
  public void testSerialization() throws IOException {
    LSpell ls = new LSpell("healing_spells");
    ls.spells.put("minor_heal", 1);
    ls.spells.put("cure_wounds", 3);
    ls.spells.put("restoration", 7);

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(ls).toString();

    assertTrue(xml.contains("id=\"healing_spells\""));
    assertTrue(xml.contains("minor_heal"));
    assertTrue(xml.contains("cure_wounds"));
    assertTrue(xml.contains("restoration"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml =
        "<list id=\"ice_magic\">"
            + "<spell id=\"frost_bolt\" l=\"2\" />"
            + "<spell id=\"ice_shard\" l=\"4\" />"
            + "<spell id=\"blizzard\" l=\"8\" />"
            + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    LSpell ls = mapper.fromXml(input, LSpell.class);

    assertNotNull(ls);
    assertEquals("ice_magic", ls.id);
    assertEquals(3, ls.spells.size());

    // Serialize back
    String serialized = mapper.toXml(ls).toString();
    assertTrue(serialized.contains("ice_magic"));
    assertTrue(serialized.contains("frost_bolt"));
    assertTrue(serialized.contains("blizzard"));
  }

  @Test
  public void testToElementBridge() {
    LSpell ls = new LSpell("bridge_test");
    ls.spells.put("lightning_bolt", 3);
    ls.spells.put("chain_lightning", 6);

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = ls.toElement();

    assertEquals("list", element.getName());
    assertEquals("bridge_test", element.getAttributeValue("id"));
    assertEquals(2, element.getChildren("spell").size());

    // Verify spell elements
    boolean foundLightning = false;
    boolean foundChain = false;
    for (org.jdom2.Element spell : element.getChildren("spell")) {
      String id = spell.getAttributeValue("id");
      if ("lightning_bolt".equals(id)) {
        assertEquals("3", spell.getAttributeValue("l"));
        foundLightning = true;
      } else if ("chain_lightning".equals(id)) {
        assertEquals("6", spell.getAttributeValue("l"));
        foundChain = true;
      }
    }
    assertTrue(foundLightning);
    assertTrue(foundChain);
  }
}
