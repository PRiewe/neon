package neon.editor.maps;

import neon.editor.DataStore;
import neon.editor.resources.RMap;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;

public class MapEditorTest {
    @Test
    void testLoadSampleModMaps() throws IOException {
        FileSystem fileSystem = new FileSystem();
        fileSystem.mount("src/test/resources/");
        ResourceManager resourceManager = new ResourceManager();
        DataStore dataStore = new DataStore(resourceManager, fileSystem);

        dataStore.loadData("sampleMod1", true, false);

        StubTreeNode root = new StubTreeNode();
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        MapEditor.loadMapsHeadless(        dataStore.getResourceManager().getResources(RMap.class),
                treeModel,
                dataStore
        );

    }
}
