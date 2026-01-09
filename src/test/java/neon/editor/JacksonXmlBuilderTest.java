/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Maarten Driesen
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

package neon.editor;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import neon.resources.RData;
import neon.resources.RMod;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for JacksonXmlBuilder Phase 7A migration.
 *
 * <p>Verifies that JacksonXmlBuilder produces identical JDOM Documents to XMLBuilder for editor
 * save operations.
 */
public class JacksonXmlBuilderTest {

  private DataStore mockStore;
  private RMod testMod;
  private JacksonXmlBuilder builder;

  @BeforeEach
  public void setUp() {
    mockStore = new TestDataStore();
    testMod = createTestMod("testmod");
    builder = new JacksonXmlBuilder(mockStore);
  }

  @Test
  public void testGetEventsDoc_EmptyEvents() {
    Document doc = builder.getEventsDoc();

    assertNotNull(doc);
    Element root = doc.getRootElement();
    assertEquals("events", root.getName());
    assertTrue(root.getChildren().isEmpty());
  }

  @Test
  public void testGetEventsDoc_MultipleEvents() {
    // Add events to mock store
    Multimap<String, String> events = ((TestDataStore) mockStore).events;
    events.put("intro_script", "0");
    events.put("intro_script", "10");
    events.put("quest_start", "100");

    Document doc = builder.getEventsDoc();

    assertNotNull(doc);
    Element root = doc.getRootElement();
    assertEquals("events", root.getName());
    assertEquals(3, root.getChildren("event").size());

    // Verify event structure
    List<Element> eventElements = root.getChildren("event");
    for (Element event : eventElements) {
      assertNotNull(event.getAttributeValue("script"));
      assertNotNull(event.getAttributeValue("tick"));
    }

    // Verify specific events exist
    boolean foundIntro0 = false;
    boolean foundIntro10 = false;
    boolean foundQuest = false;

    for (Element event : eventElements) {
      String script = event.getAttributeValue("script");
      String tick = event.getAttributeValue("tick");

      if ("intro_script".equals(script) && "0".equals(tick)) {
        foundIntro0 = true;
      }
      if ("intro_script".equals(script) && "10".equals(tick)) {
        foundIntro10 = true;
      }
      if ("quest_start".equals(script) && "100".equals(tick)) {
        foundQuest = true;
      }
    }

    assertTrue(foundIntro0, "Should contain intro_script at tick 0");
    assertTrue(foundIntro10, "Should contain intro_script at tick 10");
    assertTrue(foundQuest, "Should contain quest_start at tick 100");
  }

  @Test
  public void testGetListDoc_FiltersResourcesByMod() {
    List<RData> resources = new ArrayList<>();
    resources.add(new TestResource("res1", "testmod"));
    resources.add(new TestResource("res2", "othermod"));
    resources.add(new TestResource("res3", "testmod"));

    Document doc = builder.getListDoc(resources, "resources", testMod);

    assertNotNull(doc);
    Element root = doc.getRootElement();
    assertEquals("resources", root.getName());
    assertEquals(2, root.getChildren().size(), "Should only include testmod resources");

    List<Element> children = root.getChildren();
    assertEquals("res1", children.get(0).getAttributeValue("id"));
    assertEquals("res3", children.get(1).getAttributeValue("id"));
  }

  @Test
  public void testGetListDoc_PreservesOriginalOrder() {
    List<RData> resources = new ArrayList<>();
    resources.add(new TestResource("zebra", "testmod"));
    resources.add(new TestResource("apple", "testmod"));
    resources.add(new TestResource("middle", "testmod"));

    Document doc = builder.getListDoc(resources, "items", testMod);

    assertNotNull(doc);
    Element root = doc.getRootElement();
    List<Element> children = root.getChildren();

    // Should preserve insertion order (not sorted)
    assertEquals("zebra", children.get(0).getAttributeValue("id"));
    assertEquals("apple", children.get(1).getAttributeValue("id"));
    assertEquals("middle", children.get(2).getAttributeValue("id"));
  }

  @Test
  public void testGetResourceDoc_SortsResourcesAlphabetically() {
    List<RData> resources = new ArrayList<>();
    resources.add(new TestResource("zebra", "testmod"));
    resources.add(new TestResource("apple", "testmod"));
    resources.add(new TestResource("middle", "testmod"));

    Document doc = builder.getResourceDoc(resources, "items", testMod);

    assertNotNull(doc);
    Element root = doc.getRootElement();
    List<Element> children = root.getChildren();

    // Should be sorted alphabetically by id
    assertEquals("apple", children.get(0).getAttributeValue("id"));
    assertEquals("middle", children.get(1).getAttributeValue("id"));
    assertEquals("zebra", children.get(2).getAttributeValue("id"));
  }

