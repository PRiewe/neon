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

package neon.entities.components;

import java.awt.Rectangle;
import java.io.Serializable;

import net.phys2d.raw.Body;
import net.phys2d.raw.shapes.Box;

public class PhysicsComponent implements Component, Serializable {
  private final long uid;
  private final int width;
  private final int height;
  private final double centerX;
  private final double centerY;
  transient private Body theBody;

  public PhysicsComponent(long uid, Rectangle bounds) {
    this.width = bounds.width;
    this.height = bounds.height;
    this.centerX = bounds.getCenterX();
    this.centerY = bounds.getCenterY();
    this.uid = uid;
    theBody = new Body(new Box(bounds.width, bounds.height), 1);

    theBody.setUserData(uid);
    theBody.setEnabled(true);
    theBody.setPosition((float) bounds.getCenterX(), (float) bounds.getCenterY());
  }

  public synchronized Body getTheBody() {
    if(theBody == null) {
      theBody = new Body(new Box(width,height),1);
      theBody.setUserData(uid);
      theBody.setEnabled(true);
      theBody.setPosition((float) centerX, (float) centerY);
    }
    return theBody;
  }

  public boolean isStatic() {
    return false;
  }

  @Override
  public long getUID() {
    return uid;
  }
}
