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

package neon.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.ByteArrayInputStream;
import java.util.*;
import neon.entities.property.Skill;
import neon.resources.RCreature.AIType;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement(localName = "npc")
public class RPerson extends RData {
  @JsonIgnore public HashMap<String, Integer> factions = new HashMap<String, Integer>();
  public AIType aiType;
  public int aiRange = -1, aiConf = -1, aiAggr = -1;
  @JsonIgnore public HashMap<Skill, Integer> skills = new HashMap<Skill, Integer>();
  @JsonIgnore public HashSet<String> spells = new HashSet<String>();
  @JsonIgnore public ArrayList<String> items = new ArrayList<String>();
  @JsonIgnore public ArrayList<String> scripts = new ArrayList<String>();
  @JsonIgnore public ArrayList<Service> services = new ArrayList<Service>();

  @JacksonXmlProperty(isAttribute = true, localName = "race")
  public String species;

  /** Inner class for faction entries */
  public static class FactionEntry {
    @JacksonXmlProperty(isAttribute = true)
    public String id;

    @JacksonXmlProperty(isAttribute = true)
    public int rank;
  }

  /** Inner class for skill entries */
  public static class SkillEntry {
    @JacksonXmlProperty(isAttribute = true)
    public String id;

    @JacksonXmlProperty(isAttribute = true)
    public int rank;
  }

  /** Inner class for item entries */
  public static class ItemEntry {
    @JacksonXmlProperty(isAttribute = true)
    public String id;
  }

  /** Inner class for spell entries */
  public static class SpellEntry {
    @JacksonXmlProperty(isAttribute = true)
    public String id;
  }

  /** Inner class for AI configuration */
  @JacksonXmlRootElement(localName = "ai")
  public static class AI {
    @JacksonXmlProperty(isAttribute = true, localName = "r")
    public Integer r; // range

    @JacksonXmlProperty(isAttribute = true, localName = "a")
    public Integer a; // aggression

    @JacksonXmlProperty(isAttribute = true, localName = "c")
    public Integer c; // confidence

    @JacksonXmlText public String type; // AI type
  }

