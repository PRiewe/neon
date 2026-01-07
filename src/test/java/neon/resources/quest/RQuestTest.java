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

package neon.resources.quest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

/** Test RQuest serialization improvements (variables as String, conversation serialization). */
public class RQuestTest {

  @Test
  public void testSimpleQuestParsing() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<quest name=\"test quest\" init=\"1\">\n"
            + "  <dialog>\n"
            + "  </dialog>\n"
            + "</quest>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("test_quest", element);

    assertNotNull(quest);
    assertEquals("test quest", quest.name);
    assertTrue(quest.initial);
    assertFalse(quest.repeat);
    assertEquals(0, quest.frequency);
  }

  @Test
  public void testRepeatQuestParsing() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<repeat name=\"retrieve item\" f=\"5\">\n"
            + "  <dialog>\n"
            + "  </dialog>\n"
            + "</repeat>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("retrieve_item", element);

    assertNotNull(quest);
    assertEquals("retrieve item", quest.name);
    assertTrue(quest.repeat);
    assertEquals(5, quest.frequency);
    assertFalse(quest.initial);
  }

  @Test
  public void testQuestWithVariables() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<repeat name=\"fetch quest\" f=\"5\">\n"
            + "  <objects>\n"
            + "    <item type=\"light\">item</item>\n"
            + "    <npc id=\"trader,merchant\">npc</npc>\n"
            + "  </objects>\n"
            + "  <dialog>\n"
            + "  </dialog>\n"
            + "</repeat>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("fetch_quest", element);

    assertNotNull(quest);
    List<QuestVariable> vars = quest.getVariables();
    assertNotNull(vars);
    assertEquals(2, vars.size());

    // Check first variable (item)
    QuestVariable item = vars.get(0);
    assertEquals("item", item.name);
    assertEquals("item", item.category);
    assertNull(item.id);
    assertEquals("light", item.typeFilter);

    // Check second variable (npc)
    QuestVariable npc = vars.get(1);
    assertEquals("npc", npc.name);
    assertEquals("npc", npc.category);
    assertEquals("trader,merchant", npc.id);
    assertNull(npc.typeFilter);

    // Test backward compatibility helper method
    Element varsElement = quest.getVariablesElement();
    assertNotNull(varsElement);
    assertEquals("objects", varsElement.getName());
    assertEquals(2, varsElement.getChildren().size());
  }

  @Test
  public void testQuestWithConditions() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<quest name=\"conditional quest\">\n"
            + "  <pre>\n"
            + "    <condition>player.level >= 5</condition>\n"
            + "    <condition>!hasCompletedQuest(\"intro\")</condition>\n"
            + "  </pre>\n"
            + "  <dialog>\n"
            + "  </dialog>\n"
            + "</quest>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("conditional_quest", element);

    assertNotNull(quest);
    assertEquals(2, quest.getConditions().size());
    assertTrue(quest.getConditions().contains("player.level >= 5"));
    assertTrue(quest.getConditions().contains("!hasCompletedQuest(\"intro\")"));
  }

  @Test
  public void testQuestWithConversation() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<quest name=\"dialog quest\">\n"
            + "  <dialog>\n"
            + "    <conversation id=\"greeting\">\n"
            + "      <root id=\"hello\">\n"
            + "        <phrase>Hello there!</phrase>\n"
            + "        <answer>Greetings, traveler.</answer>\n"
            + "        <topic id=\"quest\">\n"
            + "          <phrase>Need any help?</phrase>\n"
            + "          <answer>Yes, I have a task for you.</answer>\n"
            + "        </topic>\n"
            + "      </root>\n"
            + "    </conversation>\n"
            + "  </dialog>\n"
            + "</quest>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("dialog_quest", element);

    assertNotNull(quest);
    assertEquals(1, quest.getConversations().size());

    Conversation conv = quest.getConversations().iterator().next();
    assertEquals("greeting", conv.id);
    assertNotNull(conv.getRootTopic());
    assertEquals("hello", conv.getRootTopic().id);
    assertEquals("Greetings, traveler.", conv.getRootTopic().answer);

    // Check child topics
    assertEquals(1, conv.getTopics(conv.getRootTopic()).size());
  }

  @Test
  public void testToElementSimpleQuest() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<quest name=\"simple quest\" init=\"1\">\n"
            + "  <dialog>\n"
            + "  </dialog>\n"
            + "</quest>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("simple_quest", element);
    Element serialized = quest.toElement();

    assertNotNull(serialized);
    assertEquals("quest", serialized.getName());
    assertEquals("simple quest", serialized.getAttributeValue("name"));
    assertEquals("1", serialized.getAttributeValue("init"));
    assertNotNull(serialized.getChild("dialog"));
  }

  @Test
  public void testToElementRepeatQuest() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<repeat name=\"repeatable quest\" f=\"10\">\n"
            + "  <dialog>\n"
            + "  </dialog>\n"
            + "</repeat>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("repeatable_quest", element);
    Element serialized = quest.toElement();

    assertNotNull(serialized);
    assertEquals("repeat", serialized.getName());
    assertEquals("repeatable quest", serialized.getAttributeValue("name"));
    assertEquals("10", serialized.getAttributeValue("f"));
    assertNull(serialized.getAttributeValue("init"));
  }

  @Test
  public void testToElementWithVariables() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<quest name=\"variable quest\">\n"
            + "  <objects>\n"
            + "    <item id=\"sword,axe\">weapon</item>\n"
            + "  </objects>\n"
            + "  <dialog>\n"
            + "  </dialog>\n"
            + "</quest>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("variable_quest", element);
    Element serialized = quest.toElement();

    assertNotNull(serialized);
    Element objects = serialized.getChild("objects");
    assertNotNull(objects);
    assertEquals(1, objects.getChildren().size());
    Element item = objects.getChild("item");
    assertNotNull(item);
    assertEquals("sword,axe", item.getAttributeValue("id"));
    assertEquals("weapon", item.getText());
  }

  @Test
  public void testToElementWithConversation() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<quest name=\"conversation quest\">\n"
            + "  <dialog>\n"
            + "    <conversation id=\"test_conv\">\n"
            + "      <root id=\"root_topic\">\n"
            + "        <pre>player.level > 1</pre>\n"
            + "        <phrase>Test phrase</phrase>\n"
            + "        <answer>Test answer</answer>\n"
            + "        <action>doSomething()</action>\n"
            + "      </root>\n"
            + "    </conversation>\n"
            + "  </dialog>\n"
            + "</quest>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    RQuest quest = new RQuest("conversation_quest", element);
    Element serialized = quest.toElement();

    assertNotNull(serialized);
    Element dialog = serialized.getChild("dialog");
    assertNotNull(dialog);

    Element conversation = dialog.getChild("conversation");
    assertNotNull(conversation);
    assertEquals("test_conv", conversation.getAttributeValue("id"));

    Element root = conversation.getChild("root");
    assertNotNull(root);
    assertEquals("root_topic", root.getAttributeValue("id"));
    assertEquals("player.level > 1", root.getChildText("pre"));
    assertEquals("Test phrase", root.getChildText("phrase"));
    assertEquals("Test answer", root.getChildText("answer"));
    assertEquals("doSomething()", root.getChildText("action"));
  }

  @Test
  public void testRoundTrip() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<repeat name=\"round trip test\" f=\"3\">\n"
            + "  <pre>\n"
            + "    <condition>test condition</condition>\n"
            + "  </pre>\n"
            + "  <objects>\n"
            + "    <item type=\"weapon\">test_item</item>\n"
            + "  </objects>\n"
            + "  <dialog>\n"
            + "    <conversation id=\"test\">\n"
            + "      <root id=\"start\">\n"
            + "        <phrase>Start</phrase>\n"
            + "        <answer>Response</answer>\n"
            + "      </root>\n"
            + "    </conversation>\n"
            + "  </dialog>\n"
            + "</repeat>";

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
    Element element = doc.getRootElement();

    // First parse
    RQuest quest1 = new RQuest("round_trip", element);

    // Serialize
    Element serialized = quest1.toElement();

    // Parse again
    RQuest quest2 = new RQuest("round_trip", serialized);

    // Verify all fields match
    assertEquals(quest1.name, quest2.name);
    assertEquals(quest1.repeat, quest2.repeat);
    assertEquals(quest1.frequency, quest2.frequency);
    assertEquals(quest1.initial, quest2.initial);
    assertEquals(quest1.getConditions().size(), quest2.getConditions().size());
    assertEquals(quest1.getConversations().size(), quest2.getConversations().size());

    // Verify variables preserved
    assertEquals(1, quest2.getVariables().size());
    QuestVariable var = quest2.getVariables().get(0);
    assertEquals("test_item", var.name);
    assertEquals("item", var.category);
    assertNull(var.id);
    assertEquals("weapon", var.typeFilter);
  }

  @Test
  public void testSetVariablesElement() {
    RQuest quest = new RQuest("test");

    Element vars = new Element("objects");
    Element item = new Element("item");
    item.setAttribute("id", "test_item");
    item.setText("item_var");
    vars.addContent(item);

    quest.setVariablesElement(vars);

    // Verify QuestVariable was created correctly
    assertEquals(1, quest.getVariables().size());
    QuestVariable var = quest.getVariables().get(0);
    assertEquals("item_var", var.name);
    assertEquals("item", var.category);
    assertEquals("test_item", var.id);
    assertNull(var.typeFilter);

    // Test round-trip through helper method
    Element retrieved = quest.getVariablesElement();
    assertNotNull(retrieved);
    assertEquals("objects", retrieved.getName());
    assertEquals(1, retrieved.getChildren().size());
    Element retrievedItem = retrieved.getChild("item");
    assertEquals("item_var", retrievedItem.getText());
    assertEquals("test_item", retrievedItem.getAttributeValue("id"));
  }
}
