/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Maarten Driesen
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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson model for event configuration XML structure (events.xml).
 *
 * <p>This class represents the parsed XML structure of an event configuration file.
 *
 * <p>Example:
 *
 * <pre>{@code
 * <events>
 *   <event script="intro1" tick="0" />
 *   <event script="intro2" tick="11" />
 * </events>
 * }</pre>
 *
 * @author mdriesen
 */
@JacksonXmlRootElement(localName = "events")
public class EventConfigModel {

  /** List of scheduled events. */
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "event")
  public List<ScheduledEvent> events = new ArrayList<>();

  /** A scheduled event configuration. */
  public static class ScheduledEvent {
    /** The script to execute. */
    @JacksonXmlProperty(isAttribute = true)
    public String script;

    /**
     * The game tick when this event should fire. Can be a simple integer (e.g., "0") or a
     * colon-separated format (e.g., "start:period:end").
     */
    @JacksonXmlProperty(isAttribute = true)
    public String tick;
  }
}
