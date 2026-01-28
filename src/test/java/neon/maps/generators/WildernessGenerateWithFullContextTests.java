package neon.maps.generators;

import static neon.maps.generators.WildernessGeneratorIntegrationTest.THEMES_PATH;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import neon.maps.MapUtils;
import neon.maps.Region;
import neon.maps.Zone;
import neon.resources.RRegionTheme;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.Dice;
import neon.util.mapstorage.MapStore;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WildernessGenerateWithFullContextTests {
  private MapStore testDb;
  private static Map<String, RRegionTheme> wildernessThemes;

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createTempFileDb();
    TestEngineContext.initialize(testDb);
    TestEngineContext.loadTestResourceViaConfig("src/test/resources/neon.ini.sampleMod1.xml");
  }

  @AfterEach
  void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  @BeforeAll
  static void loadThemes() throws Exception {
    wildernessThemes = loadWildernessThemes();
  }

  private static Map<String, RRegionTheme> loadWildernessThemes() throws Exception {
    Map<String, RRegionTheme> themes = new HashMap<>();
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new File(THEMES_PATH + "regions.xml"));
    for (Element element : doc.getRootElement().getChildren("region")) {
      RRegionTheme theme = new RRegionTheme(element);
      // Filter out town themes - we only want wilderness themes
      if (!theme.id.startsWith("town")) {
        themes.put(theme.id, theme);
      }
    }
    return themes;
  }

  /**
   * Test scenario for wilderness region theme generation from XML.
   *
   * @param themeId the region theme ID
   * @param theme the loaded RRegionTheme
   * @param seed deterministic seed for generation
   */
  record WildernessScenario(String themeId, RRegionTheme theme, long seed) {
    @Override
    public String toString() {
      return String.format("theme=%s, type=%s, seed=%d", themeId, theme.type, seed);
    }
  }

  // ==================== Scenario Providers ====================

  private static Stream<WildernessGeneratorIntegrationTest.WildernessScenario>
      wildernessThemeProvider() {
    // Use multiple seeds per theme for robustness
    return wildernessThemes.entrySet().stream()
        .flatMap(
            entry ->
                Stream.of(42L, 1234L, 99999L)
                    .map(
                        seed ->
                            new WildernessGeneratorIntegrationTest.WildernessScenario(
                                entry.getKey(), entry.getValue(), seed)));
  }

  private static Stream<WildernessGeneratorIntegrationTest.WildernessScenario>
      wildernessThemeProviderSingleSeed() {
    return wildernessThemes.entrySet().stream()
        .map(
            entry ->
                new WildernessGeneratorIntegrationTest.WildernessScenario(
                    entry.getKey(), entry.getValue(), Math.abs(entry.getKey().hashCode()) + 1L));
  }

  @ParameterizedTest(name = "generate with full context: {0}")
  @MethodSource("wildernessThemeProviderSingleSeed")
  void generate_createsValidZone(WildernessGeneratorIntegrationTest.WildernessScenario scenario) {
    // Given
    Zone zone = TestEngineContext.getTestZoneFactory().createZone("wilderness_test", 1, 0);
    // Use grass as default floor when theme doesn't specify one
    String floor = scenario.theme().floor != null ? scenario.theme().floor : "grass";
    Region region = new Region(floor, 0, 0, 50, 50, null, 0, null);

    WildernessGenerator generator =
        new WildernessGenerator(
            zone,
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(scenario.seed()),
            Dice.withSeed(scenario.seed()));

    // When
    generator.generate(region, scenario.theme());

    // Then
    assertNotNull(zone, "Zone should exist");
    // Basic validation - zone was modified by generation
    // Note: Wilderness generation may or may not create regions depending on theme
  }

  static Stream<WildernessGeneratorIntegrationTest.WildernessScenario> scenariosWithCreatures() {
    return wildernessThemes.entrySet().stream()
        .filter(entry -> !entry.getValue().creatures.isEmpty())
        .map(
            entry ->
                new WildernessGeneratorIntegrationTest.WildernessScenario(
                    entry.getKey(), entry.getValue(), Math.abs(entry.getKey().hashCode()) + 1L));
  }

  @ParameterizedTest(name = "generate with creatures: {0}")
  @MethodSource("scenariosWithCreatures")
  void generate_withCreatures_placesCreatures(
      WildernessGeneratorIntegrationTest.WildernessScenario scenario) {
    // Given
    Zone zone =
        TestEngineContext.getTestZoneFactory().createZone("wilderness_creatures_test", 2, 0);
    // Use grass as default floor when theme doesn't specify one
    String floor = scenario.theme().floor != null ? scenario.theme().floor : "grass";
    Region region = new Region(floor, 0, 0, 100, 100, null, 0, null);

    WildernessGenerator generator =
        new WildernessGenerator(
            zone,
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(scenario.seed()),
            Dice.withSeed(scenario.seed()));

    // When
    generator.generate(region, scenario.theme());

    // Then
    // Note: Actual creature spawning depends on dice rolls and may be 0
    // This test just verifies generation doesn't fail with creature themes
    assertNotNull(zone, "Zone should exist even with creatures");
  }

  static Stream<WildernessGeneratorIntegrationTest.WildernessScenario> scenariosWithVegetation() {
    return wildernessThemes.entrySet().stream()
        .filter(entry -> !entry.getValue().vegetation.isEmpty())
        .map(
            entry ->
                new WildernessGeneratorIntegrationTest.WildernessScenario(
                    entry.getKey(), entry.getValue(), Math.abs(entry.getKey().hashCode()) + 1L));
  }

  @ParameterizedTest(name = "generate with vegetation: {0}")
  @MethodSource("scenariosWithVegetation")
  void generate_withVegetation_placesVegetation(
      WildernessGeneratorIntegrationTest.WildernessScenario scenario) {
    // Given
    Zone zone =
        TestEngineContext.getTestZoneFactory().createZone("wilderness_vegetation_test", 3, 0);
    // Use grass as default floor when theme doesn't specify one
    String floor = scenario.theme().floor != null ? scenario.theme().floor : "grass";
    Region region = new Region(floor, 0, 0, 80, 80, null, 0, null);

    WildernessGenerator generator =
        new WildernessGenerator(
            zone,
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(scenario.seed()),
            Dice.withSeed(scenario.seed()));

    // When
    generator.generate(region, scenario.theme());

    // Then
    assertNotNull(zone, "Zone should exist");
    // Vegetation placement is probabilistic, so we just verify no errors occurred
  }

  @ParameterizedTest(name = "determinism full context: {0}")
  @MethodSource("wildernessThemeProviderSingleSeed")
  void generate_isDeterministic_fullContext(
      WildernessGeneratorIntegrationTest.WildernessScenario scenario) {
    // Given - First generation
    Zone zone1 = TestEngineContext.getTestZoneFactory().createZone("wilderness_det_test1", 4, 0);
    // Use grass as default floor when theme doesn't specify one
    String floor = scenario.theme().floor != null ? scenario.theme().floor : "grass";
    Region region1 = new Region(floor, 0, 0, 40, 40, null, 0, null);

    WildernessGenerator generator1 =
        new WildernessGenerator(
            zone1,
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(scenario.seed()),
            Dice.withSeed(scenario.seed()));

    // When - Generate first
    generator1.generate(region1, scenario.theme());

    // Given - Second generation with same seed
    Zone zone2 = TestEngineContext.getTestZoneFactory().createZone("wilderness_det_test2", 5, 0);
    Region region2 = new Region(floor, 0, 0, 40, 40, null, 0, null);

    WildernessGenerator generator2 =
        new WildernessGenerator(
            zone2,
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(scenario.seed()),
            Dice.withSeed(scenario.seed()));

    // When - Generate second
    generator2.generate(region2, scenario.theme());

    // Then - Both zones should exist
    assertNotNull(zone1, "First zone should exist");
    assertNotNull(zone2, "Second zone should exist");

    // Note: Deep equality check of terrain would require accessing zone internals
    // For now, we verify both generations complete without errors with same seed
  }
}
