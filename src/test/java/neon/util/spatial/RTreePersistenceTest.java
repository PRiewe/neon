package neon.util.spatial;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import neon.test.MapDbTestHelper;
import neon.test.PerformanceHarness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;

/**
 * Tests for RTree spatial index persistence through MapDb.
 *
 * <p>Verifies that RTree correctly persists spatial data to MapDb and can reconstruct its index
 * from persisted data. This is critical for map loading performance.
 */
class RTreePersistenceTest {

  private DB testDb;

  @BeforeEach
  void setUp() {
    testDb = MapDbTestHelper.createInMemoryDB();
  }

  @AfterEach
  void tearDown() {
    MapDbTestHelper.cleanup(testDb);
  }

  @Test
  void testInMemoryRTreeBasicOperations() {
    RTree<TestItem> tree = new RTree<>(10, 2);

    TestItem item1 = new TestItem("item1");
    tree.insert(item1, new Rectangle(0, 0, 10, 10));

    assertEquals(1, tree.size());
    assertTrue(tree.getElements().contains(item1));
  }

  @Test
  void testMapDbRTreeBasicOperations() {
    RTree<TestItem> tree = new RTree<>(10, 2, testDb, "test-tree");

    TestItem item1 = new TestItem("item1");
    tree.insert(item1, new Rectangle(0, 0, 10, 10));

    testDb.commit();

    assertEquals(1, tree.size());
    assertTrue(tree.getElements().contains(item1));
  }

  @Test
  void testMapDbRTreeReconstructsFromPersistedData() {
    // Create tree and insert items
    RTree<TestItem> tree1 = new RTree<>(10, 2, testDb, "persistent-tree");

    for (int i = 0; i < 20; i++) {
      TestItem item = new TestItem("item-" + i);
      tree1.insert(item, new Rectangle(i * 10, i * 10, 10, 10));
    }

    testDb.commit();

    // Create new tree with same name - should reconstruct from persisted data
    RTree<TestItem> tree2 = new RTree<>(10, 2, testDb, "persistent-tree");

    assertEquals(20, tree2.size());

    // Verify spatial queries work on reconstructed tree
    ArrayList<TestItem> found = tree2.getElements(new Rectangle(0, 0, 50, 50));
    assertTrue(found.size() >= 5, "Should find at least 5 items in query region");
  }

  @Test
  void testPointQueriesAfterPersistence() {
    RTree<TestItem> tree = new RTree<>(10, 2, testDb, "point-query-tree");

    // Insert items at specific locations
    TestItem item1 = new TestItem("at-origin");
    TestItem item2 = new TestItem("at-50-50");
    TestItem item3 = new TestItem("at-100-100");

    tree.insert(item1, new Rectangle(0, 0, 10, 10));
    tree.insert(item2, new Rectangle(50, 50, 10, 10));
    tree.insert(item3, new Rectangle(100, 100, 10, 10));

    testDb.commit();

    // Query for point in first item
    ArrayList<TestItem> found = tree.getElements(new Point(5, 5));
    assertEquals(1, found.size());
    assertEquals("at-origin", found.get(0).name);

    // Query for point in second item
    found = tree.getElements(new Point(55, 55));
    assertEquals(1, found.size());
    assertEquals("at-50-50", found.get(0).name);

    // Query for point outside all items
    found = tree.getElements(new Point(200, 200));
    assertEquals(0, found.size());
  }

  @Test
  void testRangeQueriesAfterPersistence() {
    RTree<TestItem> tree = new RTree<>(10, 2, testDb, "range-query-tree");

    // Create a 10x10 grid of items
    for (int y = 0; y < 10; y++) {
      for (int x = 0; x < 10; x++) {
        TestItem item = new TestItem("item-" + x + "-" + y);
        tree.insert(item, new Rectangle(x * 10, y * 10, 10, 10));
      }
    }

    testDb.commit();

    // Query for items in a specific region
    Rectangle queryRegion = new Rectangle(20, 20, 30, 30);
    ArrayList<TestItem> found = tree.getElements(queryRegion);

    // Should find items at (2,2), (2,3), (2,4), (3,2), (3,3), (3,4), (4,2), (4,3), (4,4)
    // which is 3x3 = 9 items
    assertTrue(found.size() >= 9, "Should find at least 9 items in 3x3 region");
  }

  @Test
  void testLargeDatasetPersistence() {
    int itemCount = 100;
    RTree<TestItem> tree = new RTree<>(10, 2, testDb, "large-tree");

    // Insert 100 items
    for (int i = 0; i < itemCount; i++) {
      int x = (i % 10) * 10;
      int y = (i / 10) * 10;
      TestItem item = new TestItem("item-" + i);
      tree.insert(item, new Rectangle(x, y, 10, 10));
    }

    testDb.commit();

    // Create new tree - should reconstruct all items
    RTree<TestItem> tree2 = new RTree<>(10, 2, testDb, "large-tree");

    assertEquals(itemCount, tree2.size());

    // Verify all items are queryable
    ArrayList<TestItem> all = tree2.getElements(new Rectangle(0, 0, 1000, 1000));
    assertEquals(itemCount, all.size());
  }

