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

/** Test Jackson XML parsing for RMod resources. */
public class RModJacksonTest {

  @Test
  public void testMainXmlParsing() throws IOException {
    String xml =
        "<master id=\"darkness\">"
            + "<title>Darkness Falls</title>"
            + "<currency big=\"gold pieces\" small=\"copper pieces\" />"
            + "</master>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RMod.MainXml main = mapper.fromXml(input, RMod.MainXml.class);

    assertNotNull(main);
    assertEquals("darkness", main.id);
    assertEquals("Darkness Falls", main.title);
    assertNotNull(main.currency);
    assertEquals("gold pieces", main.currency.big);
    assertEquals("copper pieces", main.currency.small);
  }

  @Test
  public void testExtensionXmlParsing() throws IOException {
    String xml = "<extension id=\"test_extension\">" + "<master>darkness</master>" + "</extension>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RMod.MainXml main = mapper.fromXml(input, RMod.MainXml.class);

    assertNotNull(main);
    assertEquals("test_extension", main.id);
    assertEquals("darkness", main.master);
  }

  @Test
  public void testCCXmlParsing() throws IOException {
    String xml =
        "<root>"
            + "<race>dwarf</race>"
            + "<race>elf</race>"
            + "<item>sword</item>"
            + "<item>shield</item>"
            + "<spell>heal</spell>"
            + "<map path=\"world\" x=\"100\" y=\"200\" z=\"0\" />"
            + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RMod.CCXml cc = mapper.fromXml(input, RMod.CCXml.class);

    assertNotNull(cc);
    assertEquals(2, cc.races.size());
    assertTrue(cc.races.contains("dwarf"));
    assertTrue(cc.races.contains("elf"));
    assertEquals(2, cc.items.size());
    assertTrue(cc.items.contains("sword"));
    assertTrue(cc.items.contains("shield"));
    assertEquals(1, cc.spells.size());
    assertTrue(cc.spells.contains("heal"));
    assertNotNull(cc.map);
    assertEquals("world", cc.map.path);
    assertEquals("100", cc.map.x);
    assertEquals("200", cc.map.y);
    assertEquals("0", cc.map.z);
  }

  @Test
  public void testCCXmlWithoutMap() throws IOException {
    String xml = "<root>" + "<race>human</race>" + "<item>dagger</item>" + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RMod.CCXml cc = mapper.fromXml(input, RMod.CCXml.class);

    assertNotNull(cc);
    assertEquals(1, cc.races.size());
    assertEquals(1, cc.items.size());
    assertEquals(0, cc.spells.size());
    assertNull(cc.map);
  }

  @Test
  public void testRModConstructor() throws IOException {
    String mainXml =
        "<master id=\"test_mod\">"
            + "<title>Test Mod</title>"
            + "<currency big=\"gold\" small=\"copper\" />"
            + "</master>";
    String ccXml =
        "<root>" + "<race>nord</race>" + "<item>axe</item>" + "<spell>fireball</spell>" + "</root>";

    JacksonMapper mapper = new JacksonMapper();
    RMod.MainXml main =
        mapper.fromXml(
            new ByteArrayInputStream(mainXml.getBytes(StandardCharsets.UTF_8)), RMod.MainXml.class);
    RMod.CCXml cc =
        mapper.fromXml(
            new ByteArrayInputStream(ccXml.getBytes(StandardCharsets.UTF_8)), RMod.CCXml.class);

    RMod mod = new RMod(main, cc);

    assertEquals("test_mod", mod.id);
    assertEquals("Test Mod", mod.get("title"));
    assertEquals("gold", mod.get("big"));
    assertEquals("copper", mod.get("small"));
    assertEquals(1, mod.ccRaces.size());
    assertTrue(mod.ccRaces.contains("nord"));
    assertEquals(1, mod.ccItems.size());
    assertTrue(mod.ccItems.contains("axe"));
    assertEquals(1, mod.ccSpells.size());
    assertTrue(mod.ccSpells.contains("fireball"));
  }

  @Test
  public void testGetMainElementBridge() throws IOException {
    String mainXml = "<master id=\"bridge_test\">" + "<title>Bridge Test</title>" + "</master>";

    JacksonMapper mapper = new JacksonMapper();
    RMod.MainXml main =
        mapper.fromXml(
            new ByteArrayInputStream(mainXml.getBytes(StandardCharsets.UTF_8)), RMod.MainXml.class);

    RMod mod = new RMod(main, null);

    // Test getMainElement() which now uses Jackson internally
    org.jdom2.Element element = mod.getMainElement();

    assertEquals("master", element.getName());
    assertEquals("bridge_test", element.getAttributeValue("id"));
    assertEquals("Bridge Test", element.getChildText("title"));
  }

  @Test
  public void testGetCCElementBridge() throws IOException {
    String mainXml = "<master id=\"cc_test\" />";
    String ccXml = "<root>" + "<race>elf</race>" + "<item>bow</item>" + "</root>";

    JacksonMapper mapper = new JacksonMapper();
    RMod.MainXml main =
        mapper.fromXml(
            new ByteArrayInputStream(mainXml.getBytes(StandardCharsets.UTF_8)), RMod.MainXml.class);
    RMod.CCXml cc =
        mapper.fromXml(
            new ByteArrayInputStream(ccXml.getBytes(StandardCharsets.UTF_8)), RMod.CCXml.class);

    RMod mod = new RMod(main, cc);

    // Test getCCElement() which now uses Jackson internally
    org.jdom2.Element element = mod.getCCElement();

    assertNotNull(element);
    assertEquals(1, element.getChildren("race").size());
    assertEquals("elf", element.getChildren("race").get(0).getText());
    assertEquals(1, element.getChildren("item").size());
    assertEquals("bow", element.getChildren("item").get(0).getText());
  }

  @Test
  public void testMainXmlSerialization() throws IOException {
    RMod.MainXml main = new RMod.MainXml();
    main.id = "serialize_test";
    main.title = "Serialization Test";
    main.currency = new RMod.MainXml.Currency();
    main.currency.big = "platinum";
    main.currency.small = "silver";

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(main).toString();

    assertTrue(xml.contains("id=\"serialize_test\""));
    assertTrue(xml.contains("Serialization Test"));
    assertTrue(xml.contains("platinum"));
    assertTrue(xml.contains("silver"));
  }

  @Test
  public void testCCXmlSerialization() throws IOException {
    RMod.CCXml cc = new RMod.CCXml();
    cc.races.add("hobbit");
    cc.items.add("ring");
    cc.spells.add("invisibility");

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(cc).toString();

    assertTrue(xml.contains("hobbit"));
    assertTrue(xml.contains("ring"));
    assertTrue(xml.contains("invisibility"));
  }
}
