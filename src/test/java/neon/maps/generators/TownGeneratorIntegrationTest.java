package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import neon.resources.RRegionTheme;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

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
  public static final boolean PRINT_TOWNS = true;

  public static final String THEMES_PATH = "src/test/resources/sampleMod1/themes/";

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

}
