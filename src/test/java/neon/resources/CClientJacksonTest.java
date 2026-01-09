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

import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for CClient resources. */
public class CClientJacksonTest {

  @Test
  public void testBasicParsing() throws IOException {
    String xml = "<root>" + "<keys>numpad</keys>" + "<lang>en</lang>" + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CClient client = mapper.fromXml(input, CClient.class);

    assertNotNull(client);
    assertEquals(CClient.NUMPAD, client.getSettings());
    assertEquals("en", client.getLang());
    assertEquals(KeyEvent.VK_NUMPAD8, client.up);
    assertEquals(KeyEvent.VK_NUMPAD2, client.down);
    assertEquals(KeyEvent.VK_M, client.map);
  }

  @Test
  public void testQwertyLayout() throws IOException {
    String xml = "<root>" + "<keys>qwerty</keys>" + "<lang>en</lang>" + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CClient client = mapper.fromXml(input, CClient.class);

    assertNotNull(client);
    assertEquals(CClient.QWERTY, client.getSettings());
    assertEquals(KeyEvent.VK_W, client.up);
    assertEquals(KeyEvent.VK_X, client.down);
    assertEquals(KeyEvent.VK_A, client.left);
    assertEquals(KeyEvent.VK_D, client.right);
  }

  @Test
  public void testAzertyLayout() throws IOException {
    String xml = "<root>" + "<keys>azerty</keys>" + "<lang>en</lang>" + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CClient client = mapper.fromXml(input, CClient.class);

    assertNotNull(client);
    assertEquals(CClient.AZERTY, client.getSettings());
    assertEquals(KeyEvent.VK_Z, client.up);
    assertEquals(KeyEvent.VK_X, client.down);
    assertEquals(KeyEvent.VK_Q, client.left);
    assertEquals(KeyEvent.VK_D, client.right);
  }

  @Test
  public void testCustomKeyBindings() throws IOException {
    String xml =
        "<root>"
            + "<keys map=\"VK_K\" act=\"VK_SPACE\" magic=\"VK_G\" shoot=\"VK_F\" look=\"VK_L\" "
            + "talk=\"VK_T\" unmount=\"VK_U\" sneak=\"VK_V\" journal=\"VK_J\">numpad</keys>"
            + "<lang>en</lang>"
            + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CClient client = mapper.fromXml(input, CClient.class);

    assertNotNull(client);
    assertEquals(KeyEvent.VK_K, client.map);
    assertEquals(KeyEvent.VK_SPACE, client.act);
    assertEquals(KeyEvent.VK_G, client.magic);
    assertEquals(KeyEvent.VK_F, client.shoot);
    assertEquals(KeyEvent.VK_L, client.look);
    assertEquals(KeyEvent.VK_T, client.talk);
    assertEquals(KeyEvent.VK_U, client.unmount);
    assertEquals(KeyEvent.VK_V, client.sneak);
    assertEquals(KeyEvent.VK_J, client.journal);
  }

  @Test
  public void testPartialCustomBindings() throws IOException {
    String xml =
        "<root>"
            + "<keys map=\"VK_K\" shoot=\"VK_B\">qwerty</keys>"
            + "<lang>en</lang>"
            + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    CClient client = mapper.fromXml(input, CClient.class);

    assertNotNull(client);
    assertEquals(CClient.QWERTY, client.getSettings());
    assertEquals(KeyEvent.VK_K, client.map); // custom
    assertEquals(KeyEvent.VK_B, client.shoot); // custom
    assertEquals(KeyEvent.VK_G, client.magic); // default
  }

  @Test
  public void testSerialization() throws IOException {
    CClient client = new CClient();
    client.setKeys(CClient.QWERTY);
    client.setLang("en");

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(client).toString();

    assertTrue(xml.contains("qwerty"));
    assertTrue(xml.contains("<lang>en</lang>"));
  }

  @Test
  public void testRoundTrip() throws IOException {
    String originalXml = "<root>" + "<keys>qwertz</keys>" + "<lang>en</lang>" + "</root>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(originalXml.getBytes(StandardCharsets.UTF_8));

    // Parse
    CClient client = mapper.fromXml(input, CClient.class);

    assertNotNull(client);
    assertEquals(CClient.QWERTZ, client.getSettings());
    assertEquals("en", client.getLang());

    // Serialize back
    String serialized = mapper.toXml(client).toString();
    assertTrue(serialized.contains("qwertz"));
    assertTrue(serialized.contains("en"));
  }

  @Test
  public void testToElementBridge() {
    CClient client = new CClient();
    client.setKeys(CClient.AZERTY);
    client.setLang("en");

    // Call toElement() which now uses Jackson internally
    org.jdom2.Element element = client.toElement();

    assertEquals("root", element.getName());
    assertNotNull(element.getChild("keys"));
    assertEquals("azerty", element.getChild("keys").getText());
    assertEquals("en", element.getChildText("lang"));
  }
}
