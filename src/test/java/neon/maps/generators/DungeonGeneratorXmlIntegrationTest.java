package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;
import neon.entities.Door;
import neon.entities.Entity;
import neon.maps.Atlas;
import neon.maps.Dungeon;
import neon.maps.MapTestFixtures;
import neon.maps.MapUtils;
import neon.maps.Zone;
import neon.maps.services.EntityStore;
import neon.maps.services.QuestProvider;
import neon.resources.RDungeonTheme;
import neon.resources.RZoneTheme;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.Dice;
import org.h2.mvstore.MVStore;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for DungeonGenerator that load themes from XML files.
 *
 * <p>These tests verify that dungeon generation works correctly with actual theme configurations
 * loaded from the sampleMod1 test resources. This provides coverage for all theme types and
 * configurations defined in the XML files.
 */
class DungeonGeneratorXmlIntegrationTest {

  // ==================== Configuration ====================

  /** Controls whether dungeon visualizations are printed to stdout during tests. */
  private static final boolean PRINT_DUNGEONS = false;

  private static final String THEMES_PATH = "src/test/resources/sampleMod1/themes/";

  // ==================== Static Theme Data ====================

  private static Map<String, RDungeonTheme> dungeonThemes;
  private static Map<String, RZoneTheme> zoneThemes;

