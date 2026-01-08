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

/** Test Jackson XML parsing for RRegionTheme resources. */
public class RRegionThemeJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<region id=\"forest_theme\" floor=\"grass\" random=\"PLAIN\">"
            + "<creature n=\"35\">forest_1</creature>"
            + "<feature n=\"1\" s=\"50\" t=\"water\">lake</feature>"
            + "<plant a=\"10\">oak_tree</plant>"
            + "</region>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RRegionTheme theme = mapper.fromXml(input, RRegionTheme.class);

    assertNotNull(theme);
    assertEquals("forest_theme", theme.id);
    assertEquals("grass", theme.floor);
    assertEquals(RRegionTheme.Type.PLAIN, theme.type);

    // Check creatures
    assertEquals(1, theme.creatures.size());
    assertEquals(35, theme.creatures.get("forest_1"));

    // Check features
    assertEquals(1, theme.features.size());
    RRegionTheme.Feature feature = theme.features.get(0);
    assertEquals("1", feature.n);
    assertEquals("50", feature.s);
    assertEquals("water", feature.t);
    assertEquals("lake", feature.value);

    // Check vegetation
    assertEquals(1, theme.vegetation.size());
    assertEquals(10, theme.vegetation.get("oak_tree"));
  }

  @Test
  public void testTownTheme() throws IOException {
    String xml =
        "<region id=\"town_theme\" floor=\"cobblestone\" random=\"town;stone_wall;oak_door\">"
            + "<creature n=\"10\">guard</creature>"
            + "<creature n=\"5\">merchant</creature>"
            + "</region>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RRegionTheme theme = mapper.fromXml(input, RRegionTheme.class);

    assertNotNull(theme);
    assertEquals("town_theme", theme.id);
    assertEquals(RRegionTheme.Type.town, theme.type);
    assertEquals("stone_wall", theme.wall);
    assertEquals("oak_door", theme.door);
    assertEquals(2, theme.creatures.size());
  }

  @Test
  public void testMultipleFeatures() throws IOException {
    String xml =
        "<region id=\"complex_theme\" random=\"CHAOTIC\">"
            + "<feature n=\"1\" s=\"50\" t=\"water\">lake</feature>"
            + "<feature n=\"2\" s=\"20\" t=\"mountain\">hill</feature>"
            + "<feature n=\"3\" t=\"forest\">grove</feature>"
            + "</region>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RRegionTheme theme = mapper.fromXml(input, RRegionTheme.class);

    assertNotNull(theme);
    assertEquals(3, theme.features.size());

    RRegionTheme.Feature lake = theme.features.get(0);
    assertEquals("lake", lake.value);
    assertEquals("1", lake.n);
    assertEquals("50", lake.s);

    RRegionTheme.Feature hill = theme.features.get(1);
    assertEquals("hill", hill.value);
    assertEquals("2", hill.n);
    assertEquals("20", hill.s);
  }

  @Test
  public void testSerialization() throws IOException {
    RRegionTheme theme = new RRegionTheme("test_theme");
    theme.floor = "grass";
    theme.type = RRegionTheme.Type.PLAIN;
    theme.creatures.put("wolf", 20);

    RRegionTheme.Feature feature = new RRegionTheme.Feature();
    feature.n = "1";
    feature.s = "30";
    feature.t = "water";
    feature.value = "pond";
    theme.features.add(feature);

    theme.vegetation.put("pine_tree", 15);

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(theme).toString();

    assertTrue(xml.contains("id=\"test_theme\""));
    assertTrue(xml.contains("floor=\"grass\""));
    assertTrue(xml.contains("PLAIN"));
    assertTrue(xml.contains("wolf"));
    assertTrue(xml.contains("pond"));
    assertTrue(xml.contains("pine_tree"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml =
        "<region id=\"roundtrip_theme\" floor=\"sand\" random=\"BEACH\">"
            + "<creature n=\"5\">crab</creature>"
            + "<feature n=\"2\" s=\"40\" t=\"water\">tide_pool</feature>"
            + "<plant a=\"8\">palm_tree</plant>"
            + "</region>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    RRegionTheme theme = mapper.fromXml(input, RRegionTheme.class);

    assertNotNull(theme);
    assertEquals("roundtrip_theme", theme.id);
    assertEquals(RRegionTheme.Type.BEACH, theme.type);

    // Serialize back
    String serialized = mapper.toXml(theme).toString();
    assertTrue(serialized.contains("roundtrip_theme"));
    assertTrue(serialized.contains("BEACH"));
    assertTrue(serialized.contains("crab"));
    assertTrue(serialized.contains("tide_pool"));
  }

  @Test
  public void testToElementBridge() {
    RRegionTheme theme = new RRegionTheme("bridge_test");
    theme.floor = "dirt";
    theme.type = RRegionTheme.Type.PLAIN;
    theme.creatures.put("rat", 10);

    RRegionTheme.Feature feature = new RRegionTheme.Feature();
    feature.n = "1";
    feature.s = "25";
    feature.t = "water";
    feature.value = "stream";
    theme.features.add(feature);

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = theme.toElement();

    assertEquals("region", element.getName());
    assertEquals("bridge_test", element.getAttributeValue("id"));
    assertEquals("dirt", element.getAttributeValue("floor"));
    assertTrue(element.getAttributeValue("random").contains("PLAIN"));

    // Verify feature was serialized
    assertEquals(1, element.getChildren("feature").size());
    org.jdom2.Element featureEl = element.getChildren("feature").get(0);
    assertEquals("stream", featureEl.getText().trim());
    assertEquals("1", featureEl.getAttributeValue("n"));
    assertEquals("25", featureEl.getAttributeValue("s"));
    assertEquals("water", featureEl.getAttributeValue("t"));
  }

  @Test
  public void testFeatureModel() {
    // Test that Feature objects work correctly for WildernessGenerator
    RRegionTheme.Feature feature = new RRegionTheme.Feature();
    feature.n = "100";
    feature.s = "50";
    feature.t = "water";
    feature.value = "lake";

    // These are the operations WildernessGenerator performs
    assertEquals("100", feature.n);
    assertEquals("50", feature.s);
    assertEquals("water", feature.t);
    assertEquals("lake", feature.value);
  }
}
