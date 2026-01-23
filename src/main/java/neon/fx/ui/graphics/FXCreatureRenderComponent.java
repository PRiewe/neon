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
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import neon.entities.Creature;
import neon.entities.components.ShapeComponent;

/**
 * Renders creatures using JavaFX Canvas. Uses character-based textures from FXTextureFactory.
 *
 * @author mdriesen
 */
public class FXCreatureRenderComponent implements FXRenderable {
  private final Creature creature;
  private int z = 0;

  public FXCreatureRenderComponent(Creature creature) {
    this.creature = creature;
  }

  @Override
  public int getZ() {
    return z;
  }

  @Override
  public void setZ(int z) {
    this.z = z;
  }

  @Override
  public void paint(GraphicsContext gc, float zoom, boolean isSelected) {
    // Get creature components
    ShapeComponent shape = creature.getShapeComponent();
    if (shape == null) {
      return;
    }

    Rectangle bounds = shape.getBounds();
    if (bounds == null) {
      return;
    }

    // Get glyph and color from creature's species
    String glyph = "@"; // Default glyph for creatures
    String colorName = "white"; // Default color

    if (creature.species != null) {
      glyph = creature.species.text;
      colorName = creature.species.color;
    }

    // Get texture
    int size = Math.max((int) (bounds.width * zoom), 8); // Minimum 8 pixels
    Image texture = FXTextureFactory.getImage(glyph, size, colorName);

    // Draw texture at creature position
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

  @Override
  public Rectangle2D getBounds() {
    ShapeComponent shape = creature.getShapeComponent();
    if (shape == null) {
      return new Rectangle2D(0, 0, 0, 0);
    }

    Rectangle bounds = shape.getBounds();
    if (bounds == null) {
      return new Rectangle2D(0, 0, 0, 0);
    }

    return new Rectangle2D(bounds.x, bounds.y, bounds.width, bounds.height);
  }

  /**
   * Get the creature this component renders.
   *
   * @return the creature
   */
  public Creature getCreature() {
    return creature;
  }
}
