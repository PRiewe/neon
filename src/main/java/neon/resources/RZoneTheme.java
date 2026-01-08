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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement(localName = "zone")
public class RZoneTheme extends RData {
  public String type, floor, walls, doors;

  @JacksonXmlProperty(isAttribute = true)
  public int min;

  @JacksonXmlProperty(isAttribute = true)
  public int max;

  public HashMap<String, Integer> creatures = new HashMap<String, Integer>();
  public HashMap<String, Integer> items = new HashMap<String, Integer>();
  public ArrayList<Feature> features = new ArrayList<Feature>();

  /** Inner class for Jackson XML parsing of feature elements */
  @JacksonXmlRootElement(localName = "feature")
  public static class Feature {
    @JacksonXmlProperty(isAttribute = true, localName = "t")
    public String t; // terrain type

    @JacksonXmlProperty(isAttribute = true, localName = "s")
    public int s; // size

    @JacksonXmlProperty(isAttribute = true, localName = "n")
    public int n; // number

    @JacksonXmlText public String value; // feature name/text
  }

  /** Inner class for creature entries */
  public static class CreatureEntry {
    @JacksonXmlProperty(isAttribute = true, localName = "n")
    public int n;

    @JacksonXmlText public String value;
  }

  /** Inner class for item entries */
  public static class ItemEntry {
    @JacksonXmlProperty(isAttribute = true, localName = "n")
    public int n;

    @JacksonXmlText public String value;
  }

  // No-arg constructor for Jackson deserialization
  public RZoneTheme() {
    super("unknown");
  }

  public RZoneTheme(String id, String... path) {
    super(id, path);
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RZoneTheme(Element props, String... path) {
    super(props.getAttributeValue("id"), path);
    String[] params = props.getAttributeValue("type").split(";");
    type = params[0];
    floor = params[1];
    walls = params[2];
    doors = params[3];
    min = Integer.parseInt(props.getAttributeValue("min"));
    max = Integer.parseInt(props.getAttributeValue("max"));

    for (Element creature : props.getChildren("creature")) {
      creatures.put(creature.getText(), Integer.parseInt(creature.getAttributeValue("n")));
    }

    for (Element item : props.getChildren("item")) {
      items.put(item.getText(), Integer.parseInt(item.getAttributeValue("n")));
    }

    for (Element featureEl : props.getChildren("feature")) {
      Feature feature = new Feature();
      feature.t = featureEl.getAttributeValue("t");
      feature.value = featureEl.getText();
      feature.s = Integer.parseInt(featureEl.getAttributeValue("s"));
      feature.n = Integer.parseInt(featureEl.getAttributeValue("n"));
      features.add(feature);
    }
  }

  /** Jackson setter for the "type" attribute - parses type, floor, walls, doors */
  @JacksonXmlProperty(isAttribute = true, localName = "type")
  public void setTypeAttribute(String typeAttr) {
    if (typeAttr != null) {
      String[] params = typeAttr.split(";");
      type = params[0];
      if (params.length > 1) floor = params[1];
      if (params.length > 2) walls = params[2];
      if (params.length > 3) doors = params[3];
    }
  }

  /** Jackson getter for the "type" attribute - serializes type, floor, walls, doors */
  @JacksonXmlProperty(isAttribute = true, localName = "type")
  public String getTypeAttribute() {
    return type + ";" + floor + ";" + walls + ";" + doors;
  }

  /** Jackson setter for creature entries - converts list to HashMap */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "creature")
  public void setCreatureList(List<CreatureEntry> creatureList) {
    if (creatureList != null) {
      for (CreatureEntry entry : creatureList) {
        creatures.put(entry.value, entry.n);
      }
    }
  }

  /** Jackson getter for creature entries - converts HashMap to list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "creature")
  public List<CreatureEntry> getCreatureList() {
    List<CreatureEntry> list = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : creatures.entrySet()) {
      CreatureEntry ce = new CreatureEntry();
      ce.value = entry.getKey();
      ce.n = entry.getValue();
      list.add(ce);
    }
    return list;
  }

  /** Jackson setter for item entries - converts list to HashMap */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "item")
  public void setItemList(List<ItemEntry> itemList) {
    if (itemList != null) {
      for (ItemEntry entry : itemList) {
        items.put(entry.value, entry.n);
      }
    }
  }

  /** Jackson getter for item entries - converts HashMap to list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "item")
  public List<ItemEntry> getItemList() {
    List<ItemEntry> list = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : items.entrySet()) {
      ItemEntry ie = new ItemEntry();
      ie.value = entry.getKey();
      ie.n = entry.getValue();
      list.add(ie);
    }
    return list;
  }

  /** Jackson setter for feature list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "feature")
  public void setFeatures(List<Feature> features) {
    this.features = new ArrayList<>(features);
  }

  /** Jackson getter for feature list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "feature")
  public List<Feature> getFeatures() {
    return features;
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
      throw new RuntimeException("Failed to serialize RZoneTheme to Element", e);
    }
  }
}
