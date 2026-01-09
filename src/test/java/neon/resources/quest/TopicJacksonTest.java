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
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.resources.quest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Test Jackson XML serialization for Topic resources. */
public class TopicJacksonTest {

  @Test
  public void testSimpleTopicToElement() {
    Topic topic =
        new Topic(
            "quest_001", "conv_001", "topic_1", null, "Hello there!", "Greetings, traveler!", null);

    org.jdom2.Element element = topic.toElement();

    assertNotNull(element);
    assertEquals("topic", element.getName());
    assertEquals("topic_1", element.getAttributeValue("id"));
    assertEquals("Hello there!", element.getChildText("phrase"));
    assertEquals("Greetings, traveler!", element.getChildText("answer"));
    assertNull(element.getChild("pre"));
    assertNull(element.getChild("action"));
  }

  @Test
  public void testTopicWithAllElements() {
    Topic topic =
        new Topic(
            "quest_002",
            "conv_002",
            "topic_2",
            "player.hasItem('sword')",
            "I have a sword",
            "Impressive weapon!",
            "player.addGold(100)");

    org.jdom2.Element element = topic.toElement();

    assertNotNull(element);
    assertEquals("topic", element.getName());
    assertEquals("topic_2", element.getAttributeValue("id"));
    assertEquals("I have a sword", element.getChildText("phrase"));
    assertEquals("player.hasItem('sword')", element.getChildText("pre"));
    assertEquals("Impressive weapon!", element.getChildText("answer"));
    assertEquals("player.addGold(100)", element.getChildText("action"));
  }

  @Test
  public void testTopicWithConditionOnly() {
    Topic topic =
        new Topic(
            "quest_003",
            "conv_003",
            "topic_3",
            "player.level >= 5",
            "What quests do you have?",
            null,
            null);

    org.jdom2.Element element = topic.toElement();

    assertNotNull(element);
    assertEquals("topic_3", element.getAttributeValue("id"));
    assertEquals("What quests do you have?", element.getChildText("phrase"));
    assertEquals("player.level >= 5", element.getChildText("pre"));
    assertNull(element.getChild("answer"));
    assertNull(element.getChild("action"));
  }

  @Test
  public void testTopicWithActionOnly() {
    Topic topic =
        new Topic(
            "quest_004", "conv_004", "topic_4", null, "Goodbye", "Farewell!", "quest.complete()");

    org.jdom2.Element element = topic.toElement();

    assertNotNull(element);
    assertEquals("topic_4", element.getAttributeValue("id"));
    assertEquals("Goodbye", element.getChildText("phrase"));
    assertEquals("Farewell!", element.getChildText("answer"));
    assertNull(element.getChild("pre"));
    assertEquals("quest.complete()", element.getChildText("action"));
  }

  @Test
  public void testTopicPhraseRequired() {
    // Phrase is required, others are optional
    Topic topic = new Topic("quest_005", "conv_005", "topic_5", null, "Just a phrase", null, null);

    org.jdom2.Element element = topic.toElement();

    assertNotNull(element);
    assertEquals("topic_5", element.getAttributeValue("id"));
    assertEquals("Just a phrase", element.getChildText("phrase"));
  }
}
