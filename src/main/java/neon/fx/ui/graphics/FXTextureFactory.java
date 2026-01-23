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

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for generating character-based textures as JavaFX Images. Textures are cached by
 * character, size, and color.
 *
 * @author mdriesen
 */
@Slf4j
public class FXTextureFactory {
  private static final Map<Integer, Map<String, Image>> images = new HashMap<>();
  private static Font baseFont;

  static {
    // Try to load DejaVu font, fallback to monospace
    try {
      baseFont = Font.loadFont(new FileInputStream(new File("lib/DejaVuSansMono.ttf")), 12);
      if (baseFont == null) {
        throw new Exception("Font loading returned null");
      }
    } catch (Exception e) {
      log.warn("Failed to load DejaVuSansMono.ttf, using system monospace font", e);
      baseFont = Font.font("Monospace", 12);
    }
  }

  /**
   * Get a texture image for the given character, size, and color.
   *
   * @param text the character to render
   * @param size the font size
   * @param colorName the color name
   * @return the generated image
   */
  public static Image getImage(String text, int size, String colorName) {
    Color color = FXColorFactory.getColor(colorName);
    return getImage(text, size, color, FontWeight.NORMAL);
  }

  /**
   * Get a texture image for the given character, size, and color.
   *
   * @param text the character to render
   * @param size the font size
   * @param color the color
   * @return the generated image
   */
  public static Image getImage(String text, int size, Color color) {
    return getImage(text, size, color, FontWeight.NORMAL);
  }

  /**
   * Get a texture image for the given character, size, color, and weight.
   *
   * @param text the character to render
   * @param size the font size
   * @param color the color
   * @param weight the font weight (NORMAL, BOLD, etc.)
   * @return the generated image
   */
  public static Image getImage(String text, int size, Color color, FontWeight weight) {
    String key =
        text
            + "-"
            + color.toString()
            + "-"
            + (weight == FontWeight.BOLD ? "B" : weight == FontWeight.EXTRA_BOLD ? "EB" : "N");

    // Check cache
    if (!images.containsKey(size)) {
      images.put(size, new HashMap<>());
    }
    Map<String, Image> sizeCache = images.get(size);
    if (sizeCache.containsKey(key)) {
      return sizeCache.get(key);
    }

    // Generate new texture
    Canvas canvas = new Canvas(size, size);
    GraphicsContext gc = canvas.getGraphicsContext2D();

    // Clear background
    gc.setFill(Color.TRANSPARENT);
    gc.fillRect(0, 0, size, size);

    // Draw character
    Font font;
    if (baseFont.getFamily().equals("Monospace") || baseFont.getFamily().equals("System")) {
      font = Font.font(baseFont.getFamily(), weight, size);
    } else {
      font = Font.font(baseFont.getFamily(), weight, size * 0.9); // Slightly smaller for TTF fonts
    }

    gc.setFont(font);
    gc.setFill(color);
    gc.setTextAlign(TextAlignment.CENTER);
    gc.setTextBaseline(javafx.geometry.VPos.CENTER);
    gc.fillText(text, size / 2.0, size / 2.0);

    // Convert canvas to image using WritableImage
    WritableImage image = new WritableImage(size, size);
    canvas.snapshot(null, image);

    // Cache and return
    sizeCache.put(key, image);
    return image;
  }

  /** Clear the texture cache. */
  public static void clearCache() {
    images.clear();
  }

  /**
   * Get cache statistics.
   *
   * @return string describing cache contents
   */
  public static String getCacheStats() {
    int totalImages = images.values().stream().mapToInt(Map::size).sum();
    return "FXTextureFactory cache: " + images.size() + " sizes, " + totalImages + " images";
  }
}
