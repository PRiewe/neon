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

package neon.editor;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import neon.resources.RData;
import neon.resources.RMod;
import neon.systems.files.JacksonMapper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 * Jackson-based XML document builder for serializing game resources.
 *
 * <p>This class replaces {@link XMLBuilder} as part of Phase 7A of the JDOM-to-Jackson migration.
 * It uses Jackson for events serialization and leverages the existing {@code toElement()} bridge
 * pattern for resources (as established in RSpell, RCreature, and RMod).
 *
 * <p><b>Phase 7A Scope:</b> This class still produces JDOM Documents for compatibility with the
 * existing {@code ModFiler.saveFile(Document)} API. In Phase 7B, when {@code toElement()} methods
 * are removed from all resources, this class will be updated to serialize directly to
 * ByteArrayOutputStream without the JDOM intermediate representation.
 *
 * <p><b>Migration Path:</b>
 *
 * <ul>
 *   <li>Phase 7A (current): XMLBuilder → JacksonXmlBuilder (this class)
 *   <li>Phase 7B: Remove toElement() from resources, update this class to skip JDOM
 *   <li>Phase 7C: Direct Jackson serialization, eliminate this class
 * </ul>
 *
 * @author mdriesen
 */
public class JacksonXmlBuilder {
  private final JacksonMapper mapper = new JacksonMapper();
  private final DataStore store;

  /**
   * Creates a new JacksonXmlBuilder.
   *
   * @param store the data store containing events and active mod information
   */
  public JacksonXmlBuilder(DataStore store) {
    this.store = store;
  }

  /**
   * Creates an events XML document using Jackson serialization.
   *
   * <p>Serializes the scheduled game events from {@code DataStore.getEvents()} into an XML document
   * with the structure:
   *
   * <pre>{@code
   * <events>
   *   <event script="intro1" tick="0" />
   *   <event script="intro2" tick="11" />
   * </events>
   * }</pre>
   *
   * @return JDOM Document containing events
   */
  public Document getEventsDoc() {
    EventsModel model = new EventsModel();
    model.events = new ArrayList<>();

    // Convert Multimap<String, String> to List<Event>
    for (Map.Entry<String, Collection<String>> entry : store.getEvents().asMap().entrySet()) {
      String script = entry.getKey();
      for (String tick : entry.getValue()) {
        EventsModel.Event event = new EventsModel.Event();
        event.script = script;
        event.tick = tick;
        model.events.add(event);
      }
    }

    return toDocument(model);
  }

  /**
   * Creates a resource document with unsorted resources.
   *
   * <p>Resources maintain their natural iteration order. Filters resources to only include those
   * belonging to the specified mod.
   *
   * <p>Use this for: factions, recipes, terrain, themes (resources where order doesn't matter).
   *
   * @param resources the collection of resources to serialize
   * @param rootName the XML root element name (e.g., "factions", "terrain")
   * @param mod the active mod (filters by mod ID)
   * @return JDOM Document with unsorted resources
   */
  public Document getListDoc(Collection<? extends RData> resources, String rootName, RMod mod) {
    return buildResourceDoc(resources, rootName, mod, false);
  }

  /**
   * Creates a resource document with resources sorted alphabetically by ID.
   *
   * <p>Resources are sorted alphabetically by their {@code id} field. Filters resources to only
   * include those belonging to the specified mod.
   *
   * <p>Use this for: items, creatures, spells (resources that benefit from alphabetical order).
   *
   * @param resources the collection of resources to serialize
   * @param rootName the XML root element name (e.g., "items", "monsters")
   * @param mod the active mod (filters by mod ID)
   * @return JDOM Document with sorted resources
   */
  public Document getResourceDoc(Collection<? extends RData> resources, String rootName, RMod mod) {
    return buildResourceDoc(resources, rootName, mod, true);
  }

  /**
   * Builds a resource document, filtering by mod and optionally sorting by ID.
   *
   * <p>This method uses Jackson to serialize each resource directly, eliminating the need for
   * {@code toElement()} methods in resource classes. Each resource is serialized individually and
   * combined under a common root element.
   *
   * @param resources the collection of resources
   * @param rootName the XML root element name
   * @param mod the active mod
   * @param sorted whether to sort resources alphabetically by ID
   * @return JDOM Document
   */
  private Document buildResourceDoc(
      Collection<? extends RData> resources, String rootName, RMod mod, boolean sorted) {

    // Filter resources belonging to this mod
    List<RData> filtered =
        resources.stream()
            .filter(r -> r.getPath()[0].equals(mod.get("id")))
            .collect(Collectors.toList());

    // Sort if requested
    if (sorted) {
      filtered.sort(Comparator.comparing(r -> r.id));
    }

    // Build JDOM element with children using Jackson serialization
    Element root = new Element(rootName);
    for (RData resource : filtered) {
      try {
        // Serialize resource to XML with Jackson
        String xml = mapper.toXml(resource).toString();

        // Parse XML to Element
        Element element =
            new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
        element.detach();
        root.addContent(element);
      } catch (Exception e) {
        throw new RuntimeException("Failed to serialize resource: " + resource.id, e);
      }
    }

    return new Document(root);
  }

  /**
   * Converts a Jackson model to a JDOM Document using the Jackson→JDOM bridge pattern.
   *
   * <p>This follows the same pattern as {@code RSpell.toElement()} (line 384-398) and {@code
   * RMod.getMainElement()} (line 179-203): serialize to XML via Jackson, then parse back to JDOM.
   *
   * <p>This bridge is temporary and will be removed in Phase 7B when {@code ModFiler} is updated to
   * save ByteArrayOutputStream directly.
   *
   * @param model the Jackson-annotated model object to serialize
   * @return JDOM Document
   */
  private Document toDocument(Object model) {
    try {
      // Serialize model to XML using Jackson
      String xml = mapper.toXml(model).toString();

      // Parse XML string back to JDOM Document
      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes()));
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert Jackson model to JDOM Document", e);
    }
  }

  /**
   * Jackson model for events.xml structure.
   *
   * <p>Represents the XML structure:
   *
   * <pre>{@code
   * <events>
   *   <event script="script_name" tick="timestamp" />
   *   ...
   * </events>
   * }</pre>
   */
  @JacksonXmlRootElement(localName = "events")
  static class EventsModel {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "event")
    public List<Event> events;

    /** Represents a single scheduled event. */
    static class Event {
      @JacksonXmlProperty(isAttribute = true)
      public String script;

      @JacksonXmlProperty(isAttribute = true)
      public String tick;
    }
  }
}
