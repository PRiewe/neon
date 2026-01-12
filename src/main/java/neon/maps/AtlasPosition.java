package neon.maps;

import neon.entities.Door;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.generators.DungeonGenerator;
import neon.maps.services.EntityStore;
import neon.maps.services.QuestProvider;
import neon.narrative.QuestTracker;
import neon.resources.ResourceManager;

import java.rmi.server.UID;

public class AtlasPosition {
  private int currentZone = 0;
  private int currentMap = 0;
  private final Atlas atlas;
  private final ZoneActivator zoneActivator;
  private final ResourceManager resourceProvider;
  private final QuestTracker questProvider;
  private final UIDStore entityStore;

  public AtlasPosition(
      Atlas atlas,
      ZoneActivator zoneActivator,
      ResourceManager resourceProvider,
      QuestTracker questProvider,
      UIDStore entityStore) {
    this.atlas = atlas;
    this.zoneActivator = zoneActivator;
    this.resourceProvider = resourceProvider;
    this.questProvider = questProvider;
    this.entityStore = entityStore;
  }

  /**
   * @return the current map
   */
  public Map getCurrentMap() {
    return atlas.getMap(currentMap);
  }

  /**
   * @return the current zone
   */
  public Zone getCurrentZone() {
    return atlas.getMap(currentMap).getZone(currentZone);
  }

  /**
   * @return the current zone
   */
  public int getCurrentZoneIndex() {
    return currentZone;
  }

  /**
   * Sets the current zone.
   *
   * @param i the index of the current zone
   */
  public void setCurrentZone(int i, Player player) {
    currentZone = i;
    zoneActivator.activateZone(getCurrentZone(), player);
  }

  /**
   * Enter a new zone through a door.
   *
   * @param door
   * @param previousZone
   */
  public void enterZone(Door door, Zone previousZone, Player player) {
    if (door.portal.getDestZone() > -1) {
      setCurrentZone(door.portal.getDestZone(), player);
    } else {
      setCurrentZone(0, player);
    }

    if (getCurrentMap() instanceof Dungeon && getCurrentZone().isRandom()) {
      new DungeonGenerator(
              getCurrentZone(), entityStore, resourceProvider, (QuestProvider) questProvider)
          .generate(door, previousZone, atlas);
    }
  }

  /**
   * Set the current map.
   *
   * @param map the new current map
   */
  public void setMap(Map map) {
    atlas.putMapIfNeeded(map);
    currentMap = map.getUID();
  }
}
