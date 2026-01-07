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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import neon.entities.property.Ability;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement(localName = "sign")
public class RSign extends RData {
  // Jackson-friendly representation (deserialized via setters)
  // (id, name inherited from parent with Jackson annotations)
  private List<Power> powerList = new ArrayList<>();
  private List<AbilityEntry> abilityList = new ArrayList<>();

  // Public fields for game code compatibility
  public ArrayList<String> powers = new ArrayList<>();
  public EnumMap<Ability, Integer> abilities = new EnumMap<>(Ability.class);

  /** Inner class for power XML element */
  public static class Power {
    @JacksonXmlProperty(isAttribute = true)
    public String id;

    public Power() {}

    public Power(String id) {
      this.id = id;
    }
  }

  /** Inner class for ability XML element */
  public static class AbilityEntry {
    @JacksonXmlProperty(isAttribute = true)
    public Ability id;

    @JacksonXmlProperty(isAttribute = true)
    public int size;

    public AbilityEntry() {}

    public AbilityEntry(Ability id, int size) {
      this.id = id;
      this.size = size;
    }
  }

  // No-arg constructor for Jackson deserialization
  public RSign() {
    super("unknown");
  }

  public RSign(String id, String... path) {
    super(id, path);
  }

  public RSign(RSign sign) {
    super(sign.id, sign.path);
    powers.addAll(sign.powers);
    abilities.putAll(sign.abilities);
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RSign(Element sign, String... path) {
    super(sign, path);
    for (Element power : sign.getChildren("power")) {
      powers.add(power.getAttributeValue("id"));
    }
    for (Element ability : sign.getChildren("ability")) {
      abilities.put(
          Ability.valueOf(ability.getAttributeValue("id").toUpperCase()),
          Integer.parseInt(ability.getAttributeValue("size")));
    }
  }

  /**
   * Sync powerList to powers field (called by Jackson after deserialization).
   *
   * @param powerList the deserialized power list
   */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "power")
  public void setPowerList(List<Power> powerList) {
    this.powerList = powerList;
    this.powers.clear();
    for (Power p : powerList) {
      this.powers.add(p.id);
    }
  }

  /**
   * Sync abilityList to abilities EnumMap (called by Jackson after deserialization).
   *
   * @param abilityList the deserialized ability list
   */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "ability")
  public void setAbilityList(List<AbilityEntry> abilityList) {
    this.abilityList = abilityList;
    this.abilities.clear();
    for (AbilityEntry e : abilityList) {
      this.abilities.put(e.id, e.size);
    }
  }

  /**
   * Creates a JDOM Element from this resource using Jackson serialization.
   *
   * @return JDOM Element representation
   */
  public Element toElement() {
    try {
      // Sync legacy fields to Jackson-friendly lists before serialization
      syncToJacksonLists();

      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(this).toString();
      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RSign to Element", e);
    }
  }

  /** Sync public fields to Jackson lists for serialization. */
  private void syncToJacksonLists() {
    powerList.clear();
    for (String powerId : powers) {
      powerList.add(new Power(powerId));
    }

    abilityList.clear();
    for (Map.Entry<Ability, Integer> entry : abilities.entrySet()) {
      if (entry.getValue() > 0) {
        abilityList.add(new AbilityEntry(entry.getKey(), entry.getValue()));
      }
    }
  }
}
