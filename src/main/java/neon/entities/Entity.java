/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012-2013 - Maarten Driesen
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

package neon.entities;

import java.io.*;
import neon.entities.components.PhysicsComponent;
import neon.entities.components.ScriptComponent;
import neon.entities.components.ShapeComponent;

/**
 * This class represents a game entity that can be drawn on screen.
 *
 * @author mdriesen
 */
public abstract class Entity extends EntityBase implements Serializable {

  // components
  public final ShapeComponent bounds;

  private final long uid;
  private final String id;

  /**
   * @param id the id of the resource this entity is an instance of
   * @param uid
   */
  public Entity(String id, long uid) {
    super();
    this.id = id;
    this.uid = uid;
    // components
    bounds = new ShapeComponent(this, 0, 0, 1, 1);
    components.putInstance(PhysicsComponent.class, new PhysicsComponent(uid, bounds));
    components.putInstance(ScriptComponent.class, new ScriptComponent(uid));
  }

  /**
   * @return the uid of this entity
   */
  public long getUID() {
    return uid;
  }

  /**
   * @return the id of the resource this entity is an instance of
   */
  public String getID() {
    return id;
  }

  public ShapeComponent getShapeComponent() {
    return bounds;
  }
}
