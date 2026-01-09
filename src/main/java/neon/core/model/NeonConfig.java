/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Peter Riewe
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

package neon.core.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Jackson model for neon.ini.xml configuration file.
 *
 * @author priewe
 */
@JacksonXmlRootElement(localName = "root")
public class NeonConfig {

  @JacksonXmlProperty(localName = "files")
  public FilesElement files = new FilesElement();

  @JacksonXmlProperty(localName = "threads")
  public ThreadsElement threads = new ThreadsElement();

  @JacksonXmlProperty(localName = "ai")
  public String ai;

  @JacksonXmlProperty(localName = "log")
  public String log;

  @JacksonXmlProperty(localName = "lang")
  public String lang;

  @JacksonXmlProperty(localName = "keys")
  public String keys;

  /** Empty files element */
  public static class FilesElement {
    // Empty element placeholder
  }

  /** Threads configuration */
  public static class ThreadsElement {
    @JacksonXmlProperty(isAttribute = true, localName = "generate")
    public String generate;
  }
}
