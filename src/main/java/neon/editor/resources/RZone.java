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

package neon.editor.resources;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import neon.resources.RData;
import neon.resources.RZoneTheme;
import neon.resources.ResourceManager;
import neon.ui.graphics.Renderable;
import neon.ui.graphics.Scene;
import org.jdom2.Element;

/*
 * possible RZone constructions:
 * 	1. new outdoor map: RZone is created directly, with 1 region 	RZone(id, tree): ok
 * 	(2. new dungeon: RZone is not created directly)
 * 	3. new dungeon level:
 * 		a. RZone is created with theme								RZone(id, theme): ok?
 * 		b. RZone is created with 1 region							RZone(id, tree): ok
 * 	4. outdoor map loadMod												RZone(properties): ok
 * 	5. dungeon map loadMod												RZone(properties): ok
 *
 * the following now happens somewhat messily with load():
 * 	6. outdoor map display
 * 	7. dungeon map display
 */
@Slf4j
public class RZone extends RData {
  public RMap map;
  public RZoneTheme theme;
  private Scene scene;
  private final List<String> outs = new ArrayList<>();

  // zone loaded as element from file
  RZone(ResourceManager rm, Element properties, RMap map, String... path) {
    // messy trick because id is final.
    super(
        (map.isDungeon()
            ? properties.getAttributeValue("name")
            : properties.getChild("header").getChildText("name")),
        path);
    this.map = map;
    name = id;
    theme = (RZoneTheme) rm.getResource(properties.getAttributeValue("theme"), "theme");
    String out = properties.getAttributeValue("out");
    if (out != null) {
      outs.addAll(Arrays.asList(out.split(",")));
    }
    scene = new Scene();
  }

  // new zone with theme
  public RZone(String id, String mod, RZoneTheme theme, RMap map) {
    super(id, mod);
    name = id;
    this.map = map;
    this.theme = theme;
    scene = new Scene();
  }

  // new zone with renderables
  public RZone(String id, String mod, Instance instance, RMap map) {
    super(id, mod);
    name = id;
    scene = new Scene();
    scene.addElement(instance, instance.getBounds(), instance.z);
    this.map = map;
  }

  public Scene getScene() {
    return scene;
  }

  public String toString() {
    return name;
  }

  public IRegion getRegion(int x, int y) {
    IRegion region = null;
    for (Renderable instance : scene.getElements(new Rectangle(x, y, 1, 1))) {
      if (instance instanceof IRegion) {
        if (region == null || instance.getZ() > region.z) {
          region = (IRegion) instance;
        }
      }
    }
    return region;
  }

  public ArrayList<Integer> load(RZoneFactory rf, Element zone) {
    ArrayList<Integer> uids = new ArrayList<Integer>();
    scene = new Scene();
    try { // creatures
      for (Element creature : zone.getChild("creatures").getChildren()) {
        Instance r = rf.getInstance(creature, this);
        scene.addElement(r, r.getBounds(), r.z);
        uids.add(Integer.parseInt(creature.getAttributeValue("uid")));
      }
    } catch (NullPointerException e) {
      log.warn("No creatures found in zone {}", zone);
    } // if map contains no creatures
    try { // regions
      var regionList = zone.getChild("regions").getChildren();
      for (Element region : regionList) {
        Instance r = new IRegion(rf.dataStore(), region);
        scene.addElement(r, r.getBounds(), r.z);
      }
    } catch (NullPointerException e) {
      log.warn("No regions found in zone {}", zone);
    } // if map contains no regions
    try { // items
      for (Element item : zone.getChild("items").getChildren()) {
        Instance r = rf.getInstance(item, this);
        scene.addElement(r, r.getBounds(), r.z);
        uids.add(Integer.parseInt(item.getAttributeValue("uid")));
        if (item.getName().equals("container")) { // add container items to uids
          for (Element e : item.getChildren()) {
            uids.add(Integer.parseInt(e.getAttributeValue("uid")));
          }
        }
      }
    } catch (NullPointerException e) {
      log.warn("No items found in zone {}", zone);
    } // if map contains no items
    return uids;
  }

  public Element toElement() {
    Element level = new Element("level");
    level.setAttribute("name", name);
    if (this.theme != null) {
      level.setAttribute("theme", theme.id);
    }
    if (!this.outs.isEmpty()) {
      String outValue = String.join(",", this.outs);
      level.setAttribute("out", outValue);
    }
    Element creatures = new Element("creatures");
    Element items = new Element("items");
    Element regions = new Element("regions");
    for (Renderable r : scene.getElements()) {
      Instance i = (Instance) r;
      Element element = i.toElement();
      element.detach();
      switch (element.getName()) {
        case "region" -> regions.addContent(element);
        case "creature" -> creatures.addContent(element);
        case "item", "door", "container" -> items.addContent(element);
      }
    }
    if (!creatures.getChildren().isEmpty()) {
      level.addContent(creatures);
    }
    if (!items.getChildren().isEmpty()) {
      level.addContent(items);
    }
    if (!regions.getChildren().isEmpty()) {
      level.addContent(regions);
    }
    return level;
  }
}
