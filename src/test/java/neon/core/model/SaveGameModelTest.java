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

package neon.core.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for SaveGameModel. */
public class SaveGameModelTest {

  @Test
  public void testBasicPlayerData() throws IOException {
    String xml =
        """
        <save version="2.0">
          <player name="TestHero" race="human" gender="male" spec="COMBAT" prof="warrior" sign="warrior" map="1" l="0" x="100" y="200">
            <skills BLADE="10.0" CLIMBING="5.0"/>
            <stats str="15" con="14" dex="12" int="10" wis="11" cha="9"/>
            <money>500</money>
          </player>
          <journal/>
          <events/>
          <timer ticks="1000"/>
        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    assertNotNull(save);
    assertEquals("2.0", save.version);
    assertNotNull(save.player);
    assertEquals("TestHero", save.player.name);
    assertEquals("human", save.player.race);
    assertEquals("male", save.player.gender);
    assertEquals("COMBAT", save.player.specialisation);
    assertEquals("warrior", save.player.profession);
    assertEquals("warrior", save.player.sign);
    assertEquals(1, save.player.map);
    assertEquals(0, save.player.level);
    assertEquals(100, save.player.x);
    assertEquals(200, save.player.y);

    assertNotNull(save.player.skills);
    assertEquals(10.0f, save.player.skills.BLADE);
    assertEquals(5.0f, save.player.skills.CLIMBING);

    assertNotNull(save.player.stats);
    assertEquals(15, save.player.stats.str);
    assertEquals(14, save.player.stats.con);
    assertEquals(12, save.player.stats.dex);
    assertEquals(10, save.player.stats.int_);
    assertEquals(11, save.player.stats.wis);
    assertEquals(9, save.player.stats.cha);

    assertNotNull(save.player.money);
    assertEquals(500, save.player.money.value);

    assertNotNull(save.timer);
    assertEquals(1000, save.timer.ticks);
  }

  @Test
  public void testPlayerItems() throws IOException {
    String xml =
        """
        <save>
          <player name="Hero" race="elf" gender="female" spec="MAGIC" prof="mage" sign="mage" map="2" l="1" x="50" y="75">
            <skills/>
            <stats str="10" con="10" dex="10" int="10" wis="10" cha="10"/>
            <money>0</money>
            <item uid="123"/>
            <item uid="456"/>
            <item uid="789"/>
          </player>
          <journal/>
          <events/>
          <timer ticks="0"/>

        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    assertEquals(3, save.player.items.size());
    assertEquals(123L, save.player.items.get(0).uid);
    assertEquals(456L, save.player.items.get(1).uid);
    assertEquals(789L, save.player.items.get(2).uid);
  }

  @Test
  public void testPlayerSpells() throws IOException {
    String xml =
        """
        <save>
          <player name="Wizard" race="human" gender="male" spec="MAGIC" prof="wizard" sign="wizard" map="1" l="0" x="0" y="0">
            <skills/>
            <stats str="10" con="10" dex="10" int="10" wis="10" cha="10"/>
            <money>0</money>
            <spell>fireball</spell>
            <spell>ice_storm</spell>
            <spell>lightning_bolt</spell>
          </player>
          <journal/>
          <events/>
          <timer ticks="0"/>

        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    assertEquals(3, save.player.spells.size());
    assertEquals("fireball", save.player.spells.get(0).id);
    assertEquals("ice_storm", save.player.spells.get(1).id);
    assertEquals("lightning_bolt", save.player.spells.get(2).id);
  }

  @Test
  public void testPlayerFeats() throws IOException {
    String xml =
        """
        <save>
          <player name="Fighter" race="dwarf" gender="male" spec="COMBAT" prof="fighter" sign="fighter" map="1" l="0" x="0" y="0">
            <skills/>
            <stats str="10" con="10" dex="10" int="10" wis="10" cha="10"/>
            <money>0</money>
            <feat>DODGE</feat>
            <feat>POWER_ATTACK</feat>
          </player>
          <journal/>
          <events/>
          <timer ticks="0"/>

        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    assertEquals(2, save.player.feats.size());
    assertEquals("DODGE", save.player.feats.get(0).name);
    assertEquals("POWER_ATTACK", save.player.feats.get(1).name);
  }

