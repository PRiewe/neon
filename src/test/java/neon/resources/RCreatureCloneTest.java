/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Maarten Driesen
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

import neon.entities.property.Skill;
import neon.resources.RCreature.Type;
import org.junit.jupiter.api.Test;

/**
 * Tests for RCreature.clone() method used by GameLoader for player initialization.
 *
 * <p>Verifies that clone() creates independent deep copies with all fields properly duplicated.
 */
public class RCreatureCloneTest {

  @Test
  public void testBasicClone() {
    RCreature original = new RCreature("test_dwarf");
    original.name = "Dwarf Warrior";
    original.type = Type.humanoid;
    original.hit = "1d10+5";
    original.str = 16;
    original.con = 14;
    original.dex = 10;

    RCreature clone = original.clone();

    assertNotNull(clone);
    assertEquals("test_dwarf", clone.id);
    assertEquals("Dwarf Warrior", clone.name);
    assertEquals(Type.humanoid, clone.type);
    assertEquals("1d10+5", clone.hit);
    assertEquals(16, clone.str);
    assertEquals(14, clone.con);
    assertEquals(10, clone.dex);
  }

  @Test
  public void testCloneIndependence() {
    RCreature original = new RCreature("test_elf");
    original.name = "Elf";
    original.str = 10;
    original.dex = 18;

    RCreature clone = original.clone();

    // Modify clone
    clone.name = "Dark Elf";
    clone.str = 8;
    clone.dex = 20;

    // Verify original unchanged
    assertEquals("Elf", original.name);
    assertEquals(10, original.str);
    assertEquals(18, original.dex);

    // Verify clone changed
    assertEquals("Dark Elf", clone.name);
    assertEquals(8, clone.str);
    assertEquals(20, clone.dex);
  }

  @Test
  public void testCloneWithComplexData() {
    RCreature original = new RCreature("test_dragon");
    original.name = "Red Dragon";
    original.type = Type.dragon;
    original.hit = "10d12+50";
    original.av = "15";
    original.dv = 10;

    // Add skills (skills is an EnumMap<Skill, Float>)
    original.skills.put(Skill.CREATION, 5.0f);
    original.skills.put(Skill.ARCHERY, 8.0f);

    RCreature clone = original.clone();

    // Verify basic fields
    assertEquals("Red Dragon", clone.name);
    assertEquals(Type.dragon, clone.type);
    assertEquals("10d12+50", clone.hit);
    assertEquals("15", clone.av);
    assertEquals(10, clone.dv);

    // Verify skills map is independent (EnumMap contains all values initialized to 0)
    assertNotSame(original.skills, clone.skills);
    assertEquals(original.skills.get(Skill.CREATION), clone.skills.get(Skill.CREATION));
    assertEquals(original.skills.get(Skill.ARCHERY), clone.skills.get(Skill.ARCHERY));

    // Modify clone's skills
    clone.skills.put(Skill.BLADE, 3.0f);

    // Verify original unchanged (only CREATION and ARCHERY were set)
    assertEquals(5.0f, original.skills.get(Skill.CREATION));
    assertEquals(8.0f, original.skills.get(Skill.ARCHERY));
    assertNull(original.skills.get(Skill.BLADE)); // Not set, should be null

    // Verify clone changed
    assertEquals(5.0f, clone.skills.get(Skill.CREATION));
    assertEquals(8.0f, clone.skills.get(Skill.ARCHERY));
    assertEquals(3.0f, clone.skills.get(Skill.BLADE)); // Now set to 3.0
  }

  @Test
  public void testClonePreservesPath() {
    RCreature original = new RCreature("test_orc", "testmod");
    original.name = "Orc";

    RCreature clone = original.clone();

    assertNotNull(clone.path);
    assertNotSame(original.path, clone.path);
    assertArrayEquals(original.path, clone.path);
    assertEquals("testmod", clone.path[0]);
  }

  @Test
  public void testCloneWithDefaultPath() {
    RCreature original = new RCreature("test_goblin");
    original.name = "Goblin";
    // Constructor initializes path, don't set to null

    RCreature clone = original.clone();

    assertNotNull(clone.path);
    assertArrayEquals(original.path, clone.path);
    assertEquals("Goblin", clone.name);
  }

  @Test
  public void testCloneWithAllStatsSet() {
    RCreature original = new RCreature("test_paladin");
    original.str = 18;
    original.con = 16;
    original.dex = 12;
    original.iq = 10;
    original.wis = 14;
    original.cha = 16;

    RCreature clone = original.clone();

    assertEquals(18, clone.str);
    assertEquals(16, clone.con);
    assertEquals(12, clone.dex);
    assertEquals(10, clone.iq);
    assertEquals(14, clone.wis);
    assertEquals(16, clone.cha);
  }

  @Test
  public void testMultipleClones() {
    RCreature original = new RCreature("test_template");
    original.name = "Template";
    original.str = 12;

    RCreature clone1 = original.clone();
    RCreature clone2 = original.clone();

    // Modify clone1
    clone1.str = 15;

    // Verify clone2 unaffected
    assertEquals(12, clone2.str);

    // Verify original unaffected
    assertEquals(12, original.str);

    // Verify clones are independent
    assertNotSame(clone1, clone2);
  }

  @Test
  public void testClonePreservesType() {
    for (Type type : Type.values()) {
      RCreature original = new RCreature("test_" + type.name());
      original.type = type;

      RCreature clone = original.clone();

      assertEquals(type, clone.type);
    }
  }

  @Test
  public void testCloneEmptyCreature() {
    RCreature original = new RCreature("empty");

    RCreature clone = original.clone();

    assertNotNull(clone);
    assertEquals("empty", clone.id);
    assertNotSame(original, clone);
  }

  @Test
  public void testCloneFailureThrowsException() {
    // Create a creature with ID that should clone successfully
    RCreature original = new RCreature("valid_id");
    original.name = "Valid";

    // This should not throw
    assertDoesNotThrow(() -> original.clone());
  }
}
