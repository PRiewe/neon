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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public class RMod extends Resource {
  public ArrayList<String> ccItems = new ArrayList<String>();
  public ArrayList<String> ccRaces = new ArrayList<String>();
  public ArrayList<String> ccSpells = new ArrayList<String>();
  private HashMap<String, String> info = new HashMap<String, String>();
  private ArrayList<String[]> maps = new ArrayList<String[]>();

  /** Jackson model for main.xml */
  @JacksonXmlRootElement
  public static class MainXml {
    @JacksonXmlProperty(isAttribute = true)
    public String id;

    @JacksonXmlProperty(localName = "title")
    @JsonProperty(required = false)
    public String title;

    @JacksonXmlProperty(localName = "currency")
    @JsonProperty(required = false)
    public Currency currency;

    @JacksonXmlProperty(localName = "master")
    @JsonProperty(required = false)
    public String master;

    public static class Currency {
      @JacksonXmlProperty(isAttribute = true)
      public String big;

      @JacksonXmlProperty(isAttribute = true)
      public String small;
    }
  }

  /** Jackson model for cc.xml */
  @JacksonXmlRootElement(localName = "root")
  public static class CCXml {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "race")
    public List<String> races = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    public List<String> items = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "spell")
    public List<String> spells = new ArrayList<>();

    @JacksonXmlProperty(localName = "map")
    @JsonProperty(required = false)
    public MapStart map;

    public static class MapStart {
      @JacksonXmlProperty(isAttribute = true)
      public String path;

      @JacksonXmlProperty(isAttribute = true)
      public String x;

      @JacksonXmlProperty(isAttribute = true)
      public String y;

      @JacksonXmlProperty(isAttribute = true)
      @JsonProperty(required = false)
      public String z;
    }
  }

  // No-arg constructor for Jackson deserialization
  public RMod() {
    super("unknown");
  }

  // Jackson constructor
  public RMod(MainXml main, CCXml cc, String... path) {
    super(main.id, path);

    // main.xml
    info.put("id", main.id);
    if (main.master != null) {
      info.put("master", main.master);
    }
    if (main.title != null) {
      info.put("title", main.title);
    }
    if (main.currency != null) {
      info.put("big", main.currency.big);
      info.put("small", main.currency.small);
    }

    // cc.xml
    if (cc != null) {
      ccRaces.addAll(cc.races);
      ccItems.addAll(cc.items);
      ccSpells.addAll(cc.spells);
      if (cc.map != null) {
        info.put("map", cc.map.path);
        info.put("x", cc.map.x);
        info.put("y", cc.map.y);
        if (cc.map.z != null) {
          info.put("z", cc.map.z);
        }
      }
    }
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RMod(Element main, Element cc, String... path) {
    super(main.getAttributeValue("id"), path);

    // main.xml
    info.put("id", main.getAttributeValue("id"));
    info.put("master", main.getChildText("master"));
    if (main.getChildText("title") != null) {
      info.put("title", main.getChildText("title"));
    }
    if (main.getChild("currency") != null) {
      info.put("big", main.getChild("currency").getAttributeValue("big"));
      info.put("small", main.getChild("currency").getAttributeValue("small"));
    }

    // cc.xml
    if (cc != null) { // strings here, because resources are not yet loaded
      for (Element race : cc.getChildren("race")) {
        ccRaces.add(race.getText());
      }
      for (Element item : cc.getChildren("item")) {
        ccItems.add(item.getText());
      }
      for (Element spell : cc.getChildren("spell")) {
        ccSpells.add(spell.getText());
      }
      if (cc.getChild("map") != null) {
        info.put("map", cc.getChild("map").getAttributeValue("path"));
        info.put("x", cc.getChild("map").getAttributeValue("x"));
        info.put("y", cc.getChild("map").getAttributeValue("y"));
        info.put("z", cc.getChild("map").getAttributeValue("z"));
      }
    }
  }

  /**
   * @return the root element of the main.xml file for this mod using Jackson serialization.
   */
  public Element getMainElement() {
    try {
      MainXml main = new MainXml();
      main.id = info.get("id");
      main.title = info.get("title");
      if (info.get("big") != null || info.get("small") != null) {
        main.currency = new MainXml.Currency();
        main.currency.big = info.get("big");
        main.currency.small = info.get("small");
      }
      if (isExtension()) {
        main.master = info.get("master");
      }

      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(main).toString();
      Element element =
          new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
      // Set correct root element name
      element.setName(isExtension() ? "extension" : "master");
      return element;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RMod main to Element", e);
    }
  }

  /**
   * @return the root element of the cc.xml file for this mod using Jackson serialization.
   */
  public Element getCCElement() {
    try {
      CCXml cc = new CCXml();
      cc.items.addAll(ccItems);
      cc.spells.addAll(ccSpells);
      cc.races.addAll(ccRaces);
      if (info.get("map") != null) {
        cc.map = new CCXml.MapStart();
        cc.map.path = info.get("map");
        cc.map.x = info.get("x");
        cc.map.y = info.get("y");
        cc.map.z = info.get("z");
      }

      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(cc).toString();
      Element element =
          new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
      return element;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize RMod cc to Element", e);
    }
  }

  public List<String> getList(String key) {
    ArrayList<String> list = new ArrayList<String>();
    if (key.equals("items")) {
      list.addAll(ccItems);
    } else if (key.equals("spells")) {
      list.addAll(ccSpells);
    } else if (key.equals("races")) {
      list.addAll(ccRaces);
    }
    return list;
  }

  /**
   * @return whether this is an extension mod or not.
   */
  public boolean isExtension() {
    return info.get("master") != null;
  }

  public String get(String key) {
    if (info.get(key) != null) {
      return info.get(key);
    } else {
      return null;
    }
  }

  public void set(String key, String value) {
    info.put(key, value);
  }

  /**
   * @return a list with the paths to all maps in this mod
   */
  public Collection<String[]> getMaps() {
    return maps;
  }

  public void addMaps(ArrayList<String[]> maps) {
    this.maps.addAll(maps);
  }

  @Override
  public void load() {}

  @Override
  public void unload() {}
}
