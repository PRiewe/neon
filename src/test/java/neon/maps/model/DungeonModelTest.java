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
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for DungeonModel. */
public class DungeonModelTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<dungeon>"
            + "<header uid=\"8\"><name>Test Dungeon</name></header>"
            + "<level name=\"entrance\" l=\"0\">"
            + "<creatures />"
            + "<items />"
            + "<regions>"
            + "<region x=\"0\" y=\"0\" w=\"10\" h=\"10\" l=\"0\" text=\"stone\" />"
            + "</regions>"
            + "</level>"
            + "</dungeon>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    DungeonModel dungeon = mapper.fromXml(input, DungeonModel.class);

    assertNotNull(dungeon);
    assertEquals(8, dungeon.header.uid);
    assertEquals("Test Dungeon", dungeon.header.name);
    assertEquals(1, dungeon.levels.size());
    DungeonModel.Level level = dungeon.levels.get(0);
    assertEquals("entrance", level.name);
    assertEquals(0, level.l);
    assertEquals(1, level.regions.size());
  }

  @Test
  public void testMultipleLevels() throws IOException {
    String xml =
        "<dungeon>"
            + "<header uid=\"5\"><name>Multi Level</name></header>"
            + "<level name=\"upper\" l=\"0\">"
            + "<creatures />"
            + "<items />"
            + "<regions />"
            + "</level>"
            + "<level name=\"lower\" l=\"1\">"
            + "<creatures />"
            + "<items />"
            + "<regions />"
            + "</level>"
            + "</dungeon>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    DungeonModel dungeon = mapper.fromXml(input, DungeonModel.class);

    assertEquals(2, dungeon.levels.size());
    assertEquals("upper", dungeon.levels.get(0).name);
    assertEquals(0, dungeon.levels.get(0).l);
    assertEquals("lower", dungeon.levels.get(1).name);
    assertEquals(1, dungeon.levels.get(1).l);
  }

  @Test
  public void testLevelWithTheme() throws IOException {
    String xml =
        "<dungeon>"
            + "<header uid=\"3\"><name>Themed</name></header>"
            + "<level name=\"crypt\" l=\"0\" theme=\"undead_crypt\" out=\"1,2\">"
            + "<creatures />"
            + "<items />"
            + "<regions />"
            + "</level>"
            + "</dungeon>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    DungeonModel dungeon = mapper.fromXml(input, DungeonModel.class);

    DungeonModel.Level level = dungeon.levels.get(0);
    assertEquals("undead_crypt", level.theme);
    assertEquals("1,2", level.out);
  }

  @Test
  public void testLevelWithCreaturesAndItems() throws IOException {
    String xml =
        "<dungeon>"
            + "<header uid=\"2\"><name>Populated</name></header>"
            + "<level name=\"hall\" l=\"0\">"
            + "<creatures>"
            + "<creature x=\"5\" y=\"5\" id=\"skeleton\" uid=\"10\" />"
            + "<creature x=\"8\" y=\"8\" id=\"zombie\" uid=\"11\" />"
            + "</creatures>"
            + "<items>"
            + "<item x=\"3\" y=\"3\" id=\"sword\" uid=\"1\" />"
            + "<door x=\"10\" y=\"10\" id=\"door\" uid=\"2\" state=\"locked\" lock=\"10\">"
            + "<dest x=\"5\" y=\"5\" map=\"1\" />"
            + "</door>"
            + "</items>"
            + "<regions />"
            + "</level>"
            + "</dungeon>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    DungeonModel dungeon = mapper.fromXml(input, DungeonModel.class);

    DungeonModel.Level level = dungeon.levels.get(0);
    assertEquals(2, level.creatures.size());
    assertEquals("skeleton", level.creatures.get(0).id);
    assertEquals(1, level.items.items.size());
    assertEquals(1, level.items.doors.size());
    assertEquals("door", level.items.doors.get(0).id);
  }

  @Test
  public void testThemedDungeonHeader() throws IOException {
    String xml =
        "<dungeon>"
            + "<header uid=\"7\" theme=\"goblin_cave\">"
            + "<name>Goblin Lair</name>"
            + "</header>"
            + "<level name=\"entrance\" l=\"0\">"
            + "<creatures />"
            + "<items />"
            + "<regions />"
            + "</level>"
            + "</dungeon>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    DungeonModel dungeon = mapper.fromXml(input, DungeonModel.class);

    assertEquals("goblin_cave", dungeon.header.theme);
    assertEquals("Goblin Lair", dungeon.header.name);
  }

  @Test
  public void testEmptyLevel() throws IOException {
    String xml =
        "<dungeon>"
            + "<header uid=\"1\"><name>Empty</name></header>"
            + "<level name=\"void\" l=\"0\">"
            + "<creatures />"
            + "<items />"
            + "<regions />"
            + "</level>"
            + "</dungeon>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    DungeonModel dungeon = mapper.fromXml(input, DungeonModel.class);

    DungeonModel.Level level = dungeon.levels.get(0);
    assertEquals(0, level.creatures.size());
    assertEquals(0, level.items.items.size());
    assertEquals(0, level.items.doors.size());
    assertEquals(0, level.items.containers.size());
    assertEquals(0, level.regions.size());
  }

  @Test
  public void testComplexLevel() throws IOException {
    String xml =
        "<dungeon>"
            + "<header uid=\"9\"><name>Complex</name></header>"
            + "<level name=\"treasury\" l=\"2\">"
            + "<creatures>"
            + "<creature x=\"10\" y=\"10\" id=\"dragon\" uid=\"100\" />"
            + "</creatures>"
            + "<items>"
            + "<item x=\"5\" y=\"5\" id=\"gold_pile\" uid=\"50\" />"
            + "<container x=\"15\" y=\"15\" id=\"treasure_chest\" uid=\"51\" lock=\"20\">"
            + "<item id=\"diamond\" uid=\"52\" />"
            + "<item id=\"crown\" uid=\"53\" />"
            + "</container>"
            + "<door x=\"0\" y=\"10\" id=\"vault_door\" uid=\"54\" state=\"locked\" lock=\"25\">"
            + "<dest x=\"20\" y=\"20\" z=\"1\" map=\"9\" />"
            + "</door>"
            + "</items>"
            + "<regions>"
            + "<region x=\"0\" y=\"0\" w=\"30\" h=\"30\" l=\"0\" text=\"marble\" random=\"vault\" />"
            + "</regions>"
            + "</level>"
            + "</dungeon>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    DungeonModel dungeon = mapper.fromXml(input, DungeonModel.class);

    DungeonModel.Level level = dungeon.levels.get(0);
    assertEquals(1, level.creatures.size());
    assertEquals(1, level.items.items.size());
    assertEquals(1, level.items.containers.size());
    assertEquals(1, level.items.doors.size());
    assertEquals(1, level.regions.size());
  }
}
