package neon.ui.graphics.shapes;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for JVSvgShape serialization with MVStore. */
class JVSvgShapeSerializationTest {

  private MVStore testDb;
  private MVMap<String, JVSvgShape> shapeMap;

  @BeforeEach
  void setUp() {
    // Create an in-memory MVStore for testing
    testDb = MVStore.open(null);
    shapeMap = testDb.openMap("shapes");
  }

  @AfterEach
  void tearDown() {
    if (testDb != null && !testDb.isClosed()) {
      testDb.close();
    }
  }

  @Test
  void testSerializeAndDeserializeCircle() {
    // Create a simple SVG shape
    String svgContent = "<circle cx=\"3\" cy=\"3\" r=\"3\" fill=\"blue\"/>";
    JVSvgShape originalShape = new JVSvgShape(svgContent);
    originalShape.setX(10);
    originalShape.setY(20);

    // Store in MVStore
    shapeMap.put("test-circle", originalShape);
    testDb.commit();

    // Retrieve from MVStore
    JVSvgShape deserializedShape = shapeMap.get("test-circle");

    // Verify the shape was properly deserialized
    assertNotNull(deserializedShape, "Deserialized shape should not be null");

    // Check bounds (circle with radius 3 should have diameter 6)
    Rectangle bounds = deserializedShape.getBounds();
    assertEquals(10, bounds.x, "X coordinate should be preserved");
    assertEquals(20, bounds.y, "Y coordinate should be preserved");
    assertEquals(6, bounds.width, "Width should be preserved");
    assertEquals(6, bounds.height, "Height should be preserved");
  }

  @Test
  void testSerializeAndDeserializeComplexSvg() {
    // Create a more complex SVG shape
    String svgContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\">"
            + "<rect x=\"0\" y=\"0\" width=\"20\" height=\"20\" fill=\"red\"/>"
            + "</svg>";
    JVSvgShape originalShape = new JVSvgShape(svgContent);
    originalShape.setX(5);
    originalShape.setY(15);

    // Store in MVStore
    shapeMap.put("test-rect", originalShape);
    testDb.commit();

    // Retrieve from MVStore
    JVSvgShape deserializedShape = shapeMap.get("test-rect");

    // Verify the shape was properly deserialized
    assertNotNull(deserializedShape, "Deserialized shape should not be null");

    Rectangle bounds = deserializedShape.getBounds();
    assertEquals(5, bounds.x, "X coordinate should be preserved");
    assertEquals(15, bounds.y, "Y coordinate should be preserved");
    assertEquals(20, bounds.width, "Width should be preserved");
    assertEquals(20, bounds.height, "Height should be preserved");
  }

  @Test
  void testMultipleShapesInMap() {
    // Create multiple shapes
    JVSvgShape shape1 = new JVSvgShape("<circle cx=\"5\" cy=\"5\" r=\"5\" fill=\"blue\"/>");
    shape1.setX(0);
    shape1.setY(0);

    JVSvgShape shape2 = new JVSvgShape("<circle cx=\"3\" cy=\"3\" r=\"3\" fill=\"red\"/>");
    shape2.setX(10);
    shape2.setY(10);

    // Store multiple shapes
    shapeMap.put("shape1", shape1);
    shapeMap.put("shape2", shape2);
    testDb.commit();

    // Verify both shapes can be retrieved
    JVSvgShape retrieved1 = shapeMap.get("shape1");
    JVSvgShape retrieved2 = shapeMap.get("shape2");

    assertNotNull(retrieved1);
    assertNotNull(retrieved2);

    assertEquals(0, retrieved1.getBounds().x);
    assertEquals(10, retrieved2.getBounds().x);
  }

  @Test
  void testPersistenceAcrossStoreReopening() throws Exception {
    // Create a temporary file-backed MVStore
    java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("mvstore-test-", ".dat");

    try {
      // First session: create and store a shape
      {
        MVStore db = MVStore.open(tempFile.toString());
        MVMap<String, JVSvgShape> map = db.openMap("shapes");

        JVSvgShape shape = new JVSvgShape("<circle cx=\"7\" cy=\"7\" r=\"7\" fill=\"green\"/>");
        shape.setX(25);
        shape.setY(35);

        map.put("persistent-shape", shape);
        db.commit();
        db.close();
      }

      // Second session: reopen and retrieve the shape
      {
        MVStore db = MVStore.open(tempFile.toString());
        MVMap<String, JVSvgShape> map = db.openMap("shapes");

        JVSvgShape retrievedShape = map.get("persistent-shape");
        assertNotNull(retrievedShape, "Shape should persist across store reopening");

        Rectangle bounds = retrievedShape.getBounds();
        assertEquals(25, bounds.x, "X coordinate should persist");
        assertEquals(35, bounds.y, "Y coordinate should persist");
        assertEquals(14, bounds.width, "Width should persist");
        assertEquals(14, bounds.height, "Height should persist");

        db.close();
      }
    } finally {
      // Clean up temp file
      java.nio.file.Files.deleteIfExists(tempFile);
    }
  }
}
