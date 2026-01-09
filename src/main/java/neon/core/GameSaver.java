/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
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

package neon.core;

import com.google.common.collect.Multimap;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import neon.core.event.MagicTask;
import neon.core.event.SaveEvent;
import neon.core.event.ScriptAction;
import neon.core.event.TaskQueue;
import neon.core.model.SaveGameModel;
import neon.entities.Player;
import neon.entities.property.Feat;
import neon.entities.property.Skill;
import neon.magic.Spell;
import neon.maps.Atlas;
import neon.resources.RSpell;
import neon.systems.files.JacksonMapper;
import neon.systems.files.StringTranslator;
import neon.util.fsm.Action;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

@Listener(references = References.Strong)
public class GameSaver {
  private TaskQueue queue;

  public GameSaver(TaskQueue queue) {
    this.queue = queue;
  }

  /** Saves the current game. */
  @Handler
  public void saveGame(SaveEvent se) {
    Player player = Engine.getPlayer();

    // Build save game model
    SaveGameModel save = new SaveGameModel();
    save.version = "2.0";
    save.player = buildPlayerData(player);
    save.journal = buildJournalData(player);
    save.events = buildEventsData();
    save.timer = new SaveGameModel.TimerData();
    save.timer.ticks = Engine.getTimer().getTime();
    save.quests = null; // No random quests yet

    // Ensure directories exist
    File saves = new File("saves");
    if (!saves.exists()) {
      saves.mkdir();
    }

    File dir = new File("saves/" + player.getName());
    if (!dir.exists()) {
      dir.mkdir();
    }

    // first copy everything from temp to save, to ensure savedoc is not overwritten
    Engine.getAtlas().getCache().commit();
    Engine.getStore().getCache().commit();
    Engine.getFileSystem().storeTemp(dir);

    // Serialize with Jackson
    try {
      JacksonMapper mapper = new JacksonMapper();
      ByteArrayOutputStream out = mapper.toXml(save);
      String xml = out.toString("UTF-8");
      Engine.getFileSystem()
          .saveFile(xml, new StringTranslator(), "saves", player.getName(), "save.xml");
    } catch (Exception e) {
      throw new RuntimeException("Failed to save game", e);
    }
  }

  private SaveGameModel.EventsData buildEventsData() {
    SaveGameModel.EventsData events = new SaveGameModel.EventsData();

    // all normal tasks (for now only script tasks)
    Multimap<String, Action> tasks = queue.getTasks();
    for (String key : tasks.keySet()) {
      for (Action action : tasks.get(key)) {
        if (action instanceof ScriptAction) {
          SaveGameModel.TaskEvent event = new SaveGameModel.TaskEvent();
          event.description = key;
          event.script = ((ScriptAction) action).getScript();
          events.tasks.add(event);
        }
      }
    }

    // all timer tasks
    Multimap<Integer, TaskQueue.RepeatEntry> repeats = queue.getTimerTasks();
    for (Integer key : repeats.keySet()) {
      for (TaskQueue.RepeatEntry entry : repeats.get(key)) {
        SaveGameModel.TimerEvent event = new SaveGameModel.TimerEvent();
        event.tick = key + ":" + entry.getPeriod() + ":" + entry.getStop();

        if (entry.getScript() != null) {
          event.taskType = "script";
          event.script = entry.getScript();
        } else if (entry.getTask() instanceof MagicTask) {
          event.taskType = "magic";
          Spell spell = ((MagicTask) entry.getTask()).getSpell();
          event.effect = spell.getEffect().name();
          if (spell.getTarget() != null) {
            event.target = spell.getTarget().getUID();
          }
          if (spell.getCaster() != null) {
            event.caster = spell.getCaster().getUID();
          }
          if (spell.getScript() != null) {
            event.script = spell.getScript();
          }
          event.spellType = spell.getType().name();
          event.magnitude = spell.getMagnitude();
        }

        events.timerEvents.add(event);
      }
    }

    return events;
  }

