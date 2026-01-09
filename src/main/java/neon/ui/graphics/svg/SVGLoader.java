/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
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

package neon.ui.graphics.svg;

import java.awt.Color;
import neon.ui.graphics.shapes.*;
import neon.util.ColorFactory;
import org.jdom2.Element;

/**
 * SVG loader that uses the jsvg library for full SVG 1.1+ support.
 *
 * <p>This class provides a simple interface for loading SVG content and converting it to JVShape
 * instances that can be rendered in the game engine.
 */
public class SVGLoader {
  /**
   * Loads an SVG shape from a string.
   *
   * <p>This method uses the jsvg library to parse and render SVG content. It supports all SVG 1.1+
   * features including circles, rectangles, paths, polygons, gradients, and text.
   *
   * @param svgContent the SVG content as a string (can be a fragment or complete document)
   * @return a JVShape that can be rendered
   * @throws RuntimeException if the SVG cannot be parsed
   */
  public static JVShape loadShape(String svgContent) {
    return new JVSvgShape(svgContent);
  }

  /**
   * Loads an SVG shape from a JDOM Element.
   *
   * @param shape the JDOM element containing SVG shape data
   * @return a JVShape that can be rendered
   * @deprecated Use {@link #loadShape(String)} instead. This method uses the legacy JDOM-based
   *     parser that only supports circles. The String-based method uses jsvg for full SVG support.
   */
  @Deprecated
  public static JVShape loadShape(Element shape) {
    // Legacy implementation for backward compatibility
    Color color = ColorFactory.getColor(shape.getAttributeValue("fill"));
    if (shape.getAttribute("opacity") != null) {
      int opacity = (int) (Float.parseFloat(shape.getAttributeValue("opacity")) * 255);
      color = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }

    if (shape.getName().equals("circle")) {
      int radius = Integer.parseInt(shape.getAttributeValue("r"));
      return new JVEllipse(radius, color);
    } else {
      return new JVRectangle(null, null);
    }
  }
}
