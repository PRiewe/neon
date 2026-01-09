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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

@JacksonXmlRootElement // No localName - accepts any element name (weapon, armor, etc.)
public class RItem extends RData implements Serializable {
  public enum Type {
    aid,
    armor,
    book,
    clothing,
    coin,
    container,
    door,
    food,
    item,
    light,
    potion,
    scroll,
    weapon;
  }

  private static XMLOutputter outputter = new XMLOutputter();
  private static SAXBuilder builder = new SAXBuilder();

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public int cost;

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public float weight;

  @JacksonXmlProperty(isAttribute = true, localName = "z")
  @JsonProperty(required = false)
  private String zAttribute; // "top" or null

  public boolean top;
  public Type type;

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public String spell;

  // SVG content stored in a wrapper to properly deserialize inner XML
  @JacksonXmlProperty(localName = "svg")
  @JsonProperty(required = false)
  private SvgWrapper svgWrapper;

  // Public field for backward compatibility
  @com.fasterxml.jackson.annotation.JsonIgnore public String svg;

  /**
   * Get SVG content.
   *
   * @return SVG content string
   */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getSvg() {
    return svg;
  }

  /**
   * Set SVG content (used by Jackson deserialization).
   *
   * @param wrapper the SVG wrapper
   */
  @JsonSetter("svg")
  private void setSvgWrapper(SvgWrapper wrapper) {
    this.svgWrapper = wrapper;
    this.svg = (wrapper != null && wrapper.content != null) ? wrapper.content.trim() : null;
  }

  /**
   * Get SVG wrapper for serialization (used by Jackson serialization).
   *
   * @return SVG wrapper or null if no SVG content
   */
  @com.fasterxml.jackson.annotation.JsonGetter("svg")
  private SvgWrapper getSvgWrapper() {
    if (svg == null || svg.isEmpty()) {
      return null;
    }
    SvgWrapper wrapper = new SvgWrapper();
    wrapper.content = svg;
    return wrapper;
  }

  /** Wrapper class to deserialize/serialize SVG child element. */
  private static class SvgWrapper implements Serializable {
    @JacksonXmlText public String content;

    public SvgWrapper() {}
  }

  /**
   * Sync z attribute to top field (called by Jackson after deserialization).
   *
   * @param zValue the z attribute value
   */
  public void setZ(String zValue) {
    this.zAttribute = zValue;
    this.top = zValue != null;
  }

  /**
   * Get z attribute for serialization.
   *
   * @return z attribute or null
   */
  public String getZ() {
    return top ? "top" : null;
  }

  // No-arg constructor for Jackson deserialization
  public RItem() {
    super("unknown");
    this.type = Type.item; // Default type for generic items
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RItem(Element item, String... path) {
    super(item, path);
    type = Type.valueOf(item.getName());
    if (item.getAttribute("cost") != null) {
      cost = Integer.parseInt(item.getAttributeValue("cost"));
    }
    if (item.getAttribute("weight") != null) {
      weight = Float.parseFloat(item.getAttributeValue("weight"));
    }
    top = item.getAttribute("z") != null;
    if (item.getAttribute("spell") != null) {
      spell = item.getAttributeValue("spell");
    }
    if (item.getChild("svg") != null) {
      svg = outputter.outputString((Element) item.getChild("svg").getChildren().get(0));
    }
  }

  public RItem(String id, Type type, String... path) {
    super(id, path);
    this.type = type;
  }

  /**
   * Creates a JDOM Element from this resource using Jackson serialization.
   *
   * @return JDOM Element representation
   */
  public Element toElement() {
    try {
      neon.systems.files.JacksonMapper mapper = new neon.systems.files.JacksonMapper();
      String xml = mapper.toXml(this).toString();
      Element element =
          new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();

      // Fix root element name to match type (Jackson uses generic name)
      element.setName(type.toString());

      return element;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RItem to Element", e);
    }
  }

  public static class Door extends RItem implements Serializable {
    @com.fasterxml.jackson.annotation.JsonIgnore public String closed = " ";

    @com.fasterxml.jackson.annotation.JsonIgnore public String locked = " ";

    /** Inner class for door states */
    public static class States implements Serializable {
      @JacksonXmlProperty(isAttribute = true)
      public String closed;

      @JacksonXmlProperty(isAttribute = true)
      public String locked;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore private States statesElement;

    /** Called by Jackson after deserialization to sync states to fields */
    @com.fasterxml.jackson.annotation.JsonSetter("states")
    public void setStatesElement(States states) {
      this.statesElement = states;
      if (states != null) {
        if (states.closed != null) {
          this.closed = states.closed;
        }
        if (states.locked != null) {
          this.locked = states.locked;
        }
      }
    }

    /**
     * Get States object for serialization (creates from public fields).
     *
     * @return states object or null if all defaults
     */
    @com.fasterxml.jackson.annotation.JsonGetter("states")
    public States getStatesElement() {
      // Only serialize if values differ from defaults
      if ((!closed.equals(text) && !closed.equals(" "))
          || (!locked.equals(closed) && !locked.equals(" "))) {
        States states = new States();
        if (!closed.equals(text) && !closed.equals(" ")) {
          states.closed = closed;
        }
        if (!locked.equals(closed) && !locked.equals(" ")) {
          states.locked = locked;
        }
        return states;
      }
      return null;
    }

    // No-arg constructor for Jackson deserialization
    public Door() {
      super();
      this.type = Type.door;
    }

    public Door(Element door, String... path) {
      super(door, path);
      Element states = door.getChild("states");
      if (states != null) {
        if (states.getAttribute("closed") != null) {
          closed = states.getAttributeValue("closed");
        } else {
          closed = text;
        }
        if (states.getAttribute("locked") != null) {
          locked = states.getAttributeValue("locked");
        } else {
          locked = closed;
        }
      }
    }

    public Door(String id, Type type, String... path) {
      super(id, type, path);
    }

    @Override
    public Element toElement() {
      Element door = super.toElement();
      if ((!closed.equals(text) && !closed.equals(" "))
          || (!locked.equals(closed) && !locked.equals(" "))) {
        Element states = new Element("states");
        if (!closed.equals(text) && !closed.equals(" ")) {
          states.setAttribute("closed", closed);
        }
        if (!locked.equals(closed) && !locked.equals(" ")) {
          states.setAttribute("locked", locked);
        }
        door.addContent(states);
      }
      return door;
    }
  }

  public static class Potion extends RItem implements Serializable {
    // No-arg constructor for Jackson deserialization
    public Potion() {
      super();
      this.type = Type.potion;
    }

    public Potion(Element potion, String... path) {
      super(potion, path);
    }

    public Potion(String id, Type type, String... path) {
      super(id, type, path);
    }
  }

  public static class Container extends RItem implements Serializable {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    public ArrayList<String> contents = new ArrayList<String>();

    // No-arg constructor for Jackson deserialization
    public Container() {
      super();
      this.type = Type.container;
    }

    public Container(Element container, String... path) {
      super(container, path);
      for (Element item : container.getChildren("item")) {
        contents.add(item.getText());
      }
    }

    public Container(String id, Type type, String... path) {
      super(id, type, path);
    }
  }

  public static class Text extends RItem implements Serializable {
    @com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText public String content;

    // No-arg constructor for Jackson deserialization
    public Text() {
      super();
      this.type = Type.book; // Default to book, can also be scroll
    }

    public Text(Element text, String... path) {
      super(text, path);
      content = text.getText();
    }

    public Text(String id, Type type, String... path) {
      super(id, type, path);
    }

    public Element toElement() {
      Element book = super.toElement();
      book.setText(content);
      return book;
    }
  }
}