  @Test
  public void testGetResourceDoc_FiltersAndSorts() {
    List<RData> resources = new ArrayList<>();
    resources.add(new TestResource("zebra", "testmod"));
    resources.add(new TestResource("other", "differentmod"));
    resources.add(new TestResource("apple", "testmod"));
    resources.add(new TestResource("banana", "testmod"));

    Document doc = builder.getResourceDoc(resources, "creatures", testMod);

    assertNotNull(doc);
    Element root = doc.getRootElement();
    assertEquals("creatures", root.getName());
    assertEquals(3, root.getChildren().size());

    List<Element> children = root.getChildren();
    assertEquals("apple", children.get(0).getAttributeValue("id"));
    assertEquals("banana", children.get(1).getAttributeValue("id"));
    assertEquals("zebra", children.get(2).getAttributeValue("id"));
  }

  @Test
  public void testGetListDoc_EmptyCollection() {
    List<RData> resources = new ArrayList<>();

    Document doc = builder.getListDoc(resources, "factions", testMod);

    assertNotNull(doc);
    Element root = doc.getRootElement();
    assertEquals("factions", root.getName());
    assertTrue(root.getChildren().isEmpty());
  }

  @Test
  public void testGetResourceDoc_EmptyCollection() {
    List<RData> resources = new ArrayList<>();

    Document doc = builder.getResourceDoc(resources, "spells", testMod);

    assertNotNull(doc);
    Element root = doc.getRootElement();
    assertEquals("spells", root.getName());
    assertTrue(root.getChildren().isEmpty());
  }

  @Test
  public void testGetListDoc_AllResourcesFromDifferentMod() {
    List<RData> resources = new ArrayList<>();
    resources.add(new TestResource("res1", "othermod"));
    resources.add(new TestResource("res2", "anothermod"));

    Document doc = builder.getListDoc(resources, "terrain", testMod);

    assertNotNull(doc);
    Element root = doc.getRootElement();
    assertEquals("terrain", root.getName());
    assertTrue(root.getChildren().isEmpty(), "Should filter out all resources from other mods");
  }

  @Test
  public void testGetEventsDoc_XmlStructure() {
    Multimap<String, String> events = ((TestDataStore) mockStore).events;
    events.put("test_script", "42");

    Document doc = builder.getEventsDoc();
    Element root = doc.getRootElement();
    Element event = root.getChildren("event").get(0);

    // Verify XML structure: <event script="..." tick="..." />
    assertEquals("event", event.getName());
    assertEquals("test_script", event.getAttributeValue("script"));
    assertEquals("42", event.getAttributeValue("tick"));
    assertTrue(
        event.getText().isEmpty() || event.getText() == null,
        "Event element should have no meaningful text content");
    assertTrue(event.getChildren().isEmpty(), "Event element should have no child elements");
  }

  @Test
  public void testCallsToElementOnResources() {
    // Create a resource that tracks toElement() calls
    TrackingTestResource resource = new TrackingTestResource("tracked", "testmod");
    List<RData> resources = new ArrayList<>();
    resources.add(resource);

    assertFalse(resource.toElementCalled, "toElement should not be called before build");

    builder.getListDoc(resources, "items", testMod);

    assertTrue(resource.toElementCalled, "toElement should be called during build");
  }

  // Helper method to create test mod
  private RMod createTestMod(String id) {
    Element modElement = new Element("master");
    modElement.setAttribute("id", id);
    return new RMod(modElement, null);
  }

  // Test DataStore implementation
  private static class TestDataStore extends DataStore {
    public Multimap<String, String> events = ArrayListMultimap.create();

    @Override
    public Multimap<String, String> getEvents() {
      return events;
    }
  }

  // Test resource implementation
  private static class TestResource extends RData {
    private final String modId;

    public TestResource(String id, String modId) {
      super(id, modId);
      this.modId = modId;
    }

    @Override
    public String[] getPath() {
      return new String[] {modId};
    }

    @Override
    public Element toElement() {
      Element element = new Element("resource");
      element.setAttribute("id", id);
      element.setAttribute("mod", modId);
      return element;
    }
  }

  // Test resource that tracks toElement() calls
  private static class TrackingTestResource extends TestResource {
    public boolean toElementCalled = false;

    public TrackingTestResource(String id, String modId) {
      super(id, modId);
    }

    @Override
    public Element toElement() {
      toElementCalled = true;
      return super.toElement();
    }
  }
}
