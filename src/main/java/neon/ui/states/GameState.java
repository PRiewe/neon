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

package neon.ui.states;

import java.awt.event.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EventObject;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;
import neon.core.GameStores;
import neon.core.ScriptInterface;
import neon.core.event.*;
import neon.core.handlers.CombatUtils;
import neon.core.handlers.TurnHandler;
import neon.entities.components.HealthComponent;
import neon.entities.property.Attribute;
import neon.resources.CClient;
import neon.resources.RScript;
import neon.ui.*;
import neon.ui.dialog.MapDialog;
import neon.util.fsm.*;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import net.phys2d.raw.CollisionEvent;
import net.phys2d.raw.CollisionListener;

@Slf4j
public class GameState extends State implements KeyListener, CollisionListener {
  private final GamePanel panel;
  private final CClient keys;
  private final MBassador<EventObject> bus;
  private final UserInterface ui;
  private final GameContext context;
  private final GameStores gameStores;

  public GameState(
      State parent,
      MBassador<EventObject> bus,
      UserInterface ui,
      GameContext context,
      GameStores gameStores) {
    super(parent, "game module");
    this.bus = bus;
    this.ui = ui;
    this.context = context;
    keys = (CClient) gameStores.getResources().getResource("client", "config");
    panel = new GamePanel(gameStores.getStore(), new CombatUtils(gameStores.getStore()), context);
    this.gameStores = gameStores;
    setVariable("panel", panel);

    // makes functions available for scripting:
    context
        .getScriptEngine()
        .getBindings("js")
        .putMember("engine", new ScriptInterface(panel, gameStores.getStore(), context));
    bus.subscribe(new TurnHandler(panel, gameStores, context));
  }

  @Override
  public void enter(TransitionEvent e) {
    ui.showPanel(panel);
    if (e.toString().equals("start")) {
      context.getPhysicsEngine().addListener(this);
      // in case game starts, the events of the current clock tick must be executed now
      bus.publishAsync(new TurnEvent(context.getTimer().getTime(), true));
    }
    panel.setVisible(true);
    panel.addKeyListener(this);
    panel.repaint();

    if (e.getParameter("message") != null) {
      panel.print(e.getParameter("message").toString());
    }
  }

  @Override
  public void exit(TransitionEvent t) {
    panel.removeKeyListener(this);
  }

  public void keyTyped(KeyEvent key) {
    switch (key.getKeyChar()) {
      case '+':
        panel.zoomIn();
        break;
      case '-':
        panel.zoomOut();
        break;
    }
  }

  public void keyReleased(KeyEvent key) {}

  public void keyPressed(KeyEvent key) {
    int code = key.getKeyCode();
    switch (code) {
      case KeyEvent.VK_CONTROL -> bus.publishAsync(new TransitionEvent("inventory"));
      case KeyEvent.VK_F5 -> save(false);
      case KeyEvent.VK_ESCAPE -> save(true);
      case KeyEvent.VK_F1 -> {
        InputStream input = GameState.class.getResourceAsStream("/neon/core/help.html");
        String help = new Scanner(input, StandardCharsets.UTF_8).useDelimiter("\\A").next();
        ui.showHelp(help);
      }
      case KeyEvent.VK_F2 -> panel.toggleHUD();
      case KeyEvent.VK_F3 -> ui.showConsole(context.getScriptEngine());
      default -> {
        if (code == keys.map) {
          new MapDialog(
                  ui.getWindow(),
                  context.getAtlasPosition().getCurrentZone(),
                  gameStores.getStore())
              .show();
        } else if (code == keys.sneak) {
          gameStores
              .getStore()
              .getPlayer()
              .setSneaking(!gameStores.getStore().getPlayer().isSneaking());
          panel.repaint();
        } else if (code == keys.journal) {
          bus.publishAsync(new TransitionEvent("journal"));
        }
      }
    }
  }

  private void save(boolean quit) {
    if (quit) {
      if (ui.showQuestion("Do you wish to quit?")) {
        if (ui.showQuestion("Do you wish to save?")) {
          bus.publish(new SaveEvent(this));
        }
        context.quit();
      } else {
        panel.repaint();
      }
    } else {
      if (ui.showQuestion("Do you wish to save?")) {
        bus.publish(new SaveEvent(this));
      }
      panel.repaint();
    }
  }

  // for now only check if the player is on a region that should run a script
  public void collisionOccured(CollisionEvent event) {
    Object one = event.getBodyA().getUserData();
    Object two = event.getBodyB().getUserData();

    try {
      if (one.equals(0L) && two instanceof neon.maps.Region) {
        for (String s : ((neon.maps.Region) two).getScripts()) {
          RScript rs = (RScript) gameStores.getResources().getResource(s, "script");
          context.execute(rs.script);
        }
      } else if (one instanceof neon.maps.Region && two.equals(0L)) {
        for (String s : ((neon.maps.Region) one).getScripts()) {
          RScript rs = (RScript) gameStores.getResources().getResource(s, "script");
          context.execute(rs.script);
        }
      }
    } catch (Exception e) {
    }
  }

  public void separationOccured(CollisionEvent event) {
    // Handle separation if needed
  }

  @Handler
  public void handleCombat(CombatEvent ce) {
    log.trace("handleCombat {}", ce);
    if (ce.isFinished()) {
      if (ce.getDefender() == gameStores.getStore().getPlayer()) {
        panel.print("You were attacked!");
      } else {
        switch (ce.getResult()) {
          case CombatEvent.DODGE -> panel.print("The " + ce.getDefender() + " dodges the attack.");
          case CombatEvent.BLOCK -> panel.print("The " + ce.getDefender() + " blocks the attack.");
          case CombatEvent.ATTACK -> {
            HealthComponent health = ce.getDefender().getHealthComponent();
            panel.print("You strike the " + ce.getDefender() + " (" + health.getHealth() + ").");
          }
          case CombatEvent.DIE -> {
            panel.print("You killed the " + ce.getDefender() + ".");
            bus.publishAsync(new DeathEvent(ce.getDefender(), context.getTimer().getTime()));
          }
        }
      }
    }
  }

  @Handler
  public void handleSkill(SkillEvent se) {
    log.trace("handleSkill {}", se);
    if (se.getStat() != Attribute.NONE) {
      panel.print("Stat raised: " + se.getSkill().stat);
    } else if (se.hasLevelled()) {
      panel.print("Level up.");
    } else {
      panel.print("Skill raised: " + se.getSkill() + ".");
    }
  }
}
