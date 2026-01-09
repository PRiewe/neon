/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import neon.entities.property.Habitat;
import neon.entities.property.Skill;
import neon.entities.property.Subtype;
import neon.resources.jackson.SkillMapDeserializer;
import neon.resources.jackson.SkillMapSerializer;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement // No localName - accepts any element name (humanoid, animal, etc.)
public class RCreature extends RData {
  public enum Size {
    tiny,
    small,
    medium,
    large,
    huge;
  }

  public enum Type {
    animal,
    construct,
    daemon,
    dragon,
    goblin,
    humanoid,
    monster,
    player;
  }

  public enum AIType {
    wander,
    guard,
    schedule;
  }

  // Jackson annotations for fields (id, text, color, name inherited from parent)
  @JacksonXmlProperty(isAttribute = true)
  public String hit;

  @JacksonXmlProperty(isAttribute = true)
  public int speed;

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public int mana;

  @JacksonXmlProperty(isAttribute = true)
  public Size size = Size.medium;

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public Habitat habitat = Habitat.LAND;

  // Nested elements (deserialized via setters to sync with public fields)
  private Stats statsObj;
  private AIConfig aiObj;
  private AVElement avElement;
  private DVElement dvElement;

  // Public fields for game code compatibility
  public String av;
  public int dv;

  @JsonSerialize(using = SkillMapSerializer.class)
  public final EnumMap<Skill, Float> skills;

  // Public fields for game code compatibility
  public float str, dex, con, iq, wis, cha;
  public AIType aiType = AIType.guard;
  public int aiRange = 10, aiConf = 0, aiAggr = 0;
  public final ArrayList<Subtype> subtypes;
  public Type type = Type.animal; // Set externally based on element name

  /** Inner class for stats XML element */
  public static class Stats implements Serializable {
    @JacksonXmlProperty(isAttribute = true)
    public float str;

    @JacksonXmlProperty(isAttribute = true)
    public float con;

    @JacksonXmlProperty(isAttribute = true)
    public float dex;

    @JacksonXmlProperty(isAttribute = true, localName = "int")
    public float iq; // "int" is reserved keyword

    @JacksonXmlProperty(isAttribute = true)
    public float wis;

    @JacksonXmlProperty(isAttribute = true)
    public float cha;

    public Stats() {}
  }

  /** Inner class for AI configuration */
  public static class AIConfig implements Serializable {
    @JacksonXmlText public AIType aiType = AIType.guard;

    @JacksonXmlProperty(isAttribute = true, localName = "r")
    @JsonProperty(required = false)
    public int aiRange = 10;

    @JacksonXmlProperty(isAttribute = true, localName = "a")
    @JsonProperty(required = false)
    public int aiAggr = 0;

    @JacksonXmlProperty(isAttribute = true, localName = "c")
    @JsonProperty(required = false)
    public int aiConf = 0;

    public AIConfig() {}
  }

  /** Inner class for AV (armor value) XML element */
  public static class AVElement implements Serializable {
    @JacksonXmlText public String value;

    public AVElement() {}
  }

  /** Inner class for DV (defense value) XML element */
  public static class DVElement implements Serializable {
    @JacksonXmlText public Integer value;

    public DVElement() {}
  }

  /**
   * Sync Stats object to individual public fields (called by Jackson after deserialization).
   *
   * @param stats the deserialized stats object
   */
  @JacksonXmlProperty(localName = "stats")
  public void setStats(Stats stats) {
    this.statsObj = stats;
    this.str = stats.str;
    this.con = stats.con;
    this.dex = stats.dex;
    this.iq = stats.iq;
    this.wis = stats.wis;
    this.cha = stats.cha;
  }

  /**
   * Get Stats object for serialization (creates from public fields).
   *
   * @return stats object
   */
  public Stats getStats() {
    Stats s = new Stats();
    s.str = this.str;
    s.con = this.con;
    s.dex = this.dex;
    s.iq = this.iq;
    s.wis = this.wis;
    s.cha = this.cha;
    return s;
  }

  /**
   * Sync AIConfig object to individual public fields (called by Jackson after deserialization).
   *
   * @param ai the deserialized AI config
   */
  @JacksonXmlProperty(localName = "ai")
  public void setAi(AIConfig ai) {
    this.aiObj = ai;
    this.aiType = ai.aiType;
    this.aiRange = ai.aiRange;
    this.aiAggr = ai.aiAggr;
    this.aiConf = ai.aiConf;
  }

  /**
   * Sync skills map (called by Jackson after deserialization).
   *
   * @param skillsMap the deserialized skills map
   */
  @JacksonXmlProperty(localName = "skills")
  public void setSkills(
      @JsonDeserialize(using = SkillMapDeserializer.class) EnumMap<Skill, Float> skillsMap) {
    if (skillsMap != null) {
      this.skills.putAll(skillsMap);
    }
  }

  /**
   * Get skills for serialization (only non-zero values).
   *
   * @return skills map
   */
  public EnumMap<Skill, Float> getSkills() {
    return skills;
  }

