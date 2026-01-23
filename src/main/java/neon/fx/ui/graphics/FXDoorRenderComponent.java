/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2024 - Maarten Driesen
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

package neon.fx.ui.graphics;

import java.awt.Rectangle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import neon.entities.Door;
import neon.entities.components.Lock;
import neon.entities.components.ShapeComponent;
import neon.resources.RItem;

/**
 * Renders doors using JavaFX Canvas. Rendering takes into account the state of the door (open,
 * locked, or closed).
 *
 * @author mdriesen
 */
public class FXDoorRenderComponent extends FXItemRenderComponent {
  public FXDoorRenderComponent(Door door) {
    super(door);
  }

  @Override
  public void paint(GraphicsContext gc, float zoom, boolean isSelected) {
    ShapeComponent shape = item.getShapeComponent();
    if (shape == null) {
      return;
    }

    Rectangle bounds = shape.getBounds();
    if (bounds == null) {
      return;
    }

    // Get base glyph and color
    String glyph = item.resource.text;
    String colorName = item.resource.color;

    // Check door state and use appropriate glyph
    Lock lock = ((Door) item).lock;
    if (lock != null && item.resource instanceof RItem.Door) {
      RItem.Door doorResource = (RItem.Door) item.resource;
      if (lock.isLocked()) {
        glyph = doorResource.locked;
      } else if (lock.isClosed()) {
        glyph = doorResource.closed;
      }
    }

    // Get texture
    int size = Math.max((int) (bounds.width * zoom), 8); // Minimum 8 pixels
    Image texture = FXTextureFactory.getImage(glyph, size, colorName);

    // Draw texture at door position
    double x = bounds.x;
    double y = bounds.y;
    double w = bounds.width;
    double h = bounds.height;

    gc.drawImage(texture, x, y, w, h);

    // Draw selection border if selected
    if (isSelected) {
      gc.setStroke(Color.YELLOW);
      gc.setLineWidth(2 / zoom);
      gc.strokeRect(x, y, w, h);
    }
  }
}
