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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.ByteArrayInputStream;
import neon.magic.Effect;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement // Accepts any element name (spell, disease, poison, etc.)
public class RSpell extends RData {
  public enum SpellType {
    SPELL,
    DISEASE,
    POISON,
    CURSE,
    POWER,
    ENCHANT
  }

  public SpellType type; // Set externally based on element name

  @JacksonXmlProperty(isAttribute = true)
  public Effect effect;

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public int size;

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public int range;

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public int duration;

  @JacksonXmlProperty(isAttribute = true, localName = "area")
  @JsonProperty(required = false)
  public int radius;

  @JacksonXmlProperty(isAttribute = true)
  public int cost;

  @JacksonXmlText
  @JsonProperty(required = false)
  public String script;

  // No-arg constructor for Jackson deserialization
  public RSpell() {
    super("unknown");
  }

  public RSpell(String id, SpellType type, String... path) {
    super(id, path);
    this.type = type;
  }

  /**
   * Initializes a spell with the given parameters.
   *
   * @param id the name of this spell
   * @param range the range of this spell
   * @param duration the duration of this spell
   * @param effect the <code>Effect</code> of this spell
   * @param size the size of this spell
   * @param type the type of this spell
   */
  public RSpell(
      String id, int range, int duration, String effect, int radius, int size, String type) {
    super(id);
    this.range = range;
    this.duration = duration;
    this.effect = Effect.valueOf(effect.toUpperCase());
    this.size = size;
    this.type = SpellType.valueOf(type.toUpperCase());
    this.radius = radius;
    script = null;
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RSpell(Element spell, String... path) {
    super(spell, path);
    type = SpellType.valueOf(spell.getName().toUpperCase());
    effect = Effect.valueOf(spell.getAttributeValue("effect").toUpperCase());
    script = spell.getText();

    if (spell.getAttribute("size") != null) {
      size = Integer.parseInt(spell.getAttributeValue("size"));
    } else {
      size = 0;
    }
    if (spell.getAttribute("range") != null) {
      range = Integer.parseInt(spell.getAttributeValue("range"));
    } else {
      range = 0;
    }
    if (spell.getAttribute("duration") != null) {
      duration = Integer.parseInt(spell.getAttributeValue("duration"));
    } else {
      duration = 0;
    }
    if (spell.getAttribute("area") != null) {
      radius = Integer.parseInt(spell.getAttributeValue("area"));
    } else {
      radius = 0;
    }
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
      throw new RuntimeException("Failed to serialize RSpell to Element", e);
    }
  }

  // scrolls/books have a regular spell
  public static class Enchantment extends RSpell {
    @JacksonXmlProperty(isAttribute = true)
    public String item; // valid: clothing/armor, weapon, container/door, food/potion

    // No-arg constructor for Jackson deserialization
    public Enchantment() {
      super();
      this.type = SpellType.ENCHANT;
    }

    public Enchantment(Element enchantment, String... path) {
      super(enchantment, path);
      item = enchantment.getAttributeValue("item");
    }

    public Enchantment(String id, String... path) {
      super(id, SpellType.ENCHANT, path);
    }

    @Override
    public Element toElement() {
      Element enchantment = super.toElement();
      if (item != null) {
        enchantment.setAttribute("item", item);
      }
      return enchantment;
    }
  }

  public static class Power extends RSpell {
    @JacksonXmlProperty(isAttribute = true, localName = "int")
    public int interval;

    // No-arg constructor for Jackson deserialization
    public Power() {
      super();
      this.type = SpellType.POWER;
    }

    public Power(Element power, String... path) {
      super(power, path);
      interval = Integer.parseInt(power.getAttributeValue("int"));
    }

    public Power(String id, String... path) {
      super(id, SpellType.POWER, path);
      interval = 0;
    }

    @Override
    public Element toElement() {
      Element power = super.toElement();
      power.setAttribute("int", Integer.toString(interval));
      return power;
    }
  }
}
