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

  // ========== Roundtrip Tests (Serialization + Deserialization) ==========

  @Test
  public void testSimpleArmorRoundtrip() throws IOException {
    // Create armor programmatically
    RClothing original = new RClothing("test_cuirass", RItem.Type.armor);
    original.name = "Test Cuirass";
    original.text = "[";
    original.color = "gray";
    original.cost = 200;
    original.weight = 20.0f;
    original.slot = Slot.CUIRASS;
    original.rating = 10;
    original.kind = ArmorType.HEAVY;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RClothing roundtrip = mapper.fromXml(input, RClothing.class);

    // Verify all fields match
    assertEquals(original.id, roundtrip.id);
    assertEquals(original.name, roundtrip.name);
    assertEquals(original.slot, roundtrip.slot);
    assertEquals(original.rating, roundtrip.rating);
    assertEquals(original.kind, roundtrip.kind);
    assertEquals(0, roundtrip.magnitude); // No enchantment
  }

  @Test
  public void testClothingWithoutArmorRoundtrip() throws IOException {
    // Create clothing (not armor) programmatically
    RClothing original = new RClothing("test_robe", RItem.Type.clothing);
    original.name = "Test Robe";
    original.text = "(";
    original.color = "blue";
    original.cost = 50;
    original.weight = 3.0f;
    original.slot = Slot.SHIRT;
    original.rating = 0;
    original.kind = ArmorType.NONE;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RClothing roundtrip = mapper.fromXml(input, RClothing.class);

    // Verify all fields match
    assertEquals(original.id, roundtrip.id);
    assertEquals(original.name, roundtrip.name);
    assertEquals(original.slot, roundtrip.slot);
    assertEquals(0, roundtrip.rating);
    assertEquals(ArmorType.NONE, roundtrip.kind);
  }

  @Test
  public void testArmorWithEnchantmentRoundtrip() throws IOException {
    // Create enchanted armor programmatically
    RClothing original = new RClothing("test_helm", RItem.Type.armor);
    original.name = "Test Helm";
    original.text = "]";
    original.color = "red";
    original.cost = 500;
    original.weight = 5.0f;
    original.slot = Slot.HELMET;
    original.rating = 5;
    original.kind = ArmorType.LIGHT;
    original.magnitude = 10;
    original.mana = 100;
    original.effect = Effect.FIRE_SHIELD;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RClothing roundtrip = mapper.fromXml(input, RClothing.class);

    // Verify all fields match including enchantment
    assertEquals(original.id, roundtrip.id);
    assertEquals(original.slot, roundtrip.slot);
    assertEquals(original.rating, roundtrip.rating);
    assertEquals(original.kind, roundtrip.kind);
    assertEquals(original.magnitude, roundtrip.magnitude);
    assertEquals(original.mana, roundtrip.mana);
    assertEquals(original.effect, roundtrip.effect);
  }

  @Test
  public void testMediumArmorRoundtrip() throws IOException {
    // Create medium armor programmatically
    RClothing original = new RClothing("test_chausses", RItem.Type.armor);
    original.text = "[";
    original.color = "brown";
    original.cost = 75;
    original.weight = 8.0f;
    original.slot = Slot.CHAUSSES;
    original.rating = 3;
    original.kind = ArmorType.MEDIUM;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RClothing roundtrip = mapper.fromXml(input, RClothing.class);

    // Verify all fields match
    assertEquals(original.id, roundtrip.id);
    assertEquals(original.slot, roundtrip.slot);
    assertEquals(original.rating, roundtrip.rating);
    assertEquals(ArmorType.MEDIUM, roundtrip.kind);
  }

  @Test
  public void testAllArmorTypesRoundtrip() throws IOException {
    // Test that all armor types serialize/deserialize correctly
    for (ArmorType armorType : ArmorType.values()) {
      // Skip NONE for this test since it's for clothing, not armor
      if (armorType == ArmorType.NONE) continue;

      RClothing original =
          new RClothing("test_" + armorType.name().toLowerCase(), RItem.Type.armor);
      original.slot = Slot.CUIRASS;
      original.rating = 5;
      original.kind = armorType;

      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(original).toString();
      InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
      RClothing roundtrip = mapper.fromXml(input, RClothing.class);

      assertEquals(armorType, roundtrip.kind, "Failed for armor type: " + armorType);
      assertEquals(5, roundtrip.rating);
    }
  }

  @Test
  public void testAllSlotsRoundtrip() throws IOException {
    // Test that all equipment slots serialize/deserialize correctly
    for (Slot slotType : Slot.values()) {
      RClothing original =
          new RClothing("test_" + slotType.name().toLowerCase(), RItem.Type.clothing);
      original.slot = slotType;

      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(original).toString();
      InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
      RClothing roundtrip = mapper.fromXml(input, RClothing.class);

      assertEquals(slotType, roundtrip.slot, "Failed for slot type: " + slotType);
    }
  }
}
