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

package neon.entities.serialization;

import java.awt.Rectangle;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import neon.ai.AIFactory;
import neon.core.Engine;
import neon.core.GameContext;
import neon.entities.*;
import neon.entities.components.HealthComponent;
import neon.entities.property.Slot;
import neon.magic.SpellFactory;
import neon.resources.RCreature;

// TODO: factions
public class CreatureSerializer {
  private static final long serialVersionUID = -2452444993764883434L;
  private final AIFactory aiFactory;
  private final SpellFactory spellFactory;

  private final GameContext gameContext;

  public CreatureSerializer(GameContext gameContext) {
    this.gameContext = gameContext;
    this.aiFactory = new AIFactory(gameContext);
    spellFactory = new SpellFactory(gameContext.getResourceManageer());
  }

  public Creature deserialize(DataInput in) throws IOException {
    String id = in.readUTF();
    String species = in.readUTF();
    int x = in.readInt();
    int y = in.readInt();
    long uid = in.readLong();
    Creature creature = getCreature(id, x, y, uid, species);
    Rectangle bounds = creature.getShapeComponent();
    bounds.setLocation(x, y);
    creature.brain = aiFactory.getAI(creature);

    HealthComponent health = creature.getHealthComponent();
    health.setHealth(in.readInt());
    health.addBaseHealthMod(in.readFloat());
    health.heal(in.readFloat());
    creature.getInventoryComponent().addMoney(in.readInt());
    creature.getMagicComponent().setBaseModifier(in.readFloat());
    creature.getMagicComponent().setModifier(in.readFloat());
    String spell = in.readUTF();
    if (!spell.isEmpty()) {
      creature.getMagicComponent().equipSpell(spellFactory.getSpell(spell));
    }

    int date = in.readInt();
    if (date != 0) {
      creature.die(date);
    }

    byte iCount = in.readByte();
    for (int i = 0; i < iCount; i++) {
      creature.getInventoryComponent().addItem(in.readLong());
    }

    byte sCount = in.readByte();
    for (int i = 0; i < sCount; i++) {
      creature.getInventoryComponent().put(Slot.valueOf(in.readUTF()), in.readLong());
    }

    sCount = in.readByte();
    for (int i = 0; i < sCount; i++) {
      creature.getScriptComponent().addScript(in.readUTF());
    }

    return creature;
  }

  public void serialize(DataOutput out, Creature creature) throws IOException {
    out.writeUTF(creature.getID());
    out.writeUTF(creature.species.id);
    Rectangle bounds = creature.getShapeComponent();
    out.writeInt(bounds.x);
    out.writeInt(bounds.y);
    out.writeLong(creature.getUID());

    HealthComponent health = creature.getHealthComponent();
    out.writeInt(health.getBaseHealth());
    out.writeFloat(health.getBaseHealthMod());
    out.writeFloat(health.getHealthMod());
    out.writeInt(creature.getInventoryComponent().getMoney());
    out.writeFloat(creature.getMagicComponent().getBaseModifier());
    out.writeFloat(creature.getMagicComponent().getModifier());
    if (creature.getMagicComponent().getSpell() != null) {
      out.writeUTF(creature.getMagicComponent().getSpell().id);
    } else {
      out.writeUTF("");
    }
    out.writeInt(creature.getTimeOfDeath());

    out.writeByte(creature.getInventoryComponent().getItems().size());
    for (long uid : creature.getInventoryComponent()) {
      out.writeLong(uid);
    }

    out.writeByte(creature.getInventoryComponent().slots().size());
    for (Slot slot : creature.getInventoryComponent().slots()) {
      out.writeUTF(slot.name());
      out.writeLong(creature.getInventoryComponent().get(slot));
    }

    out.writeByte(creature.getScriptComponent().getScripts().size());
    for (String script : creature.getScriptComponent().getScripts()) {
      out.writeUTF(script);
    }
  }

  private Creature getCreature(String id, int x, int y, long uid, String species) {
    Creature creature;

    RCreature rc = (RCreature) Engine.getResources().getResource(species);
    creature =
        switch (rc.type) {
          case construct -> new Construct(id, uid, rc);
          case humanoid -> new Hominid(id, uid, rc);
          case daemon -> new Daemon(id, uid, rc);
          case dragon -> new Dragon(id, uid, rc);
          case goblin -> new Hominid(id, uid, rc);
          default -> new Creature(id, uid, rc);
        };

    return creature;
  }
}
