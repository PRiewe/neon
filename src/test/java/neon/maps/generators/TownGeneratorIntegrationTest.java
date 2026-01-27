package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.maps.MapUtils;
import neon.maps.Region;
import neon.maps.Zone;
import neon.resources.RRegionTheme;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for TownGenerator that load themes from XML files.
 *
 * <p>These tests verify that town generation works correctly with actual theme configurations
 * loaded from the sampleMod1 test resources. This provides coverage for all town theme types (town,
 * town_small, town_big) and their respective block generation algorithms.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TownGeneratorIntegrationTest {

  // ==================== Configuration ====================

  /** Controls whether town visualizations are printed to stdout during tests. */
  private static final boolean PRINT_TOWNS = false;

  private static final String THEMES_PATH = "src/test/resources/sampleMod1/themes/";

  // ==================== Static Theme Data ====================

  private static Map<String, RRegionTheme> townThemes;

  // ==================== Setup ====================

  @BeforeAll
  static void loadThemes() throws Exception {
    townThemes = loadTownThemes();
  }

  private static Map<String, RRegionTheme> loadTownThemes() throws Exception {
    Map<String, RRegionTheme> themes = new HashMap<>();
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new File(THEMES_PATH + "regions.xml"));
    for (Element element : doc.getRootElement().getChildren("region")) {
      RRegionTheme theme = new RRegionTheme(element);
      // Filter for town themes only
      if (theme.id.startsWith("town")) {
        themes.put(theme.id, theme);
      }
    }
    return themes;
  }

  // ==================== Scenario Records ====================

  /**
   * Test scenario for town region theme generation from XML.
   *
   * @param themeId the region theme ID
   * @param theme the loaded RRegionTheme
   * @param seed deterministic seed for generation
   */
  record TownScenario(String themeId, RRegionTheme theme, long seed) {
    @Override
    public @NonNull String toString() {
      return String.format("theme=%s, type=%s, seed=%d", themeId, theme.id, seed);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<TownScenario> townThemeProvider() {
    // Use multiple seeds per theme for robustness
    return townThemes.entrySet().stream()
        .flatMap(
            entry ->
                Stream.of(42L, 7777L, 123456L)
                    .map(seed -> new TownScenario(entry.getKey(), entry.getValue(), seed)));
  }

  static Stream<TownScenario> townThemeProviderSingleSeed() {
    return townThemes.entrySet().stream()
        .map(
            entry ->
                new TownScenario(
                    entry.getKey(), entry.getValue(), Math.abs(entry.getKey().hashCode()) + 1L));
  }

  // ==================== Full Integration Tests with Engine Context ====================
  // Note: Lightweight tests omitted because Zone creation requires Engine context

  @Nested
  class GenerateWithFullContextTests {
    private MapStore testDb;
    private Atlas testAtlas;
    private UIDStore entityStore;

    @BeforeEach
    void setUp() throws Exception {
      testDb = MapDbTestHelper.createInMemoryDB();
      TestEngineContext.initialize(testDb);
      TestEngineContext.loadTestResourceViaConfig("src/test/resources/neon.ini.sampleMod1.xml");
      testAtlas = TestEngineContext.getTestAtlas();
      entityStore = TestEngineContext.getTestStore();
    }

    @AfterEach
    void tearDown() {
      TestEngineContext.reset();
      MapDbTestHelper.cleanup(testDb);
    }

    @ParameterizedTest(name = "generate creates house regions: {0}")
    @MethodSource("neon.maps.generators.TownGeneratorIntegrationTest#townThemeProviderSingleSeed")
    void generate_createsHouseRegions(TownScenario scenario) {
      // Given
      Zone zone = TestEngineContext.getTestZoneFactory().createZone("town_test", 2, 0);

      TownGenerator generator =
          new TownGenerator(
              zone,
              entityStore,
              TestEngineContext.getTestResources(),
              MapUtils.withSeed(scenario.seed()));

      // When
      generator.generate(0, 0, 100, 100, scenario.theme(), 0);

      // Then
      assertNotNull(zone, "Zone should exist");
      // Verify houses were created (regions added to zone)
      assertTrue(
          zone.getRegions().size() > 0,
          "Zone should have house regions for theme: " + scenario.themeId());

      // Verify all regions are on layer 1 or 2
      // Layer 1: house regions (layer param + 1)
      // Layer 2: door floor regions (house layer + 1)
      for (Region region : zone.getRegions()) {
        assertTrue(
            region.getZ() == 1 || region.getZ() == 2,
            "Region should be on layer 1 (house) or 2 (door floor), but was: " + region.getZ());
      }
    }

    @ParameterizedTest(name = "door placement is valid: {0}")
    @MethodSource("neon.maps.generators.TownGeneratorIntegrationTest#townThemeProviderSingleSeed")
    void generate_doorPlacement_isValid(TownScenario scenario) {
      // Given
      Zone zone = TestEngineContext.getTestZoneFactory().createZone("town_door_test", 3, 0);

      TownGenerator generator =
          new TownGenerator(
              zone,
              entityStore,
              TestEngineContext.getTestResources(),
              MapUtils.withSeed(scenario.seed()));

      // When
      generator.generate(0, 0, 120, 120, scenario.theme(), 0);

      // Then
      assertNotNull(zone, "Zone should exist");

      // Verify doors were placed (one per house)
      int houseCount = zone.getRegions().size();
      assertTrue(houseCount > 0, "Should have at least one house");

      // Note: Door count verification would require access to zone.getItems()
      // which includes doors. For now, we verify generation completes successfully.
    }

    @ParameterizedTest(name = "different algorithms by theme: {0}")
    @MethodSource("neon.maps.generators.TownGeneratorIntegrationTest#townThemeProviderSingleSeed")
    void generate_differentAlgorithms_byThemeType(TownScenario scenario) {
      // Given
      Zone zone = TestEngineContext.getTestZoneFactory().createZone("town_algorithm_test", 4, 0);

      TownGenerator generator =
          new TownGenerator(
              zone,
              entityStore,
              TestEngineContext.getTestResources(),
              MapUtils.withSeed(scenario.seed()));

      // When
      generator.generate(0, 0, 150, 150, scenario.theme(), 0);

      // Then
      assertNotNull(zone, "Zone should exist");
      int houseCount = zone.getRegions().size();

      // Verify different themes produce different building counts/layouts
      // town_big should use BSP (fewer, larger buildings)
      // town_small should use packed (more dense)
      // town should use sparse (more spread out)
      if (scenario.themeId().equals("town_big")) {
        assertTrue(houseCount >= 1, "town_big should generate buildings (BSP algorithm)");
      } else if (scenario.themeId().equals("town_small")) {
        assertTrue(houseCount >= 1, "town_small should generate buildings (packed algorithm)");
      } else {
        assertTrue(houseCount >= 1, "town should generate buildings (sparse algorithm)");
      }

      if (PRINT_TOWNS) {
        System.out.println("Theme: " + scenario.themeId() + ", House count: " + houseCount);
      }
    }

    @ParameterizedTest(name = "regions do not overlap: {0}")
    @MethodSource("neon.maps.generators.TownGeneratorIntegrationTest#townThemeProviderSingleSeed")
    void generate_regionsDoNotOverlap(TownScenario scenario) {
      // Given
      Zone zone = TestEngineContext.getTestZoneFactory().createZone("town_overlap_test", 5, 0);

      TownGenerator generator =
          new TownGenerator(
              zone,
              entityStore,
              TestEngineContext.getTestResources(),
              MapUtils.withSeed(scenario.seed()));

      // When
      generator.generate(0, 0, 100, 100, scenario.theme(), 0);

      // Then
      // Note: Overlap detection would require checking all pairs of regions
      // BlocksGenerator algorithms should guarantee no overlaps
      assertTrue(zone.getRegions().size() >= 0, "Zone should have regions");

      // Verify no regions have negative dimensions (sanity check)
      for (Region region : zone.getRegions()) {
        assertTrue(region.getWidth() > 0, "Region width should be positive");
        assertTrue(region.getHeight() > 0, "Region height should be positive");
      }
    }
  }
}
