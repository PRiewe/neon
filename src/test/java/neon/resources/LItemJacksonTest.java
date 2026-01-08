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

/** Test Jackson XML parsing for LItem resources. */
public class LItemJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<list id=\"treasure_hoard\">"
            + "<item id=\"gold_coin\" l=\"1\" />"
            + "<item id=\"silver_ring\" l=\"5\" />"
            + "<item id=\"ruby\" l=\"10\" />"
            + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    LItem li = mapper.fromXml(input, LItem.class);

    assertNotNull(li);
    assertEquals("treasure_hoard", li.id);
    assertEquals(3, li.items.size());
    assertEquals(1, li.items.get("gold_coin"));
    assertEquals(5, li.items.get("silver_ring"));
    assertEquals(10, li.items.get("ruby"));
  }

  @Test
  public void testSingleItem() throws IOException {
    String xml = "<list id=\"legendary_item\">" + "<item id=\"excalibur\" l=\"50\" />" + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    LItem li = mapper.fromXml(input, LItem.class);

    assertNotNull(li);
    assertEquals("legendary_item", li.id);
    assertEquals(1, li.items.size());
    assertEquals(50, li.items.get("excalibur"));
  }

  @Test
  public void testSerialization() throws IOException {
    LItem li = new LItem("loot_table");
    li.items.put("iron_sword", 2);
    li.items.put("health_potion", 1);
    li.items.put("leather_armor", 3);

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(li).toString();

    assertTrue(xml.contains("id=\"loot_table\""));
    assertTrue(xml.contains("iron_sword"));
    assertTrue(xml.contains("health_potion"));
    assertTrue(xml.contains("leather_armor"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml =
        "<list id=\"weapon_cache\">"
            + "<item id=\"dagger\" l=\"1\" />"
            + "<item id=\"longsword\" l=\"5\" />"
            + "<item id=\"battleaxe\" l=\"8\" />"
            + "</list>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    LItem li = mapper.fromXml(input, LItem.class);

    assertNotNull(li);
    assertEquals("weapon_cache", li.id);
    assertEquals(3, li.items.size());

    // Serialize back
    String serialized = mapper.toXml(li).toString();
    assertTrue(serialized.contains("weapon_cache"));
    assertTrue(serialized.contains("dagger"));
    assertTrue(serialized.contains("battleaxe"));
  }

  @Test
  public void testToElementBridge() {
    LItem li = new LItem("bridge_test");
    li.items.put("apple", 1);
    li.items.put("bread", 2);

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = li.toElement();

    assertEquals("list", element.getName());
    assertEquals("bridge_test", element.getAttributeValue("id"));
    assertEquals(2, element.getChildren("item").size());

    // Verify item elements
    boolean foundApple = false;
    boolean foundBread = false;
    for (org.jdom2.Element item : element.getChildren("item")) {
      String id = item.getAttributeValue("id");
      if ("apple".equals(id)) {
        assertEquals("1", item.getAttributeValue("l"));
        foundApple = true;
      } else if ("bread".equals(id)) {
        assertEquals("2", item.getAttributeValue("l"));
        foundBread = true;
      }
    }
    assertTrue(foundApple);
    assertTrue(foundBread);
  }
}
