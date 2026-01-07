/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2024 - Maarten Driesen
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

package neon.systems.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Jackson XML mapper utility for parsing and serializing XML to/from POJOs. Provides a cleaner,
 * annotation-based alternative to manual JDOM2 parsing.
 *
 * @author mdriesen
 */
@Slf4j
public class JacksonMapper {
  private final XmlMapper mapper;

  public JacksonMapper() {
    this.mapper = new XmlMapper();
    // Configure mapper to be lenient with missing properties
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    // Handle missing required properties gracefully
    mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
    // Accept case-insensitive enum values (e.g., "block" → Modifier.BLOCK)
    mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
  }

  /**
   * Deserialize XML from an InputStream to a specified type.
   *
   * @param <T> the type of object to deserialize to
   * @param input the input stream containing XML
   * @param valueType the class of the type to deserialize to
   * @return the deserialized object, or null if an error occurs
   */
  public <T> T fromXml(InputStream input, Class<T> valueType) {
    try {
      T result = mapper.readValue(input, valueType);
      input.close();
      return result;
    } catch (IOException e) {
      log.error("Failed to deserialize XML to {}", valueType.getSimpleName(), e);
      return null;
    }
  }

  /**
   * Serialize an object to XML and write to an OutputStream.
   *
   * @param object the object to serialize
   * @return ByteArrayOutputStream containing the XML, or empty stream if an error occurs
   */
  public ByteArrayOutputStream toXml(Object object) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(out, object);
    } catch (IOException e) {
      log.error("Failed to serialize {} to XML", object.getClass().getSimpleName(), e);
    }
    return out;
  }

  /**
   * Get the underlying XmlMapper instance for advanced configuration.
   *
   * @return the XmlMapper instance
   */
  public XmlMapper getMapper() {
    return mapper;
  }
}
