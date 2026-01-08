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

  /** Skills data with dynamic attributes */
  public static class SkillsData {
    // Skills are stored as XML attributes on the <skills> element
    // Jackson will preserve all attributes as a map
    @JacksonXmlProperty(isAttribute = true, localName = "blade")
    public Float blade;

    @JacksonXmlProperty(isAttribute = true, localName = "blunt")
    public Float blunt;

    @JacksonXmlProperty(isAttribute = true, localName = "axe")
    public Float axe;

    @JacksonXmlProperty(isAttribute = true, localName = "spear")
    public Float spear;

    @JacksonXmlProperty(isAttribute = true, localName = "bow")
    public Float bow;

    @JacksonXmlProperty(isAttribute = true, localName = "thrown")
    public Float thrown;

    @JacksonXmlProperty(isAttribute = true, localName = "athletics")
    public Float athletics;

    @JacksonXmlProperty(isAttribute = true, localName = "security")
    public Float security;

    @JacksonXmlProperty(isAttribute = true, localName = "sneak")
    public Float sneak;

    @JacksonXmlProperty(isAttribute = true, localName = "light_armor")
    public Float light_armor;

    @JacksonXmlProperty(isAttribute = true, localName = "medium_armor")
    public Float medium_armor;

    @JacksonXmlProperty(isAttribute = true, localName = "heavy_armor")
    public Float heavy_armor;

    @JacksonXmlProperty(isAttribute = true, localName = "alteration")
    public Float alteration;

    @JacksonXmlProperty(isAttribute = true, localName = "conjuration")
    public Float conjuration;

    @JacksonXmlProperty(isAttribute = true, localName = "destruction")
    public Float destruction;

    @JacksonXmlProperty(isAttribute = true, localName = "illusion")
    public Float illusion;

    @JacksonXmlProperty(isAttribute = true, localName = "restoration")
    public Float restoration;

    @JacksonXmlProperty(isAttribute = true, localName = "alchemy")
    public Float alchemy;

    @JacksonXmlProperty(isAttribute = true, localName = "enchanting")
    public Float enchanting;
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
