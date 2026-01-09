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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson model for save game XML structure.
 *
 * <p>This class represents the parsed XML structure of a save game file. It is designed to separate
 * XML parsing (Jackson's responsibility) from game object construction (GameLoader's
 * responsibility).
 *
 * @author priewe
 */
@JacksonXmlRootElement(localName = "save")
public class SaveGameModel {

  @JacksonXmlProperty(isAttribute = true, localName = "version")
  public String version = "2.0"; // Add versioning for future compatibility

  @JacksonXmlProperty(localName = "player")
  public PlayerSaveData player;

  @JacksonXmlProperty(localName = "journal")
  public JournalData journal = new JournalData();

  @JacksonXmlProperty(localName = "events")
  public EventsData events = new EventsData();

  @JacksonXmlProperty(localName = "timer")
  public TimerData timer;

  @JacksonXmlProperty(localName = "quests")
  public QuestsData quests; // Optional - null if no random quests

  /** Player save data */
  public static class PlayerSaveData {
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String name;

    @JacksonXmlProperty(isAttribute = true, localName = "race")
    public String race;

    @JacksonXmlProperty(isAttribute = true, localName = "gender")
    public String gender;

    @JacksonXmlProperty(isAttribute = true, localName = "spec")
    public String specialisation;

    @JacksonXmlProperty(isAttribute = true, localName = "prof")
    public String profession;

    @JacksonXmlProperty(isAttribute = true, localName = "sign")
    public String sign;

    @JacksonXmlProperty(isAttribute = true, localName = "map")
    public int map;

    @JacksonXmlProperty(isAttribute = true, localName = "l")
    public int level;

    @JacksonXmlProperty(isAttribute = true, localName = "x")
    public int x;

    @JacksonXmlProperty(isAttribute = true, localName = "y")
    public int y;

    @JacksonXmlProperty(localName = "skills")
    public SkillsData skills;

    @JacksonXmlProperty(localName = "stats")
    public StatsData stats;

