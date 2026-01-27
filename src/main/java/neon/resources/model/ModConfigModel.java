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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Jackson model for mod configuration XML structure (main.xml).
 *
 * <p>This class represents the parsed XML structure of a mod configuration file. The root element
 * can be either {@code <master>} or {@code <extension>}.
 *
 * <p>Example master mod:
 *
 * <pre>{@code
 * <master id="darkness">
 *   <title>Darkness Falls</title>
 *   <currency big="gold pieces" small="copper pieces" />
 * </master>
 * }</pre>
 *
 * <p>Example extension mod:
 *
 * <pre>{@code
 * <extension id="my-extension">
 *   <title>My Extension</title>
 *   <master>darkness</master>
 * </extension>
 * }</pre>
 *
 * @author mdriesen
 */
public class ModConfigModel {

  /** The mod ID (from the id attribute on the root element). */
  @JacksonXmlProperty(isAttribute = true)
  public String id;

  /** The mod title. */
  @JacksonXmlProperty(localName = "title")
  public String title;

  /** Currency configuration (optional). */
  @JacksonXmlProperty(localName = "currency")
  public CurrencyConfig currency;

  /**
   * Master mod reference (only for extension mods). If this is an extension mod, this field
   * contains the ID of the master mod.
   */
  @JacksonXmlProperty(localName = "master")
  public String master;

  /** Currency configuration for big and small currency units. */
  public static class CurrencyConfig {
    @JacksonXmlProperty(isAttribute = true)
    public String big;

    @JacksonXmlProperty(isAttribute = true)
    public String small;
  }
}
