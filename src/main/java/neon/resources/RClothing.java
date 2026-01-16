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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import neon.entities.property.Slot;
import neon.magic.Effect;
import org.jdom2.Element;

public class RClothing extends RItem implements Serializable {
  public enum ArmorType {
    LIGHT,
    MEDIUM,
    HEAVY,
    NONE
  }

  // Nested elements (deserialized via setters to sync with public fields)
  @com.fasterxml.jackson.annotation.JsonIgnore private StatsElement statsElement;

  @com.fasterxml.jackson.annotation.JsonIgnore private EnchantElement enchantElement;

  // Public fields for game code compatibility - marked JsonIgnore as they're serialized via nested
  // elements
  @com.fasterxml.jackson.annotation.JsonIgnore public ArmorType kind;

  @com.fasterxml.jackson.annotation.JsonIgnore public int rating;

  @com.fasterxml.jackson.annotation.JsonIgnore public Slot slot;

  // enchantment - marked JsonIgnore as they're serialized via nested element
  @com.fasterxml.jackson.annotation.JsonIgnore public int magnitude;

  @com.fasterxml.jackson.annotation.JsonIgnore public int mana;

  @com.fasterxml.jackson.annotation.JsonIgnore public Effect effect;

  /** Inner class for stats XML element */
  public static class StatsElement implements Serializable {
    @JacksonXmlProperty(isAttribute = true)
    public Slot slot;

    @JacksonXmlProperty(isAttribute = true, localName = "ar")
    @JsonProperty(required = false)
    public Integer rating;

    @JacksonXmlProperty(isAttribute = true, localName = "class")
    @JsonProperty(required = false)
    public ArmorType armorClass;

    public StatsElement() {}
  }

  /** Inner class for enchant XML element */
  public static class EnchantElement implements Serializable {
    @JacksonXmlProperty(isAttribute = true, localName = "mag")
    public int magnitude;

    @JacksonXmlProperty(isAttribute = true)
    public int mana;

    @JacksonXmlProperty(isAttribute = true)
    public Effect effect;

    public EnchantElement() {}
  }

  // No-arg constructor for Jackson deserialization
  public RClothing() {
    super();
    this.type = Type.clothing; // Default type (can also be armor)
  }

  public RClothing(String id, Type type, String... path) {
    super(id, type, path);
  }

  /**
   * Sync stats element to public fields (called by Jackson after deserialization).
   *
   * @param stats the deserialized stats element
   */
  @JacksonXmlProperty(localName = "stats")
  public void setStats(StatsElement stats) {
    this.statsElement = stats;
    this.slot = stats.slot;
    if (stats.rating != null) {
      this.rating = stats.rating;
      this.kind = stats.armorClass;
    } else {
      this.rating = 0;
      this.kind = ArmorType.NONE;
    }
  }

  /**
   * Get stats element for serialization.
   *
   * @return stats element
   */
  @com.fasterxml.jackson.annotation.JsonGetter("stats")
  public StatsElement getStats() {
    StatsElement stats = new StatsElement();
    stats.slot = slot;
    if (type == Type.armor) {
      stats.armorClass = kind;
      stats.rating = rating;
    }
    return stats;
  }

  /**
   * Sync enchant element to public fields (called by Jackson after deserialization).
   *
   * @param enchant the deserialized enchant element
   */
  @JacksonXmlProperty(localName = "enchant")
  @JsonProperty(required = false)
  public void setEnchant(EnchantElement enchant) {
    this.enchantElement = enchant;
    if (enchant != null) {
      this.magnitude = enchant.magnitude;
      this.mana = enchant.mana;
      this.effect = enchant.effect;
    } else {
      this.magnitude = 0;
      this.mana = 0;
      this.effect = null;
    }
  }

  /**
   * Get enchant element for serialization.
   *
   * @return enchant element or null
   */
  @com.fasterxml.jackson.annotation.JsonGetter("enchant")
  public EnchantElement getEnchant() {
    if (magnitude > 0 || mana > 0 || effect != null) {
      EnchantElement enchant = new EnchantElement();
      enchant.magnitude = magnitude;
      enchant.mana = mana;
      enchant.effect = effect;
      return enchant;
    }
    return null;
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RClothing(Element cloth, String... path) {
    super(cloth, path);
    Element stats = cloth.getChild("stats");
    slot = Slot.valueOf(stats.getAttributeValue("slot").toUpperCase());

    if (cloth.getName().equals("armor")) {
      rating = Integer.parseInt(stats.getAttributeValue("ar"));
      kind = ArmorType.valueOf(stats.getAttributeValue("class").toUpperCase());
    } else {
      rating = 0;
      kind = ArmorType.NONE;
    }

    if (cloth.getChild("enchant") != null) {
      Element enchantment = cloth.getChild("enchant");
      magnitude = Integer.parseInt(enchantment.getAttributeValue("mag"));
      mana = Integer.parseInt(enchantment.getAttributeValue("mana"));
      effect = Effect.valueOf(enchantment.getAttributeValue("effect").toUpperCase());
    } else {
      magnitude = 0;
      mana = 0;
      effect = null;
    }
  }

  @Override
  public Element toElement() {
    Element clothing = super.toElement();
    Element stats = new Element("stats");
    stats.setAttribute("slot", slot.toString());
    if (type == Type.armor) {
      stats.setAttribute("class", kind.toString());
      stats.setAttribute("ar", Integer.toString(rating));
    }
    clothing.addContent(stats);
    return clothing;
  }
}
