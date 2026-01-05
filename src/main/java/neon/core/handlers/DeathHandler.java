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

import lombok.extern.slf4j.Slf4j;
import neon.core.Engine;
import neon.core.event.DeathEvent;
import neon.entities.Creature;
import neon.entities.components.ScriptComponent;
import neon.resources.RScript;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author mdriesen
 */
@Listener(references = References.Strong) // strong, om gc te vermijden
@Slf4j
public class DeathHandler {
  public DeathHandler() {}

  @Handler
  public void handle(DeathEvent de) {
    log.trace("handle {}", de);
    Creature creature = de.getCreature();

    // creature laten doodgaan
    creature.die(de.getTime());

    // scripts draaien op creature
    ScriptComponent sc = creature.getScriptComponent();

    for (String s : sc.getScripts()) {
      RScript rs = (RScript) Engine.getResources().getResource(s, "script");
      Context se = Engine.getScriptEngine();

      se.eval("js", rs.script);
      Value processFunction = se.getBindings("js").getMember("onDeath");
      processFunction.execute("0");
    }
  }
}
