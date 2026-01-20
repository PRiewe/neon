package neon.editor;

import neon.editor.resources.RMap;
import neon.resources.*;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.test.TestEngineContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataStoreIntegrationTest {

    @Test
    void loadSampleMod() throws IOException {
        FileSystem fileSystem = new FileSystem();
        fileSystem.mount("src/test/resources/");
        ResourceManager resourceManager = new ResourceManager();
        DataStore dataStore = new DataStore(resourceManager,fileSystem);

        dataStore.loadData("sampleMod1",true,false);
        var map = resourceManager.getAllResources();
        System.out.format("ResourceManager items: %s%n",resourceManager.getAllResources().size());
        var groups = map.entrySet().stream().collect(Collectors.groupingBy(x->x.getValue().getClass()));
        for(var e : groups.entrySet()) {
            System.out.format("%s | %s %n",e.getKey(),e.getValue().size());
        }
        // Need to adjust counts if sampleMod scenario is changed.
        assertEquals(12,groups.getOrDefault(RPerson.class,List.of()).size());
        assertEquals(31,groups.getOrDefault(RTerrain.class, List.of()).size());
        assertEquals(8,groups.getOrDefault(RMap.class, List.of()).size());
    }
}
