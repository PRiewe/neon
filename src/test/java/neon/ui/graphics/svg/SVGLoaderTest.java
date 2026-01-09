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

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import neon.ui.graphics.shapes.JVShape;
import neon.ui.graphics.shapes.JVSvgShape;
import org.junit.jupiter.api.Test;

/** Unit tests for SVGLoader. */
public class SVGLoaderTest {

  @Test
  public void testLoadSimpleCircle() {
    // Test loading a simple circle fragment (current use case)
    String svg = "<circle r=\"3\" fill=\"forestGreen\" opacity=\"0.2\" />";
    JVShape shape = SVGLoader.loadShape(svg);

    assertNotNull(shape, "Shape should not be null");
    assertTrue(shape instanceof JVSvgShape, "Shape should be a JVSvgShape");

    // Circle with radius 3 should have diameter 6
    Rectangle bounds = shape.getBounds();
    assertEquals(6, bounds.width, "Circle width should be diameter (2 * radius)");
    assertEquals(6, bounds.height, "Circle height should be diameter (2 * radius)");
  }

  @Test
  public void testLoadCompleteSvgDocument() {
    // Test loading a complete SVG document
    String svg =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" width=\"100\""
            + " height=\"100\">"
            + "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"blue\"/>"
            + "</svg>";

    JVShape shape = SVGLoader.loadShape(svg);

    assertNotNull(shape, "Shape should not be null");
    Rectangle bounds = shape.getBounds();
    assertEquals(100, bounds.width, "SVG width should match viewBox");
    assertEquals(100, bounds.height, "SVG height should match viewBox");
  }

  @Test
  public void testLoadRectangle() {
    // Test loading a rectangle (verifies full SVG support beyond circles)
    String svg =
        "<svg viewBox=\"0 0 50 30\">"
            + "<rect x=\"0\" y=\"0\" width=\"50\" height=\"30\" fill=\"red\"/>"
            + "</svg>";

    JVShape shape = SVGLoader.loadShape(svg);

    assertNotNull(shape, "Shape should not be null");
    Rectangle bounds = shape.getBounds();
    assertEquals(50, bounds.width);
    assertEquals(30, bounds.height);
  }

  @Test
  public void testLoadComplexPath() {
    // Test loading a path element (advanced SVG feature)
    String svg =
        "<svg viewBox=\"0 0 20 20\">"
            + "<path d=\"M10 0 L20 20 L0 20 Z\" fill=\"green\"/>"
            + "</svg>";

    JVShape shape = SVGLoader.loadShape(svg);

    assertNotNull(shape, "Shape should not be null");
    Rectangle bounds = shape.getBounds();
    assertEquals(20, bounds.width);
    assertEquals(20, bounds.height);
  }

  @Test
  public void testSetPosition() {
    // Test that position can be set correctly
    String svg = "<circle r=\"5\" fill=\"blue\" />";
    JVShape shape = SVGLoader.loadShape(svg);

    shape.setX(100);
    shape.setY(200);

    Rectangle bounds = shape.getBounds();
    assertEquals(100, bounds.x, "X position should be set");
    assertEquals(200, bounds.y, "Y position should be set");
  }

  @Test
  public void testPaintDoesNotThrow() {
    // Test that painting doesn't throw exceptions
    String svg = "<circle r=\"5\" fill=\"blue\" opacity=\"0.5\" />";
    JVShape shape = SVGLoader.loadShape(svg);

    shape.setX(10);
    shape.setY(10);

    // Create a graphics context to paint to
    BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();

    // Should not throw any exceptions
    assertDoesNotThrow(() -> shape.paint(g2d, 1.0f, false));
    assertDoesNotThrow(() -> shape.paint(g2d, 2.0f, true)); // Test with zoom and selection

    g2d.dispose();
  }

  @Test
  public void testZoomScaling() {
    // Test that different zoom levels work correctly
    String svg = "<circle r=\"5\" fill=\"blue\" />";
    JVShape shape = SVGLoader.loadShape(svg);

    shape.setX(0);
    shape.setY(0);

    BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();

    // Test various zoom levels
    assertDoesNotThrow(() -> shape.paint(g2d, 0.5f, false));
    assertDoesNotThrow(() -> shape.paint(g2d, 1.0f, false));
    assertDoesNotThrow(() -> shape.paint(g2d, 2.0f, false));
    assertDoesNotThrow(() -> shape.paint(g2d, 5.0f, false));

    g2d.dispose();
  }

  @Test
  public void testMultipleCircles() {
    // Test loading SVG with multiple elements
    String svg =
        "<svg viewBox=\"0 0 100 100\">"
            + "<circle cx=\"25\" cy=\"25\" r=\"20\" fill=\"red\"/>"
            + "<circle cx=\"75\" cy=\"75\" r=\"20\" fill=\"blue\"/>"
            + "</svg>";

    JVShape shape = SVGLoader.loadShape(svg);

    assertNotNull(shape, "Shape should not be null");
    Rectangle bounds = shape.getBounds();
    assertEquals(100, bounds.width);
    assertEquals(100, bounds.height);
  }

  @Test
  public void testMalformedXmlThrowsException() {
    // Test that truly malformed XML (not just invalid SVG) throws an exception
    String malformedXml = "<svg><circle r='5' unclosed>";

    assertThrows(
        RuntimeException.class,
        () -> SVGLoader.loadShape(malformedXml),
        "Malformed XML should throw RuntimeException");
  }

  @Test
  public void testEmptyCircle() {
    // Test edge case with radius 0
    String svg = "<circle r=\"0\" fill=\"black\" />";

    JVShape shape = SVGLoader.loadShape(svg);

    assertNotNull(shape, "Shape should not be null even with r=0");
    Rectangle bounds = shape.getBounds();
    assertEquals(0, bounds.width, "Width should be 0 for circle with r=0");
  }

  @Test
  public void testGradient() {
    // Test that gradients are supported (advanced SVG feature)
    String svg =
        "<svg viewBox=\"0 0 100 100\">"
            + "<defs>"
            + "<linearGradient id=\"grad1\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"0%\">"
            + "<stop offset=\"0%\" style=\"stop-color:rgb(255,255,0);stop-opacity:1\" />"
            + "<stop offset=\"100%\" style=\"stop-color:rgb(255,0,0);stop-opacity:1\" />"
            + "</linearGradient>"
            + "</defs>"
            + "<rect width=\"100\" height=\"100\" fill=\"url(#grad1)\"/>"
            + "</svg>";

    JVShape shape = SVGLoader.loadShape(svg);

    assertNotNull(shape, "Shape with gradient should not be null");
    Rectangle bounds = shape.getBounds();
    assertEquals(100, bounds.width);
    assertEquals(100, bounds.height);
  }

  @Test
  public void testTreeItemSvg() {
    // Test with actual tree item SVG from the game (fig tree)
    String svg = "<circle r=\"3\" fill=\"forestGreen\" opacity=\"0.2\" />";
    JVShape shape = SVGLoader.loadShape(svg);

    assertNotNull(shape);

    // Verify it can be rendered
    BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();

    shape.setX(20);
    shape.setY(20);
    assertDoesNotThrow(() -> shape.paint(g2d, 2.0f, false));

    g2d.dispose();

    // Verify dimensions
    assertEquals(6, shape.getBounds().width);
    assertEquals(6, shape.getBounds().height);
  }
}
