package neon.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
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
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;

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

  @Test
  void loadAndSaveSampleMod() throws IOException {
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

    for (String file : fileList) {
      compareXmlFiles("src/test/resources/sampleMod1", newDirFile.getAbsolutePath(), file);
    }
  }

  private void compareXmlFiles(String sourceFolder, String targetFolder, String fileName) {
    String targetFileName = targetFolder + File.separator + fileName;
    String sourceFileName = sourceFolder + File.separator + fileName;
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
                "event", ElementSelectors.byNameAndAttributes("script")),
            ElementSelectors.byNameAndAttributes("id"));
    if (fileName.startsWith("objects")) {
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
            if (comparison.getControlDetails().getValue() != null
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

    if (fileName.equals("objects/items.xml")) {
      System.out.format("%s has diffs: %s%n", targetFileName, myDiff.fullDescription());
    } else if (myDiff.hasDifferences()) {
      System.out.format("%s has diffs: %s%n", targetFileName, myDiff.toString());
    } else {
      System.out.format("%s matches%n", fileName);
    }
  }
}