  /** Inner class for service definitions */
  @JacksonXmlRootElement(localName = "service")
  public static class Service {
    @JacksonXmlProperty(isAttribute = true)
    public String id;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "skill")
    public List<String> skills = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "dest")
    public List<Destination> destinations = new ArrayList<>();

    /** Inner class for travel destinations */
    public static class Destination {
      @JacksonXmlProperty(isAttribute = true)
      public int x;

      @JacksonXmlProperty(isAttribute = true)
      public int y;

      @JacksonXmlProperty(isAttribute = true)
      public String name;

      @JacksonXmlProperty(isAttribute = true)
      public int cost;
    }
  }

  // No-arg constructor for Jackson deserialization
  public RPerson() {
    super("unknown");
  }

  public RPerson(String id, String... path) {
    super(id, path);
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RPerson(Element person, String... path) {
    super(person.getAttributeValue("id"), path);
    name = person.getAttributeValue("name");
    species = person.getAttributeValue("race");

    if (person.getChild("factions") != null) {
      for (Element f : person.getChild("factions").getChildren()) {
        int rank =
            f.getAttribute("rank") != null ? Integer.parseInt(f.getAttributeValue("rank")) : 0;
        factions.put(f.getAttributeValue("id"), rank);
      }
    }

    Element brain = person.getChild("ai");
    if (brain != null && !brain.getText().isEmpty()) {
      aiType = AIType.valueOf(brain.getText());
    } else {
      aiType = null;
    }
    if (brain != null && brain.getAttribute("r") != null) {
      aiRange = Integer.parseInt(brain.getAttributeValue("r"));
    } else {
      aiRange = -1;
    }
    if (brain != null && brain.getAttribute("a") != null) {
      aiAggr = Integer.parseInt(brain.getAttributeValue("a"));
    } else {
      aiAggr = -1;
    }
    if (brain != null && brain.getAttribute("c") != null) {
      aiConf = Integer.parseInt(brain.getAttributeValue("c"));
    } else {
      aiConf = -1;
    }

    Element skillList = person.getChild("skills");
    if (skillList != null) {
      for (Element skill : skillList.getChildren()) {
        skills.put(
            Skill.valueOf(skill.getAttributeValue("id").toUpperCase()),
            Integer.parseInt(skill.getAttributeValue("rank")));
      }
    }

    Element itemList = person.getChild("items");
    if (itemList != null) {
      for (Element item : itemList.getChildren()) {
        items.add(item.getAttributeValue("id"));
      }
    }

    Element spellList = person.getChild("spells");
    if (spellList != null) {
      for (Element spell : spellList.getChildren()) {
        spells.add(spell.getAttributeValue("id"));
      }
    }

    // Parse services into Service objects
    for (Element serviceEl : person.getChildren("service")) {
      Service service = new Service();
      service.id = serviceEl.getAttributeValue("id");

      // Training service - has skill children
      for (Element skillEl : serviceEl.getChildren("skill")) {
        service.skills.add(skillEl.getText());
      }

      // Travel service - has dest children
      for (Element destEl : serviceEl.getChildren("dest")) {
        Service.Destination dest = new Service.Destination();
        dest.x = Integer.parseInt(destEl.getAttributeValue("x"));
        dest.y = Integer.parseInt(destEl.getAttributeValue("y"));
        dest.name = destEl.getAttributeValue("name");
        dest.cost = Integer.parseInt(destEl.getAttributeValue("cost"));
        service.destinations.add(dest);
      }

      services.add(service);
    }

    for (Element script : person.getChildren("script")) {
      scripts.add(script.getText());
    }
  }

  /** Jackson setter for factions - converts list to HashMap */
  @JacksonXmlElementWrapper(localName = "factions")
  @JacksonXmlProperty(localName = "faction")
  public void setFactionList(List<FactionEntry> factionList) {
    if (factionList != null) {
      for (FactionEntry entry : factionList) {
        factions.put(entry.id, entry.rank);
      }
    }
  }

  /** Jackson getter for factions - converts HashMap to list */
  @JacksonXmlElementWrapper(localName = "factions")
  @JacksonXmlProperty(localName = "faction")
  public List<FactionEntry> getFactionList() {
    List<FactionEntry> list = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : factions.entrySet()) {
      FactionEntry fe = new FactionEntry();
      fe.id = entry.getKey();
      fe.rank = entry.getValue();
      list.add(fe);
    }
    return list;
  }

  /** Jackson setter for skills - converts list to HashMap */
  @JacksonXmlElementWrapper(localName = "skills")
  @JacksonXmlProperty(localName = "skill")
  public void setSkillList(List<SkillEntry> skillList) {
    if (skillList != null) {
      for (SkillEntry entry : skillList) {
        skills.put(Skill.valueOf(entry.id.toUpperCase()), entry.rank);
      }
    }
  }

  /** Jackson getter for skills - converts HashMap to list */
  @JacksonXmlElementWrapper(localName = "skills")
  @JacksonXmlProperty(localName = "skill")
  public List<SkillEntry> getSkillList() {
    List<SkillEntry> list = new ArrayList<>();
    for (Map.Entry<Skill, Integer> entry : skills.entrySet()) {
      SkillEntry se = new SkillEntry();
      se.id = entry.getKey().toString();
      se.rank = entry.getValue();
      list.add(se);
    }
    return list;
  }

  /** Jackson setter for items - converts list to ArrayList */
  @JacksonXmlElementWrapper(localName = "items")
  @JacksonXmlProperty(localName = "item")
  public void setItemList(List<ItemEntry> itemList) {
    if (itemList != null) {
      for (ItemEntry entry : itemList) {
        items.add(entry.id);
      }
    }
  }

  /** Jackson getter for items - converts ArrayList to list */
  @JacksonXmlElementWrapper(localName = "items")
  @JacksonXmlProperty(localName = "item")
  public List<ItemEntry> getItemList() {
    List<ItemEntry> list = new ArrayList<>();
    for (String item : items) {
      ItemEntry ie = new ItemEntry();
      ie.id = item;
      list.add(ie);
    }
    return list;
  }

  /** Jackson setter for spells - converts list to HashSet */
  @JacksonXmlElementWrapper(localName = "spells")
  @JacksonXmlProperty(localName = "spell")
  public void setSpellList(List<SpellEntry> spellList) {
    if (spellList != null) {
      for (SpellEntry entry : spellList) {
        spells.add(entry.id);
      }
    }
  }

  /** Jackson getter for spells - converts HashSet to list */
  @JacksonXmlElementWrapper(localName = "spells")
  @JacksonXmlProperty(localName = "spell")
  public List<SpellEntry> getSpellList() {
    List<SpellEntry> list = new ArrayList<>();
    for (String spell : spells) {
      SpellEntry se = new SpellEntry();
      se.id = spell;
      list.add(se);
    }
    return list;
  }

  /** Jackson setter for AI configuration */
  @JacksonXmlProperty(localName = "ai")
  public void setAI(AI ai) {
    if (ai != null) {
      if (ai.type != null && !ai.type.isEmpty()) {
        aiType = AIType.valueOf(ai.type);
      }
      aiRange = (ai.r != null) ? ai.r : -1;
      aiAggr = (ai.a != null) ? ai.a : -1;
      aiConf = (ai.c != null) ? ai.c : -1;
    }
  }

  /** Jackson getter for AI configuration */
  @JacksonXmlProperty(localName = "ai")
  public AI getAI() {
    if (aiType == null && aiRange == -1 && aiAggr == -1 && aiConf == -1) {
      return null;
    }
    AI ai = new AI();
    ai.type = (aiType != null) ? aiType.toString() : null;
    ai.r = (aiRange != -1) ? aiRange : null;
    ai.a = (aiAggr != -1) ? aiAggr : null;
    ai.c = (aiConf != -1) ? aiConf : null;
    return ai;
  }

  /** Jackson setter for services */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "service")
  public void setServices(List<Service> services) {
    this.services = new ArrayList<>(services);
  }

  /** Jackson getter for services */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "service")
  public List<Service> getServices() {
    return services;
  }

  /** Jackson setter for scripts */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "script")
  public void setScripts(List<String> scripts) {
    this.scripts = new ArrayList<>(scripts);
  }

  /** Jackson getter for scripts */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "script")
  public List<String> getScripts() {
    return scripts;
  }

  /**
   * Creates a JDOM Element from this resource using Jackson serialization.
   *
   * @return JDOM Element representation
   */
  public Element toElement() {
    try {
      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(this).toString();
      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RPerson to Element", e);
    }
  }
}
