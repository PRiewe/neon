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
import neon.entities.property.Slot;
import neon.magic.Effect;
import neon.resources.RClothing.ArmorType;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RClothing resources. */
public class RClothingJacksonTest {

  @Test
  public void testSimpleArmorParsing() throws IOException {
    String xml =
        "<armor id=\"iron_cuirass\" name=\"Iron Cuirass\" char=\"[\" color=\"gray\" cost=\"200\" weight=\"20.0\">"
            + "<stats slot=\"cuirass\" ar=\"10\" class=\"heavy\" />"
            + "</armor>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RClothing clothing = mapper.fromXml(input, RClothing.class);

    assertNotNull(clothing);
    assertEquals("iron_cuirass", clothing.id);
    assertEquals("Iron Cuirass", clothing.name);
    assertEquals("[", clothing.text);
    assertEquals("gray", clothing.color);
    assertEquals(200, clothing.cost);
    assertEquals(20.0f, clothing.weight);
    assertEquals(Slot.CUIRASS, clothing.slot);
    assertEquals(10, clothing.rating);
    assertEquals(ArmorType.HEAVY, clothing.kind);
    assertEquals(0, clothing.magnitude); // No enchantment
    assertEquals(0, clothing.mana);
    assertNull(clothing.effect);
  }

  @Test
  public void testClothingWithoutArmor() throws IOException {
    // Clothing items don't have ar and class attributes
    String xml =
        "<clothing id=\"blue_robe\" name=\"Blue Robe\" char=\"(\" color=\"blue\" cost=\"50\" weight=\"3.0\">"
            + "<stats slot=\"shirt\" />"
            + "</clothing>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RClothing clothing = mapper.fromXml(input, RClothing.class);

    assertNotNull(clothing);
    assertEquals("blue_robe", clothing.id);
    assertEquals("Blue Robe", clothing.name);
    assertEquals(Slot.SHIRT, clothing.slot);
    assertEquals(0, clothing.rating); // No armor rating for clothing
    assertEquals(ArmorType.NONE, clothing.kind);
  }

  @Test
  public void testArmorWithEnchantment() throws IOException {
    String xml =
        "<armor id=\"magic_helm\" name=\"Helm of Fire Shield\" char=\"]\" color=\"red\" cost=\"500\" weight=\"5.0\">"
            + "<stats slot=\"helmet\" ar=\"5\" class=\"light\" />"
            + "<enchant mag=\"10\" mana=\"100\" effect=\"fire_shield\" />"
            + "</armor>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RClothing clothing = mapper.fromXml(input, RClothing.class);

    assertNotNull(clothing);
    assertEquals("magic_helm", clothing.id);
    assertEquals(Slot.HELMET, clothing.slot);
    assertEquals(5, clothing.rating);
    assertEquals(ArmorType.LIGHT, clothing.kind);
    assertEquals(10, clothing.magnitude);
    assertEquals(100, clothing.mana);
    assertEquals(Effect.FIRE_SHIELD, clothing.effect);
  }

  @Test
  public void testMediumArmor() throws IOException {
    String xml =
        "<armor id=\"leather_chausses\" char=\"[\" color=\"brown\" cost=\"75\" weight=\"8.0\">"
            + "<stats slot=\"chausses\" ar=\"3\" class=\"medium\" />"
            + "</armor>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RClothing clothing = mapper.fromXml(input, RClothing.class);

    assertNotNull(clothing);
    assertEquals("leather_chausses", clothing.id);
    assertEquals(Slot.CHAUSSES, clothing.slot);
    assertEquals(3, clothing.rating);
    assertEquals(ArmorType.MEDIUM, clothing.kind);
  }

  @Test
  public void testCaseInsensitiveEnums() throws IOException {
    // Jackson should handle case-insensitive enum parsing
    String xml =
        "<armor id=\"test_boots\" char=\"[\" color=\"black\" cost=\"50\" weight=\"4.0\">"
            + "<stats slot=\"BOOTS\" ar=\"2\" class=\"LIGHT\" />"
            + "<enchant mag=\"5\" mana=\"25\" effect=\"LEVITATE\" />"
            + "</armor>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RClothing clothing = mapper.fromXml(input, RClothing.class);

    assertNotNull(clothing);
    assertEquals(Slot.BOOTS, clothing.slot);
    assertEquals(ArmorType.LIGHT, clothing.kind);
    assertEquals(Effect.LEVITATE, clothing.effect);
  }
}
