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
import neon.entities.property.Skill;
import neon.resources.RCreature.AIType;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RPerson resources. */
public class RPersonJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<npc id=\"guard_captain\" name=\"Captain Smith\" race=\"human\">"
            + "<factions>"
            + "<faction id=\"city_guard\" rank=\"5\" />"
            + "</factions>"
            + "<ai r=\"10\" a=\"50\" c=\"75\">guard</ai>"
            + "</npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals("guard_captain", person.id);
    assertEquals("Captain Smith", person.name);
    assertEquals("human", person.species);
    assertEquals(1, person.factions.size());
    assertEquals(5, person.factions.get("city_guard"));
    assertEquals(AIType.guard, person.aiType);
    assertEquals(10, person.aiRange);
    assertEquals(50, person.aiAggr);
    assertEquals(75, person.aiConf);
  }

  @Test
  public void testSkillsParsing() throws IOException {
    String xml =
        "<npc id=\"blacksmith\" race=\"dwarf\">"
            + "<skills>"
            + "<skill id=\"BLUNT\" rank=\"80\" />"
            + "<skill id=\"AXE\" rank=\"90\" />"
            + "</skills>"
            + "</npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals(2, person.skills.size());
    assertEquals(80, person.skills.get(Skill.BLUNT));
    assertEquals(90, person.skills.get(Skill.AXE));
  }

  @Test
  public void testItemsAndSpells() throws IOException {
    String xml =
        "<npc id=\"wizard\" race=\"elf\">"
            + "<items>"
            + "<item id=\"staff\" />"
            + "<item id=\"robe\" />"
            + "</items>"
            + "<spells>"
            + "<spell id=\"fireball\" />"
            + "<spell id=\"lightning\" />"
            + "</spells>"
            + "</npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals(2, person.items.size());
    assertTrue(person.items.contains("staff"));
    assertTrue(person.items.contains("robe"));
    assertEquals(2, person.spells.size());
    assertTrue(person.spells.contains("fireball"));
    assertTrue(person.spells.contains("lightning"));
  }

  @Test
  public void testSimpleServices() throws IOException {
    String xml =
        "<npc id=\"merchant\" race=\"human\">"
            + "<service id=\"trade\" />"
            + "<service id=\"repair\" />"
            + "</npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals(2, person.services.size());
    assertEquals("trade", person.services.get(0).id);
    assertEquals("repair", person.services.get(1).id);
  }

  @Test
  public void testTrainingService() throws IOException {
    String xml =
        "<npc id=\"trainer\" race=\"human\">"
            + "<service id=\"training\">"
            + "<skill>BLADE</skill>"
            + "<skill>BLOCK</skill>"
            + "</service>"
            + "</npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals(1, person.services.size());
    RPerson.Service service = person.services.get(0);
    assertEquals("training", service.id);
    assertEquals(2, service.skills.size());
    assertEquals("BLADE", service.skills.get(0));
    assertEquals("BLOCK", service.skills.get(1));
  }

  @Test
  public void testTravelService() throws IOException {
    String xml =
        "<npc id=\"ferryman\" race=\"human\">"
            + "<service id=\"travel\">"
            + "<dest x=\"1000\" y=\"2000\" name=\"North Town\" cost=\"10\" />"
            + "<dest x=\"3000\" y=\"4000\" name=\"South City\" cost=\"25\" />"
            + "</service>"
            + "</npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals(1, person.services.size());
    RPerson.Service service = person.services.get(0);
    assertEquals("travel", service.id);
    assertEquals(2, service.destinations.size());

    RPerson.Service.Destination dest1 = service.destinations.get(0);
    assertEquals(1000, dest1.x);
    assertEquals(2000, dest1.y);
    assertEquals("North Town", dest1.name);
    assertEquals(10, dest1.cost);

    RPerson.Service.Destination dest2 = service.destinations.get(1);
    assertEquals(3000, dest2.x);
    assertEquals(4000, dest2.y);
    assertEquals("South City", dest2.name);
    assertEquals(25, dest2.cost);
  }

  @Test
  public void testScripts() throws IOException {
    String xml =
        "<npc id=\"quest_giver\" race=\"human\">"
            + "<script>init_quest.js</script>"
            + "<script>complete_quest.js</script>"
            + "</npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals(2, person.scripts.size());
    assertEquals("init_quest.js", person.scripts.get(0));
    assertEquals("complete_quest.js", person.scripts.get(1));
  }

  @Test
  public void testSerialization() throws IOException {
    RPerson person = new RPerson("test_npc");
    person.name = "Test Character";
    person.species = "human";
    person.factions.put("guild", 3);
    person.aiType = AIType.wander;
    person.aiRange = 5;
    person.aiAggr = 25;
    person.aiConf = 50;
    person.skills.put(Skill.BLADE, 50);
    person.items.add("sword");
    person.spells.add("heal");

    RPerson.Service service = new RPerson.Service();
    service.id = "trade";
    person.services.add(service);

    person.scripts.add("test.js");

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(person).toString();

    assertTrue(xml.contains("id=\"test_npc\""));
    assertTrue(xml.contains("name=\"Test Character\""));
    assertTrue(xml.contains("race=\"human\""));
    assertTrue(xml.contains("guild"));
    assertTrue(xml.contains("wander"));
    assertTrue(xml.contains("BLADE"));
    assertTrue(xml.contains("sword"));
    assertTrue(xml.contains("heal"));
    assertTrue(xml.contains("trade"));
    assertTrue(xml.contains("test.js"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml =
        "<npc id=\"complex_npc\" name=\"Complex NPC\" race=\"elf\">"
            + "<factions>"
            + "<faction id=\"elves\" rank=\"10\" />"
            + "<faction id=\"mages\" rank=\"5\" />"
            + "</factions>"
            + "<ai r=\"15\" a=\"30\" c=\"60\">wander</ai>"
            + "<skills>"
            + "<skill id=\"ILLUSION\" rank=\"70\" />"
            + "</skills>"
            + "<items>"
            + "<item id=\"staff\" />"
            + "</items>"
            + "<spells>"
            + "<spell id=\"invisibility\" />"
            + "</spells>"
            + "<service id=\"trade\" />"
            + "<service id=\"training\">"
            + "<skill>ILLUSION</skill>"
            + "</service>"
            + "<script>elf_greeting.js</script>"
            + "</npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals("complex_npc", person.id);
    assertEquals("Complex NPC", person.name);
    assertEquals(2, person.factions.size());
    assertEquals(1, person.skills.size());
    assertEquals(1, person.items.size());
    assertEquals(1, person.spells.size());
    assertEquals(2, person.services.size());
    assertEquals(1, person.scripts.size());

    // Serialize back
    String serialized = mapper.toXml(person).toString();
    assertTrue(serialized.contains("complex_npc"));
    assertTrue(serialized.contains("elf"));
    assertTrue(serialized.contains("elves"));
    assertTrue(serialized.contains("wander"));
    assertTrue(serialized.contains("ILLUSION"));
    assertTrue(serialized.contains("invisibility"));
  }

  @Test
  public void testToElementBridge() {
    RPerson person = new RPerson("bridge_test");
    person.species = "dwarf";
    person.name = "Test Dwarf";
    person.factions.put("miners", 7);
    person.aiType = AIType.guard;
    person.aiRange = 8;
    person.skills.put(Skill.AXE, 80);
    person.items.add("pickaxe");
    person.spells.add("earth_shield");

    RPerson.Service service = new RPerson.Service();
    service.id = "repair";
    person.services.add(service);

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = person.toElement();

    assertEquals("npc", element.getName());
    assertEquals("bridge_test", element.getAttributeValue("id"));
    assertEquals("dwarf", element.getAttributeValue("race"));

    // Verify complex structures were serialized
    assertNotNull(element.getChild("factions"));
    assertNotNull(element.getChild("ai"));
    assertEquals("guard", element.getChild("ai").getText());
    assertNotNull(element.getChild("skills"));
    assertNotNull(element.getChild("items"));
    assertNotNull(element.getChild("spells"));
    assertEquals(1, element.getChildren("service").size());
  }

  @Test
  public void testEmptyNPC() throws IOException {
    String xml = "<npc id=\"empty\" race=\"human\"></npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertEquals("empty", person.id);
    assertEquals("human", person.species);
    assertEquals(0, person.factions.size());
    assertEquals(0, person.skills.size());
    assertEquals(0, person.items.size());
    assertEquals(0, person.spells.size());
    assertEquals(0, person.services.size());
    assertEquals(0, person.scripts.size());
  }

  @Test
  public void testAIWithoutType() throws IOException {
    String xml = "<npc id=\"test\" race=\"human\"><ai r=\"5\" /></npc>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RPerson person = mapper.fromXml(input, RPerson.class);

    assertNotNull(person);
    assertNull(person.aiType);
    assertEquals(5, person.aiRange);
    assertEquals(-1, person.aiAggr);
    assertEquals(-1, person.aiConf);
  }
}
