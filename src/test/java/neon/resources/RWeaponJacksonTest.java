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
}
