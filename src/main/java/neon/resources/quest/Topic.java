/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012-2013 - Maarten Driesen
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import neon.systems.files.JacksonMapper;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 * A single topic in a conversation branch.
 *
 * @author mdriesen
 */
@JacksonXmlRootElement(localName = "topic")
public class Topic implements Serializable {
  /** The resource ID of the quest this topic belongs to. */
  public final String questID;

  public final String conversationID;

  @JacksonXmlProperty(isAttribute = true)
  public final String id; // unique id string

  @JacksonXmlProperty(localName = "phrase")
  @JsonProperty(required = false)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String phrase; // what the player says

  @JacksonXmlProperty(localName = "pre")
  @JsonProperty(required = false)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String condition; // script conditions

  @JacksonXmlProperty(localName = "answer")
  @JsonProperty(required = false)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String answer; // NPC's response

  @JacksonXmlProperty(localName = "action")
  @JsonProperty(required = false)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String action; // script to execute afterwards

  /**
   * Initializes a topic from a JDOM {@code Element}.
   *
   * @param topic
   */
  public Topic(String questID, String conversationID, Element topic) {
    this.questID = questID;
    this.conversationID = conversationID;

    // id and phrase must always exist
    id = topic.getAttributeValue("id");
    phrase = topic.getChildText("phrase");

    if (topic.getChild("pre") != null) {
      condition = topic.getChildText("pre");
    }
    if (topic.getChild("answer") != null) {
      answer = topic.getChildText("answer");
    }
    if (topic.getChild("action") != null) {
      action = topic.getChildText("action");
    }
  }

  /**
   * Initializes a new topic.
   *
   * @param questID the resource ID of the quest this topic belongs to
   * @param id a unique ID for this topic
   * @param pre script preconditions
   * @param phrase the phrase the player says
   * @param answer the NPC's response
   * @param action script that is executed after the answer
   */
  public Topic(
      String questID,
      String conversationID,
      String id,
      String pre,
      String phrase,
      String answer,
      String action) {
    this.questID = questID;
    this.conversationID = conversationID;
    this.id = id;
    this.phrase = phrase;
    this.condition = pre;
    this.answer = answer;
    this.action = action;
  }

  /**
   * @return a JDOM {@code Element} describing this topic using Jackson serialization
   */
  public Element toElement() {
    try {
      JacksonMapper mapper = new JacksonMapper();
      String xml = mapper.toXml(this).toString();
      return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes())).getRootElement();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize Topic to Element", e);
    }
  }
}
