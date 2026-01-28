/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
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

package neon.core.event;

import java.util.EventObject;
import neon.core.GameContext;
import neon.entities.Creature;
import neon.magic.Effect;
import neon.magic.MagicUtils;
import neon.magic.Spell;
import neon.util.fsm.Action;

public class MagicTask implements Action {
  private final Spell spell;
  private final int stop;
  private final GameContext gameContext;

  public MagicTask(Spell spell, int stop, GameContext gameContext) {
    this.spell = spell;
    this.stop = stop;
    this.gameContext = gameContext;
  }

  public Spell getSpell() {
    return spell;
  }

  public void run(EventObject e) {
    Creature target = (Creature) spell.getTarget();
    if (target.getActiveSpells().contains(spell)) {
      if (stop == gameContext.getTimer().getTime()) {
        MagicUtils.removeSpell(target, spell);
      } else if (spell.getEffect().getDuration() == Effect.REPEAT) {
        spell.getHandler().repeatEffect(spell);
      }
    }
  }
}
