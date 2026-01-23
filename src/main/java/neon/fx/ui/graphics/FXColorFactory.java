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

import java.util.HashMap;
import java.util.Map;
import javafx.scene.paint.Color;

/**
 * Factory for converting color names to JavaFX Color objects. Supports CSS3/X11 color names.
 *
 * @author mdriesen
 */
public class FXColorFactory {
  private static final Map<String, Color> colors = new HashMap<>();

  static {
    // Basic colors
    colors.put("black", Color.BLACK);
    colors.put("white", Color.WHITE);
    colors.put("red", Color.RED);
    colors.put("green", Color.GREEN);
    colors.put("blue", Color.BLUE);
    colors.put("yellow", Color.YELLOW);
    colors.put("cyan", Color.CYAN);
    colors.put("magenta", Color.MAGENTA);
    colors.put("gray", Color.GRAY);
    colors.put("grey", Color.GRAY);
    colors.put("darkGray", Color.DARKGRAY);
    colors.put("lightGray", Color.LIGHTGRAY);
    colors.put("orange", Color.ORANGE);
    colors.put("pink", Color.PINK);
    colors.put("brown", Color.BROWN);
    colors.put("purple", Color.PURPLE);

    // Extended colors (common CSS3 colors)
    colors.put("indianRed", Color.rgb(205, 92, 92));
    colors.put("lightCoral", Color.rgb(240, 128, 128));
    colors.put("salmon", Color.rgb(250, 128, 114));
    colors.put("darkSalmon", Color.rgb(233, 150, 122));
    colors.put("lightSalmon", Color.rgb(255, 160, 120));
    colors.put("crimson", Color.rgb(220, 20, 60));
    colors.put("fireBrick", Color.rgb(178, 34, 34));
    colors.put("darkRed", Color.rgb(139, 0, 0));
    colors.put("lightPink", Color.rgb(255, 182, 193));
    colors.put("hotPink", Color.rgb(255, 105, 180));
    colors.put("deepPink", Color.rgb(255, 20, 147));
    colors.put("gold", Color.GOLD);
    colors.put("silver", Color.SILVER);
    colors.put("beige", Color.BEIGE);
    colors.put("tan", Color.TAN);
    colors.put("khaki", Color.KHAKI);
    colors.put("olive", Color.OLIVE);
    colors.put("lime", Color.LIME);
    colors.put("aqua", Color.AQUA);
    colors.put("teal", Color.TEAL);
    colors.put("navy", Color.NAVY);
    colors.put("maroon", Color.MAROON);
    colors.put("indigo", Color.INDIGO);
    colors.put("violet", Color.VIOLET);
    colors.put("plum", Color.PLUM);
    colors.put("orchid", Color.ORCHID);
    colors.put("lavender", Color.LAVENDER);
    colors.put("thistle", Color.THISTLE);
    colors.put("wheat", Color.WHEAT);
    colors.put("coral", Color.CORAL);
    colors.put("tomato", Color.TOMATO);
    colors.put("sienna", Color.SIENNA);
    colors.put("chocolate", Color.CHOCOLATE);
    colors.put("peru", Color.PERU);
    colors.put("sandyBrown", Color.SANDYBROWN);
    colors.put("goldenRod", Color.GOLDENROD);
    colors.put("darkGoldenRod", Color.DARKGOLDENROD);
    colors.put("darkOrange", Color.DARKORANGE);
    colors.put("forestGreen", Color.FORESTGREEN);
    colors.put("limeGreen", Color.LIMEGREEN);
    colors.put("seaGreen", Color.SEAGREEN);
    colors.put("darkGreen", Color.DARKGREEN);
    colors.put("darkOliveGreen", Color.DARKOLIVEGREEN);
    colors.put("mediumSeaGreen", Color.MEDIUMSEAGREEN);
    colors.put("lightSeaGreen", Color.LIGHTSEAGREEN);
    colors.put("darkSeaGreen", Color.DARKSEAGREEN);
    colors.put("darkCyan", Color.DARKCYAN);
    colors.put("lightCyan", Color.LIGHTCYAN);
    colors.put("turquoise", Color.TURQUOISE);
    colors.put("cadetBlue", Color.CADETBLUE);
    colors.put("steelBlue", Color.STEELBLUE);
    colors.put("lightSteelBlue", Color.LIGHTSTEELBLUE);
    colors.put("powderBlue", Color.POWDERBLUE);
    colors.put("lightBlue", Color.LIGHTBLUE);
    colors.put("skyBlue", Color.SKYBLUE);
    colors.put("lightSkyBlue", Color.LIGHTSKYBLUE);
    colors.put("deepSkyBlue", Color.DEEPSKYBLUE);
    colors.put("dodgerBlue", Color.DODGERBLUE);
    colors.put("cornflowerBlue", Color.CORNFLOWERBLUE);
    colors.put("mediumSlateBlue", Color.MEDIUMSLATEBLUE);
    colors.put("royalBlue", Color.ROYALBLUE);
    colors.put("darkBlue", Color.DARKBLUE);
    colors.put("mediumBlue", Color.MEDIUMBLUE);
    colors.put("midnightBlue", Color.MIDNIGHTBLUE);
    colors.put("darkSlateBlue", Color.DARKSLATEBLUE);
    colors.put("slateBlue", Color.SLATEBLUE);
    colors.put("mediumPurple", Color.MEDIUMPURPLE);
    colors.put("darkMagenta", Color.DARKMAGENTA);
    colors.put("darkViolet", Color.DARKVIOLET);
    colors.put("darkOrchid", Color.DARKORCHID);
    colors.put("mediumOrchid", Color.MEDIUMORCHID);
    colors.put("blueViolet", Color.BLUEVIOLET);
    colors.put("mediumVioletRed", Color.MEDIUMVIOLETRED);
    colors.put("paleVioletRed", Color.PALEVIOLETRED);
  }

  /**
   * Get a JavaFX Color by name.
   *
   * @param name the color name
   * @return the corresponding Color, or WHITE if not found
   */
  public static Color getColor(String name) {
    return colors.getOrDefault(name, Color.WHITE);
  }

  /**
   * Get a JavaFX Color by RGB values.
   *
   * @param r red component (0-255)
   * @param g green component (0-255)
   * @param b blue component (0-255)
   * @return the corresponding Color
   */
  public static Color getColor(int r, int g, int b) {
    return Color.rgb(r, g, b);
  }
}