  @Test
  void testMultipleTreesSameDb() {
    // Create two independent trees in the same DB
    RTree<TestItem> tree1 = new RTree<>(10, 2, testDb, "tree1");
    RTree<TestItem> tree2 = new RTree<>(10, 2, testDb, "tree2");

    tree1.insert(new TestItem("tree1-item"), new Rectangle(0, 0, 10, 10));
    tree2.insert(new TestItem("tree2-item"), new Rectangle(50, 50, 10, 10));

    testDb.commit();

    assertEquals(1, tree1.size());
    assertEquals(1, tree2.size());

    // Verify trees are independent
    ArrayList<TestItem> tree1Items = tree1.getElements(new Rectangle(0, 0, 100, 100));
    assertEquals(1, tree1Items.size());
    assertEquals("tree1-item", tree1Items.get(0).name);

    ArrayList<TestItem> tree2Items = tree2.getElements(new Rectangle(0, 0, 100, 100));
    assertEquals(1, tree2Items.size());
    assertEquals("tree2-item", tree2Items.get(0).name);
  }

  @Test
  void testInMemoryVsMapDbPerformance() throws Exception {
    int itemCount = 100;

    // Test in-memory performance
    PerformanceHarness.MeasuredResult<RTree<TestItem>> inMemoryResult =
        PerformanceHarness.measure(
            () -> {
              RTree<TestItem> tree = new RTree<>(10, 2);
              for (int i = 0; i < itemCount; i++) {
                tree.insert(new TestItem("item-" + i), new Rectangle(i * 5, i * 5, 10, 10));
              }
              return tree;
            });

    // Test MapDb performance
    PerformanceHarness.MeasuredResult<RTree<TestItem>> mapDbResult =
        PerformanceHarness.measure(
            () -> {
              RTree<TestItem> tree = new RTree<>(10, 2, testDb, "perf-tree");
              for (int i = 0; i < itemCount; i++) {
                tree.insert(new TestItem("item-" + i), new Rectangle(i * 5, i * 5, 10, 10));
              }
              testDb.commit();
              return tree;
            });

    System.out.printf(
        "[PERF] In-memory RTree insert %d items: %d ms (%d ns)%n",
        itemCount, inMemoryResult.getDurationMillis(), inMemoryResult.getDurationNanos());

    System.out.printf(
        "[PERF] MapDb RTree insert %d items: %d ms (%d ns)%n",
        itemCount, mapDbResult.getDurationMillis(), mapDbResult.getDurationNanos());

    assertEquals(itemCount, inMemoryResult.getResult().size());
    assertEquals(itemCount, mapDbResult.getResult().size());
  }

  @Test
  void testSpatialQueryPerformance() throws Exception {
    // Create tree with 200 items
    RTree<TestItem> tree = new RTree<>(10, 2, testDb, "query-perf-tree");

    for (int i = 0; i < 200; i++) {
      int x = (i % 20) * 10;
      int y = (i / 20) * 10;
      tree.insert(new TestItem("item-" + i), new Rectangle(x, y, 10, 10));
    }

    testDb.commit();

    // Measure query performance
    Rectangle queryRegion = new Rectangle(50, 50, 100, 100);

    PerformanceHarness.MeasuredResult<ArrayList<TestItem>> result =
        PerformanceHarness.measure(() -> tree.getElements(queryRegion));

    System.out.printf(
        "[PERF] RTree spatial query (200 items): %d ms (%d ns), found %d items%n",
        result.getDurationMillis(), result.getDurationNanos(), result.getResult().size());

    assertTrue(result.getResult().size() > 0, "Should find items in query region");
    assertTrue(result.getDurationMillis() < 100, "Spatial query should complete within 100ms");
  }

  @Test
  void testBoundingBoxPersistence() {
    RTree<TestItem> tree = new RTree<>(10, 2, testDb, "bbox-tree");

    // Insert items with specific bounds
    tree.insert(new TestItem("small"), new Rectangle(0, 0, 5, 5));
    tree.insert(new TestItem("large"), new Rectangle(10, 10, 50, 50));
    tree.insert(new TestItem("tall"), new Rectangle(70, 0, 10, 100));

    testDb.commit();

    // Reconstruct tree
    RTree<TestItem> tree2 = new RTree<>(10, 2, testDb, "bbox-tree");

    // Verify bounding boxes are preserved by testing queries
    ArrayList<TestItem> found = tree2.getElements(new Rectangle(0, 0, 3, 3));
    assertEquals(1, found.size());
    assertEquals("small", found.get(0).name);

    found = tree2.getElements(new Rectangle(15, 15, 10, 10));
    assertEquals(1, found.size());
    assertEquals("large", found.get(0).name);

    found = tree2.getElements(new Rectangle(72, 50, 5, 5));
    assertEquals(1, found.size());
    assertEquals("tall", found.get(0).name);
  }

  /** Simple test item for RTree. */
  static class TestItem implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;

    TestItem(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof TestItem) {
        return name.equals(((TestItem) obj).name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return "TestItem[" + name + "]";
    }
  }
}
