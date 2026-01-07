package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import neon.util.Dice;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for CaveGenerator with deterministic seeded random behavior. */
class CaveGeneratorTest {

  // ==================== Scenario Records ====================

  /**
   * Test scenario for open cave generation.
   *
   * @param seed random seed for deterministic behavior
   * @param width cave width
   * @param height cave height
   * @param sparseness how sparse the cave should be (higher = more open)
   */
  record CaveScenario(long seed, int width, int height, int sparseness) {

    @Override
    public String toString() {
      return String.format("seed=%d, %dx%d, sparse=%d", seed, width, height, sparseness);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<CaveScenario> caveScenarios() {
    return Stream.of(
        new CaveScenario(42L, 30, 30, 3),
        new CaveScenario(999L, 40, 30, 2),
        new CaveScenario(123L, 35, 35, 4),
        new CaveScenario(264L, 50, 40, 3));
  }

  // ==================== Open Cave Tests ====================

  @ParameterizedTest(name = "generateOpenCave: {0}")
  @MethodSource("caveScenarios")
  void generateOpenCave_generatesValidCave(CaveScenario scenario) {
    // Given
    CaveGenerator generator = new CaveGenerator(Dice.withSeed(scenario.seed()));

    // When
    int[][] tiles =
        generator.generateOpenCave(scenario.width(), scenario.height(), scenario.sparseness());

    // Then: visualize
    System.out.println("Open Cave: " + scenario);
    System.out.println(TileVisualization.visualizeTiles(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Cave width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Cave height should match"),
        () -> TileAssertions.assertFloorTilesExist(tiles, "Cave should have floor tiles"),
        () -> TileConnectivityAssertions.assertFullyConnected(tiles, "Cave should be connected"));
  }

  @ParameterizedTest(name = "generateOpenCave determinism: {0}")
  @MethodSource("caveScenarios")
  void generateOpenCave_isDeterministic(CaveScenario scenario) {
    // Given: two generators with the same seed
    CaveGenerator generator1 = new CaveGenerator(Dice.withSeed(scenario.seed()));
    CaveGenerator generator2 = new CaveGenerator(Dice.withSeed(scenario.seed()));

    // When
    int[][] tiles1 =
        generator1.generateOpenCave(scenario.width(), scenario.height(), scenario.sparseness());
    int[][] tiles2 =
        generator2.generateOpenCave(scenario.width(), scenario.height(), scenario.sparseness());

    // Then
    TileAssertions.assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Assertion Helpers ====================

  // ==================== Visualization ====================

}