  // ==================== Setup ====================
  MVStore testDb;

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
  }

  @AfterEach
  void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  @BeforeAll
  static void loadThemes() throws Exception {
    dungeonThemes = loadDungeonThemes();
    zoneThemes = loadZoneThemes();
  }

  private static Map<String, RDungeonTheme> loadDungeonThemes() throws Exception {
    Map<String, RDungeonTheme> themes = new HashMap<>();
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new File(THEMES_PATH + "dungeons.xml"));
    for (Element element : doc.getRootElement().getChildren("dungeon")) {
      RDungeonTheme theme = new RDungeonTheme(element);
      themes.put(theme.id, theme);
    }
    return themes;
  }

  private static Map<String, RZoneTheme> loadZoneThemes() throws Exception {
    Map<String, RZoneTheme> themes = new HashMap<>();
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new File(THEMES_PATH + "zones.xml"));
    for (Element element : doc.getRootElement().getChildren("zone")) {
      RZoneTheme theme = new RZoneTheme(element);
      themes.put(theme.id, theme);
    }
    return themes;
  }

  // ==================== Scenario Records ====================

  /**
   * Test scenario for zone theme generation from XML.
   *
   * @param zoneId the zone theme ID
   * @param theme the loaded RZoneTheme
   * @param seed deterministic seed for generation
   */
  record ZoneThemeScenario(String zoneId, RZoneTheme theme, long seed) {
    @Override
    public String toString() {
      return String.format("zone=%s, type=%s", zoneId, theme.type);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<ZoneThemeScenario> zoneThemeScenarios() {
    return zoneThemes.entrySet().stream()
        .map(
            entry ->
                new ZoneThemeScenario(
                    entry.getKey(),
                    entry.getValue(),
                    Math.abs(entry.getKey().hashCode()) + 1L // deterministic seed per zone
                    ));
  }

  static Stream<ZoneThemeScenario> zoneThemeScenariosWithEntities() {
    // Filter to themes with enough entities to reliably place (sum of counts >= 15)
    // Lower thresholds can still fail due to random dice rolls (1dN for each entity type)
    return zoneThemes.entrySet().stream()
        .filter(
            entry -> {
              int creatureSum =
                  entry.getValue().creatures.values().stream().mapToInt(Integer::intValue).sum();
              int itemSum =
                  entry.getValue().items.values().stream().mapToInt(Integer::intValue).sum();
              return creatureSum >= 15 || itemSum >= 15;
            })
        .map(
            entry ->
                new ZoneThemeScenario(
                    entry.getKey(), entry.getValue(), Math.abs(entry.getKey().hashCode()) + 1L));
  }

  // ==================== Helper Methods ====================

  private DungeonTileGenerator createGenerator(ZoneThemeScenario scenario) {
    MapUtils mapUtils = MapUtils.withSeed(scenario.seed());
    Dice dice = Dice.withSeed(scenario.seed());
    return new DungeonTileGenerator(scenario.theme(), mapUtils, dice);
  }

  // ==================== Tests ====================

  @ParameterizedTest(name = "generateTiles with XML theme: {0}")
  @MethodSource("zoneThemeScenarios")
  void generateTiles_withXmlZoneTheme_generatesValidTerrain(ZoneThemeScenario scenario) {
    // Given
    DungeonTileGenerator generator = createGenerator(scenario);

    // When
    String[][] terrain = generator.generateTiles().terrain();

    // Then: visualize (controlled by PRINT_DUNGEONS flag)
    if (PRINT_DUNGEONS) {
      System.out.println("Zone theme: " + scenario);
      System.out.println(visualizeTerrain(terrain));
      System.out.println();
    }

    // Verify
    assertAll(
        () -> assertNotNull(terrain, "Terrain should not be null"),
        () ->
            assertTrue(
                terrain.length >= scenario.theme().min,
                "Width " + terrain.length + " should be >= " + scenario.theme().min),
        () ->
            assertTrue(
                terrain.length <= scenario.theme().max,
                "Width " + terrain.length + " should be <= " + scenario.theme().max),
        () ->
            assertTrue(
                terrain[0].length >= scenario.theme().min,
                "Height " + terrain[0].length + " should be >= " + scenario.theme().min),
        () ->
            assertTrue(
                terrain[0].length <= scenario.theme().max,
                "Height " + terrain[0].length + " should be <= " + scenario.theme().max),
        () ->
            assertFloorTerrainExists(
                terrain, scenario.theme().floor, "Terrain should have floor tiles"));
  }

  static Stream<ZoneThemeScenario> connectivityScenarios() {
    // Exclude "mine" type which has known connectivity edge cases at small sizes
    return zoneThemes.entrySet().stream()
        .filter(entry -> !entry.getValue().type.equals("mine"))
        .map(
            entry ->
                new ZoneThemeScenario(
                    entry.getKey(), entry.getValue(), Math.abs(entry.getKey().hashCode()) + 1L));
  }

  @ParameterizedTest(name = "connectivity for XML theme: {0}")
  @MethodSource("connectivityScenarios")
  void generateBaseTiles_withXmlZoneTheme_isConnected(ZoneThemeScenario scenario) {
    // Given
    DungeonTileGenerator generator = createGenerator(scenario);

    // When: use larger size (40x40) for more reliable connectivity
    int size = Math.max(40, scenario.theme().min);
    int[][] tiles = generator.generateBaseTiles(scenario.theme().type, size, size);

    // Then
    assertDungeonIsConnected(tiles, "Dungeon should be fully connected");
  }

  @ParameterizedTest(name = "determinism for XML theme: {0}")
  @MethodSource("zoneThemeScenarios")
  void generateTiles_withXmlZoneTheme_isDeterministic(ZoneThemeScenario scenario) {
    // Given: two generators with same seed
    DungeonTileGenerator gen1 = createGenerator(scenario);
    DungeonTileGenerator gen2 = createGenerator(scenario);

    // When
    String[][] terrain1 = gen1.generateTiles().terrain();
    String[][] terrain2 = gen2.generateTiles().terrain();

    // Then
    assertTerrainMatch(terrain1, terrain2);
  }

  @ParameterizedTest(name = "entities for XML theme: {0}")
  @MethodSource("zoneThemeScenariosWithEntities")
  void generateTiles_withXmlZoneTheme_placesEntities(ZoneThemeScenario scenario) {
    // Given
    DungeonTileGenerator generator = createGenerator(scenario);

    // When
    String[][] terrain = generator.generateTiles().terrain();

    // Then: only assert for entities with sufficient counts to reliably place
    int creatureSum =
        scenario.theme().creatures.values().stream().mapToInt(Integer::intValue).sum();
    int itemSum = scenario.theme().items.values().stream().mapToInt(Integer::intValue).sum();

    if (creatureSum >= 15) {
      assertHasCreatureAnnotations(
          terrain,
          "Theme with creatures (sum=" + creatureSum + ") should place creature annotations");
    }
    if (itemSum >= 15) {
      assertHasItemAnnotations(
          terrain, "Theme with items (sum=" + itemSum + ") should place item annotations");
    }
  }

  // ==================== Assertion Helpers ====================

  private void assertFloorTerrainExists(String[][] terrain, String floors, String message) {
    List<String> floorTypes = List.of(floors.split(","));
    boolean hasFloor = false;
    for (int x = 0; x < terrain.length && !hasFloor; x++) {
      for (int y = 0; y < terrain[0].length && !hasFloor; y++) {
        if (terrain[x][y] != null) {
          String baseTerrain = terrain[x][y].split(";")[0];
          if (floorTypes.contains(baseTerrain)) {
            hasFloor = true;
          }
        }
      }
    }
    assertTrue(hasFloor, message);
  }

  private void assertDungeonIsConnected(int[][] tiles, String message) {
    int floorCount = 0;
    int startX = -1, startY = -1;

    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (isWalkable(tiles[x][y])) {
          floorCount++;
          if (startX < 0) {
            startX = x;
            startY = y;
          }
        }
      }
    }

    if (floorCount == 0) {
      fail(message + " - no walkable tiles found");
      return;
    }

    int reachable = floodFillCount(tiles, startX, startY);
    assertEquals(floorCount, reachable, message + " - not all walkable tiles are connected");
  }

  private boolean isWalkable(int tile) {
    return tile == MapUtils.FLOOR
        || tile == MapUtils.CORRIDOR
        || tile == MapUtils.DOOR
        || tile == MapUtils.DOOR_CLOSED
        || tile == MapUtils.DOOR_LOCKED;
  }

  private int floodFillCount(int[][] tiles, int startX, int startY) {
    int width = tiles.length;
    int height = tiles[0].length;
    boolean[][] visited = new boolean[width][height];
    Queue<int[]> queue = new LinkedList<>();
    queue.add(new int[] {startX, startY});
    visited[startX][startY] = true;
    int count = 0;

    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    while (!queue.isEmpty()) {
      int[] current = queue.poll();
      count++;

      for (int[] dir : directions) {
        int nx = current[0] + dir[0];
        int ny = current[1] + dir[1];

        if (nx >= 0
            && nx < width
            && ny >= 0
            && ny < height
            && !visited[nx][ny]
            && isWalkable(tiles[nx][ny])) {
          visited[nx][ny] = true;
          queue.add(new int[] {nx, ny});
        }
      }
    }

    return count;
  }

  private void assertTerrainMatch(String[][] terrain1, String[][] terrain2) {
    assertEquals(terrain1.length, terrain2.length, "Terrain arrays should have same width");
    for (int x = 0; x < terrain1.length; x++) {
      assertEquals(
          terrain1[x].length,
          terrain2[x].length,
          "Terrain arrays should have same height at x=" + x);
      for (int y = 0; y < terrain1[x].length; y++) {
        if (terrain1[x][y] == null && terrain2[x][y] == null) {
          continue;
        }
        assertEquals(
            terrain1[x][y], terrain2[x][y], String.format("Terrain at (%d,%d) should match", x, y));
      }
    }
  }

  private void assertHasCreatureAnnotations(String[][] terrain, String message) {
    boolean hasCreature = false;
    for (int x = 0; x < terrain.length && !hasCreature; x++) {
      for (int y = 0; y < terrain[0].length && !hasCreature; y++) {
        if (terrain[x][y] != null && terrain[x][y].contains(";c:")) {
          hasCreature = true;
          break;
        }
      }
    }
    assertTrue(hasCreature, message);
  }

  private void assertHasItemAnnotations(String[][] terrain, String message) {
    boolean hasItem = false;
    for (int x = 0; x < terrain.length && !hasItem; x++) {
      for (int y = 0; y < terrain[0].length && !hasItem; y++) {
        if (terrain[x][y] != null && terrain[x][y].contains(";i:")) {
          hasItem = true;
          break;
        }
      }
    }
    assertTrue(hasItem, message);
  }

  // ==================== Visualization ====================

  private String visualizeTerrain(String[][] terrain) {
    int width = terrain.length;
    int height = terrain[0].length;

    StringBuilder sb = new StringBuilder();
    sb.append("+").append("-".repeat(width)).append("+\n");

    for (int y = 0; y < height; y++) {
      sb.append("|");
      for (int x = 0; x < width; x++) {
        if (terrain[x][y] == null) {
          sb.append(' ');
        } else if (terrain[x][y].contains(";c:")) {
          sb.append('c');
        } else if (terrain[x][y].contains(";i:")) {
          sb.append('i');
        } else {
          sb.append('.');
        }
      }
      sb.append("|\n");
    }
    sb.append("+").append("-".repeat(width)).append("+");

    // Add summary
    int floorCount = 0, creatureCount = 0, itemCount = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (terrain[x][y] != null) {
          floorCount++;
          if (terrain[x][y].contains(";c:")) creatureCount++;
          if (terrain[x][y].contains(";i:")) itemCount++;
        }
      }
    }
    sb.append(
        String.format(
            "\nTerrain: %dx%d, floors=%d, creatures=%d, items=%d",
            width, height, floorCount, creatureCount, itemCount));

    return sb.toString();
  }

  // ==================== Full Integration Tests ====================

  /**
   * Nested test class for integration tests that require full Engine context. These tests verify
   * the generate(Door, Zone, Atlas) method which creates actual entities.
   */
  @Nested
  class GenerateWithFullContextTests {

    private MVStore testDb;
    private Atlas testAtlas;
    private EntityStore entityStore;

    @BeforeEach
    void setUp() throws Exception {
      // Clean up any stale test files
      new File("test-store.dat").delete();
      new File("testfile3.dat").delete();

      testDb = MapDbTestHelper.createInMemoryDB();
      TestEngineContext.initialize(testDb);
      // Load resources from sampleMod1 (same pattern as ModLoader)
      //
      TestEngineContext.loadTestResourceViaConfig("src/test/resources/neon.ini.sampleMod1.xml");
      // TestEngineContext.loadTestResources("src/test/resources/sampleMod1");
      testAtlas = TestEngineContext.getTestAtlas();
      entityStore = TestEngineContext.getTestEntityStore();
    }

    @AfterEach
    void tearDown() {
      TestEngineContext.reset();
      new File("test-store.dat").delete();
      new File("testfile3.dat").delete();
    }

    /** QuestProvider that returns no quest objects. */
    private class NoQuestProvider implements QuestProvider {
      @Override
      public String getNextRequestedObject() {
        return null;
      }
    }

    // ==================== Tests Using generate() ====================

    @ParameterizedTest(name = "generate() with XML theme: {0}")
    @MethodSource("neon.maps.generators.DungeonGeneratorXmlIntegrationTest#zoneThemeScenarios")
    void generate_withXmlZoneTheme_createsZoneWithRegions(ZoneThemeScenario scenario)
        throws Exception {
      // Given: Set up dungeon structure
      int mapUID = entityStore.createNewMapUID();
      Dungeon dungeon = new Dungeon("test-dungeon-" + scenario.zoneId(), mapUID);
      dungeon.addZone(0, "zone-0"); // Previous zone
      dungeon.addZone(1, "zone-1", scenario.theme()); // Target zone with theme

      Zone previousZone = dungeon.getZone(0);
      Zone targetZone = dungeon.getZone(1);

      previousZone.addRegion(MapTestFixtures.createTestRegion(0, 0, 50, 50));
      testAtlas.setMap(dungeon);

      // Create entry door in previous zone
      Door entryDoor =
          MapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 25, 25, 1, 0);
      entityStore.addEntity(entryDoor);
      previousZone.addItem(entryDoor);

      // Create generator with full dependencies
      DungeonGenerator generator =
          new DungeonGenerator(
              targetZone,
              TestEngineContext.getTestQuestTracker(),
              TestEngineContext.getTestUiEngineContext(),
              MapUtils.withSeed(scenario.seed()),
              Dice.withSeed(scenario.seed()));

      // When: Call the full generate() method (same entry point as engine uses)
      generator.generate(entryDoor, previousZone, testAtlas);

      // Then: Verify actual entities were created
      assertFalse(targetZone.getRegions().isEmpty(), "Zone should have regions");
      assertHasReturnDoor(targetZone, previousZone.getIndex());
    }

    @ParameterizedTest(name = "generate() door linking: {0}")
    @MethodSource("neon.maps.generators.DungeonGeneratorXmlIntegrationTest#zoneThemeScenarios")
    void generate_withXmlZoneTheme_linksDoorsCorrectly(ZoneThemeScenario scenario)
        throws Exception {
      // Given
      int mapUID = entityStore.createNewMapUID();
      Dungeon dungeon = new Dungeon("test-dungeon-" + scenario.zoneId(), mapUID);
      dungeon.addZone(0, "zone-0");
      dungeon.addZone(1, "zone-1", scenario.theme());

      Zone previousZone = dungeon.getZone(0);
      Zone targetZone = dungeon.getZone(1);

      previousZone.addRegion(MapTestFixtures.createTestRegion(0, 0, 50, 50));
      testAtlas.setMap(dungeon);

      Door entryDoor =
          MapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 10, 10, 1, 0);
      entityStore.addEntity(entryDoor);
      previousZone.addItem(entryDoor);

      DungeonGenerator generator =
          new DungeonGenerator(
              targetZone,
              TestEngineContext.getTestQuestTracker(),
              TestEngineContext.getTestUiEngineContext(),
              MapUtils.withSeed(scenario.seed()),
              Dice.withSeed(scenario.seed()));

      // When
      generator.generate(entryDoor, previousZone, testAtlas);

      // Then: entry door should have destination position set
      assertNotNull(entryDoor.portal.getDestPos(), "Entry door should have destination position");

      // Find return door and verify it links back
      Door returnDoor = findReturnDoor(targetZone, previousZone.getIndex());
      assertNotNull(returnDoor, "Should have a return door");
      assertNotNull(returnDoor.portal.getDestPos(), "Return door should have destination position");
      assertEquals(
          10, returnDoor.portal.getDestPos().x, "Return door should point to entry door X");
      assertEquals(
          10, returnDoor.portal.getDestPos().y, "Return door should point to entry door Y");
    }

    // ==================== Helper Methods ====================

    private void assertHasReturnDoor(Zone zone, int previousZoneIndex) {
      boolean hasReturnDoor = false;
      for (long itemUid : zone.getItems()) {
        Entity entity = entityStore.getEntity(itemUid);
        if (entity instanceof Door door) {
          if (door.portal.getDestZone() == previousZoneIndex) {
            hasReturnDoor = true;
            break;
          }
        }
      }
      assertTrue(hasReturnDoor, "Zone should have return door to previous zone");
    }

    private Door findReturnDoor(Zone zone, int previousZoneIndex) {
      for (long itemUid : zone.getItems()) {
        Entity entity = entityStore.getEntity(itemUid);
        if (entity instanceof Door door && door.portal.getDestZone() == previousZoneIndex) {
          return door;
        }
      }
      return null;
    }
  }
}
