package neon.editor;

import neon.editor.resources.RMap;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.test.TestEngineContext;
import org.junit.jupiter.api.Test;

public class DataStoreIntegrationTest {


    void loadSampleMod() {
        FileSystem fileSystem = new FileSystem("src/test/resources/");
        ResourceManager resourceManager = new ResourceManager();
        DataStore dataStore = new DataStore(resourceManager,fileSystem);

        dataStore.loadData("sampleMod1",true,false);
        System.out.format("ResourceManager items: %s",resourceManager.getResources(RMap.class));
    }
}
