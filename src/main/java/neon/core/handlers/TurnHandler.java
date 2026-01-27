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
import neon.core.GameContext;
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
import neon.maps.services.EntityStore;
import neon.maps.services.GameContextEntityStore;
import neon.maps.services.GameContextResourceProvider;
import neon.maps.services.ResourceProvider;
import neon.resources.CServer;
import neon.resources.RRegionTheme;
import neon.ui.GamePanel;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

@Listener(references = References.Strong) // strong, om gc te vermijden
@Slf4j
public class TurnHandler {
  private final GamePanel panel;
  private Generator generator;
  private final int range;
  private final EntityStore entityStore;
  private final ResourceProvider resourceProvider;
  private final GameContext gameContext;

  public TurnHandler(GamePanel panel, GameContext gameContext) {
    this.panel = panel;
    this.entityStore = new GameContextEntityStore(panel.getContext());
    this.resourceProvider = new GameContextResourceProvider(panel.getContext());
    this.gameContext = gameContext;

    CServer ini = (CServer) panel.getContext().getResources().getResource("ini", "config");
    range = ini.getAIRange();
  }

  @Handler
  public void tick(TurnEvent te) {
    log.trace("tick {}", te);
    // indien beurt gedaan is (timer krijgt extra tick):
    //	1) random regions controleren
    //	2) monsters controleren
    //	3) speler controleren

    // kijken of terrain moet gegenereerd worden
    if (Configuration.gThread) {
      if (generator == null || !generator.isAlive()) {
        generator = new Generator();
        generator.start();
      }
    } else {
      checkRegions();
    }

    // monsters controleren
    Player player = panel.getContext().getPlayer();
    for (long uid : panel.getContext().getAtlas().getCurrentZone().getCreatures()) {
      Creature creature = (Creature) panel.getContext().getStore().getEntity(uid);
      if (!creature.hasCondition(Condition.DEAD)) {
        HealthComponent health = creature.getHealthComponent();
        health.heal(creature.getStatsComponent().getCon() / 100f);
        creature.getMagicComponent().addMana(creature.getStatsComponent().getWis() / 100f);
        Rectangle pBounds = player.getShapeComponent();
        Rectangle cBounds = creature.getShapeComponent();
        if (pBounds.getLocation().distance(cBounds.getLocation()) < range) {
          int spd = getSpeed(creature);
          Region region =
              panel.getContext().getAtlas().getCurrentZone().getRegion(cBounds.getLocation());
          if (creature.species.habitat == Habitat.LAND && region.getMovMod() == Modifier.SWIM) {
            spd = spd / 4; // zwemmende creatures hebben penalty
          }
          if (player.isSneaking()) {
            spd = spd * 2; // player krijgt penalty bij sneaken
          }

          while (spd > getSpeed(player) * Math.random()) {
            creature.brain.act();
            spd -= getSpeed(player);
          }
        }
      }
    }

    // player in gereedheid brengen voor volgende beurt
    HealthComponent health = player.getHealthComponent();
    health.heal(player.getStatsComponent().getCon() / 100f);
    player.getMagicComponent().addMana(player.getStatsComponent().getWis() / 100f);

    // en systems updaten
    panel.getContext().getPhysicsEngine().update();
    panel.getContext().post(new UpdateEvent(this));
  }

  private class Generator extends Thread {
    @Override
    public void run() {
      // enkel repainten nadat er iets gegenereerd is
      if (checkRegions()) {
        panel.getContext().post(new UpdateEvent(this));
      }
    }
  }

  /*
   * Checks if any regions are visible that should be randomly generated.
   */
  private boolean checkRegions() { // die boolean is eigenlijk maar louche
    Rectangle window = panel.getVisibleRectangle();
    Zone zone = panel.getContext().getAtlas().getCurrentZone();
    boolean fixed = true;
    boolean generated = false; // om aan te geven dat er iets gegenereerd werd

    do {
      Collection<Region> buffer = zone.getRegions(window);
      fixed = true;
      for (Region r : buffer) {
        if (!r.isFixed()) {
          generated = true;
          fixed = false;
          RRegionTheme theme = r.getTheme();
          r.fix(); // vanaf hier wordt theme null
          if (theme.id.startsWith("town")) {
            new TownGenerator(zone, gameContext)
                .generate(r.getX(), r.getY(), r.getWidth(), r.getHeight(), theme, r.getZ());
          } else {
            new WildernessGenerator(zone, gameContext).generate(r, theme);
          }
        }
      }
    } while (!fixed);

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
