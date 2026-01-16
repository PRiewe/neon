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

package neon.resources.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import neon.resources.RData;

/**
 * Generic wrapper model for serializing collections of resources to XML.
 *
 * <p>Allows Jackson to serialize heterogeneous resource collections with proper polymorphic type
 * handling. Jackson will use each resource's own annotations to determine the XML structure.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ResourceCollectionModel<RSpell> model = new ResourceCollectionModel<>("spells");
 * model.resources.add(spell1);
 * model.resources.add(spell2);
 * // Serializes to: <spells><spell>...</spell><spell>...</spell></spells>
 * }</pre>
 *
 * @param <T> the resource type (must extend RData)
 */
@JacksonXmlRootElement
public class ResourceCollectionModel<T extends RData> {
  // Don't wrap resources in an extra container element
  @JacksonXmlElementWrapper(useWrapping = false)
  // Use polymorphic type info to preserve actual resource class
  @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
  @JacksonXmlProperty
  public List<T> resources = new ArrayList<>();

  /** For Jackson deserialization. */
  public ResourceCollectionModel() {}

  /**
   * Creates a new resource collection model.
   *
   * @param resources the list of resources to include
   */
  public ResourceCollectionModel(List<T> resources) {
    this.resources = resources;
  }
}
