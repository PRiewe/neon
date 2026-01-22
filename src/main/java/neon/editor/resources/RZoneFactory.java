package neon.editor.resources;

import lombok.Getter;
import neon.editor.DataStore;
import neon.resources.RPerson;
import org.jdom2.Element;

public class RZoneFactory {
  @Getter private final DataStore dataStore;

  public RZoneFactory(DataStore dataStore) {
    this.dataStore = dataStore;
  }

  public Instance getInstance(Element e, RZone zone) {
    if (dataStore.getResourceManager().getResource(e.getAttributeValue("id")) instanceof RPerson) {
      return new IPerson(dataStore.getResourceManager(), e);
    } else if (e.getName().equals("door")) {
      return new IDoor(dataStore.getResourceManager(), e, zone);
    } else if (e.getName().equals("container")) {
      return new IContainer(dataStore.getResourceManager(), e);
    } else {
      return new IObject(dataStore.getResourceManager(), e);
    }
  }

  public RZone newRZone(Element properties, RMap map, String... path) {
    return new RZone(dataStore.getResourceManager(), properties, map, path);
  }
}
