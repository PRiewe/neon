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

/** Test Jackson XML parsing for RZoneTheme resources. */
public class RZoneThemeJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<zone id=\"dungeon_cave\" type=\"cave;stone;rock_wall;iron_door\" min=\"20\" max=\"40\">"
            + "<creature n=\"15\">goblin</creature>"
            + "<item n=\"10\">gold</item>"
            + "<feature t=\"water\" s=\"5\" n=\"2\">lake</feature>"
            + "</zone>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RZoneTheme theme = mapper.fromXml(input, RZoneTheme.class);

    assertNotNull(theme);
    assertEquals("dungeon_cave", theme.id);
    assertEquals("cave", theme.type);
    assertEquals("stone", theme.floor);
    assertEquals("rock_wall", theme.walls);
    assertEquals("iron_door", theme.doors);
    assertEquals(20, theme.min);
    assertEquals(40, theme.max);

    // Check creatures
    assertEquals(1, theme.creatures.size());
    assertEquals(15, theme.creatures.get("goblin"));

    // Check items
    assertEquals(1, theme.items.size());
    assertEquals(10, theme.items.get("gold"));

    // Check features
    assertEquals(1, theme.features.size());
    RZoneTheme.Feature feature = theme.features.get(0);
    assertEquals("water", feature.t);
    assertEquals(5, feature.s);
    assertEquals(2, feature.n);
    assertEquals("lake", feature.value);
  }

  @Test
  public void testMultipleFeatures() throws IOException {
    String xml =
        "<zone id=\"complex_dungeon\" type=\"bsp\">"
            + "<feature t=\"lava\" s=\"3\" n=\"5\">lake</feature>"
            + "<feature t=\"moss\" s=\"10\" n=\"8\">patch</feature>"
            + "<feature t=\"slime\" s=\"2\" n=\"3\">stain</feature>"
            + "</zone>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RZoneTheme theme = mapper.fromXml(input, RZoneTheme.class);

    assertNotNull(theme);
    assertEquals(3, theme.features.size());

    RZoneTheme.Feature lava = theme.features.get(0);
    assertEquals("lava", lava.t);
    assertEquals(3, lava.s);
    assertEquals(5, lava.n);
    assertEquals("lake", lava.value);

    RZoneTheme.Feature moss = theme.features.get(1);
    assertEquals("moss", moss.t);
    assertEquals(10, moss.s);
    assertEquals(8, moss.n);
    assertEquals("patch", moss.value);

    RZoneTheme.Feature slime = theme.features.get(2);
    assertEquals("slime", slime.t);
    assertEquals(2, slime.s);
    assertEquals(3, slime.n);
    assertEquals("stain", slime.value);
  }

  @Test
  public void testSerialization() throws IOException {
    RZoneTheme theme = new RZoneTheme("test_zone");
    theme.type = "maze";
    theme.floor = "dirt";
    theme.walls = "stone_wall";
    theme.doors = "wood_door";
    theme.min = 15;
    theme.max = 30;

    theme.creatures.put("rat", 20);
    theme.items.put("torch", 5);

    RZoneTheme.Feature feature = new RZoneTheme.Feature();
    feature.t = "water";
    feature.s = 4;
    feature.n = 3;
    feature.value = "river";
    theme.features.add(feature);

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(theme).toString();

    assertTrue(xml.contains("id=\"test_zone\""));
    assertTrue(xml.contains("maze"));
    assertTrue(xml.contains("dirt"));
    assertTrue(xml.contains("stone_wall"));
    assertTrue(xml.contains("wood_door"));
    assertTrue(xml.contains("rat"));
    assertTrue(xml.contains("torch"));
    assertTrue(xml.contains("river"));
    assertTrue(xml.contains("water"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml =
        "<zone id=\"roundtrip_zone\" type=\"packed;gravel;brick_wall;metal_door\" min=\"10\" max=\"25\">"
            + "<creature n=\"12\">skeleton</creature>"
            + "<item n=\"8\">sword</item>"
            + "<feature t=\"acid\" s=\"6\" n=\"4\">lake</feature>"
            + "</zone>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    RZoneTheme theme = mapper.fromXml(input, RZoneTheme.class);

    assertNotNull(theme);
    assertEquals("roundtrip_zone", theme.id);
    assertEquals("packed", theme.type);
    assertEquals("gravel", theme.floor);
    assertEquals("brick_wall", theme.walls);
    assertEquals("metal_door", theme.doors);

    // Serialize back
    String serialized = mapper.toXml(theme).toString();
    assertTrue(serialized.contains("roundtrip_zone"));
    assertTrue(serialized.contains("packed"));
    assertTrue(serialized.contains("skeleton"));
    assertTrue(serialized.contains("acid"));
  }

  @Test
  public void testToElementBridge() {
    RZoneTheme theme = new RZoneTheme("bridge_test");
    theme.type = "cave";
    theme.floor = "rock";
    theme.walls = "stone_wall";
    theme.doors = "cave_door";
    theme.min = 12;
    theme.max = 28;

    theme.creatures.put("bat", 10);
    theme.items.put("gem", 3);

    RZoneTheme.Feature feature = new RZoneTheme.Feature();
    feature.t = "magma";
    feature.s = 7;
    feature.n = 2;
    feature.value = "patch";
    theme.features.add(feature);

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = theme.toElement();

    assertEquals("zone", element.getName());
    assertEquals("bridge_test", element.getAttributeValue("id"));
    assertTrue(element.getAttributeValue("type").contains("cave"));
    assertEquals("12", element.getAttributeValue("min"));
    assertEquals("28", element.getAttributeValue("max"));

    // Verify feature was serialized
    assertEquals(1, element.getChildren("feature").size());
    org.jdom2.Element featureEl = element.getChildren("feature").get(0);
    assertEquals("patch", featureEl.getText().trim());
    assertEquals("magma", featureEl.getAttributeValue("t"));
    assertEquals("7", featureEl.getAttributeValue("s"));
    assertEquals("2", featureEl.getAttributeValue("n"));
  }

  @Test
  public void testEmptyTheme() throws IOException {
    String xml = "<zone id=\"empty_zone\" type=\"maze\"></zone>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RZoneTheme theme = mapper.fromXml(input, RZoneTheme.class);

    assertNotNull(theme);
    assertEquals("empty_zone", theme.id);
    assertEquals("maze", theme.type);
    assertEquals(0, theme.creatures.size());
    assertEquals(0, theme.items.size());
    assertEquals(0, theme.features.size());
  }

  @Test
  public void testFeatureModel() {
    // Test that Feature objects work correctly for DungeonGenerator
    RZoneTheme.Feature feature = new RZoneTheme.Feature();
    feature.t = "ice";
    feature.s = 12;
    feature.n = 7;
    feature.value = "lake";

    // These are the operations DungeonGenerator performs
    assertEquals("ice", feature.t);
    assertEquals(12, feature.s);
    assertEquals(7, feature.n);
    assertEquals("lake", feature.value);
  }
}
