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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import neon.systems.files.JacksonMapper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

@JacksonXmlRootElement(localName = "root")
public class CClient extends Resource {
  // keyboard settings
  public static final int NUMPAD = 0;
  public static final int AZERTY = 1;
  public static final int QWERTY = 2;
  public static final int QWERTZ = 3;

  /** Inner class for keys configuration */
  public static class KeysConfig {
    @JacksonXmlProperty(isAttribute = true, localName = "map")
    public String map;

    @JacksonXmlProperty(isAttribute = true, localName = "act")
    public String act;

    @JacksonXmlProperty(isAttribute = true, localName = "magic")
    public String magic;

    @JacksonXmlProperty(isAttribute = true, localName = "shoot")
    public String shoot;

    @JacksonXmlProperty(isAttribute = true, localName = "look")
    public String look;

    @JacksonXmlProperty(isAttribute = true, localName = "talk")
    public String talk;

    @JacksonXmlProperty(isAttribute = true, localName = "unmount")
    public String unmount;

    @JacksonXmlProperty(isAttribute = true, localName = "sneak")
    public String sneak;

    @JacksonXmlProperty(isAttribute = true, localName = "journal")
    public String journal;

    @JacksonXmlText public String layout; // numpad, azerty, qwerty, qwertz
  }

  public int up = KeyEvent.VK_NUMPAD8;
  public int upright = KeyEvent.VK_NUMPAD9;
  public int right = KeyEvent.VK_NUMPAD6;
  public int downright = KeyEvent.VK_NUMPAD3;
  public int down = KeyEvent.VK_NUMPAD2;
  public int downleft = KeyEvent.VK_NUMPAD1;
  public int left = KeyEvent.VK_NUMPAD4;
  public int upleft = KeyEvent.VK_NUMPAD7;
  public int wait = KeyEvent.VK_NUMPAD5;

  public int map = KeyEvent.VK_M;
  public int magic = KeyEvent.VK_G;
  public int shoot = KeyEvent.VK_F;
  public int look = KeyEvent.VK_L;
  public int act = KeyEvent.VK_SPACE;
  public int talk = KeyEvent.VK_T;
  public int unmount = KeyEvent.VK_U;
  public int sneak = KeyEvent.VK_V;
  public int journal = KeyEvent.VK_J;

  @JsonIgnore private int keys = NUMPAD;

  // language settings
  @JsonIgnore private Properties strings;

  @JacksonXmlProperty(localName = "lang")
  private String lang = "en";

  // other settings
  private String bigCoin = "\u20AC"; // Euro symbol
  private String smallCoin = "c";
  @Setter @Getter private String title = "";

  // No-arg constructor for Jackson deserialization
  public CClient() {
    super("client");
    // Load default locale
    Properties defaults = new Properties();
    try (FileInputStream stream = new FileInputStream("data/locale/locale.en");
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      defaults.load(reader);
    } catch (IOException e) {
      e.printStackTrace();
    }
    strings = defaults;
  }

