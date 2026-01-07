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
import neon.magic.Effect;
import neon.resources.RSpell.SpellType;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RSpell resources. */
public class RSpellJacksonTest {

  @Test
  public void testSimpleSpellParsing() throws IOException {
    String xml =
        "<spell id=\"fireball\" effect=\"damage_health\" range=\"10\" duration=\"0\" size=\"5\" area=\"3\" cost=\"25\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell spell = mapper.fromXml(input, RSpell.class);

    assertNotNull(spell);
    assertEquals("fireball", spell.id);
    assertEquals(Effect.DAMAGE_HEALTH, spell.effect);
    assertEquals(10, spell.range);
    assertEquals(0, spell.duration);
    assertEquals(5, spell.size);
    assertEquals(3, spell.radius);
    assertEquals(25, spell.cost);
    assertNull(spell.script);
  }

  @Test
  public void testSpellWithScript() throws IOException {
    String xml =
        "<spell id=\"custom_spell\" effect=\"scripted\" cost=\"50\">\n"
            + "  var target = get(uid);\n"
            + "  target.damage(10);\n"
            + "</spell>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell spell = mapper.fromXml(input, RSpell.class);

    assertNotNull(spell);
    assertEquals("custom_spell", spell.id);
    assertEquals(Effect.SCRIPTED, spell.effect);
    assertEquals(50, spell.cost);
    assertNotNull(spell.script);
    assertTrue(spell.script.trim().contains("var target = get(uid);"));
  }

  @Test
  public void testOptionalFieldsDefaultToZero() throws IOException {
    String xml = "<spell id=\"minimal_spell\" effect=\"restore_health\" cost=\"10\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell spell = mapper.fromXml(input, RSpell.class);

    assertNotNull(spell);
    assertEquals(0, spell.range);
    assertEquals(0, spell.duration);
    assertEquals(0, spell.size);
    assertEquals(0, spell.radius);
  }

  @Test
  public void testDiseaseType() throws IOException {
    String xml =
        "<disease id=\"plague\" effect=\"drain_health\" duration=\"100\" size=\"2\" cost=\"0\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell spell = mapper.fromXml(input, RSpell.class);

    assertNotNull(spell);
    assertEquals("plague", spell.id);
    assertEquals(Effect.DRAIN_HEALTH, spell.effect);
    assertEquals(100, spell.duration);
  }

  @Test
  public void testPoisonType() throws IOException {
    String xml =
        "<poison id=\"venom\" effect=\"damage_health\" duration=\"50\" size=\"1\" cost=\"0\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell spell = mapper.fromXml(input, RSpell.class);

    assertNotNull(spell);
    assertEquals("venom", spell.id);
    assertEquals(Effect.DAMAGE_HEALTH, spell.effect);
  }

  @Test
  public void testEnchantmentSubclass() throws IOException {
    String xml =
        "<enchant id=\"fire_sword\" effect=\"fire_damage\" size=\"5\" cost=\"100\" item=\"weapon\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell.Enchantment enchantment = mapper.fromXml(input, RSpell.Enchantment.class);

    assertNotNull(enchantment);
    assertEquals("fire_sword", enchantment.id);
    assertEquals(Effect.FIRE_DAMAGE, enchantment.effect);
    assertEquals(5, enchantment.size);
    assertEquals(100, enchantment.cost);
    assertEquals("weapon", enchantment.item);
    assertEquals(SpellType.ENCHANT, enchantment.type);
  }

  @Test
  public void testEnchantmentWithClothingItem() throws IOException {
    String xml =
        "<enchant id=\"protection_robe\" effect=\"fire_shield\" size=\"10\" cost=\"200\" item=\"clothing/armor\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell.Enchantment enchantment = mapper.fromXml(input, RSpell.Enchantment.class);

    assertNotNull(enchantment);
    assertEquals("protection_robe", enchantment.id);
    assertEquals(Effect.FIRE_SHIELD, enchantment.effect);
    assertEquals("clothing/armor", enchantment.item);
  }

  @Test
  public void testPowerSubclass() throws IOException {
    String xml =
        "<power id=\"regeneration\" effect=\"restore_health\" size=\"5\" cost=\"50\" int=\"10\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell.Power power = mapper.fromXml(input, RSpell.Power.class);

    assertNotNull(power);
    assertEquals("regeneration", power.id);
    assertEquals(Effect.RESTORE_HEALTH, power.effect);
    assertEquals(5, power.size);
    assertEquals(50, power.cost);
    assertEquals(10, power.interval);
    assertEquals(SpellType.POWER, power.type);
  }

  @Test
  public void testCaseInsensitiveEffect() throws IOException {
    String xml = "<spell id=\"test\" effect=\"DAMAGE_HEALTH\" cost=\"10\" />";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RSpell spell = mapper.fromXml(input, RSpell.class);

    assertNotNull(spell);
    assertEquals(Effect.DAMAGE_HEALTH, spell.effect);
  }

  @Test
  public void testToElementPreservesType() {
    // Create a spell with SpellType.DISEASE
    RSpell spell = new RSpell();
    spell.type = SpellType.DISEASE;
    spell.effect = Effect.DRAIN_HEALTH;
    spell.cost = 0;
    spell.duration = 100;

    org.jdom2.Element element = spell.toElement();

    // Element name should match the spell type
    assertEquals("DISEASE", element.getName());
    assertEquals("DRAIN_HEALTH", element.getAttributeValue("effect"));
    assertEquals("100", element.getAttributeValue("duration"));
  }

  @Test
  public void testEnchantmentToElement() {
    RSpell.Enchantment enchantment = new RSpell.Enchantment();
    enchantment.effect = Effect.FIRE_SHIELD;
    enchantment.item = "weapon";
    enchantment.cost = 150;
    enchantment.size = 8;

    org.jdom2.Element element = enchantment.toElement();

    assertEquals("ENCHANT", element.getName());
    assertEquals("FIRE_SHIELD", element.getAttributeValue("effect"));
    assertEquals("weapon", element.getAttributeValue("item"));
    assertEquals("150", element.getAttributeValue("cost"));
    assertEquals("8", element.getAttributeValue("size"));
  }

  @Test
  public void testPowerToElement() {
    RSpell.Power power = new RSpell.Power();
    power.effect = Effect.RESTORE_HEALTH;
    power.interval = 15;
    power.cost = 75;

    org.jdom2.Element element = power.toElement();

    assertEquals("POWER", element.getName());
    assertEquals("RESTORE_HEALTH", element.getAttributeValue("effect"));
    assertEquals("15", element.getAttributeValue("int"));
    assertEquals("75", element.getAttributeValue("cost"));
  }
}
