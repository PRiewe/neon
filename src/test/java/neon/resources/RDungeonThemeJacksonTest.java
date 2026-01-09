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

/** Test Jackson XML parsing for RDungeonTheme resources. */
public class RDungeonThemeJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<dungeon id=\"dark_cave\" min=\"3\" max=\"7\" b=\"2\" zones=\"cave1;cave2;lava\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RDungeonTheme theme = mapper.fromXml(input, RDungeonTheme.class);

    assertNotNull(theme);
    assertEquals("dark_cave", theme.id);
    assertEquals(3, theme.min);
    assertEquals(7, theme.max);
    assertEquals(2, theme.branching);
    assertEquals("cave1;cave2;lava", theme.zones);
  }

  @Test
  public void testParsingWithoutZones() throws IOException {
    String xml = "<dungeon id=\"simple_dungeon\" min=\"1\" max=\"3\" b=\"1\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RDungeonTheme theme = mapper.fromXml(input, RDungeonTheme.class);

    assertNotNull(theme);
    assertEquals("simple_dungeon", theme.id);
    assertEquals(1, theme.min);
    assertEquals(3, theme.max);
    assertEquals(1, theme.branching);
    assertNull(theme.zones); // zones is optional
  }

  @Test
  public void testSerialization() throws IOException {
    RDungeonTheme theme = new RDungeonTheme("test_dungeon");
    theme.min = 5;
    theme.max = 10;
    theme.branching = 3;
    theme.zones = "zone1;zone2";

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(theme).toString();

    // Verify XML contains expected attributes
    assertTrue(xml.contains("id=\"test_dungeon\""));
    assertTrue(xml.contains("min=\"5\""));
    assertTrue(xml.contains("max=\"10\""));
    assertTrue(xml.contains("b=\"3\""));
    assertTrue(xml.contains("zones=\"zone1;zone2\""));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml =
        "<dungeon id=\"crypt\" min=\"2\" max=\"5\" b=\"2\" zones=\"tomb;crypt;ossuary\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    RDungeonTheme theme = mapper.fromXml(input, RDungeonTheme.class);

    assertNotNull(theme);
    assertEquals("crypt", theme.id);
    assertEquals(2, theme.min);
    assertEquals(5, theme.max);
    assertEquals(2, theme.branching);
    assertEquals("tomb;crypt;ossuary", theme.zones);

    // Serialize back
    String serialized = mapper.toXml(theme).toString();
    assertTrue(serialized.contains("crypt"));
    assertTrue(serialized.contains("min=\"2\""));
    assertTrue(serialized.contains("max=\"5\""));
  }

  @Test
  public void testToElementBridge() {
    RDungeonTheme theme = new RDungeonTheme("bridge_test");
    theme.min = 4;
    theme.max = 8;
    theme.branching = 2;
    theme.zones = "test1;test2";

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = theme.toElement();

    // Verify JDOM Element contains expected attributes
    assertEquals("dungeon", element.getName());
    assertEquals("bridge_test", element.getAttributeValue("id"));
    assertEquals("4", element.getAttributeValue("min"));
    assertEquals("8", element.getAttributeValue("max"));
    assertEquals("2", element.getAttributeValue("b"));
    assertEquals("test1;test2", element.getAttributeValue("zones"));
  }
}
