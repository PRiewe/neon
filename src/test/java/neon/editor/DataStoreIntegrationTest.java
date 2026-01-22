package neon.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.tree.DefaultTreeModel;
import neon.editor.maps.MapEditor;
import neon.editor.maps.StubTreeNode;
import neon.editor.resources.RMap;
import neon.resources.*;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.systems.files.XMLTranslator;
import org.jdom2.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataStoreIntegrationTest {
  String[] fileList = {
    "cc.xml",
    "events.xml",
    "factions.xml",
    "main.xml",
    "signs.xml",
    "spells.xml",
    "tattoos.xml",
    "terrain.xml",
    "themes/dungeons.xml",
    "themes/regions.xml",
    "themes/zones.xml",
    "quests/default.xml",
    "quests/main.xml",
    "quests/random1.xml",
    "quests/random2.xml",
    "quests/random3.xml",
    "quests/random4.xml",
    "objects/alchemy.xml",
    "objects/crafting.xml",
    "objects/items.xml",
    "objects/monsters.xml",
    "objects/npc.xml",
    "maps/ban_rajas.xml",
    "maps/d2.xml",
    "maps/d3.xml",
    "maps/kusunda.xml",
    "maps/kusunda_guard.xml",
    "maps/kusunda_ice.xml",
    "maps/kusunda_stinky.xml",
    "maps/world.xml",
  };

  @Test
  void loadSampleMod() throws IOException {
    FileSystem fileSystem = new FileSystem();
    fileSystem.mount("src/test/resources/");
    ResourceManager resourceManager = new ResourceManager();
    DataStore dataStore = new DataStore(resourceManager, fileSystem);

    dataStore.loadData("sampleMod1", true, false);
    StubTreeNode root = new StubTreeNode();
    DefaultTreeModel treeModel = new DefaultTreeModel(root);
    MapEditor.loadMapsHeadless(
        dataStore.getResourceManager().getResources(RMap.class), treeModel, dataStore);
    var map = resourceManager.getAllResources();
    System.out.format("ResourceManager items: %s%n", resourceManager.getAllResources().size());
    var groups =
        map.entrySet().stream().collect(Collectors.groupingBy(x -> x.getValue().getClass()));
    for (var e : groups.entrySet()) {
      System.out.format("%s | %s %n", e.getKey(), e.getValue().size());
    }
    // Need to adjust counts if sampleMod scenario is changed.
    assertEquals(12, groups.getOrDefault(RPerson.class, List.of()).size());
    assertEquals(31, groups.getOrDefault(RTerrain.class, List.of()).size());
    assertEquals(8, groups.getOrDefault(RMap.class, List.of()).size());
  }

  Stream<CompareXmlScenario> loadAndSaveSampleMod() throws IOException {
    FileSystem fileSystem = new FileSystem();
    fileSystem.mount("src/test/resources/");
    ResourceManager resourceManager = new ResourceManager();
    DataStore dataStore = new DataStore(resourceManager, fileSystem);

    dataStore.loadData("sampleMod1", true, false);
    StubTreeNode root = new StubTreeNode();
    DefaultTreeModel treeModel = new DefaultTreeModel(root);
    MapEditor.loadMapsHeadless(
        dataStore.getResourceManager().getResources(RMap.class), treeModel, dataStore);
    Collection<RMap> maps = resourceManager.getResources(RMap.class);
    for (var mappee : maps) {
      mappee.load();
    }
    var map = resourceManager.getAllResources();
    System.out.format("ResourceManager items: %s%n", resourceManager.getAllResources().size());
    var groups =
        map.entrySet().stream().collect(Collectors.groupingBy(x -> x.getValue().getClass()));
    //    for (var e : groups.entrySet()) {
    //      System.out.format("%s | %s %n", e.getKey(), e.getValue().size());
    //    }

    var tmp = Files.createTempDirectory("neon_");
    StringBuilder newDir = new StringBuilder();
    newDir.append(tmp.toFile().getAbsolutePath());
    newDir.append(File.separator);
    newDir.append("sampleMod1");
    var newDirFile = new File(newDir.toString());
    newDirFile.mkdir();

    System.out.format("TempDir %s%n", tmp);
    //    System.out.format("PAths: %s%n",fileSystem.getPaths());
    //    System.out.format("PAthh: %s%n",dataStore.getActive().getPath());
    FileSystem fileSystemOut = new FileSystem();
    fileSystemOut.mount(newDirFile.getAbsolutePath());
    String zoneFileName = newDirFile.getAbsolutePath() + File.separator + "spells.xml";
    // fileSystemOut.createDirectory("sampleMod1");
    // Element savedZoneThemes = fileSystemOut.getFile(new XMLTranslator(),
    // newDirFile.getAbsolutePath(),"themes", "zones.xml").getRootElement();
    Document originalZoneThemes =
        fileSystem.getFile(new XMLTranslator(), "sampleMod1", "spells.xml");

    ModFiler.save(dataStore, fileSystemOut);
    List<CompareXmlScenario> compareXmlScenarios = new ArrayList<>();
    for (String file : fileList) {
      compareXmlScenarios.add(
          new CompareXmlScenario(
              "src/test/resources/sampleMod1", newDirFile.getAbsolutePath(), file));
    }
    return compareXmlScenarios.stream();
  }

  record CompareXmlScenario(String sourceFolder, String targetFolder, String fileName) {
    @Override
    public String toString() {
      return fileName;
    }
  }

  @ParameterizedTest(name = "compareXmlFiles: {0}")
  @MethodSource("loadAndSaveSampleMod")
  void compareXmlFiles(CompareXmlScenario scen) {
    String targetFileName = scen.targetFolder() + File.separator + scen.fileName();
    String sourceFileName = scen.sourceFolder() + File.separator + scen.fileName();
    InputStream targetStream = null;
    try {
      targetStream = new FileInputStream(targetFileName);
    } catch (IOException io) {
      System.out.format("Error opening file %s%n", targetFileName);
      return;
    }
    NodeMatcher nodeMatcher =
        new DefaultNodeMatcher(
            ElementSelectors.selectorForElementNamed("plant", ElementSelectors.byNameAndText),
            ElementSelectors.selectorForElementNamed("item", ElementSelectors.byNameAndText),
            ElementSelectors.selectorForElementNamed("feature", ElementSelectors.byNameAndText),
            ElementSelectors.selectorForElementNamed("creature", ElementSelectors.byNameAndText),
            ElementSelectors.selectorForElementNamed(
                "region", ElementSelectors.byNameAndAttributes("x", "y")),
            ElementSelectors.selectorForElementNamed(
                "event", ElementSelectors.byNameAndAttributes("script")),
            ElementSelectors.byNameAndAttributes("id"));
    if (scen.fileName().startsWith("themes")) {
      nodeMatcher =
          new DefaultNodeMatcher(
              ElementSelectors.selectorForElementNamed("plant", ElementSelectors.byNameAndText),
              ElementSelectors.selectorForElementNamed("item", ElementSelectors.byNameAndText),
              ElementSelectors.selectorForElementNamed("feature", ElementSelectors.byNameAndText),
              ElementSelectors.selectorForElementNamed("creature", ElementSelectors.byNameAndText),
              ElementSelectors.selectorForElementNamed(
                  "event", ElementSelectors.byNameAndAttributes("script")),
              ElementSelectors.byNameAndAttributes("id"));
    }
    if (scen.fileName().startsWith("objects")) {
      nodeMatcher = new DefaultNodeMatcher(ElementSelectors.byNameAndAttributes("id"));
    }
    InputStream sourceStream = null;
    try {
      sourceStream = new FileInputStream(sourceFileName);
    } catch (FileNotFoundException e) {
      System.out.format("Error opening file %s%n", sourceFileName);
      return;
    }
    DifferenceEvaluator differ =
        new DifferenceEvaluator() {
          @Override
          public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
            // If the test document has an extra attribute not present in the control document
            if (outcome == ComparisonResult.DIFFERENT) {
              if (comparison.getType() == ComparisonType.ATTR_NAME_LOOKUP) {
                if (comparison.getControlDetails().getValue() == null) {
                  return ComparisonResult.SIMILAR;
                } else {
                  return outcome;
                }
              } else if (comparison.getType() == ComparisonType.ELEMENT_NUM_ATTRIBUTES) {
                if (comparison.getControlDetails().getXPath().contains("dest")) {
                  return ComparisonResult.SIMILAR;
                }
              }
              // Downgrade the difference to SIMILAR (or EQUAL if you prefer)

              else if (comparison.getControlDetails().getValue() != null
                  && comparison.getTestDetails().getValue() != null) {
                if (comparison
                    .getControlDetails()
                    .getValue()
                    .toString()
                    .equalsIgnoreCase(comparison.getTestDetails().getValue().toString())) {
                  return ComparisonResult.SIMILAR;
                } else {
                  try {
                    double control =
                        Double.parseDouble(comparison.getControlDetails().getValue().toString());
                    double test =
                        Double.parseDouble(comparison.getTestDetails().getValue().toString());
                    if (Math.abs(control - test) < 0.0001) {
                      return ComparisonResult.SIMILAR;
                    }
                  } catch (NumberFormatException e) {
                    // do nothing
                  }
                }
              }
            }
            return outcome;
          }
        };
    Diff myDiff =
        DiffBuilder.compare(Input.fromStream(sourceStream))
            .withTest(Input.fromStream(targetStream))
            .checkForSimilar()
            .ignoreComments()
            .ignoreWhitespace() // a different order is always 'similar' not equals.
            .withNodeMatcher(nodeMatcher)
            .withDifferenceEvaluator(
                DifferenceEvaluators.chain(DifferenceEvaluators.Default, differ))
            .build();

    assertFalse(
        myDiff.hasDifferences(), String.format("%s has diffs: %s%n", targetFileName, myDiff));
  }
}
