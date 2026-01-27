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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;

/**
 * JavaFX equivalent of JVectorPane. Provides a Canvas-based rendering system with zoom and camera
 * support.
 *
 * @author mdriesen
 */
@Slf4j
public class FXVectorPane extends Pane {
  private final Canvas canvas;
  private final GraphicsContext gc;
  private final List<FXRenderable> renderables = new ArrayList<>();
  private float zoom = 1.0f;
  private double cameraX = 0;
  private double cameraY = 0;
  private AnimationTimer renderLoop;
  private boolean isRendering = false;
  private boolean lightingEnabled = true;
  private GameContext context;

  public FXVectorPane(double width, double height) {
    canvas = new Canvas(width, height);
    gc = canvas.getGraphicsContext2D();

    // Add canvas to pane
    getChildren().add(canvas);

    // Bind canvas size to pane size
    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());

    // Re-render when size changes
    widthProperty().addListener((obs, oldVal, newVal) -> render());
    heightProperty().addListener((obs, oldVal, newVal) -> render());
  }

  /**
   * Set the renderables to display.
   *
   * @param newRenderables the collection of renderables
   */
  public void setRenderables(Collection<FXRenderable> newRenderables) {
    renderables.clear();
    if (newRenderables != null) {
      renderables.addAll(newRenderables);
    }
  }

  /**
   * Set the zoom level.
   *
   * @param zoom the zoom factor (1.0 = 100%)
   */
  public void setZoom(float zoom) {
    this.zoom = Math.max(0.1f, Math.min(5.0f, zoom)); // Clamp between 0.1 and 5.0
  }

  /**
   * Get the current zoom level.
   *
   * @return the zoom factor
   */
  public float getZoom() {
    return zoom;
  }

  /**
   * Update the camera position.
   *
   * @param x the camera X coordinate
   * @param y the camera Y coordinate
   */
  public void updateCamera(double x, double y) {
    this.cameraX = x;
    this.cameraY = y;
  }

  /**
   * Center the camera on a specific point.
   *
   * @param x the X coordinate to center on
   * @param y the Y coordinate to center on
   */
  public void centerOn(double x, double y) {
    this.cameraX = x - (canvas.getWidth() / (2 * zoom));
    this.cameraY = y - (canvas.getHeight() / (2 * zoom));
  }

  /** Render the current frame. */
  public void render() {
    // Clear canvas
    gc.setFill(Color.BLACK);
    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

    // Save transform
    gc.save();

    // Apply camera transform
    gc.translate(-cameraX * zoom, -cameraY * zoom);
    gc.scale(zoom, zoom);

    // Sort renderables by Z-order
    List<FXRenderable> sortedRenderables = new ArrayList<>(renderables);
    sortedRenderables.sort(Comparator.comparingInt(FXRenderable::getZ));

    // Render each renderable
    for (FXRenderable renderable : sortedRenderables) {
      try {
        renderable.paint(gc, zoom, false);
      } catch (Exception e) {
        log.error("Error rendering object", e);
      }
    }

    // Restore transform
    gc.restore();

    // Apply lighting effects (after all entities are rendered)
    if (lightingEnabled && context != null) {
      try {
        FXLightFilter.applyLighting(gc, context, cameraX, cameraY, zoom);
      } catch (Exception e) {
        log.error("Error applying lighting", e);
      }
    }
  }

  /** Start the continuous rendering loop. */
  public void startRendering() {
    if (isRendering) {
      return;
    }

    isRendering = true;
    renderLoop =
        new AnimationTimer() {
          @Override
          public void handle(long now) {
            render();
          }
        };
    renderLoop.start();
  }

  /** Stop the continuous rendering loop. */
  public void stopRendering() {
    if (renderLoop != null) {
      renderLoop.stop();
      renderLoop = null;
    }
    isRendering = false;
  }

  /**
   * Check if continuous rendering is active.
   *
   * @return true if rendering loop is running
   */
  public boolean isRendering() {
    return isRendering;
  }

  /**
   * Get the camera X position.
   *
   * @return the camera X coordinate
   */
  public double getCameraX() {
    return cameraX;
  }

  /**
   * Get the camera Y position.
   *
   * @return the camera Y coordinate
   */
  public double getCameraY() {
    return cameraY;
  }

  /**
   * Set the game context for lighting calculations.
   *
   * @param context the game context
   */
  public void setContext(GameContext context) {
    this.context = context;
  }

  /**
   * Enable or disable lighting effects.
   *
   * @param enabled true to enable lighting, false to disable
   */
  public void setLightingEnabled(boolean enabled) {
    this.lightingEnabled = enabled;
  }

  /**
   * Check if lighting is enabled.
   *
   * @return true if lighting is enabled
   */
  public boolean isLightingEnabled() {
    return lightingEnabled;
  }
}
