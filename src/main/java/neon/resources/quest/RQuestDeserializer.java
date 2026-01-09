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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Custom Jackson deserializer for RQuest to handle complex nested structures like variables
 * (objects) and conversations (dialog).
 *
 * @author Peter Riewe
 */
public class RQuestDeserializer extends JsonDeserializer<RQuest> {

  @Override
  public RQuest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    FromXmlParser xmlParser = (FromXmlParser) p;
    JsonNode node = xmlParser.readValueAsTree();

    // Create quest with ID from filename (will be set by caller)
    RQuest quest = new RQuest();

    // Determine if this is a repeat quest based on presence of "f" attribute
    // (Only repeat quests have frequency attribute)
    quest.repeat = node.has("f");

    // Parse attributes
    if (node.has("name")) {
      quest.name = node.get("name").asText();
    }

    if (node.has("init")) {
      quest.initial = true;
    }

    if (node.has("f")) {
      quest.frequency = node.get("f").asInt();
    }

    // Parse <pre> conditions
    if (node.has("pre")) {
      JsonNode preNode = node.get("pre");
      if (preNode.has("condition")) {
        JsonNode conditionNode = preNode.get("condition");
        if (conditionNode.isArray()) {
          for (JsonNode cond : conditionNode) {
            quest.getConditions().add(cond.asText());
          }
        } else {
          quest.getConditions().add(conditionNode.asText());
        }
      }
    }

    // Parse <objects> variables
    if (node.has("objects")) {
      JsonNode objectsNode = node.get("objects");
      Iterator<Map.Entry<String, JsonNode>> fields = objectsNode.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String category = entry.getKey();
        JsonNode varNode = entry.getValue();

        // Handle both single element and array of elements
        List<JsonNode> varNodes = new ArrayList<>();
        if (varNode.isArray()) {
          varNode.forEach(varNodes::add);
        } else {
          varNodes.add(varNode);
        }

        for (JsonNode vNode : varNodes) {
          QuestVariable var = new QuestVariable();
          var.category = category;
          var.name = vNode.asText();
          if (vNode.has("id")) {
            var.id = vNode.get("id").asText();
          }
          if (vNode.has("type")) {
            var.typeFilter = vNode.get("type").asText();
          }
          quest.getVariables().add(var);
        }
      }
    }

    // Parse <dialog> conversations
    if (node.has("dialog")) {
      JsonNode dialogNode = node.get("dialog");
      if (dialogNode.has("conversation")) {
        JsonNode conversationNode = dialogNode.get("conversation");
        List<JsonNode> convNodes = new ArrayList<>();
        if (conversationNode.isArray()) {
          conversationNode.forEach(convNodes::add);
        } else {
          convNodes.add(conversationNode);
        }

        for (JsonNode convNode : convNodes) {
          String convId = convNode.has("id") ? convNode.get("id").asText() : "default";
          Conversation conversation =
              new Conversation(quest.id != null ? quest.id : "unknown", convId);

          // Parse root topic
          if (convNode.has("root")) {
            JsonNode rootNode = convNode.get("root");
            Topic root = parseTopic(rootNode, quest.id != null ? quest.id : "unknown", convId);
            conversation.setRootTopic(root);

            // Parse child topics of root
            parseChildTopics(
                rootNode, conversation, root, quest.id != null ? quest.id : "unknown", convId);
          }

          quest.getConversations().add(conversation);
        }
      }
    }

    return quest;
  }

  /**
   * Parse a topic from a JSON node.
   *
   * @param topicNode JSON node containing topic data
   * @param questId Quest ID
   * @param convId Conversation ID
   * @return Parsed Topic
   */
  private Topic parseTopic(JsonNode topicNode, String questId, String convId) {
    String id = topicNode.has("id") ? topicNode.get("id").asText() : "default";
    String pre = topicNode.has("pre") ? topicNode.get("pre").asText() : null;
    String phrase = topicNode.has("phrase") ? topicNode.get("phrase").asText() : null;
    String answer = topicNode.has("answer") ? topicNode.get("answer").asText() : null;
    String action = topicNode.has("action") ? topicNode.get("action").asText() : null;

    return new Topic(questId, convId, id, pre, phrase, answer, action);
  }

  /**
   * Recursively parse child topics and add them to the conversation.
   *
   * @param parentNode Parent topic JSON node
   * @param conversation Conversation to add topics to
   * @param parentTopic Parent topic object
   * @param questId Quest ID
   * @param convId Conversation ID
   */
  private void parseChildTopics(
      JsonNode parentNode,
      Conversation conversation,
      Topic parentTopic,
      String questId,
      String convId) {
    if (parentNode.has("topic")) {
      JsonNode topicNode = parentNode.get("topic");
      List<JsonNode> topicNodes = new ArrayList<>();
      if (topicNode.isArray()) {
        topicNode.forEach(topicNodes::add);
      } else {
        topicNodes.add(topicNode);
      }

      for (JsonNode childNode : topicNodes) {
        Topic childTopic = parseTopic(childNode, questId, convId);
        conversation.addSubTopic(parentTopic, childTopic);

        // Recursively parse nested topics
        parseChildTopics(childNode, conversation, childTopic, questId, convId);
      }
    }
  }
}
