package neon.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

public class DataStoreIntegrationTest {

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
    System.out.println(
        fileSystemOut.listFiles(
            newDirFile
                .getAbsolutePath())); // Need to adjust counts if sampleMod scenario is changed.

    InputStream stream = new FileInputStream(zoneFileName);

    String originalZoneFilename = "src/test/resources/sampleMod1" + File.separator + "spells.xml";
    InputStream stream2 = new FileInputStream(originalZoneFilename);
    Diff myDiff =
        DiffBuilder.compare(Input.fromStream(stream))
            .withTest(Input.fromStream(stream2))
            .checkForSimilar()
            .ignoreComments()
            .ignoreWhitespace() // a different order is always 'similar' not equals.
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAttributes("id")))
            .build();

    System.out.format("Diff: %s%n", myDiff.fullDescription());
  }
}
