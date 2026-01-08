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

package neon.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for CServer resources. */
public class CServerJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml =
        "<root>"
            + "<files>"
            + "<file>darkness</file>"
            + "<file>another_mod</file>"
            + "</files>"
            + "<log>FINEST</log>"
            + "<threads generate=\"on\" />"
            + "<ai>20</ai>"
            + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CServer server = mapper.fromXml(input, CServer.class);

    assertNotNull(server);
    assertEquals(2, server.getMods().size());
    assertTrue(server.getMods().contains("darkness"));
    assertTrue(server.getMods().contains("another_mod"));
    assertEquals("FINEST", server.getLogLevel());
    assertTrue(server.isMapThreaded());
    assertEquals(20, server.getAIRange());
  }

  @Test
  public void testEmptyFiles() throws IOException {
    String xml =
        "<root>"
            + "<files />"
            + "<log>INFO</log>"
            + "<threads generate=\"off\" />"
            + "<ai>10</ai>"
            + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CServer server = mapper.fromXml(input, CServer.class);

    assertNotNull(server);
    assertEquals(0, server.getMods().size());
    assertEquals("INFO", server.getLogLevel());
    assertFalse(server.isMapThreaded());
    assertEquals(10, server.getAIRange());
  }

  @Test
  public void testSingleMod() throws IOException {
    String xml =
        "<root>"
            + "<files>"
            + "<file>darkness</file>"
            + "</files>"
            + "<log>WARNING</log>"
            + "<threads generate=\"on\" />"
            + "<ai>15</ai>"
            + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CServer server = mapper.fromXml(input, CServer.class);

    assertNotNull(server);
    assertEquals(1, server.getMods().size());
    assertEquals("darkness", server.getMods().get(0));
  }

  @Test
  public void testThreadsOff() throws IOException {
    String xml =
        "<root>"
            + "<files />"
            + "<log>SEVERE</log>"
            + "<threads generate=\"off\" />"
            + "<ai>5</ai>"
            + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CServer server = mapper.fromXml(input, CServer.class);

    assertNotNull(server);
    assertFalse(server.isMapThreaded());
  }

  @Test
  public void testSerialization() throws IOException {
    CServer server = new CServer();
    server.getMods().add("darkness");
    server.getMods().add("test_mod");

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(server).toString();

    assertTrue(xml.contains("darkness"));
    assertTrue(xml.contains("test_mod"));
    assertTrue(xml.contains("FINEST"));
    assertTrue(xml.contains("generate=\"on\""));
    assertTrue(xml.contains("<ai>20</ai>"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml =
        "<root>"
            + "<files>"
            + "<file>mod1</file>"
            + "<file>mod2</file>"
            + "<file>mod3</file>"
            + "</files>"
            + "<log>DEBUG</log>"
            + "<threads generate=\"on\" />"
            + "<ai>25</ai>"
            + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    CServer server = mapper.fromXml(input, CServer.class);

    assertNotNull(server);
    assertEquals(3, server.getMods().size());
    assertEquals("DEBUG", server.getLogLevel());
    assertTrue(server.isMapThreaded());
    assertEquals(25, server.getAIRange());

    // Serialize back
    String serialized = mapper.toXml(server).toString();
    assertTrue(serialized.contains("mod1"));
    assertTrue(serialized.contains("mod2"));
    assertTrue(serialized.contains("mod3"));
    assertTrue(serialized.contains("DEBUG"));
    assertTrue(serialized.contains("generate=\"on\""));
    assertTrue(serialized.contains("<ai>25</ai>"));
  }

  @Test
  public void testToElementBridge() {
    CServer server = new CServer();
    server.getMods().add("darkness");

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = server.toElement();

    assertEquals("root", element.getName());
    assertNotNull(element.getChild("files"));
    assertEquals(1, element.getChild("files").getChildren("file").size());
    assertEquals("darkness", element.getChild("files").getChild("file").getText());
    assertEquals("FINEST", element.getChildText("log"));
    assertEquals("on", element.getChild("threads").getAttributeValue("generate"));
    assertEquals("20", element.getChildText("ai"));
  }
}