  // Keep JDOM constructor for backward compatibility during migration
  public CClient(String... path) {
    super("client", path);

    // load file
    Document doc = new Document();
    try (FileInputStream in = new FileInputStream(path[0])) {
      doc = new SAXBuilder().build(in);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Element root = doc.getRootElement();

    // keyboard
    setKeys(root.getChild("keys"));

    // language
    Properties defaults = new Properties(); // load locale.en as default
    try (FileInputStream stream = new FileInputStream("data/locale/locale.en");
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      defaults.load(reader);
    } catch (IOException e) {
      e.printStackTrace();
    }

    String lang = root.getChild("lang").getText();
    strings = new Properties(defaults); // initialize locale with 'en' defaults
    try (FileInputStream stream = new FileInputStream("data/locale/locale." + lang);
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      strings.load(reader);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void load() {}

  @Override
  public void unload() {}

  /**
   * Return the string value with the given name.
   *
   * @param name
   * @return
   */
  public String getString(String name) {
    return strings.getProperty(name);
  }

  public String getBig() {
    return bigCoin;
  }

  public void setBig(String name) {
    bigCoin = name;
  }

  public String getSmall() {
    return smallCoin;
  }

  public void setSmall(String name) {
    smallCoin = name;
  }

  public int getSettings() {
    return keys;
  }

  public void setKeys(Element settings) {
    if (settings != null) {
      // movement keys
      switch (settings.getText()) {
        case "azerty":
          setKeys(AZERTY);
          break;
        case "qwerty":
          setKeys(QWERTY);
          break;
        case "qwertz":
          setKeys(QWERTZ);
          break;
      }

      // other keys
      if (settings.getAttribute("map") != null) {
        map = getKeyCode(settings.getAttributeValue("map"));
      }
      if (settings.getAttribute("act") != null) {
        act = getKeyCode(settings.getAttributeValue("act"));
      }
      if (settings.getAttribute("magic") != null) {
        magic = getKeyCode(settings.getAttributeValue("magic"));
      }
      if (settings.getAttribute("shoot") != null) {
        shoot = getKeyCode(settings.getAttributeValue("shoot"));
      }
      if (settings.getAttribute("look") != null) {
        look = getKeyCode(settings.getAttributeValue("look"));
      }
      if (settings.getAttribute("talk") != null) {
        talk = getKeyCode(settings.getAttributeValue("talk"));
      }
      if (settings.getAttribute("unmount") != null) {
        unmount = getKeyCode(settings.getAttributeValue("unmount"));
      }
      if (settings.getAttribute("sneak") != null) {
        sneak = getKeyCode(settings.getAttributeValue("sneak"));
      }
      if (settings.getAttribute("journal") != null) {
        journal = getKeyCode(settings.getAttributeValue("journal"));
      }
    }
  }

  public void setKeys(int choice) {
    keys = choice;
    switch (keys) {
      case NUMPAD:
        up = KeyEvent.VK_NUMPAD8;
        upright = KeyEvent.VK_NUMPAD9;
        right = KeyEvent.VK_NUMPAD6;
        downright = KeyEvent.VK_NUMPAD3;
        down = KeyEvent.VK_NUMPAD2;
        downleft = KeyEvent.VK_NUMPAD1;
        left = KeyEvent.VK_NUMPAD4;
        upleft = KeyEvent.VK_NUMPAD7;
        wait = KeyEvent.VK_NUMPAD5;
        break;
      case AZERTY:
        up = KeyEvent.VK_Z;
        upright = KeyEvent.VK_E;
        right = KeyEvent.VK_D;
        downright = KeyEvent.VK_C;
        down = KeyEvent.VK_X;
        downleft = KeyEvent.VK_W;
        left = KeyEvent.VK_Q;
        upleft = KeyEvent.VK_A;
        wait = KeyEvent.VK_S;
        break;
      case QWERTY:
        up = KeyEvent.VK_W;
        upright = KeyEvent.VK_E;
        right = KeyEvent.VK_D;
        downright = KeyEvent.VK_C;
        down = KeyEvent.VK_X;
        downleft = KeyEvent.VK_Z;
        left = KeyEvent.VK_A;
        upleft = KeyEvent.VK_Q;
        wait = KeyEvent.VK_S;
        break;
      case QWERTZ:
        up = KeyEvent.VK_W;
        upright = KeyEvent.VK_E;
        right = KeyEvent.VK_D;
        downright = KeyEvent.VK_C;
        down = KeyEvent.VK_X;
        downleft = KeyEvent.VK_Y;
        left = KeyEvent.VK_A;
        upleft = KeyEvent.VK_Q;
        wait = KeyEvent.VK_S;
        break;
    }
  }

  /** Jackson setter for keys configuration */
  @JacksonXmlProperty(localName = "keys")
  public void setKeysConfig(KeysConfig config) {
    if (config != null) {
      // Set layout based on text content
      if (config.layout != null) {
        switch (config.layout) {
          case "azerty":
            setKeys(AZERTY);
            break;
          case "qwerty":
            setKeys(QWERTY);
            break;
          case "qwertz":
            setKeys(QWERTZ);
            break;
          default:
            setKeys(NUMPAD);
        }
      }

      // Set custom keybindings from attributes
      if (config.map != null) {
        map = getKeyCode(config.map);
      }
      if (config.act != null) {
        act = getKeyCode(config.act);
      }
      if (config.magic != null) {
        magic = getKeyCode(config.magic);
      }
      if (config.shoot != null) {
        shoot = getKeyCode(config.shoot);
      }
      if (config.look != null) {
        look = getKeyCode(config.look);
      }
      if (config.talk != null) {
        talk = getKeyCode(config.talk);
      }
      if (config.unmount != null) {
        unmount = getKeyCode(config.unmount);
      }
      if (config.sneak != null) {
        sneak = getKeyCode(config.sneak);
      }
      if (config.journal != null) {
        journal = getKeyCode(config.journal);
      }
    }
  }

  /** Jackson getter for keys configuration */
  @JacksonXmlProperty(localName = "keys")
  public KeysConfig getKeysConfig() {
    KeysConfig config = new KeysConfig();
    // Set layout based on keys field
    switch (keys) {
      case AZERTY:
        config.layout = "azerty";
        break;
      case QWERTY:
        config.layout = "qwerty";
        break;
      case QWERTZ:
        config.layout = "qwertz";
        break;
      default:
        config.layout = "numpad";
    }
    // Note: We don't serialize the individual key bindings as attributes
    // in this getter - they would need to be converted back to VK_ strings
    return config;
  }

  /** Jackson setter for language - loads locale file */
  @JacksonXmlProperty(localName = "lang")
  public void setLang(String language) {
    this.lang = language;
    // Load locale file
    Properties defaults = new Properties();
    try (FileInputStream stream = new FileInputStream("data/locale/locale.en");
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      defaults.load(reader);
    } catch (IOException e) {
      e.printStackTrace();
    }

    strings = new Properties(defaults);
    try (FileInputStream stream = new FileInputStream("data/locale/locale." + language);
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      strings.load(reader);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Jackson getter for language */
  @JacksonXmlProperty(localName = "lang")
  public String getLang() {
    return lang;
  }

  private static int getKeyCode(String code) {
    return switch (code) {
      case "VK_B" -> KeyEvent.VK_B;
      case "VK_F" -> KeyEvent.VK_F;
      case "VK_G" -> KeyEvent.VK_G;
      case "VK_H" -> KeyEvent.VK_H;
      case "VK_I" -> KeyEvent.VK_I;
      case "VK_J" -> KeyEvent.VK_J;
      case "VK_K" -> KeyEvent.VK_K;
      case "VK_L" -> KeyEvent.VK_L;
      case "VK_M" -> KeyEvent.VK_M;
      case "VK_N" -> KeyEvent.VK_N;
      case "VK_O" -> KeyEvent.VK_O;
      case "VK_P" -> KeyEvent.VK_P;
      case "VK_R" -> KeyEvent.VK_R;
      case "VK_T" -> KeyEvent.VK_T;
      case "VK_U" -> KeyEvent.VK_U;
      case "VK_V" -> KeyEvent.VK_V;
      case "VK_SPACE" -> KeyEvent.VK_SPACE;
      default -> 0;
    };
  }

  /**
   * Creates a JDOM Element from this resource using Jackson serialization.
   *
   * @return JDOM Element representation
   */
  public Element toElement() {
    try {
      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(this).toString();
      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize CClient to Element", e);
    }
  }
}
