package neon.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final MapStore db;
    private final Path filePath;

    public TestDatabase(MapStore db, Path filePath) {
      this.db = db;
      this.filePath = filePath;
    }

    public MapStore getDb() {
      return db;
    }

    public Path getFilePath() {
      return filePath;
    }

    public boolean isFileBacked() {
      return filePath != null;
    }
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
   * Creates a temporary file-backed MapDb database for testing.
   *
   * <p>Tests real persistence behavior. The file is created in the system temp directory.
   *
   * @return a TestDatabase instance with both DB and file path
   * @throws IOException if temp file creation fails
   */
  public static TestDatabase createTempFileDB() throws IOException {
    Path tempFile = Files.createTempFile("neon-test-db-", ".dat");
    MapStore db = new MapStoreMVStoreAdapter(MVStore.open(tempFile.toString()));

    return new TestDatabase(db, tempFile);
  }

  /**
   * Creates a temporary file-backed MapDb database with a specific name prefix.
   *
   * @param prefix the prefix for the temp file name
   * @return a TestDatabase instance with both DB and file path
   * @throws IOException if temp file creation fails
   */
  public static TestDatabase createTempFileDB(String prefix) throws IOException {
    Path tempFile = Files.createTempFile(prefix, ".dat");
    MapStore db = new MapStoreMVStoreAdapter(MVStore.open(tempFile.toString()));
    return new TestDatabase(db, tempFile);
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

  /**
   * Cleans up a test database and deletes its backing file if present.
   *
   * <p>Closes the database, deletes the file, and releases all resources.
   *
   * @param testDb the test database to cleanup
   */
  public static void cleanup(TestDatabase testDb) {
    if (testDb == null) {
      return;
    }

    // Close database
    if (testDb.db != null && !testDb.db.isClosed()) {
      testDb.db.close();
    }

    // Delete backing file if it exists
    if (testDb.filePath != null) {
      try {
        Files.deleteIfExists(testDb.filePath);
      } catch (IOException e) {
        System.err.println("Warning: Failed to delete test database file: " + e.getMessage());
      }
    }
  }

  /**
   * Asserts that a database is open and ready for use.
   *
   * @param db the database to check
   * @throws IllegalStateException if the database is null or closed
   */
  public static void assertDbOpen(MapStore db) {
    if (db == null) {
      throw new IllegalStateException("Database is null");
    }
    if (db.isClosed()) {
      throw new IllegalStateException("Database is closed");
    }
  }

  /**
   * Asserts that a named collection exists in the database.
   *
   * @param db the database to check
   * @param collectionName the name of the collection
   * @throws IllegalStateException if the collection doesn't exist
   */
  public static void assertCollectionExists(MapStore db, String collectionName) {
    assertDbOpen(db);
    if (!db.getMapNames().contains(collectionName)) {
      throw new IllegalStateException("Collection '" + collectionName + "' does not exist");
    }
  }

  /**
   * Gets the number of entries in a named collection.
   *
   * @param db the database
   * @param collectionName the name of the collection
   * @return the number of entries, or -1 if collection doesn't exist
   */
  public static int getCollectionSize(MapStore db, String collectionName) {
    var map = db.openMap(collectionName);
    return map.size();
  }

  /**
   * Commits all pending changes to the database.
   *
   * @param db the database to commit
   */
  public static void commit(MapStore db) {
    if (db != null && !db.isClosed()) {
      db.commit();
    }
  }
}
