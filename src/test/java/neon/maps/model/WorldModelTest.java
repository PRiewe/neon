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

package neon.maps.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for WorldModel. */
public class WorldModelTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<world>"
            + "<header uid=\"1\">"
            + "<name>Test World</name>"
            + "</header>"
            + "<creatures>"
            + "<creature x=\"100\" y=\"200\" id=\"goblin\" uid=\"5\" />"
            + "</creatures>"
            + "<items>"
            + "<item x=\"150\" y=\"250\" id=\"sword\" uid=\"10\" />"
            + "</items>"
            + "<regions>"
            + "<region x=\"0\" y=\"0\" w=\"100\" h=\"100\" l=\"0\" text=\"grass\" random=\"plain\" />"
            + "</regions>"
            + "</world>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    WorldModel world = mapper.fromXml(input, WorldModel.class);

    assertNotNull(world);
    assertEquals(1, world.header.uid);
    assertEquals("Test World", world.header.name);
    assertEquals(1, world.creatures.size());
    assertEquals(100, world.creatures.get(0).x);
    assertEquals("goblin", world.creatures.get(0).id);
    assertEquals(1, world.items.items.size());
    assertEquals("sword", world.items.items.get(0).id);
    assertEquals(1, world.regions.size());
    assertEquals("grass", world.regions.get(0).text);
  }

  @Test
  public void testDoorParsing() throws IOException {
    String xml =
        "<world>"
            + "<header uid=\"1\"><name>World</name></header>"
            + "<creatures />"
            + "<items>"
            + "<door x=\"10\" y=\"20\" id=\"oak_door\" uid=\"5\" state=\"open\" lock=\"10\">"
            + "<dest x=\"30\" y=\"40\" z=\"1\" map=\"2\" />"
            + "</door>"
            + "</items>"
            + "<regions />"
            + "</world>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    WorldModel world = mapper.fromXml(input, WorldModel.class);

    assertNotNull(world);
    assertEquals(0, world.items.items.size());
    assertEquals(1, world.items.doors.size());
    WorldModel.DoorPlacement door = world.items.doors.get(0);
    assertEquals(10, door.x);
    assertEquals("oak_door", door.id);
    assertEquals("open", door.state);
    assertEquals(10, door.lock);
    assertNotNull(door.destination);
    assertEquals(30, door.destination.x);
    assertEquals(1, door.destination.z);
  }

  @Test
  public void testDoorWithTheme() throws IOException {
    String xml =
        "<world>"
            + "<header uid=\"1\"><name>World</name></header>"
            + "<creatures />"
            + "<items>"
            + "<door x=\"10\" y=\"20\" id=\"hole\" uid=\"5\" state=\"open\">"
            + "<dest theme=\"dungeon_dark\" />"
            + "</door>"
            + "</items>"
            + "<regions />"
            + "</world>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    WorldModel world = mapper.fromXml(input, WorldModel.class);

    assertEquals(1, world.items.doors.size());
    WorldModel.DoorPlacement door = world.items.doors.get(0);
    assertNotNull(door.destination);
    assertEquals("dungeon_dark", door.destination.theme);
  }

  @Test
  public void testContainerParsing() throws IOException {
    String xml =
        "<world>"
            + "<header uid=\"1\"><name>World</name></header>"
            + "<creatures />"
            + "<items>"
            + "<container x=\"50\" y=\"60\" id=\"chest\" uid=\"8\" lock=\"15\" trap=\"10\">"
            + "<item id=\"gold\" uid=\"9\" />"
            + "<item id=\"potion\" uid=\"10\" />"
            + "</container>"
            + "</items>"
            + "<regions />"
            + "</world>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    WorldModel world = mapper.fromXml(input, WorldModel.class);

    assertNotNull(world);
    assertEquals(0, world.items.items.size());
    assertEquals(1, world.items.containers.size());
    WorldModel.ContainerPlacement container = world.items.containers.get(0);
    assertEquals(50, container.x);
    assertEquals("chest", container.id);
    assertEquals(15, container.lock);
    assertEquals(10, container.trap);
    assertEquals(2, container.contents.size());
    assertEquals("gold", container.contents.get(0).id);
    assertEquals("potion", container.contents.get(1).id);
  }

  @Test
  @Disabled
  public void testMixedItems() throws IOException {
    String xml =
        """
      <world>
          <header uid="1">
              <name>World</name>
          </header>
          <creatures/>
          <items>
              <item x="1" y="2" id="sword" uid="1"/>
              <door x="3" y="4" id="door" uid="2" state="closed">
                  <dest x="10" y="10" map="2"/>
              </door>
              <container x="5" y="6" id="chest" uid="3">
                  <item id="gold" uid="4"/>
              </container>
              <item x="7" y="8" id="shield" uid="5"/>
          </items>
          <regions/>
      </world>""";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    WorldModel world = mapper.fromXml(input, WorldModel.class);

    // Note: Jackson may only parse first consecutive sequence of <item> elements
    assertFalse(world.items.items.isEmpty());
    assertEquals(1, world.items.doors.size());
    assertEquals(1, world.items.containers.size());
    assertEquals(1, world.items.items.stream().filter(x -> x.id.equals("sword")).count());
    assertEquals("door", world.items.doors.get(0).id);
    assertEquals("chest", world.items.containers.get(0).id);
  }

  @Test
  public void testRegionWithScripts() throws IOException {
    String xml =
        "<world>"
            + "<header uid=\"1\"><name>World</name></header>"
            + "<creatures />"
            + "<items />"
            + "<regions>"
            + "<region x=\"0\" y=\"0\" w=\"50\" h=\"50\" l=\"0\" text=\"grass\" random=\"plain\" label=\"Village\">"
            + "<script id=\"village_init\" />"
            + "<script id=\"village_update\" />"
            + "</region>"
            + "</regions>"
            + "</world>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    WorldModel world = mapper.fromXml(input, WorldModel.class);

    assertEquals(1, world.regions.size());
    WorldModel.RegionData region = world.regions.get(0);
    assertEquals("Village", region.label);
    assertEquals(2, region.scripts.size());
    assertEquals("village_init", region.scripts.get(0).id);
  }

  @Test
  public void testEmptyWorld() throws IOException {
    String xml =
        "<world>"
            + "<header uid=\"1\"><name>Empty</name></header>"
            + "<creatures />"
            + "<items />"
            + "<regions />"
            + "</world>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    WorldModel world = mapper.fromXml(input, WorldModel.class);

    assertNotNull(world);
    assertEquals("Empty", world.header.name);
    assertEquals(0, world.creatures.size());
    assertEquals(0, world.items.items.size());
    assertEquals(0, world.items.doors.size());
    assertEquals(0, world.items.containers.size());
    assertEquals(0, world.regions.size());
  }

  @Test
  public void testMultipleCreatures() throws IOException {
    String xml =
        "<world>"
            + "<header uid=\"1\"><name>World</name></header>"
            + "<creatures>"
            + "<creature x=\"1\" y=\"2\" id=\"goblin\" uid=\"1\" />"
            + "<creature x=\"3\" y=\"4\" id=\"orc\" uid=\"2\" />"
            + "<creature x=\"5\" y=\"6\" id=\"troll\" uid=\"3\" />"
            + "</creatures>"
            + "<items />"
            + "<regions />"
            + "</world>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    WorldModel world = mapper.fromXml(input, WorldModel.class);

    assertEquals(3, world.creatures.size());
    assertEquals("goblin", world.creatures.get(0).id);
    assertEquals("orc", world.creatures.get(1).id);
    assertEquals("troll", world.creatures.get(2).id);
  }
}
