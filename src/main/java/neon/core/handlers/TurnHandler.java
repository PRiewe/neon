/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.core.handlers;

import java.awt.Rectangle;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import neon.core.Configuration;
import neon.core.Engine;
import neon.core.event.TurnEvent;
import neon.core.event.UpdateEvent;
import neon.entities.Creature;
import neon.entities.Player;
import neon.entities.components.HealthComponent;
import neon.entities.property.Condition;
import neon.entities.property.Habitat;
import neon.maps.*;
import neon.maps.Region.Modifier;
import neon.maps.generators.TownGenerator;
import neon.maps.generators.WildernessGenerator;
import neon.resources.CServer;
import neon.resources.RRegionTheme;
import neon.ui.GamePanel;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

@Listener(references = References.Strong) // strong, to avoid gc
@Slf4j
public class TurnHandler {
  private GamePanel panel;
  private Generator generator;
  private int range;

  public TurnHandler(GamePanel panel) {
    this.panel = panel;

    CServer ini = (CServer) Engine.getResources().getResource("ini", "config");
    range = ini.getAIRange();
  }

  @Handler
  public void tick(TurnEvent te) {
    log.trace("tick {}", te);
    // if turn is done (timer gets extra tick):
    //	1) check random regions
    //	2) check monsters
    //	3) check player

    // check if terrain should be generated
    if (Configuration.gThread) {
      if (generator == null || !generator.isAlive()) {
        generator = new Generator();
        generator.start();
      }
    } else {
      checkRegions();
    }

    // check monsters
    Player player = Engine.getPlayer();
    for (long uid : Engine.getAtlas().getCurrentZone().getCreatures()) {
      Creature creature = (Creature) Engine.getStore().getEntity(uid);
      if (!creature.hasCondition(Condition.DEAD)) {
        HealthComponent health = creature.getHealthComponent();
        health.heal(creature.getStatsComponent().getCon() / 100f);
        creature.getMagicComponent().addMana(creature.getStatsComponent().getWis() / 100f);
        Rectangle pBounds = player.getShapeComponent();
        Rectangle cBounds = creature.getShapeComponent();
        if (pBounds.getLocation().distance(cBounds.getLocation()) < range) {
          int spd = getSpeed(creature);
          Region region = Engine.getAtlas().getCurrentZone().getRegion(cBounds.getLocation());
          if (creature.species.habitat == Habitat.LAND && region.getMovMod() == Modifier.SWIM) {
            spd = spd / 4; // swimming creatures have penalty
          }
          if (player.isSneaking()) {
            spd = spd * 2; // player gets penalty when sneaking
          }

          while (spd > getSpeed(player) * Math.random()) {
            creature.brain.act();
            spd -= getSpeed(player);
          }
        }
      }
    }

    // prepare player for next turn
    HealthComponent health = player.getHealthComponent();
    health.heal(player.getStatsComponent().getCon() / 100f);
    player.getMagicComponent().addMana(player.getStatsComponent().getWis() / 100f);

    // and update systems
    Engine.getPhysicsEngine().update();
    Engine.post(new UpdateEvent(this));
  }

  private class Generator extends Thread {
    @Override
    public void run() {
      // only repaint after something has been generated
      if (checkRegions()) {
        Engine.post(new UpdateEvent(this));
      }
    }
  }

  /*
   * Checks if any regions are visible that should be randomly generated.
   */
  private boolean checkRegions() { // this boolean is actually just sketchy
    Rectangle window = panel.getVisibleRectangle();
    Zone zone = Engine.getAtlas().getCurrentZone();
    boolean fixed = true;
    boolean generated = false; // to indicate that something was generated

    do {
      Collection<Region> buffer = zone.getRegions(window);
      fixed = true;
      for (Region r : buffer) {
        if (!r.isFixed()) {
          generated = true;
          fixed = false;
          RRegionTheme theme = r.getTheme();
          r.fix(); // from here theme becomes null
          if (theme.id.startsWith("town")) {
            new TownGenerator(zone)
                .generate(r.getX(), r.getY(), r.getWidth(), r.getHeight(), theme, r.getZ());
          } else {
            new WildernessGenerator(zone).generate(r, theme);
          }
        }
      }
    } while (fixed == false);

    return generated;
  }

  /*
   * @return	a creature's speed
   */
  private static int getSpeed(Creature creature) {
    int penalty = 3;
    if (InventoryHandler.getWeight(creature) > 9 * creature.species.str) {
      return 0;
    } else if (InventoryHandler.getWeight(creature) > 6 * creature.species.str) {
      penalty = 1;
    } else if (InventoryHandler.getWeight(creature) > 3 * creature.species.str) {
      penalty = 2;
    }
    return (creature.getStatsComponent().getSpd()) * penalty / 3;
  }
}
