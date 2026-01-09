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

@JacksonXmlRootElement(localName = "region")
public class RRegionTheme extends RData {
  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty(required = false)
  public String floor;

  public Type type;

  public String door, wall;

  public HashMap<String, Integer> creatures = new HashMap<String, Integer>();

  public List<Feature> features = new ArrayList<Feature>();

  public HashMap<String, Integer> vegetation = new HashMap<String, Integer>();

  /** Inner class for Jackson XML parsing of feature elements */
  @JacksonXmlRootElement(localName = "feature")
  public static class Feature {
    @JacksonXmlProperty(isAttribute = true, localName = "n")
    public String n; // number/frequency

    @JacksonXmlProperty(isAttribute = true, localName = "s")
    @JsonProperty(required = false)
    public String s; // size

    @JacksonXmlProperty(isAttribute = true, localName = "t")
    @JsonProperty(required = false)
    public String t; // terrain type

    @JacksonXmlText public String value; // text content (e.g., "lake")
  }

  /** Inner class for creature entries */
  public static class CreatureEntry {
    @JacksonXmlProperty(isAttribute = true, localName = "n")
    public int n; // number

    @JacksonXmlText public String value; // creature ID
  }

  /** Inner class for vegetation/plant entries */
  public static class PlantEntry {
    @JacksonXmlProperty(isAttribute = true, localName = "a")
    public int a; // abundance

    @JacksonXmlText public String value; // plant ID
  }

  // No-arg constructor for Jackson deserialization
  public RRegionTheme() {
    super("unknown");
  }

  public RRegionTheme(String id, String... path) {
    super(id, path);
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RRegionTheme(Element theme, String... path) {
    super(theme.getAttributeValue("id"), path);
    String[] data = theme.getAttributeValue("random").split(";");

    for (Element creature : theme.getChildren("creature")) {
      creatures.put(creature.getText(), Integer.parseInt(creature.getAttributeValue("n")));
    }

    // Convert JDOM Elements to Feature objects
    for (Element featureEl : new ArrayList<Element>(theme.getChildren("feature"))) {
      Feature feature = new Feature();
      feature.n = featureEl.getAttributeValue("n");
      feature.s = featureEl.getAttributeValue("s");
      feature.t = featureEl.getAttributeValue("t");
      feature.value = featureEl.getText();
      features.add(feature);
    }

    floor = theme.getAttributeValue("floor");
    type = Type.valueOf(data[0]);
    for (Element plant : theme.getChildren("plant")) {
      int abundance = Integer.parseInt(plant.getAttributeValue("a"));
      vegetation.put(plant.getText(), abundance);
    }

    switch (type) { // mottig switch met ontbrekende breaks
      case town:
      case town_big:
      case town_small:
        wall = data[1];
        door = data[2];
        break;
      default:
        break;
    }
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

  /** Jackson setter for feature list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "feature")
  public void setFeatures(List<Feature> features) {
    this.features = features;
  }

  /** Jackson getter for feature list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "feature")
  public List<Feature> getFeatures() {
    return features;
  }

  /** Jackson setter for vegetation/plant entries - converts list to HashMap */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "plant")
  public void setPlantList(List<PlantEntry> plantList) {
    if (plantList != null) {
      for (PlantEntry entry : plantList) {
        vegetation.put(entry.value, entry.a);
      }
    }
  }

  /** Jackson getter for vegetation/plant entries - converts HashMap to list */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "plant")
  public List<PlantEntry> getPlantList() {
    List<PlantEntry> list = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : vegetation.entrySet()) {
      PlantEntry pe = new PlantEntry();
      pe.value = entry.getKey();
      pe.a = entry.getValue();
      list.add(pe);
    }
    return list;
  }

  /** Jackson setter for the "random" attribute - parses type, wall, door */
  @JacksonXmlProperty(isAttribute = true, localName = "random")
  public void setRandom(String random) {
    if (random != null) {
      String[] data = random.split(";");
      type = Type.valueOf(data[0]);
      if (data.length > 1) {
        wall = data[1];
      }
      if (data.length > 2) {
        door = data[2];
      }
    }
  }

  /** Jackson getter for the "random" attribute - serializes type, wall, door */
  @JacksonXmlProperty(isAttribute = true, localName = "random")
  public String getRandom() {
    String random = type.toString() + ";";
    switch (type) {
      case town:
      case town_big:
      case town_small:
        random += (wall + ";" + door);
        break;
      default:
        break;
    }
    return random;
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
      throw new RuntimeException("Failed to serialize RRegionTheme to Element", e);
    }
  }

  public enum Type {
    town,
    town_small,
    town_big,
    PLAIN,
    TERRACE,
    RIDGES,
    CHAOTIC,
    BEACH;
  }
}
