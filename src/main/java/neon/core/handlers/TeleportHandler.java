package neon.core.handlers;

import java.awt.*;
import javax.swing.*;
import neon.core.Engine;
import neon.core.GameContext;
import neon.core.GameStores;
import neon.core.event.MessageEvent;
import neon.entities.Door;
import neon.entities.Entity;
import neon.entities.Player;
import neon.maps.*;
import org.graalvm.polyglot.Context;

public class TeleportHandler {
  private final GameStores gameStores;
  private final GameContext gameContext;
  private final AtlasPosition atlasPosition;
  private final Context scriptEngine;
  private final MapLoader mapLoader;
  private final MotionHandler motionHandler;

  public TeleportHandler(
      GameStores gameStores,
      GameContext gameContext,
      AtlasPosition atlasPosition,
      Context scriptEngine) {
    this.gameStores = gameStores;
    this.gameContext = gameContext;
    this.atlasPosition = atlasPosition;
    this.scriptEngine = scriptEngine;
    this.mapLoader =
        new MapLoader(
            gameStores.getFileSystem(),
            gameStores.getStore(),
            gameStores.getResources(),
            gameStores.getZoneFactory());
    this.motionHandler = new MotionHandler(gameStores.getStore());
  }

  /**
   * Teleports a creature. Two results are possible:
   *
   * <ul>
   *   <li>OK - creature was teleported
   *   <li>DOOR - this portal is just a door and does not support teleporting
   * </ul>
   *
   * @param creature the creature to teleport.
   * @param door the portal that the creature used
   * @return the result
   */
  public byte teleport(Player creature, Door door) {
    if (door.portal.isPortal()) {
      Zone previous = atlasPosition.getCurrentZone(); // briefly buffer current zone
      if (door.portal.getDestMap() != 0) {
        // load map and have door refer back
        Map map = gameStores.getAtlas().getMap(door.portal.getDestMap());
        Zone zone = map.getZone(door.portal.getDestZone());
        for (long uid : zone.getItems(door.portal.getDestPos())) {
          Entity i = gameStores.getStore().getEntity(uid);
          if (i instanceof Door) {
            ((Door) i).portal.setDestMap(atlasPosition.getCurrentMap());
          }
        }
        atlasPosition.setMap(map);
        scriptEngine.getBindings("js").putMember("map", map);
        door.portal.setDestMap(atlasPosition.getCurrentMap());
      } else if (door.portal.getDestTheme() != null) {
        Dungeon dungeon =
            mapLoader.loadThemedDungeon(
                door.portal.getDestTheme(), door.portal.getDestTheme(), door.portal.getDestZone());
        atlasPosition.setMap(dungeon);
        door.portal.setDestMap(atlasPosition.getCurrentMap());
      }

      atlasPosition.enterZone(door, previous, creature);

      MotionHandler.walk(creature, door.portal.getDestPos());
      // check if there is a door at the destination, if so, unlock and open this door
      Rectangle bounds = creature.getShapeComponent();
      for (long uid : atlasPosition.getCurrentZone().getItems(bounds)) {
        Entity i = gameStores.getStore().getEntity(uid);
        if (i instanceof Door) {
          ((Door) i).lock.open();
        }
      }

      // if there is a sign on the door, show it now
      if (door.hasSign()) {
        Engine.post(new MessageEvent(door, door.toString(), 3, SwingConstants.BOTTOM));
      }
      return MotionHandler.OK;
    }
    return MotionHandler.DOOR;
  }
}
