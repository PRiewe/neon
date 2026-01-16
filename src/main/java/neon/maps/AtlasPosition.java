package neon.maps;

import lombok.Getter;
import neon.core.GameStores;
import neon.entities.Door;
import neon.entities.Player;
import neon.maps.generators.DungeonGenerator;
import neon.maps.services.QuestProvider;
import neon.narrative.QuestTracker;

public class AtlasPosition {
  // private int currentZone = 0;
  // private int currentMap = 0;
  private final Atlas atlas;
  private final QuestTracker questProvider;
  private final GameStores gameStores;
  public final ZoneFactory zoneFactory;

  public AtlasPosition(GameStores gameStores, QuestTracker questProvider) {
    this.atlas = gameStores.getAtlas();
    this.questProvider = questProvider;
    this.gameStores = gameStores;
    zoneFactory = new ZoneFactory(gameStores);
  }

  /**
   * @return the current map
   */
  public Map getCurrentMap() {
    return gameStores.getStore().getPlayer().getCurrentMap();
  }

  /**
   * @return the current zone
   */
  public Zone getCurrentZone() {
    return gameStores.getStore().getPlayer().getCurrentZone();
  }

  /**
   * @return the current zone
   */
  public int getCurrentZoneIndex() {
    return gameStores.getStore().getPlayer().getCurrentZone().getIndex();
  }

  /**
   * Sets the current zone.
   *
   * @param i the index of the current zone
   */
  //  public void setCurrentZone(int i, Player player) {
  //    currentZone = i;
  //    zoneActivator.activateZone(getCurrentZone(), player);
  //  }

  /**
   * Enter a new zone through a door.
   *
   * @param door
   * @param previousZone
   */
  public void enterZone(Door door, Zone previousZone, Player player) {
    if (door.portal.getDestZone() > -1) {
      Zone destinationZone =
          zoneFactory.createZone(
              getCurrentMap().getName(), getCurrentMap().getUID(), door.portal.getDestZone());
      player.setCurrentZone(destinationZone);
    } else {
      player.setCurrentZone(getCurrentMap().getZone(0));
    }

    if (getCurrentMap() instanceof Dungeon && getCurrentZone().isRandom()) {
      new DungeonGenerator(getCurrentZone(), (QuestProvider) questProvider, gameStores)
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
    gameStores.getStore().getPlayer().setCurrentMap(map);
  }

  /**
   * Set the current map.
   *
   * @param map the new current map
   */
  public void setMap(World map) {
    atlas.putMapIfNeeded(map);
    gameStores.getStore().getPlayer().setCurrentMap(map);
    gameStores.getStore().getPlayer().setCurrentZone(map.getZone());
  }
}
