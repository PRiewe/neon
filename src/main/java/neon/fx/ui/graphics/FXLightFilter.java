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

import java.awt.Point;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import neon.core.GameContext;
import neon.entities.Player;
import neon.maps.World;

/**
 * Applies lighting effects to the JavaFX rendering. Creates darkness with elliptical light areas
 * around the player and light sources. Supports day/night cycle for outdoor maps.
 *
 * <p>This is the JavaFX equivalent of the Swing LightFilter (BufferedImageOp).
 *
 * @author mdriesen
 */
public class FXLightFilter {

  /**
   * Apply lighting effects to the rendered scene. This should be called after all entities have
   * been rendered but before the final display.
   *
   * @param gc the graphics context to draw on
   * @param context the game context for accessing player, atlas, and time
   * @param cameraX the camera X position in world coordinates
   * @param cameraY the camera Y position in world coordinates
   * @param zoom the current zoom level
   */
  public static void applyLighting(
      GraphicsContext gc, GameContext context, double cameraX, double cameraY, float zoom) {
    if (context == null
        || context.getPlayer() == null
        || context.getAtlas() == null
        || context.getAtlas().getCurrentZone() == null) {
      return;
    }

    Player player = context.getPlayer();
    double canvasWidth = gc.getCanvas().getWidth();
    double canvasHeight = gc.getCanvas().getHeight();

    // Save current graphics state
    gc.save();

    // Calculate darkness alpha based on map type
    double darknessAlpha;
    if (context.getAtlas().getCurrentMap() instanceof World) {
      // Outdoor map: use day/night cycle
      int hour = (context.getTimer().getTime() / 60 + 12) % 24;
      // Formula: (hour - 12)^2 * 3/2
      // At noon (hour=12): alpha = 0 (full light)
      // At midnight (hour=0 or 24): alpha = 216 (very dark)
      darknessAlpha = (hour - 12) * (hour - 12) * 3.0 / 2.0 / 255.0;
    } else {
      // Indoor map: fixed darkness
      darknessAlpha = 200.0 / 255.0;
    }

    // Calculate light positions and radii
    double playerX = player.getShapeComponent().x;
    double playerY = player.getShapeComponent().y;
    double playerScreenX = (playerX - cameraX) * zoom;
    double playerScreenY = (playerY - cameraY) * zoom;
    double playerLightRadius = 17 * zoom / 2.0;

    // Get light sources
    Map<Point, Integer> lightMap = context.getAtlas().getCurrentZone().getLightMap();

    // Step 1: For each pixel, calculate combined lighting from all light sources
    // We'll draw darkness everywhere, with radial gradients that lighten near light sources
    // Create a composite darkness layer that accounts for all lights at once

    // Draw base darkness everywhere
    gc.setFill(Color.color(0, 0, 0, darknessAlpha));
    gc.fillRect(0, 0, canvasWidth, canvasHeight);

    // Step 2: Draw light circles that "punch through" the darkness
    // Use a lighter blend to create visible light areas
    gc.setGlobalAlpha(0.9); // Make lights slightly transparent so they blend

    // Player light - draw as a radial gradient from full brightness to darkness
    RadialGradient playerLight =
        new RadialGradient(
            0, // focusAngle
            0, // focusDistance
            playerScreenX, // centerX
            playerScreenY, // centerY
            playerLightRadius, // radius
            false, // proportional
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.color(1, 1, 1, darknessAlpha)), // Light center
            new Stop(1, Color.color(0, 0, 0, 0)) // Fade to transparent at edges
            );

    gc.setFill(playerLight);
    gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.LIGHTEN);
    gc.fillOval(
        playerScreenX - playerLightRadius,
        playerScreenY - playerLightRadius,
        playerLightRadius * 2,
        playerLightRadius * 2);

    // Light sources
    for (Point lightPos : lightMap.keySet()) {
      double lightScreenX = (lightPos.x - cameraX) * zoom;
      double lightScreenY = (lightPos.y - cameraY) * zoom;
      double lightRadius = 9 * zoom / 2.0;

      RadialGradient light =
          new RadialGradient(
              0, // focusAngle
              0, // focusDistance
              lightScreenX, // centerX
              lightScreenY, // centerY
              lightRadius, // radius
              false, // proportional
              CycleMethod.NO_CYCLE,
              new Stop(0, Color.color(1, 1, 1, darknessAlpha * 0.7)), // Light center (dimmer)
              new Stop(1, Color.color(0, 0, 0, 0)) // Fade to transparent
              );

      gc.setFill(light);
      gc.fillOval(
          lightScreenX - lightRadius, lightScreenY - lightRadius, lightRadius * 2, lightRadius * 2);
    }

    // Reset blend mode and alpha
    gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER);
    gc.setGlobalAlpha(1.0);

    // Restore graphics state
    gc.restore();
  }
}
