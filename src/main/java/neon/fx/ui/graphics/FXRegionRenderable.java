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
import neon.core.Engine;
import neon.maps.Region;
import neon.resources.RTerrain;

/**
 * Renders terrain regions using JavaFX Canvas. Uses character-based textures tiled across the
 * region.
 *
 * @author mdriesen
 */
public class FXRegionRenderable implements FXRenderable {
  private final Region region;
  private int z;

  public FXRegionRenderable(Region region) {
    this.region = region;
    this.z = region.getZ();
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
    Rectangle bounds = region.getBounds();
    if (bounds == null) {
      return;
    }

    // Get terrain resource using the region's texture type ID
    String terrainId = region.getTextureType();
    RTerrain terrain = (RTerrain) Engine.getResources().getResource(terrainId, "terrain");
    if (terrain == null) {
      return;
    }

    // Get glyph and color from terrain
    String glyph = terrain.text;
    String colorName = terrain.color;

    // Get texture for a single tile
    int tileSize = Math.max((int) zoom, 8); // Minimum 8 pixels
    Image texture = FXTextureFactory.getImage(glyph, tileSize, colorName);

    // Fill region with tiled texture
    double x = bounds.x;
    double y = bounds.y;
    double w = bounds.width;
    double h = bounds.height;

    // Tile the texture across the region
    for (double tx = x; tx < x + w; tx++) {
      for (double ty = y; ty < y + h; ty++) {
        gc.drawImage(texture, tx, ty, 1, 1);
      }
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
    Rectangle bounds = region.getBounds();
    if (bounds == null) {
      return new Rectangle2D(0, 0, 0, 0);
    }

    return new Rectangle2D(bounds.x, bounds.y, bounds.width, bounds.height);
  }

  /**
   * Get the region this renderable displays.
   *
   * @return the region
   */
  public Region getRegion() {
    return region;
  }
}
