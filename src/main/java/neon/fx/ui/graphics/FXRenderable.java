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

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;

/**
 * Interface for objects that can be rendered on a JavaFX Canvas.
 *
 * @author mdriesen
 */
public interface FXRenderable {
  /**
   * Get the Z-order for depth sorting. Higher values are rendered on top.
   *
   * @return the Z-order value
   */
  int getZ();

  /**
   * Set the Z-order for depth sorting.
   *
   * @param z the Z-order value
   */
  void setZ(int z);

  /**
   * Paint this renderable on the given graphics context.
   *
   * @param gc the graphics context
   * @param zoom the current zoom level
   * @param isSelected whether this renderable is selected
   */
  void paint(GraphicsContext gc, float zoom, boolean isSelected);

  /**
   * Get the bounding rectangle for this renderable.
   *
   * @return the bounding rectangle
   */
  Rectangle2D getBounds();
}
