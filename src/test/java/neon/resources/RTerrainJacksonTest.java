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
import neon.maps.Region.Modifier;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RTerrain resources. */
public class RTerrainJacksonTest {

  @Test
  public void testSimpleTerrainParsing() throws IOException {
    String xml = "<type id=\"grass\" char=\"·\" color=\"green\">Grass terrain</type>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RTerrain terrain = mapper.fromXml(input, RTerrain.class);

    assertNotNull(terrain);
    assertEquals("grass", terrain.id);
    assertEquals("·", terrain.text);
    assertEquals("green", terrain.color);
    assertEquals("Grass terrain", terrain.description);
    assertEquals(Modifier.NONE, terrain.modifier);
  }

  @Test
  public void testTerrainWithModifier() throws IOException {
    String xml = "<type id=\"wall\" color=\"slateGray\" char=\"#\" mod=\"block\">a wall</type>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RTerrain terrain = mapper.fromXml(input, RTerrain.class);

    assertNotNull(terrain);
    assertEquals("wall", terrain.id);
    assertEquals("#", terrain.text);
    assertEquals("slateGray", terrain.color);
    assertEquals("a wall", terrain.description);
    assertEquals(Modifier.BLOCK, terrain.modifier);
  }

  @Test
  public void testTerrainSerialization() throws IOException {
    RTerrain terrain = new RTerrain("water");
    terrain.text = "~";
    terrain.color = "blue";
    terrain.description = null; // No description
    terrain.modifier = Modifier.SWIM;

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(terrain).toString();

    // Verify XML contains expected elements
    assertTrue(xml.contains("id=\"water\""));
    assertTrue(xml.contains("char=\"~\""));
    assertTrue(xml.contains("color=\"blue\""));
    assertTrue(xml.contains("mod=\"SWIM\""));
  }

  @Test
  public void testTerrainRoundTrip() throws IOException {
    String originalXml =
        "<type id=\"cliff\" color=\"darkSlateGray\" char=\"#\" mod=\"climb\">a cliff</type>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    RTerrain terrain = mapper.fromXml(input, RTerrain.class);

    assertNotNull(terrain);
    assertEquals("cliff", terrain.id);
    assertEquals(Modifier.CLIMB, terrain.modifier);

    // Serialize back
    String serialized = mapper.toXml(terrain).toString();
    assertTrue(serialized.contains("cliff"));
    assertTrue(serialized.contains("CLIMB"));
  }

  @Test
  public void testToElementUsesJackson() {
    RTerrain terrain = new RTerrain("test_terrain");
    terrain.text = "*";
    terrain.color = "red";
    terrain.description = "Test terrain";
    terrain.modifier = Modifier.CLIMB;

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = terrain.toElement();

    // Verify JDOM Element contains expected attributes
    assertEquals("type", element.getName());
    assertEquals("test_terrain", element.getAttributeValue("id"));
    assertEquals("*", element.getAttributeValue("char"));
    assertEquals("red", element.getAttributeValue("color"));
    assertEquals("CLIMB", element.getAttributeValue("mod"));
    assertEquals(
        "Test terrain", element.getText().trim()); // Jackson pretty-printer adds whitespace
  }
}
