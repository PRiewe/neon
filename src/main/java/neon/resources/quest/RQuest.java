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

package neon.resources.quest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import neon.resources.RData;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * A resource representing a quest.
 *
 * @author mdriesen
 */
@JacksonXmlRootElement // Accepts quest or repeat element names
public class RQuest extends RData {
  // Store quest variables as XML string (Jackson-compatible)
  // Will be handled by custom deserializer
  @JsonIgnore public String variablesXml;

  @JsonIgnore public int frequency;

  // repeat quests can run more than once
  // Determined by element name (quest vs repeat)
  @JsonIgnore public boolean repeat = false;

  // initial quest is added as soon as game starts
  @JsonIgnore public boolean initial = false;

  @JsonIgnore private ArrayList<String> conditions = new ArrayList<String>();

  @JsonIgnore private ArrayList<Conversation> conversations = new ArrayList<Conversation>();

  public RQuest(String id, Element properties, String... path) {
    super(id, path);
    try {
      name = properties.getAttributeValue("name");
      if (properties.getChild("pre") != null) {
        for (Element condition : properties.getChild("pre").getChildren()) {
          conditions.add(condition.getTextTrim());
        }
      }
      if (properties.getChild("objects") != null) {
        // Convert Element to XML string for Jackson compatibility
        Element vars = properties.getChild("objects");
        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        variablesXml = outputter.outputString(vars);
      }
      repeat = properties.getName().equals("repeat");
      if (repeat) {
        frequency = Integer.parseInt(properties.getAttributeValue("f"));
      }
      initial = (properties.getAttribute("init") != null);

      if (properties.getChild("dialog") != null) {
        initDialog(properties.getChild("dialog"));
      }
    } catch (RuntimeException re) {
      System.out.printf("%s%n%s", re, properties);
    }
  }

  public RQuest(String id, String... path) {
    super(id, path);
  }

  /**
   * Helper method to get variables as a JDOM Element (backward compatibility).
   *
   * @return Element representation of variables, or null if no variables
   */
  public Element getVariablesElement() {
    if (variablesXml == null || variablesXml.isEmpty()) {
      return null;
    }
    try {
      return new SAXBuilder()
          .build(new ByteArrayInputStream(variablesXml.getBytes()))
          .getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse variables XML: " + variablesXml, e);
    }
  }

  /**
   * Helper method to set variables from a JDOM Element (backward compatibility).
   *
   * @param vars Element to convert to XML string
   */
  public void setVariablesElement(Element vars) {
    if (vars == null) {
      variablesXml = null;
    } else {
      XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
      variablesXml = outputter.outputString(vars);
    }
  }

  private void initDialog(Element dialog) {
    for (Element ce : dialog.getChildren("conversation")) {
      Conversation conversation = new Conversation(id, ce.getAttributeValue("id"));
      Topic root = new Topic(id, conversation.id, ce.getChild("root"));
      conversation.setRootTopic(root);
      for (Element te : ce.getChild("root").getChildren("topic")) {
        initTopic(conversation, root, te);
      }
      conversations.add(conversation);
    }
  }

  private void initTopic(Conversation conversation, Topic parent, Element te) {
    Topic topic = new Topic(id, conversation.id, te);
    conversation.addSubTopic(parent, topic);
    // recursively add all child topics
    for (Element ce : te.getChildren("topic")) {
      initTopic(conversation, topic, ce);
    }
  }

  /**
   * @return all dialog topics in this quest
   */
  public Collection<Conversation> getConversations() {
    return conversations;
  }

  /**
   * @return all preconditions for this quest
   */
  public Collection<String> getConditions() {
    return conditions;
  }

  public Element toElement() {
    Element quest = new Element(repeat ? "repeat" : "quest");
    quest.setAttribute("name", name != null ? name : id);
    if (initial) {
      quest.setAttribute("init", "1");
    }
    if (repeat && frequency > 0) {
      quest.setAttribute("f", Integer.toString(frequency));
    }

    if (!conditions.isEmpty()) {
      Element pre = new Element("pre");
      for (String condition : conditions) {
        pre.addContent(new Element("condition").setText(condition));
      }
      quest.addContent(pre);
    }

    // Convert XML string back to Element and clone it (to detach from its document)
    Element vars = getVariablesElement();
    if (vars != null) {
      quest.addContent(vars.clone());
    }

    Element dialog = new Element("dialog");
    for (Conversation conversation : conversations) {
      dialog.addContent(serializeConversation(conversation));
    }
    quest.addContent(dialog);

    return quest;
  }

  private Element serializeConversation(Conversation conversation) {
    Element conv = new Element("conversation");
    conv.setAttribute("id", conversation.id);

    Topic root = conversation.getRootTopic();
    Element rootEl = new Element("root");
    rootEl.setAttribute("id", root.id);

    // Add root topic content in proper order
    if (root.condition != null) {
      rootEl.addContent(new Element("pre").setText(root.condition));
    }
    if (root.phrase != null) {
      rootEl.addContent(new Element("phrase").setText(root.phrase));
    }
    if (root.answer != null) {
      rootEl.addContent(new Element("answer").setText(root.answer));
    }
    if (root.action != null) {
      rootEl.addContent(new Element("action").setText(root.action));
    }

    // Recursively add child topics
    for (Topic child : conversation.getTopics(root)) {
      rootEl.addContent(serializeTopicTree(conversation, child));
    }

    conv.addContent(rootEl);
    return conv;
  }

  private Element serializeTopicTree(Conversation conversation, Topic topic) {
    Element topicEl = topic.toElement(); // Use Topic's toElement()

    // Recursively add children
    for (Topic child : conversation.getTopics(topic)) {
      topicEl.addContent(serializeTopicTree(conversation, child));
    }

    return topicEl;
  }
}
