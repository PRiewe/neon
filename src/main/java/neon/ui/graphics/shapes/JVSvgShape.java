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

package neon.ui.graphics.shapes;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * SVG shape implementation using the jsvg library.
 *
 * <p>This class wraps jsvg's SVGDocument to provide full SVG 1.1+ support while maintaining
 * compatibility with the existing JVShape/RenderComponent architecture.
 *
 * <p>SVG content is parsed once during construction and cached for efficient rendering. Supports
 * all SVG features including:
 *
 * <ul>
 *   <li>Basic shapes (circle, rect, ellipse, polygon, path)
 *   <li>Gradients and patterns
 *   <li>Text and transforms
 *   <li>Opacity and blending modes
 * </ul>
 *
 * @since 0.5.0
 */
public class JVSvgShape extends JVShape {
  private static final long serialVersionUID = 1L;

  // The SVGDocument is not serializable, so we mark it transient
  // and store the original SVG content for re-parsing after deserialization
  private transient SVGDocument svgDocument;
  private String svgContent;
  private int x, y;
  private int width, height;

  /**
   * Creates a new SVG shape from the given SVG content string.
   *
   * @param svgContent the SVG content as a string (can be a fragment or complete document)
   * @throws RuntimeException if the SVG content cannot be parsed
   */
  public JVSvgShape(String svgContent) {
    this.svgContent = svgContent;
    // Wrap fragment in complete SVG document if needed
    String completeSvg = wrapSvgFragment(svgContent);

    // Parse SVG using jsvg
    SVGLoader loader = new SVGLoader();
    try (ByteArrayInputStream inputStream =
        new ByteArrayInputStream(completeSvg.getBytes(StandardCharsets.UTF_8))) {
      this.svgDocument = loader.load(inputStream);

      if (svgDocument == null) {
        throw new RuntimeException("Failed to parse SVG content");
      }

      // Extract dimensions from SVG
      com.github.weisj.jsvg.geometry.size.FloatSize size = svgDocument.size();
      this.width = (int) Math.ceil(size.width);
      this.height = (int) Math.ceil(size.height);

    } catch (Exception e) {
      throw new RuntimeException("Failed to load SVG: " + e.getMessage(), e);
    }
  }

  /**
   * Wraps an SVG fragment in a complete SVG document if needed.
   *
   * @param svgContent the SVG content (fragment or complete document)
   * @return a complete SVG document string
   */
  private static String wrapSvgFragment(String svgContent) {
    String trimmed = svgContent.trim();

    // Check if it's already a complete SVG document
    if (trimmed.startsWith("<?xml") || trimmed.startsWith("<svg")) {
      return svgContent;
    }

    // It's a fragment - need to wrap it in a complete SVG document
    // Try to infer dimensions from circle radius if present
    int size = inferFragmentSize(trimmed);

    return String.format(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\" "
            + "viewBox=\"0 0 %d %d\" width=\"%d\" height=\"%d\">"
            + "%s"
            + "</svg>",
        size, size, size, size, trimmed);
  }

  /**
   * Infers the size of an SVG fragment by examining its content.
   *
   * @param fragment the SVG fragment
   * @return the inferred size (width/height)
   */
  private static int inferFragmentSize(String fragment) {
    // Try to extract circle radius
    if (fragment.contains("<circle")) {
      int rIndex = fragment.indexOf("r=\"");
      if (rIndex != -1) {
        int endIndex = fragment.indexOf("\"", rIndex + 3);
        if (endIndex != -1) {
          try {
            int radius = Integer.parseInt(fragment.substring(rIndex + 3, endIndex));
            // Use diameter for size
            return radius * 2;
          } catch (NumberFormatException e) {
            // Fall through to default
          }
        }
      }
    }

    // Default size if we can't infer
    return 10;
  }

  @Override
  public void paint(Graphics2D graphics, float zoom, boolean isSelected) {
    if (svgDocument == null) {
      return;
    }

    // Save original transform
    AffineTransform originalTransform = graphics.getTransform();

    try {
      // Create transform for position and zoom
      AffineTransform transform = new AffineTransform();
      transform.translate(x * zoom, y * zoom);
      transform.scale(zoom, zoom);

      graphics.setTransform(transform);

      // Render the SVG document (passing null for component and ViewBox uses intrinsic size)
      svgDocument.render(null, graphics);

      // Restore original transform for selection overlay
      graphics.setTransform(originalTransform);

      // Draw selection overlay if selected
      if (isSelected && paint != null) {
        graphics.setPaint(paint);
        Rectangle2D selectionRect =
            new Rectangle2D.Float(x * zoom, y * zoom, width * zoom, height * zoom);
        graphics.draw(selectionRect);
      }

    } finally {
      // Ensure we restore the original transform
      graphics.setTransform(originalTransform);
    }
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(x, y, width, height);
  }

  @Override
  public void setX(int x) {
    this.x = x;
  }

  @Override
  public void setY(int y) {
    this.y = y;
  }

  /**
   * Custom serialization to handle non-serializable SVGDocument.
   *
   * @param out the output stream
   * @throws IOException if an I/O error occurs
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    // The svgContent field will be serialized automatically
    // The transient svgDocument field will NOT be serialized
  }

  /**
   * Custom deserialization to re-parse the SVG content.
   *
   * @param in the input stream
   * @throws IOException if an I/O error occurs
   * @throws ClassNotFoundException if the class cannot be found
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    // Re-parse the SVG content to reconstruct the transient svgDocument
    if (svgContent != null) {
      String completeSvg = wrapSvgFragment(svgContent);
      SVGLoader loader = new SVGLoader();
      try (ByteArrayInputStream inputStream =
          new ByteArrayInputStream(completeSvg.getBytes(StandardCharsets.UTF_8))) {
        this.svgDocument = loader.load(inputStream);
      } catch (Exception e) {
        // If re-parsing fails during deserialization, set to null
        // This is safer than throwing an exception during deserialization
        this.svgDocument = null;
      }
    }
  }
}
