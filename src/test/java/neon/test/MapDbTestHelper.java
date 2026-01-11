package neon.test;

import neon.util.mapstorage.MapStore;
import neon.util.mapstorage.MapStoreMVStoreAdapter;
import org.h2.mvstore.MVStore;

/**
 * Utility class for managing MapDb databases in tests.
 *
 * <p>Provides helper methods for creating test databases (in-memory and file-backed), cleanup, and
 * integrity checks.
 */
public class MapDbTestHelper {

  /** Represents a test database with its associated file path (if file-backed). */
  public static class TestDatabase {

    private TestDatabase() {}
    ;
  }

  /**
   * Creates an in-memory MapDb database for testing.
   *
   * <p>Fast and ideal for unit tests. No disk I/O overhead.
   *
   * @return an in-memory DB instance
   */
  public static MapStore createInMemoryDB() {
    return new MapStoreMVStoreAdapter(MVStore.open(null));
  }

  /**
   * Cleans up an in-memory database.
   *
   * <p>Closes the database and releases resources.
   *
   * @param db the database to cleanup
   */
  public static void cleanup(MapStore db) {
    if (db != null && !db.isClosed()) {
      db.close();
    }
  }
}
