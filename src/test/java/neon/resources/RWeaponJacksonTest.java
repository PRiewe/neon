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
import neon.resources.RWeapon.WeaponType;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RWeapon resources. */
public class RWeaponJacksonTest {

  @Test
  public void testSimpleWeaponParsing() throws IOException {
    String xml =
        "<weapon id=\"longsword\" name=\"Long Sword\" char=\"/\" color=\"gray\" dmg=\"1d8\" type=\"blade_one\" cost=\"50\" weight=\"4.0\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RWeapon weapon = mapper.fromXml(input, RWeapon.class);

    assertNotNull(weapon);
    assertEquals("longsword", weapon.id);
    assertEquals("Long Sword", weapon.name);
    assertEquals("/", weapon.text);
    assertEquals("gray", weapon.color);
    assertEquals("1d8", weapon.damage);
    assertEquals(WeaponType.BLADE_ONE, weapon.weaponType);
    assertEquals(50, weapon.cost);
    assertEquals(4.0f, weapon.weight);
    assertEquals(0, weapon.mana); // Not specified, should be 0
  }

  @Test
  public void testWeaponWithMana() throws IOException {
    String xml =
        "<weapon id=\"magic_staff\" char=\"|\" color=\"blue\" dmg=\"1d6\" type=\"blunt_one\" cost=\"200\" weight=\"2.5\" mana=\"50\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RWeapon weapon = mapper.fromXml(input, RWeapon.class);

    assertNotNull(weapon);
    assertEquals("magic_staff", weapon.id);
    assertEquals("1d6", weapon.damage);
    assertEquals(WeaponType.BLUNT_ONE, weapon.weaponType);
    assertEquals(50, weapon.mana);
  }

  @Test
  public void testRangedWeapon() throws IOException {
    String xml =
        "<weapon id=\"longbow\" name=\"Long Bow\" char=\"}\" color=\"brown\" dmg=\"1d8\" type=\"bow\" cost=\"100\" weight=\"3.0\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RWeapon weapon = mapper.fromXml(input, RWeapon.class);

    assertNotNull(weapon);
    assertEquals("longbow", weapon.id);
    assertEquals("Long Bow", weapon.name);
    assertEquals(WeaponType.BOW, weapon.weaponType);
    assertTrue(weapon.isRanged());
  }

  @Test
  public void testCaseInsensitiveWeaponType() throws IOException {
    // Jackson should handle case-insensitive enum parsing
    String xml =
        "<weapon id=\"dagger\" char=\"-\" color=\"silver\" dmg=\"1d4\" type=\"BLADE_ONE\" cost=\"10\" weight=\"1.0\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RWeapon weapon = mapper.fromXml(input, RWeapon.class);

    assertNotNull(weapon);
    assertEquals(WeaponType.BLADE_ONE, weapon.weaponType);
  }

  @Test
  public void testTwoHandedWeapon() throws IOException {
    String xml =
        "<weapon id=\"greatsword\" name=\"Great Sword\" char=\"/\" color=\"steel\" dmg=\"2d6\" type=\"blade_two\" cost=\"150\" weight=\"8.0\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RWeapon weapon = mapper.fromXml(input, RWeapon.class);

    assertNotNull(weapon);
    assertEquals("greatsword", weapon.id);
    assertEquals("Great Sword", weapon.name);
    assertEquals("2d6", weapon.damage);
    assertEquals(WeaponType.BLADE_TWO, weapon.weaponType);
    assertFalse(weapon.isRanged());
  }

  // ========== Roundtrip Tests (Serialization + Deserialization) ==========

  @Test
  public void testSimpleWeaponRoundtrip() throws IOException {
    // Create weapon programmatically
    RWeapon original = new RWeapon("test_sword", RItem.Type.weapon);
    original.name = "Test Sword";
    original.text = "/";
    original.color = "silver";
    original.damage = "1d8";
    original.weaponType = WeaponType.BLADE_ONE;
    original.cost = 50;
    original.weight = 4.0f;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RWeapon roundtrip = mapper.fromXml(input, RWeapon.class);

    // Verify all fields match
    assertEquals(original.id, roundtrip.id);
    assertEquals(original.name, roundtrip.name);
    assertEquals(original.text, roundtrip.text);
    assertEquals(original.color, roundtrip.color);
    assertEquals(original.damage, roundtrip.damage);
    assertEquals(original.weaponType, roundtrip.weaponType);
    assertEquals(original.cost, roundtrip.cost);
    assertEquals(original.weight, roundtrip.weight);
    assertEquals(0, roundtrip.mana); // Default value
  }

  @Test
  public void testWeaponWithManaRoundtrip() throws IOException {
    // Create enchanted weapon programmatically
    RWeapon original = new RWeapon("test_staff", RItem.Type.weapon);
    original.name = "Test Staff";
    original.text = "|";
    original.color = "blue";
    original.damage = "1d6";
    original.weaponType = WeaponType.BLUNT_ONE;
    original.cost = 200;
    original.weight = 2.5f;
    original.mana = 50;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RWeapon roundtrip = mapper.fromXml(input, RWeapon.class);

    // Verify all fields match
    assertEquals(original.id, roundtrip.id);
    assertEquals(original.name, roundtrip.name);
    assertEquals(original.damage, roundtrip.damage);
    assertEquals(original.weaponType, roundtrip.weaponType);
    assertEquals(original.mana, roundtrip.mana);
  }

  @Test
  public void testRangedWeaponRoundtrip() throws IOException {
    // Create ranged weapon programmatically
    RWeapon original = new RWeapon("test_bow", RItem.Type.weapon);
    original.name = "Test Bow";
    original.text = "}";
    original.color = "brown";
    original.damage = "1d8";
    original.weaponType = WeaponType.BOW;
    original.cost = 100;
    original.weight = 3.0f;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RWeapon roundtrip = mapper.fromXml(input, RWeapon.class);

    // Verify all fields match including ranged property
    assertEquals(original.id, roundtrip.id);
    assertEquals(original.weaponType, roundtrip.weaponType);
    assertTrue(roundtrip.isRanged());
    assertEquals(original.damage, roundtrip.damage);
  }

  @Test
  public void testTwoHandedWeaponRoundtrip() throws IOException {
    // Create two-handed weapon programmatically
    RWeapon original = new RWeapon("test_greatsword", RItem.Type.weapon);
    original.name = "Test Greatsword";
    original.text = "/";
    original.color = "steel";
    original.damage = "2d6";
    original.weaponType = WeaponType.BLADE_TWO;
    original.cost = 150;
    original.weight = 8.0f;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RWeapon roundtrip = mapper.fromXml(input, RWeapon.class);

    // Verify all fields match
    assertEquals(original.id, roundtrip.id);
    assertEquals(original.weaponType, roundtrip.weaponType);
    assertFalse(roundtrip.isRanged());
    assertEquals(original.damage, roundtrip.damage);
  }

  @Test
  public void testAllWeaponTypesRoundtrip() throws IOException {
    // Test that all weapon types serialize/deserialize correctly
    for (WeaponType type : WeaponType.values()) {
      RWeapon original = new RWeapon("test_" + type.name().toLowerCase(), RItem.Type.weapon);
      original.damage = "1d6";
      original.weaponType = type;

      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(original).toString();
      InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
      RWeapon roundtrip = mapper.fromXml(input, RWeapon.class);

      assertEquals(type, roundtrip.weaponType, "Failed for weapon type: " + type);
    }
  }
}