  /**
   * Get AIConfig object for serialization (creates from public fields).
   *
   * @return AI config object, or null if all defaults
   */
  public AIConfig getAi() {
    if (aiAggr == 0 && aiConf == 0 && aiRange == 10 && aiType == AIType.guard) {
      return null; // All defaults, don't serialize
    }
    AIConfig ai = new AIConfig();
    ai.aiType = this.aiType;
    ai.aiRange = this.aiRange;
    ai.aiAggr = this.aiAggr;
    ai.aiConf = this.aiConf;
    return ai;
  }

  /**
   * Sync AV element to public field (called by Jackson after deserialization).
   *
   * @param avElement the deserialized av element
   */
  @JacksonXmlProperty(localName = "av")
  public void setAv(AVElement avElement) {
    this.avElement = avElement;
    this.av = (avElement != null && avElement.value != null) ? avElement.value : "1d1";
  }

  /**
   * Sync DV element to public field (called by Jackson after deserialization).
   *
   * @param dvElement the deserialized dv element
   */
  @JacksonXmlProperty(localName = "dv")
  public void setDv(DVElement dvElement) {
    this.dvElement = dvElement;
    this.dv = (dvElement != null && dvElement.value != null) ? dvElement.value : 0;
  }

  /**
   * Get AV element for serialization.
   *
   * @return av element
   */
  public AVElement getAv() {
    AVElement elem = new AVElement();
    elem.value = av;
    return elem;
  }

  /**
   * Get DV element for serialization.
   *
   * @return dv element or null if 0
   */
  public DVElement getDv() {
    if (dv > 0) {
      DVElement elem = new DVElement();
      elem.value = dv;
      return elem;
    }
    return null;
  }

  // No-arg constructor for Jackson deserialization
  public RCreature() {
    super("unknown");
    subtypes = new ArrayList<>();
    skills = new EnumMap<>(Skill.class);
    // Initialize all skills to 0.0f
    for (Skill skill : Skill.values()) {
      skills.put(skill, 0f);
    }
  }

  public RCreature(String id, String... path) {
    super(id, path);
    subtypes = new ArrayList<Subtype>();
    skills = new EnumMap<Skill, Float>(Skill.class);
    hit = "1d1";
    av = "1d1";
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RCreature(Element properties, String... path) {
    super(properties, path);
    subtypes = new ArrayList<Subtype>();
    skills = initSkills(properties.getChild("skills"));

    color = properties.getAttributeValue("color");
    hit = properties.getAttributeValue("hit");
    av = properties.getChild("av").getText();
    text = properties.getAttributeValue("char");

    size = Size.valueOf(properties.getAttributeValue("size"));
    type = Type.valueOf(properties.getName());

    str = Integer.parseInt(properties.getChild("stats").getAttributeValue("str"));
    con = Integer.parseInt(properties.getChild("stats").getAttributeValue("con"));
    dex = Integer.parseInt(properties.getChild("stats").getAttributeValue("dex"));
    iq = Integer.parseInt(properties.getChild("stats").getAttributeValue("int"));
    wis = Integer.parseInt(properties.getChild("stats").getAttributeValue("wis"));
    cha = Integer.parseInt(properties.getChild("stats").getAttributeValue("cha"));

    speed = Integer.parseInt(properties.getAttributeValue("speed"));
    if (properties.getAttributeValue("mana") != null) { // not always present
      mana = Integer.parseInt(properties.getAttributeValue("mana"));
    }
    if (properties.getChild("dv") != null) { // not always present
      dv = Integer.parseInt(properties.getChild("dv").getText());
    }

    if (properties.getAttribute("habitat") != null) {
      habitat = Habitat.valueOf(properties.getAttributeValue("habitat").toUpperCase());
    }

    Element brain = properties.getChild("ai");
    if (brain != null) {
      if (!brain.getText().isEmpty()) {
        aiType = AIType.valueOf(brain.getText());
      }
      if (brain.getAttributeValue("r") != null) {
        aiRange = Integer.parseInt(brain.getAttributeValue("r"));
      }
      if (brain.getAttributeValue("a") != null) {
        aiAggr = Integer.parseInt(brain.getAttributeValue("a"));
      }
      if (brain.getAttributeValue("c") != null) {
        aiConf = Integer.parseInt(brain.getAttributeValue("c"));
      }
    }
  }

  public String getName() {
    return name != null ? name : id;
  }

  private static EnumMap<Skill, Float> initSkills(Element skills) {
    EnumMap<Skill, Float> list = new EnumMap<Skill, Float>(Skill.class);
    for (Skill skill : Skill.values()) {
      if (skills != null && skills.getAttribute(skill.toString().toLowerCase()) != null) {
        list.put(skill, Float.parseFloat(skills.getAttributeValue(skill.toString().toLowerCase())));
      } else {
        list.put(skill, 0f);
      }
    }
    return list;
  }

  /**
   * Creates a JDOM Element from this resource using Jackson serialization.
   *
   * @return JDOM Element representation
   */
  @Override
  public Element toElement() {
    try {
      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(this).toString();
      Element element =
          new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();

      // Fix root element name to match type (Jackson uses generic name)
      element.setName(type.toString());

      return element;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RCreature to Element", e);
    }
  }
}
