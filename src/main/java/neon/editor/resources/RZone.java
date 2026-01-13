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
import lombok.Getter;
import neon.editor.DataStore;
import neon.resources.RData;
import neon.resources.RPerson;
import neon.resources.RZoneTheme;
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
public class RZone extends RData {
  public RMap map;
  public RZoneTheme theme;
  private Scene scene;
  @Getter private DataStore dataStore;

  // zone loaded as element from file
  public RZone(Element properties, RMap map, String... path) {
    // messy trick because id is final.
    super(
        (map.isDungeon()
            ? properties.getAttributeValue("name")
            : properties.getChild("header").getChildText("name")),
        path);
    this.map = map;
    name = id;
    theme =
        (RZoneTheme)
            dataStore
                .getResourceManager()
                .getResource(properties.getAttributeValue("theme"), "theme");
  }

  // new zone with theme
  public RZone(String id, String mod, RZoneTheme theme, RMap map) {
    super(id, mod);
    name = id;
    this.map = map;
    this.theme = theme;
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

  public ArrayList<Integer> load(Element zone) {
    ArrayList<Integer> uids = new ArrayList<Integer>();
    scene = new Scene();
    try { // creatures
      for (Element creature : zone.getChild("creatures").getChildren()) {
        Instance r = getInstance(creature, this);
        scene.addElement(r, r.getBounds(), r.z);
        uids.add(Integer.parseInt(creature.getAttributeValue("uid")));
      }
    } catch (NullPointerException e) {
    } // if map contains no creatures
    try { // regions
      for (Element region : zone.getChild("regions").getChildren()) {
        Instance r = new IRegion(region, dataStore);
        scene.addElement(r, r.getBounds(), r.z);
      }
    } catch (NullPointerException e) {
    } // if map contains no regions
    try { // items
      for (Element item : zone.getChild("items").getChildren()) {
        Instance r = getInstance(item, this);
        scene.addElement(r, r.getBounds(), r.z);
        uids.add(Integer.parseInt(item.getAttributeValue("uid")));
        if (item.getName().equals("container")) { // add container items to uids
          for (Element e : item.getChildren()) {
            uids.add(Integer.parseInt(e.getAttributeValue("uid")));
          }
        }
      }
    } catch (NullPointerException e) {
    } // if map contains no items
    return uids;
  }

  public Instance getInstance(Element e, RZone zone) {
    if (dataStore.getResourceManager().getResource(e.getAttributeValue("id")) instanceof RPerson) {
      return new IPerson(e, dataStore);
    } else if (e.getName().equals("door")) {
      return new IDoor(e, zone, dataStore);
    } else if (e.getName().equals("container")) {
      return new IContainer(e, dataStore);
    } else {
      return new IObject(e, dataStore);
    }
  }

  public Element toElement() {
    Element level = new Element("level");
    level.setAttribute("name", name);
    Element creatures = new Element("creatures");
    Element items = new Element("items");
    Element regions = new Element("regions");
    for (Renderable r : scene.getElements()) {
      Instance i = (Instance) r;
      Element element = i.toElement();
      element.detach();
      if (element.getName().equals("region")) {
        regions.addContent(element);
      } else if (element.getName().equals("creature")) {
        creatures.addContent(element);
      } else if (element.getName().equals("item")
          || element.getName().equals("door")
          || element.getName().equals("container")) {
        items.addContent(element);
      }
    }
    level.addContent(creatures);
    level.addContent(items);
    level.addContent(regions);
    return level;
  }
}