  private SaveGameModel.PlayerSaveData buildPlayerData(Player player) {
    SaveGameModel.PlayerSaveData data = new SaveGameModel.PlayerSaveData();

    // Basic attributes
    data.name = player.getName();
    data.race = player.species.id;
    data.gender = player.getGender().toString().toLowerCase();
    data.specialisation = player.getSpecialisation().toString();
    data.profession = player.getProfession();
    data.sign = player.getSign();

    // Position
    Atlas atlas = Engine.getAtlas();
    data.map = atlas.getCurrentMap().getUID();
    data.level = atlas.getCurrentZoneIndex();
    Rectangle bounds = player.getShapeComponent();
    data.x = bounds.x;
    data.y = bounds.y;

    // Skills
    data.skills = new SaveGameModel.SkillsData();
    for (Skill s : Skill.values()) {
      float skillValue = player.getSkill(s);
      setSkillValue(data.skills, s, skillValue);
    }

    // Stats
    data.stats = new SaveGameModel.StatsData();
    data.stats.str = player.getStatsComponent().getStr();
    data.stats.con = player.getStatsComponent().getCon();
    data.stats.dex = player.getStatsComponent().getDex();
    data.stats.int_ = player.getStatsComponent().getInt();
    data.stats.wis = player.getStatsComponent().getWis();
    data.stats.cha = player.getStatsComponent().getCha();

    // Money
    data.money = new SaveGameModel.MoneyData();
    data.money.value = player.getInventoryComponent().getMoney();

    // Items
    for (long uid : player.getInventoryComponent()) {
      SaveGameModel.ItemReference item = new SaveGameModel.ItemReference();
      item.uid = uid;
      data.items.add(item);
    }

    // Spells and powers
    for (RSpell s : player.getMagicComponent().getSpells()) {
      SaveGameModel.SpellReference spell = new SaveGameModel.SpellReference();
      spell.id = s.id;
      data.spells.add(spell);
    }

    for (RSpell p : player.getMagicComponent().getPowers()) {
      SaveGameModel.SpellReference spell = new SaveGameModel.SpellReference();
      spell.id = p.id;
      data.spells.add(spell);
    }

    // Feats
    for (Feat f : player.getCharacteristicsComponent().getFeats()) {
      SaveGameModel.FeatReference feat = new SaveGameModel.FeatReference();
      feat.name = f.toString();
      data.feats.add(feat);
    }

    return data;
  }

  private void setSkillValue(SaveGameModel.SkillsData skills, Skill skill, float value) {
    switch (skill) {
      case CREATION:
        skills.CREATION = value;
        break;
      case DESTRUCTION:
        skills.DESTRUCTION = value;
        break;
      case RESTORATION:
        skills.RESTORATION = value;
        break;
      case ALTERATION:
        skills.ALTERATION = value;
        break;
      case ILLUSION:
        skills.ILLUSION = value;
        break;
      case ENCHANT:
        skills.ENCHANT = value;
        break;
      case ALCHEMY:
        skills.ALCHEMY = value;
        break;
      case CONJURATION:
        skills.CONJURATION = value;
        break;
      case ARCHERY:
        skills.ARCHERY = value;
        break;
      case AXE:
        skills.AXE = value;
        break;
      case BLUNT:
        skills.BLUNT = value;
        break;
      case BLADE:
        skills.BLADE = value;
        break;
      case SPEAR:
        skills.SPEAR = value;
        break;
      case UNARMED:
        skills.UNARMED = value;
        break;
      case CLIMBING:
        skills.CLIMBING = value;
        break;
      case SWIMMING:
        skills.SWIMMING = value;
        break;
      case SNEAK:
        skills.SNEAK = value;
        break;
      case HEAVY_ARMOR:
        skills.HEAVY_ARMOR = value;
        break;
      case MEDIUM_ARMOR:
        skills.MEDIUM_ARMOR = value;
        break;
      case LIGHT_ARMOR:
        skills.LIGHT_ARMOR = value;
        break;
      case DODGING:
        skills.DODGING = value;
        break;
      case BLOCK:
        skills.BLOCK = value;
        break;
      case UNARMORED:
        skills.UNARMORED = value;
        break;
      case MERCANTILE:
        skills.MERCANTILE = value;
        break;
      case PICKPOCKET:
        skills.PICKPOCKET = value;
        break;
      case ARMORER:
        skills.ARMORER = value;
        break;
      case LOCKPICKING:
        skills.LOCKPICKING = value;
        break;
      case MEDICAL:
        skills.MEDICAL = value;
        break;
      case DISABLE:
        skills.DISABLE = value;
        break;
      case SPEECHCRAFT:
        skills.SPEECHCRAFT = value;
        break;
      case PERFORM:
        skills.PERFORM = value;
        break;
      case DISGUISE:
        skills.DISGUISE = value;
        break;
      case RIDING:
        skills.RIDING = value;
        break;
      case NONE:
        skills.NONE = value;
        break;
    }
  }

  private SaveGameModel.JournalData buildJournalData(Player player) {
    SaveGameModel.JournalData journal = new SaveGameModel.JournalData();

    for (String q : player.getJournal().getQuests().keySet()) {
      SaveGameModel.QuestEntry quest = new SaveGameModel.QuestEntry();
      quest.id = q;
      quest.stage = player.getJournal().getQuests().get(q);
      quest.subject = player.getJournal().getSubjects().get(q);
      journal.quests.add(quest);
    }

    return journal;
  }
}
