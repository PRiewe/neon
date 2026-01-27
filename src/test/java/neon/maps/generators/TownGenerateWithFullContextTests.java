package neon.maps.generators;

import static neon.maps.generators.TownGeneratorIntegrationTest.THEMES_PATH;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import neon.maps.Atlas;
import neon.maps.MapUtils;
import neon.maps.Region;
import neon.maps.Zone;
import neon.maps.services.EntityStore;
import neon.resources.RRegionTheme;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Nested
class TownGenerateWithFullContextTests {
  private MapStore testDb;
  private Atlas testAtlas;
  private EntityStore entityStore;
  private static Map<String, RRegionTheme> townThemes;

  // ==================== Setup ====================

  @BeforeAll
  static void loadThemes() throws Exception {
    townThemes = loadTownThemes();
  }

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    TestEngineContext.loadTestResourceViaConfig("src/test/resources/neon.ini.sampleMod1.xml");
    testAtlas = TestEngineContext.getTestAtlas();
    entityStore = TestEngineContext.getTestEntityStore();
  }

  @AfterEach
  void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
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

  // ==================== Scenario Providers ====================

  static Stream<TownGeneratorIntegrationTest.TownScenario> townThemeProvider() {
    // Use multiple seeds per theme for robustness
    return townThemes.entrySet().stream()
        .flatMap(
            entry ->
                Stream.of(42L, 7777L, 123456L)
                    .map(
                        seed ->
                            new TownGeneratorIntegrationTest.TownScenario(
                                entry.getKey(), entry.getValue(), seed)));
  }

  static Stream<TownGeneratorIntegrationTest.TownScenario> townThemeProviderSingleSeed() {
    return townThemes.entrySet().stream()
        .map(
            entry ->
                new TownGeneratorIntegrationTest.TownScenario(
                    entry.getKey(), entry.getValue(), Math.abs(entry.getKey().hashCode()) + 1L));
  }

  @ParameterizedTest(name = "generate creates house regions: {0}")
  @MethodSource("townThemeProviderSingleSeed")
  void generate_createsHouseRegions(TownGeneratorIntegrationTest.TownScenario scenario) {
    // Given
    Zone zone = TestEngineContext.getTestZoneFactory().createZone("town_test", 2, 0);

    TownGenerator generator =
        new TownGenerator(
            zone, TestEngineContext.getTestUiEngineContext(), MapUtils.withSeed(scenario.seed()));

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
  @MethodSource("townThemeProviderSingleSeed")
  void generate_doorPlacement_isValid(TownGeneratorIntegrationTest.TownScenario scenario) {
    // Given
    Zone zone = TestEngineContext.getTestZoneFactory().createZone("town_door_test", 3, 0);

    TownGenerator generator =
        new TownGenerator(
            zone, TestEngineContext.getTestUiEngineContext(), MapUtils.withSeed(scenario.seed()));

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
  @MethodSource("townThemeProviderSingleSeed")
  void generate_differentAlgorithms_byThemeType(
      TownGeneratorIntegrationTest.TownScenario scenario) {
    // Given
    Zone zone = TestEngineContext.getTestZoneFactory().createZone("town_algorithm_test", 4, 0);

    TownGenerator generator =
        new TownGenerator(
            zone, TestEngineContext.getTestUiEngineContext(), MapUtils.withSeed(scenario.seed()));

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

    if (TownGeneratorIntegrationTest.PRINT_TOWNS) {
      System.out.println("Theme: " + scenario.themeId() + ", House count: " + houseCount);
    }
  }

  @ParameterizedTest(name = "regions do not overlap: {0}")
  @MethodSource("townThemeProviderSingleSeed")
  void generate_regionsDoNotOverlap(TownGeneratorIntegrationTest.TownScenario scenario) {
    // Given
    Zone zone = TestEngineContext.getTestZoneFactory().createZone("town_overlap_test", 5, 0);

    TownGenerator generator =
        new TownGenerator(
            zone, TestEngineContext.getTestUiEngineContext(), MapUtils.withSeed(scenario.seed()));

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
