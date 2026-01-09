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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import neon.systems.files.JacksonMapper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 * A resource that keeps track of all configuration settings in neon.ini.xml.
 *
 * @author mdriesen
 */
@JacksonXmlRootElement(localName = "root")
public class CServer extends Resource {
  @JsonIgnore private ArrayList<String> mods = new ArrayList<String>();

  @JacksonXmlProperty(localName = "log")
  private String log = "FINEST";

  @JsonIgnore private boolean gThread = true;

  //	private boolean audio = false;

  @JacksonXmlProperty(localName = "ai")
  private int ai = 20;

  /** Inner class for file entries */
  public static class FileEntry {
    @JacksonXmlText public String value;
  }

  /** Inner class for threads configuration */
  public static class Threads {
    @JacksonXmlProperty(isAttribute = true, localName = "generate")
    public String generate;
  }

  // No-arg constructor for Jackson deserialization
  public CServer() {
    super("ini");
  }

  // Keep JDOM constructor for backward compatibility during migration
  public CServer(String... path) {
    super("ini", path);

    // load file
    Document doc = new Document();
    try (FileInputStream in = new FileInputStream(path[0])) {
      doc = new SAXBuilder().build(in);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Element root = doc.getRootElement();

    // mods
    Element files = root.getChild("files");
    for (Element file : files.getChildren("file")) {
      mods.add(file.getText());
    }

    // logging
    log = root.getChildText("log").toUpperCase();

    // map generation thread
    gThread = root.getChild("threads").getAttributeValue("generate").equals("on");

    // ai range
    ai = Integer.parseInt(root.getChildText("ai"));
  }

  @Override
  public void load() {} // loading not possible

  @Override
  public void unload() {} // unloading not possible

  public String getLogLevel() {
    return log;
  }

  public ArrayList<String> getMods() {
    return mods;
  }

  public boolean isMapThreaded() {
    return gThread;
  }

  public int getAIRange() {
    return ai;
  }

  /** Jackson setter for mods - converts list to ArrayList */
  @JacksonXmlElementWrapper(localName = "files")
  @JacksonXmlProperty(localName = "file")
  public void setFileList(List<FileEntry> fileList) {
    if (fileList != null) {
      for (FileEntry entry : fileList) {
        mods.add(entry.value);
      }
    }
  }

  /** Jackson getter for mods - converts ArrayList to list */
  @JacksonXmlElementWrapper(localName = "files")
  @JacksonXmlProperty(localName = "file")
  public List<FileEntry> getFileList() {
    List<FileEntry> list = new ArrayList<>();
    for (String mod : mods) {
      FileEntry fe = new FileEntry();
      fe.value = mod;
      list.add(fe);
    }
    return list;
  }

  /** Jackson setter for threads configuration */
  @JacksonXmlProperty(localName = "threads")
  public void setThreads(Threads threads) {
    if (threads != null) {
      gThread = threads.generate.equals("on");
    }
  }

  /** Jackson getter for threads configuration */
  @JacksonXmlProperty(localName = "threads")
  public Threads getThreads() {
    Threads t = new Threads();
    t.generate = gThread ? "on" : "off";
    return t;
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
      throw new RuntimeException("Failed to serialize CServer to Element", e);
    }
  }
}