  @Test
  public void testJournal() throws IOException {
    String xml =
        """
        <save>
          <player name="Hero" race="human" gender="male" spec="COMBAT" prof="warrior" sign="warrior" map="1" l="0" x="0" y="0">
            <skills/>
            <stats str="10" con="10" dex="10" int="10" wis="10" cha="10"/>
            <money>0</money>
          </player>
          <journal>
            <quest id="main_quest" stage="2">Find the ancient artifact</quest>
            <quest id="side_quest" stage="1">Help the villagers</quest>
          </journal>
          <events/>
          <timer ticks="0"/>

        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    assertEquals(2, save.journal.quests.size());
    assertEquals("main_quest", save.journal.quests.get(0).id);
    assertEquals(2, save.journal.quests.get(0).stage);
    assertEquals("Find the ancient artifact", save.journal.quests.get(0).subject);
    assertEquals("side_quest", save.journal.quests.get(1).id);
    assertEquals(1, save.journal.quests.get(1).stage);
    assertEquals("Help the villagers", save.journal.quests.get(1).subject);
  }

  @Test
  public void testTaskEvents() throws IOException {
    String xml =
        """
        <save>
          <player name="Hero" race="human" gender="male" spec="COMBAT" prof="warrior" sign="warrior" map="1" l="0" x="0" y="0">
            <skills/>
            <stats str="10" con="10" dex="10" int="10" wis="10" cha="10"/>
            <money>0</money>
          </player>
          <journal/>
          <events>
            <task desc="Test task" script="test.js"/>
            <task desc="Another task" script="another.js"/>
          </events>
          <timer ticks="0"/>

        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    assertEquals(2, save.events.tasks.size());
    assertEquals("Test task", save.events.tasks.get(0).description);
    assertEquals("test.js", save.events.tasks.get(0).script);
    assertEquals("Another task", save.events.tasks.get(1).description);
    assertEquals("another.js", save.events.tasks.get(1).script);
  }

  @Test
  public void testTimerEventsScript() throws IOException {
    String xml =
        """
        <save>
          <player name="Hero" race="human" gender="male" spec="COMBAT" prof="warrior" sign="warrior" map="1" l="0" x="0" y="0">
            <skills/>
            <stats str="10" con="10" dex="10" int="10" wis="10" cha="10"/>
            <money>0</money>
          </player>
          <journal/>
          <events>
            <timer tick="100:10:200" task="script" script="timed.js"/>
          </events>
          <timer ticks="0"/>

        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    assertEquals(1, save.events.timerEvents.size());
    SaveGameModel.TimerEvent event = save.events.timerEvents.get(0);
    assertEquals("100:10:200", event.tick);
    assertEquals("script", event.taskType);
    assertEquals("timed.js", event.script);
  }

  @Test
  public void testTimerEventsMagic() throws IOException {
    String xml =
        """
        <save>
          <player name="Hero" race="human" gender="male" spec="COMBAT" prof="warrior" sign="warrior" map="1" l="0" x="0" y="0">
            <skills/>
            <stats str="10" con="10" dex="10" int="10" wis="10" cha="10"/>
            <money>0</money>
          </player>
          <journal/>
          <events>
            <timer tick="50:5:100" task="magic" effect="DAMAGE" target="123" caster="456" stype="SPELL" mag="25.5"/>
          </events>
          <timer ticks="0"/>

        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    assertEquals(1, save.events.timerEvents.size());
    SaveGameModel.TimerEvent event = save.events.timerEvents.get(0);
    assertEquals("50:5:100", event.tick);
    assertEquals("magic", event.taskType);
    assertEquals("DAMAGE", event.effect);
    assertEquals(123L, event.target);
    assertEquals(456L, event.caster);
    assertEquals("SPELL", event.spellType);
    assertEquals(25.5f, event.magnitude);
  }

