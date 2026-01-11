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
      log.error(
          "Failed to deserialize XML to {} due to {}", valueType.getSimpleName(), e.toString());
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
      log.error(
          "Failed to serialize {} to XML due to {}",
          object.getClass().getSimpleName(),
          e.toString());
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

  /**
   * Deserialize XML from a String to a specified type.
   *
   * @param <T> the type of object to deserialize to
   * @param xmlString the XML string
   * @param valueType the class of the type to deserialize to
   * @return the deserialized object, or null if an error occurs
   */
  public <T> T fromXml(String xmlString, Class<T> valueType) {
    try {
      return mapper.readValue(xmlString, valueType);
    } catch (IOException e) {
      log.error(
          "Failed to deserialize XML to {} due to {}", valueType.getSimpleName(), e.toString());
      return null;
    }
  }

  /**
   * Parse an XML file containing multiple heterogeneous child elements under a root element. This
   * is useful for resource files that contain different types of resources.
   *
   * @param input the input stream containing XML with a root element and mixed child elements
   * @param elementHandler a handler that processes each child element based on its name
   * @throws IOException if an error occurs reading the stream
   */
  public void parseMultiTypeXml(InputStream input, ElementHandler elementHandler)
      throws IOException {
    try {
      ByteArrayInputStream byteInput = new ByteArrayInputStream(input.readAllBytes());
      // Read the entire stream into a string for manipulation
      input.close();

      // Parse with basic XML parsing to extract individual elements
      javax.xml.parsers.DocumentBuilderFactory factory =
          javax.xml.parsers.DocumentBuilderFactory.newInstance();
      javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
      org.w3c.dom.Document doc = builder.parse(byteInput);

      org.w3c.dom.Element root = doc.getDocumentElement();
      org.w3c.dom.NodeList children = root.getChildNodes();

      for (int i = 0; i < children.getLength(); i++) {
        org.w3c.dom.Node node = children.item(i);
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
          org.w3c.dom.Element element = (org.w3c.dom.Element) node;
          String elementName = element.getNodeName();
          String elementXml = nodeToString(element);

          // Call handler with element name and XML string
          elementHandler.handle(elementName, elementXml);
        }
      }
    } catch (Exception e) {
      throw new IOException("Failed to parse multi-type XML", e);
    }
  }

  /** Convert a DOM Node to an XML string. */
  private String nodeToString(org.w3c.dom.Node node) {
    try {
      javax.xml.transform.TransformerFactory tf =
          javax.xml.transform.TransformerFactory.newInstance();
      javax.xml.transform.Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
      java.io.StringWriter writer = new java.io.StringWriter();
      transformer.transform(
          new javax.xml.transform.dom.DOMSource(node),
          new javax.xml.transform.stream.StreamResult(writer));
      return writer.getBuffer().toString();
    } catch (Exception e) {
      return "";
    }
  }

  /** Functional interface for handling individual XML elements in a multi-type XML document. */
  @FunctionalInterface
  public interface ElementHandler {
    /**
     * Handle an XML element.
     *
     * @param elementName the name of the XML element (e.g., "spell", "item", "creature")
     * @param elementXml the XML string for this element
     */
    void handle(String elementName, String elementXml);
  }
}
