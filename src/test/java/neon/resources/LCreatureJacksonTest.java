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

/** Test Jackson XML parsing for LCreature resources. */
public class LCreatureJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<list id=\"goblin_tribe\">"
            + "<creature id=\"goblin\" l=\"1\" />"
            + "<creature id=\"goblin_warrior\" l=\"3\" />"
            + "<creature id=\"goblin_chief\" l=\"5\" />"
            + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    LCreature lc = mapper.fromXml(input, LCreature.class);

    assertNotNull(lc);
    assertEquals("goblin_tribe", lc.id);
    assertEquals(3, lc.creatures.size());
    assertEquals(1, lc.creatures.get("goblin"));
    assertEquals(3, lc.creatures.get("goblin_warrior"));
    assertEquals(5, lc.creatures.get("goblin_chief"));
  }

  @Test
  public void testSingleCreature() throws IOException {
    String xml =
        "<list id=\"solo_dragon\">" + "<creature id=\"ancient_dragon\" l=\"20\" />" + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    LCreature lc = mapper.fromXml(input, LCreature.class);

    assertNotNull(lc);
    assertEquals("solo_dragon", lc.id);
    assertEquals(1, lc.creatures.size());
    assertEquals(20, lc.creatures.get("ancient_dragon"));
  }

  @Test
  public void testEmptyList() throws IOException {
    String xml = "<list id=\"empty_list\"></list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    LCreature lc = mapper.fromXml(input, LCreature.class);

    assertNotNull(lc);
    assertEquals("empty_list", lc.id);
    assertEquals(0, lc.creatures.size());
  }

  @Test
  public void testSerialization() throws IOException {
    LCreature lc = new LCreature("test_list");
    lc.creatures.put("rat", 1);
    lc.creatures.put("wolf", 5);
    lc.creatures.put("bear", 10);

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(lc).toString();

    assertTrue(xml.contains("id=\"test_list\""));
    assertTrue(xml.contains("rat"));
    assertTrue(xml.contains("wolf"));
    assertTrue(xml.contains("bear"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml =
        "<list id=\"undead_horde\">"
            + "<creature id=\"skeleton\" l=\"2\" />"
            + "<creature id=\"zombie\" l=\"3\" />"
            + "<creature id=\"vampire\" l=\"10\" />"
            + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    LCreature lc = mapper.fromXml(input, LCreature.class);

    assertNotNull(lc);
    assertEquals("undead_horde", lc.id);
    assertEquals(3, lc.creatures.size());

    // Serialize back
    String serialized = mapper.toXml(lc).toString();
    assertTrue(serialized.contains("undead_horde"));
    assertTrue(serialized.contains("skeleton"));
    assertTrue(serialized.contains("vampire"));
  }

  @Test
  public void testToElementBridge() {
    LCreature lc = new LCreature("bridge_test");
    lc.creatures.put("orc", 4);
    lc.creatures.put("troll", 8);

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = lc.toElement();

    assertEquals("list", element.getName());
    assertEquals("bridge_test", element.getAttributeValue("id"));
    assertEquals(2, element.getChildren("creature").size());

    // Verify creature elements
    boolean foundOrc = false;
    boolean foundTroll = false;
    for (org.jdom2.Element creature : element.getChildren("creature")) {
      String id = creature.getAttributeValue("id");
      if ("orc".equals(id)) {
        assertEquals("4", creature.getAttributeValue("l"));
        foundOrc = true;
      } else if ("troll".equals(id)) {
        assertEquals("8", creature.getAttributeValue("l"));
        foundTroll = true;
      }
    }
    assertTrue(foundOrc);
    assertTrue(foundTroll);
  }
}