  @Test
  public void testAllSkills() throws IOException {
    String xml =
        """
        <save>
          <player name="Hero" race="human" gender="male" spec="COMBAT" prof="warrior" sign="warrior" map="1" l="0" x="0" y="0">
            <skills BLADE="10.0" BLUNT="9.0" AXE="8.0" SPEAR="7.0" ARCHERY="6.0"
                    CLIMBING="15.0" LOCKPICKING="3.0" SNEAK="2.0"
                    LIGHT_ARMOR="12.0" MEDIUM_ARMOR="11.0" HEAVY_ARMOR="10.0"
                    ALTERATION="5.0" CONJURATION="4.0" DESTRUCTION="3.0" ILLUSION="2.0" RESTORATION="1.0"
                    ALCHEMY="6.0" ENCHANT="7.0"/>
            <stats str="10" con="10" dex="10" int="10" wis="10" cha="10"/>
            <money>0</money>
          </player>
          <journal/>
          <events/>
          <timer ticks="0"/>

        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    SaveGameModel.SkillsData skills = save.player.skills;
    assertEquals(10.0f, skills.BLADE);
    assertEquals(9.0f, skills.BLUNT);
    assertEquals(8.0f, skills.AXE);
    assertEquals(7.0f, skills.SPEAR);
    assertEquals(6.0f, skills.ARCHERY);
    assertEquals(15.0f, skills.CLIMBING);
    assertEquals(3.0f, skills.LOCKPICKING);
    assertEquals(2.0f, skills.SNEAK);
    assertEquals(12.0f, skills.LIGHT_ARMOR);
    assertEquals(11.0f, skills.MEDIUM_ARMOR);
    assertEquals(10.0f, skills.HEAVY_ARMOR);
    assertEquals(5.0f, skills.ALTERATION);
    assertEquals(4.0f, skills.CONJURATION);
    assertEquals(3.0f, skills.DESTRUCTION);
    assertEquals(2.0f, skills.ILLUSION);
    assertEquals(1.0f, skills.RESTORATION);
    assertEquals(6.0f, skills.ALCHEMY);
    assertEquals(7.0f, skills.ENCHANT);
  }

  @Test
  public void testCompleteGame() throws IOException {
    String xml =
        """
        <save version="2.0">
          <player name="Aldric" race="human" gender="male" spec="STEALTH" prof="thief" sign="thief" map="5" l="2" x="1024" y="768">
            <skills BLADE="15.5" CLIMBING="20.0" SNEAK="25.0" LOCKPICKING="18.5"/>
            <stats str="12" con="13" dex="18" int="14" wis="12" cha="11"/>
            <money>1500</money>
            <item uid="1001"/>
            <item uid="1002"/>
            <item uid="1003"/>
            <spell>invisibility</spell>
            <spell>unlock</spell>
            <feat>DODGE</feat>
            <feat>SNEAK_ATTACK</feat>
          </player>
          <journal>
            <quest id="thieves_guild" stage="3">Steal the crown jewels</quest>
            <quest id="merchant_quest" stage="1">Deliver package to merchant</quest>
          </journal>
          <events>
            <task desc="Guild meeting" script="guild_meeting.js"/>
            <timer tick="500:100:1000" task="script" script="patrol.js"/>
            <timer tick="750:50:1500" task="magic" effect="RESTORE" target="999" caster="1000" stype="POWER" mag="10.0"/>
          </events>
          <timer ticks="5432"/>
        </save>
        """;

    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel save = mapper.fromXml(input, SaveGameModel.class);

    // Verify player data
    assertEquals("Aldric", save.player.name);
    assertEquals("thief", save.player.profession);
    assertEquals(5, save.player.map);
    assertEquals(2, save.player.level);
    assertEquals(1024, save.player.x);
    assertEquals(768, save.player.y);

    // Verify skills
    assertEquals(15.5f, save.player.skills.BLADE);
    assertEquals(20.0f, save.player.skills.CLIMBING);
    assertEquals(25.0f, save.player.skills.SNEAK);
    assertEquals(18.5f, save.player.skills.LOCKPICKING);

    // Verify stats
    assertEquals(18, save.player.stats.dex);

    // Verify money
    assertEquals(1500, save.player.money.value);

    // Verify items
    assertEquals(3, save.player.items.size());

    // Verify spells
    assertEquals(2, save.player.spells.size());
    assertEquals("invisibility", save.player.spells.get(0).id);

    // Verify feats
    assertEquals(2, save.player.feats.size());
    assertEquals("DODGE", save.player.feats.get(0).name);

    // Verify journal
    assertEquals(2, save.journal.quests.size());
    assertEquals("thieves_guild", save.journal.quests.get(0).id);

    // Verify events
    assertEquals(1, save.events.tasks.size());
    assertEquals(2, save.events.timerEvents.size());

    // Verify timer
    assertEquals(5432, save.timer.ticks);
  }

  @Test
  public void testRoundTrip() throws IOException {
    // Create a save model
    SaveGameModel original = new SaveGameModel();
    original.version = "2.0";

    original.player = new SaveGameModel.PlayerSaveData();
    original.player.name = "TestHero";
    original.player.race = "human";
    original.player.gender = "male";
    original.player.specialisation = "COMBAT";
    original.player.profession = "warrior";
    original.player.sign = "warrior";
    original.player.map = 1;
    original.player.level = 0;
    original.player.x = 100;
    original.player.y = 200;

    original.player.skills = new SaveGameModel.SkillsData();
    original.player.skills.BLADE = 10.0f;
    original.player.skills.CLIMBING = 5.0f;

    original.player.stats = new SaveGameModel.StatsData();
    original.player.stats.str = 15;
    original.player.stats.con = 14;
    original.player.stats.dex = 12;
    original.player.stats.int_ = 10;
    original.player.stats.wis = 11;
    original.player.stats.cha = 9;

    original.player.money = new SaveGameModel.MoneyData();
    original.player.money.value = 500;

    original.timer = new SaveGameModel.TimerData();
    original.timer.ticks = 1000;

    // Serialize to XML
    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString("UTF-8");

    // Deserialize back
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    SaveGameModel deserialized = mapper.fromXml(input, SaveGameModel.class);

    // Verify
    assertEquals(original.player.name, deserialized.player.name);
    assertEquals(original.player.race, deserialized.player.race);
    assertEquals(original.player.skills.BLADE, deserialized.player.skills.BLADE);
    assertEquals(original.player.stats.str, deserialized.player.stats.str);
    assertEquals(original.player.money.value, deserialized.player.money.value);
    assertEquals(original.timer.ticks, deserialized.timer.ticks);
  }
}
