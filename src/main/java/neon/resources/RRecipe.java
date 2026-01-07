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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement(localName = "recipe")
public class RRecipe extends RData {
  // Jackson-friendly representation (deserialized via setters)
  private List<InElement> inElements = new ArrayList<>();
  private OutElement outElement;

  // Public fields for game code compatibility
  public Vector<String> ingredients = new Vector<String>();

  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public int cost = 10;

  /** Inner class for 'in' XML element */
  public static class InElement {
    @JacksonXmlText public String value;

    public InElement() {}

    public InElement(String value) {
      this.value = value;
    }
  }

  /** Inner class for 'out' XML element */
  public static class OutElement {
    @JacksonXmlText public String value;

    public OutElement() {}

    public OutElement(String value) {
      this.value = value;
    }
  }

  // No-arg constructor for Jackson deserialization
  public RRecipe() {
    super("unknown");
  }

  public RRecipe(String id, RItem item, String... path) {
    super(id, path);
    name = item.id;
  }

  public String toString() {
    return name;
  }

  /**
   * Sync in-element list to ingredients vector (called by Jackson after deserialization).
   *
   * @param inElements the deserialized in-element list
   */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "in")
  public void setInElements(List<InElement> inElements) {
    this.inElements = inElements;
    this.ingredients.clear();
    for (InElement in : inElements) {
      this.ingredients.add(in.value);
    }
  }

  /**
   * Get in-element list for serialization.
   *
   * @return list of in-elements
   */
  public List<InElement> getIn() {
    List<InElement> list = new ArrayList<>();
    for (String ingredient : ingredients) {
      list.add(new InElement(ingredient));
    }
    return list;
  }

  /**
   * Sync out-element to name field (called by Jackson after deserialization).
   *
   * @param outElement the deserialized out-element
   */
  @JacksonXmlProperty(localName = "out")
  public void setOut(OutElement outElement) {
    this.outElement = outElement;
    this.name = outElement.value;
  }

  /**
   * Get out-element for serialization.
   *
   * @return out-element
   */
  public OutElement getOut() {
    OutElement out = new OutElement();
    out.value = name;
    return out;
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RRecipe(Element properties, String... path) {
    super(properties.getAttributeValue("id"), path);
    name = properties.getChild("out").getText();
    if (properties.getAttribute("cost") != null) {
      cost = Integer.parseInt(properties.getAttributeValue("cost"));
    }
    for (Element in : properties.getChildren("in")) {
      ingredients.add(in.getText());
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
      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RRecipe to Element", e);
    }
  }
}
