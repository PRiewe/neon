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
import neon.entities.Player;
import neon.entities.components.ShapeComponent;

/**
 * Renders the player character using JavaFX Canvas. Extends creature rendering with player-specific
 * visual indicators (underline for selection, sneaking indicator).
 *
 * @author mdriesen
 */
public class FXPlayerRenderComponent implements FXRenderable {
  private final Player player;
  private int z = Byte.MAX_VALUE - 1; // High z-order, just below max

  public FXPlayerRenderComponent(Player player) {
    this.player = player;
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
    ShapeComponent shape = player.getShapeComponent();
    if (shape == null) {
      return;
    }

    Rectangle bounds = shape.getBounds();
    if (bounds == null) {
      return;
    }

    // Get glyph and color from player's species
    String glyph = "@"; // Default glyph for player
    String colorName = "white"; // Default color

    if (player.species != null) {
      glyph = player.species.text;
      colorName = player.species.color;
    }

    // Get texture
    int size = Math.max((int) (bounds.width * zoom), 8); // Minimum 8 pixels
    Image texture = FXTextureFactory.getImage(glyph, size, colorName);

    // Draw texture at player position
    double x = bounds.x;
    double y = bounds.y;
    double w = bounds.width;
    double h = bounds.height;

    gc.drawImage(texture, x, y, w, h);

    // Draw underline indicator (always visible for player)
    Color color = FXColorFactory.getColor(colorName);
    gc.setStroke(color);
    gc.setLineWidth(2 / zoom);
    gc.strokeLine(x + 2 / zoom, y + h, x + w - 4 / zoom, y + h);

    // Draw sneaking indicator (line at top)
    if (player.isSneaking()) {
      gc.strokeLine(x + 2 / zoom, y, x + w - 4 / zoom, y);
    }

    // Draw selection border if selected
    if (isSelected) {
      gc.setStroke(Color.YELLOW);
      gc.setLineWidth(2 / zoom);
      gc.strokeRect(x, y, w, h);
    }
  }

  @Override
  public Rectangle2D getBounds() {
    ShapeComponent shape = player.getShapeComponent();
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
   * Get the player this component renders.
   *
   * @return the player
   */
  public Player getPlayer() {
    return player;
  }
}
