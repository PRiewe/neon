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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import neon.entities.property.Subtype;
import neon.maps.Region.Modifier;
import org.jdom2.Element;

@JacksonXmlRootElement(localName = "type")
public class RTerrain extends RData {

  // RTerrain-specific fields
  @JacksonXmlText public String description;

  @JacksonXmlProperty(isAttribute = true, localName = "mod")
  @JsonProperty(required = false)
  public Modifier modifier = Modifier.NONE;

  @JacksonXmlProperty(isAttribute = true, localName = "sub")
  @JsonProperty(required = false)
  public Subtype type = Subtype.NONE;

  // No-arg constructor for Jackson deserialization
  public RTerrain() {
    super("unknown");
    this.text = ".";
  }

  public RTerrain(String id, String... path) {
    super(id, path);
    text = ".";
  }

  // Keep JDOM constructor for backward compatibility during migration
  public RTerrain(Element e, String... path) {
    super(e.getAttributeValue("id"), path);
    color = e.getAttributeValue("color");
    text = e.getAttributeValue("char");
    description = e.getText();
    String mov = e.getAttributeValue("mod");
    if (mov != null) {
      modifier = Modifier.valueOf(mov.toUpperCase());
    }
    String mod = e.getAttributeValue("sub");
    if (mod != null) {
      type = Subtype.valueOf(mod.toUpperCase());
    }
  }

  public Element toElement() {
    Element terrain = new Element("type");
    terrain.setAttribute("id", id);
    terrain.setAttribute("char", text);
    terrain.setAttribute("color", color);
    if (modifier != Modifier.NONE) {
      terrain.setAttribute("mod", modifier.toString());
    }
    if (description != null && !description.isEmpty()) {
      terrain.setText(description);
    }
    if (type != Subtype.NONE) {
      terrain.setAttribute("sub", type.toString());
    }
    return terrain;
  }
}
