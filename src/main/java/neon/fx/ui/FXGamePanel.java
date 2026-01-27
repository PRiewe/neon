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

package neon.fx.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import neon.core.GameContext;
import neon.entities.Player;
import neon.fx.ui.graphics.FXPlayerRenderComponent;
import neon.fx.ui.graphics.FXRenderable;
import neon.fx.ui.graphics.FXRenderableConverter;
import neon.fx.ui.graphics.FXVectorPane;
import neon.ui.graphics.Renderable;

/**
 * JavaFX equivalent of GamePanel. Combines the rendering canvas with HUD overlay showing stats and
 * messages.
 *
 * @author mdriesen
 */
public class FXGamePanel extends BorderPane {
  /**
   * -- GETTER -- Get the vector pane for rendering.
   *
   * @return the FXVectorPane
   */
  @Getter private final FXVectorPane vectorPane;

  private final Label statsLabel;
  private final TextArea messageArea;
  private final VBox statsPanel;

  public FXGamePanel(double width, double height) {
    vectorPane = new FXVectorPane(width, height);

    // Create HUD overlay
    StackPane overlay = new StackPane();
    overlay.setPickOnBounds(false); // Allow mouse events to pass through

    // Stats panel (left side)
    statsPanel = new VBox(10);
    statsPanel.setPadding(new Insets(10));
    statsPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
    statsPanel.setMaxWidth(200);
    statsPanel.setMaxHeight(300);

    statsLabel = new Label("Stats");
    statsLabel.setStyle("-fx-text-fill: white; -fx-font-family: monospace;");
    statsPanel.getChildren().add(statsLabel);
    StackPane.setAlignment(statsPanel, Pos.TOP_LEFT);

    // Message area (bottom right)
    messageArea = new TextArea();
    messageArea.setEditable(false);
    messageArea.setWrapText(true);
    messageArea.setPrefRowCount(5);
    messageArea.setMaxWidth(400);
    messageArea.setMaxHeight(150);
    messageArea.setStyle(
        "-fx-background-color: rgba(0, 0, 0, 0.7); "
            + "-fx-text-fill: white; "
            + "-fx-control-inner-background: rgba(0, 0, 0, 0.7);");
    StackPane.setAlignment(messageArea, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(messageArea, new Insets(10));

    // Add to overlay
    overlay.getChildren().addAll(vectorPane, statsPanel, messageArea);

    // Set as center of BorderPane
    setCenter(overlay);
  }

  /**
   * Update the stats display.
   *
   * @param player the player entity
   */
  public void updateStats(Player player) {
    if (player == null) {
      statsLabel.setText("No player");
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("=== STATS ===\n");
    sb.append(
        String.format(
            "Health: %d/%d\n",
            player.getHealthComponent().getHealth(), player.getHealthComponent().getBaseHealth()));

    // Simplified - POC doesn't show mana yet
    // TODO: Add mana display when Animus component is properly integrated

    sb.append(String.format("STR: %d\n", player.getStatsComponent().getStr()));
    sb.append(String.format("DEX: %d\n", player.getStatsComponent().getDex()));
    sb.append(String.format("CON: %d\n", player.getStatsComponent().getCon()));
    sb.append(String.format("INT: %d\n", player.getStatsComponent().getInt()));
    sb.append(String.format("WIS: %d\n", player.getStatsComponent().getWis()));
    sb.append(String.format("CHA: %d\n", player.getStatsComponent().getCha()));

    statsLabel.setText(sb.toString());
  }

  /**
   * Add a message to the message area.
   *
   * @param message the message to add
   */
  public void addMessage(String message) {
    messageArea.appendText(message + "\n");
    // Auto-scroll to bottom
    messageArea.setScrollTop(Double.MAX_VALUE);
  }

  /** Clear all messages. */
  public void clearMessages() {
    messageArea.clear();
  }

  /**
   * Toggle stats panel visibility.
   *
   * @param visible true to show, false to hide
   */
  public void setStatsVisible(boolean visible) {
    statsPanel.setVisible(visible);
  }

  /**
   * Toggle message area visibility.
   *
   * @param visible true to show, false to hide
   */
  public void setMessagesVisible(boolean visible) {
    messageArea.setVisible(visible);
  }

  /**
   * Repaint the game world by querying the current zone for all entities and updating the vector
   * pane. This is the critical method that connects the JavaFX rendering system to the game's
   * Atlas/Zone system.
   *
   * @param context the game context
   */
  public void repaint(GameContext context) {
    if (context == null || context.getAtlas() == null) {
      return;
    }

    // Set context for lighting (needs to be done before rendering)
    vectorPane.setContext(context);

    // Get visible rectangle based on zoom and camera
    Rectangle visible = getVisibleRectangle();

    // Query Zone for all renderables in visible area (returns Swing Renderables)
    Collection<Renderable> swingRenderables =
        context.getAtlas().getCurrentZone().getRenderables(visible);

    // Convert to FXRenderable collection
    Collection<FXRenderable> fxRenderables = FXRenderableConverter.convertAll(swingRenderables);

    // Add player (highest z-order)
    Player player = context.getPlayer();
    if (player != null) {
      fxRenderables.add(new FXPlayerRenderComponent(player));

      // Update camera to follow player position
      Point playerPos = player.getShapeComponent().getLocation();
      vectorPane.centerOn(playerPos.x, playerPos.y);
    }

    // Pass to vector pane for rendering
    vectorPane.setRenderables(fxRenderables);
  }

  /**
   * Calculate the visible rectangle in world coordinates based on the current camera position and
   * zoom level. This matches the logic from JVectorPane.getVisibleRectangle().
   *
   * @return the visible rectangle
   */
  private Rectangle getVisibleRectangle() {
    float zoom = vectorPane.getZoom();
    double cameraX = vectorPane.getCameraX();
    double cameraY = vectorPane.getCameraY();
    double width = vectorPane.getWidth();
    double height = vectorPane.getHeight();

    return new Rectangle(
        (int) (cameraX / zoom),
        (int) (cameraY / zoom),
        (int) (width / zoom + 1),
        (int) (height / zoom + 1));
  }
}