    @JacksonXmlProperty(localName = "money")
    public MoneyData money;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    public List<ItemReference> items = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "spell")
    public List<SpellReference> spells = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "feat")
    public List<FeatReference> feats = new ArrayList<>();
  }

  /** Skills data stored as XML attributes */
  public static class SkillsData {
    // Skills are stored as XML attributes - use @JacksonXmlProperty for each skill
    // All 38 skills from the Skill enum
    @JacksonXmlProperty(isAttribute = true, localName = "CREATION")
    public Float CREATION;

    @JacksonXmlProperty(isAttribute = true, localName = "DESTRUCTION")
    public Float DESTRUCTION;

    @JacksonXmlProperty(isAttribute = true, localName = "RESTORATION")
    public Float RESTORATION;

    @JacksonXmlProperty(isAttribute = true, localName = "ALTERATION")
    public Float ALTERATION;

    @JacksonXmlProperty(isAttribute = true, localName = "ILLUSION")
    public Float ILLUSION;

    @JacksonXmlProperty(isAttribute = true, localName = "ENCHANT")
    public Float ENCHANT;

    @JacksonXmlProperty(isAttribute = true, localName = "ALCHEMY")
    public Float ALCHEMY;

    @JacksonXmlProperty(isAttribute = true, localName = "CONJURATION")
    public Float CONJURATION;

    @JacksonXmlProperty(isAttribute = true, localName = "ARCHERY")
    public Float ARCHERY;

    @JacksonXmlProperty(isAttribute = true, localName = "AXE")
    public Float AXE;

    @JacksonXmlProperty(isAttribute = true, localName = "BLUNT")
    public Float BLUNT;

    @JacksonXmlProperty(isAttribute = true, localName = "BLADE")
    public Float BLADE;

    @JacksonXmlProperty(isAttribute = true, localName = "SPEAR")
    public Float SPEAR;

    @JacksonXmlProperty(isAttribute = true, localName = "UNARMED")
    public Float UNARMED;

    @JacksonXmlProperty(isAttribute = true, localName = "CLIMBING")
    public Float CLIMBING;

    @JacksonXmlProperty(isAttribute = true, localName = "SWIMMING")
    public Float SWIMMING;

    @JacksonXmlProperty(isAttribute = true, localName = "SNEAK")
    public Float SNEAK;

    @JacksonXmlProperty(isAttribute = true, localName = "HEAVY_ARMOR")
    public Float HEAVY_ARMOR;

    @JacksonXmlProperty(isAttribute = true, localName = "MEDIUM_ARMOR")
    public Float MEDIUM_ARMOR;

    @JacksonXmlProperty(isAttribute = true, localName = "LIGHT_ARMOR")
    public Float LIGHT_ARMOR;

    @JacksonXmlProperty(isAttribute = true, localName = "DODGING")
    public Float DODGING;

    @JacksonXmlProperty(isAttribute = true, localName = "BLOCK")
    public Float BLOCK;

    @JacksonXmlProperty(isAttribute = true, localName = "UNARMORED")
    public Float UNARMORED;

    @JacksonXmlProperty(isAttribute = true, localName = "MERCANTILE")
    public Float MERCANTILE;

    @JacksonXmlProperty(isAttribute = true, localName = "PICKPOCKET")
    public Float PICKPOCKET;

    @JacksonXmlProperty(isAttribute = true, localName = "ARMORER")
    public Float ARMORER;

    @JacksonXmlProperty(isAttribute = true, localName = "LOCKPICKING")
    public Float LOCKPICKING;

    @JacksonXmlProperty(isAttribute = true, localName = "MEDICAL")
    public Float MEDICAL;

    @JacksonXmlProperty(isAttribute = true, localName = "DISABLE")
    public Float DISABLE;

    @JacksonXmlProperty(isAttribute = true, localName = "SPEECHCRAFT")
    public Float SPEECHCRAFT;

    @JacksonXmlProperty(isAttribute = true, localName = "PERFORM")
    public Float PERFORM;

    @JacksonXmlProperty(isAttribute = true, localName = "DISGUISE")
    public Float DISGUISE;

    @JacksonXmlProperty(isAttribute = true, localName = "RIDING")
    public Float RIDING;

    @JacksonXmlProperty(isAttribute = true, localName = "NONE")
    public Float NONE;
  }

  /** Stats data */
  public static class StatsData {
    @JacksonXmlProperty(isAttribute = true, localName = "str")
    public int str;

    @JacksonXmlProperty(isAttribute = true, localName = "con")
    public int con;

    @JacksonXmlProperty(isAttribute = true, localName = "dex")
    public int dex;

    @JacksonXmlProperty(isAttribute = true, localName = "int")
    public int int_;

    @JacksonXmlProperty(isAttribute = true, localName = "wis")
    public int wis;

    @JacksonXmlProperty(isAttribute = true, localName = "cha")
    public int cha;
  }

  /** Money data */
  public static class MoneyData {
    @JacksonXmlText public int value;
  }

  /** Item reference */
  public static class ItemReference {
    @JacksonXmlProperty(isAttribute = true, localName = "uid")
    public long uid;
  }

  /** Spell reference */
  public static class SpellReference {
    @JacksonXmlText public String id;
  }

  /** Feat reference */
  public static class FeatReference {
    @JacksonXmlText public String name;
  }

  /** Journal data */
  public static class JournalData {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "quest")
    public List<QuestEntry> quests = new ArrayList<>();
  }

  /** Quest entry */
  public static class QuestEntry {
    @JacksonXmlProperty(isAttribute = true, localName = "id")
    public String id;

    @JacksonXmlProperty(isAttribute = true, localName = "stage")
    public int stage;

    @JacksonXmlText public String subject;
  }

  /** Events data */
  public static class EventsData {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "task")
    public List<TaskEvent> tasks = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "timer")
    public List<TimerEvent> timerEvents = new ArrayList<>();
  }

  /** Task event */
  public static class TaskEvent {
    @JacksonXmlProperty(isAttribute = true, localName = "desc")
    public String description;

    @JacksonXmlProperty(isAttribute = true, localName = "script")
    public String script;
  }

  /** Timer event */
  public static class TimerEvent {
    @JacksonXmlProperty(isAttribute = true, localName = "tick")
    public String tick; // Format: "start:period:stop"

    @JacksonXmlProperty(isAttribute = true, localName = "task")
    public String taskType; // "script" or "magic"

    @JacksonXmlProperty(isAttribute = true, localName = "script")
    public String script;

    // Magic task attributes
    @JacksonXmlProperty(isAttribute = true, localName = "effect")
    public String effect;

    @JacksonXmlProperty(isAttribute = true, localName = "target")
    public Long target;

    @JacksonXmlProperty(isAttribute = true, localName = "caster")
    public Long caster;

    @JacksonXmlProperty(isAttribute = true, localName = "stype")
    public String spellType;

    @JacksonXmlProperty(isAttribute = true, localName = "mag")
    public Float magnitude;
  }

  /** Timer data */
  public static class TimerData {
    @JacksonXmlProperty(isAttribute = true, localName = "ticks")
    public int ticks;
  }

  /** Quests data (for random quests - currently unused) */
  public static class QuestsData {
    // Empty for now, placeholder for future random quest saving
  }
}
