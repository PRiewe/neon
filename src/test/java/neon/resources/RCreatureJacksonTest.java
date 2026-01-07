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
import neon.entities.property.Habitat;
import neon.entities.property.Skill;
import neon.resources.RCreature.AIType;
import neon.resources.RCreature.Size;
import neon.resources.RCreature.Type;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RCreature resources. */
public class RCreatureJacksonTest {

  @Test
  public void testSimpleCreatureParsing() throws IOException {
    String xml =
        "<humanoid id=\"dwarf\" hit=\"1d8+2\" size=\"small\" speed=\"9\" color=\"gray\" char=\"@\" mana=\"1\">"
            + "<stats str=\"13\" dex=\"11\" con=\"14\" int=\"10\" wis=\"9\" cha=\"6\" />"
            + "<av>1d3</av>"
            + "</humanoid>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RCreature creature = mapper.fromXml(input, RCreature.class);

    assertNotNull(creature);
    assertEquals("dwarf", creature.id);
    assertEquals("1d8+2", creature.hit);
    assertEquals(Size.small, creature.size);
    assertEquals(9, creature.speed);
    assertEquals("gray", creature.color);
    assertEquals("@", creature.text);
    assertEquals(1, creature.mana);

    // Check stats
    assertEquals(13f, creature.str);
    assertEquals(11f, creature.dex);
    assertEquals(14f, creature.con);
    assertEquals(10f, creature.iq);
    assertEquals(9f, creature.wis);
    assertEquals(6f, creature.cha);

    // Check av
    assertEquals("1d3", creature.av);
  }

  @Test
  public void testCreatureWithSkills() throws IOException {
    String xml =
        "<humanoid id=\"dwarf\" hit=\"1d8+2\" size=\"small\" speed=\"9\" color=\"gray\" char=\"@\">"
            + "<stats str=\"13\" dex=\"11\" con=\"14\" int=\"10\" wis=\"9\" cha=\"6\" />"
            + "<av>1d3</av>"
            + "<skills axe=\"10\" block=\"5\" />"
            + "</humanoid>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RCreature creature = mapper.fromXml(input, RCreature.class);

    assertNotNull(creature);
    assertEquals("dwarf", creature.id);

    // Check skills
    assertNotNull(creature.skills);
    assertEquals(10f, creature.skills.get(Skill.AXE));
    assertEquals(5f, creature.skills.get(Skill.BLOCK));
    assertEquals(0f, creature.skills.get(Skill.BLADE)); // Not in XML, should be 0
  }

  @Test
  public void testCreatureWithOptionalFields() throws IOException {
    String xml =
        "<dragon id=\"red_dragon\" hit=\"3d12\" size=\"huge\" speed=\"12\" color=\"red\" char=\"D\" mana=\"50\" habitat=\"air\">"
            + "<stats str=\"25\" dex=\"10\" con=\"23\" int=\"16\" wis=\"15\" cha=\"21\" />"
            + "<av>3d8</av>"
            + "<dv>15</dv>"
            + "</dragon>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RCreature creature = mapper.fromXml(input, RCreature.class);

    assertNotNull(creature);
    assertEquals("red_dragon", creature.id);
    assertEquals(Size.huge, creature.size);
    assertEquals(50, creature.mana);
    assertEquals(Habitat.AIR, creature.habitat);
    assertEquals(15, creature.dv);
  }

  @Test
  public void testCreatureWithAI() throws IOException {
    String xml =
        "<humanoid id=\"guard\" hit=\"1d8\" size=\"medium\" speed=\"10\" color=\"blue\" char=\"@\">"
            + "<stats str=\"14\" dex=\"12\" con=\"13\" int=\"10\" wis=\"10\" cha=\"10\" />"
            + "<av>2d3</av>"
            + "<ai r=\"15\" a=\"5\" c=\"2\">guard</ai>"
            + "</humanoid>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RCreature creature = mapper.fromXml(input, RCreature.class);

    assertNotNull(creature);
    assertEquals("guard", creature.id);

    // Check AI
    assertEquals(AIType.guard, creature.aiType);
    assertEquals(15, creature.aiRange);
    assertEquals(5, creature.aiAggr);
    assertEquals(2, creature.aiConf);
  }

  @Test
  public void testCreatureDefaults() throws IOException {
    // Minimal creature - should use defaults for optional fields
    String xml =
        "<animal id=\"rat\" hit=\"1d4\" size=\"tiny\" speed=\"8\" color=\"brown\" char=\"r\">"
            + "<stats str=\"3\" dex=\"15\" con=\"10\" int=\"2\" wis=\"12\" cha=\"2\" />"
            + "<av>1</av>"
            + "</animal>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RCreature creature = mapper.fromXml(input, RCreature.class);

    assertNotNull(creature);
    assertEquals("rat", creature.id);

    // Check defaults
    assertEquals(0, creature.mana); // Default for optional int
    assertEquals(0, creature.dv); // Default for optional int
    assertEquals(Habitat.LAND, creature.habitat); // Default
    assertEquals(AIType.guard, creature.aiType); // Default
    assertEquals(10, creature.aiRange); // Default
  }

  @Test
  public void testToElementUsesJackson() {
    RCreature creature = new RCreature("test_creature");
    creature.type = Type.humanoid;
    creature.hit = "1d10";
    creature.speed = 10;
    creature.size = Size.medium;
    creature.text = "@";
    creature.color = "white";
    creature.av = "2d4";

    // Set stats
    creature.str = 15;
    creature.dex = 12;
    creature.con = 14;
    creature.iq = 10;
    creature.wis = 11;
    creature.cha = 9;

    // Call toElement() which uses Jackson internally
    org.jdom2.Element element = creature.toElement();

    // Verify JDOM Element
    assertEquals("humanoid", element.getName()); // Element name matches type
    assertEquals("test_creature", element.getAttributeValue("id"));
    assertEquals("1d10", element.getAttributeValue("hit"));
    assertEquals("medium", element.getAttributeValue("size"));
    assertEquals("10", element.getAttributeValue("speed"));

    // Check stats child element
    org.jdom2.Element stats = element.getChild("stats");
    assertNotNull(stats);
    assertEquals("15.0", stats.getAttributeValue("str"));
    assertEquals("10.0", stats.getAttributeValue("int"));
  }
}
