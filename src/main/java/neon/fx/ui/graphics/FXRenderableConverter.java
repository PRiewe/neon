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
import neon.core.Engine;
import neon.entities.Creature;
import neon.entities.Door;
import neon.entities.Entity;
import neon.entities.Item;
import neon.entities.Player;
import neon.entities.components.RenderComponent;
import neon.maps.Region;
import neon.ui.graphics.Renderable;

/**
 * Utility class to convert Swing Renderables to JavaFX FXRenderables. This is a temporary bridge
 * during the migration to JavaFX, allowing the existing Zone.getRenderables() method to work with
 * the new JavaFX rendering system.
 *
 * @author mdriesen
 */
public class FXRenderableConverter {

  /**
   * Convert a collection of Swing Renderables to FXRenderables.
   *
   * @param swingRenderables the Swing renderables to convert
   * @return a collection of FXRenderables
   */
  public static Collection<FXRenderable> convertAll(Collection<Renderable> swingRenderables) {
    Collection<FXRenderable> fxRenderables = new ArrayList<>();
    for (Renderable r : swingRenderables) {
      FXRenderable converted = convert(r);
      if (converted != null) {
        fxRenderables.add(converted);
      }
    }
    return fxRenderables;
  }

  /**
   * Convert a single Swing Renderable to an FXRenderable.
   *
   * @param renderable the Swing renderable to convert
   * @return the equivalent FXRenderable, or null if conversion not supported
   */
  public static FXRenderable convert(Renderable renderable) {
    if (renderable == null) {
      return null;
    }

    // Check for Region (doesn't have UID)
    if (renderable instanceof Region) {
      return new FXRegionRenderable((Region) renderable);
    }

    // For entity render components, get the entity via UID
    // RenderComponent extends Renderable and adds getUID() method
    if (!(renderable instanceof RenderComponent)) {
      // Not a RenderComponent - cannot get UID
      System.err.println("Warning: Unknown Renderable type: " + renderable.getClass().getName());
      return null;
    }

    RenderComponent renderComponent = (RenderComponent) renderable;
    long uid = renderComponent.getUID();
    if (uid == 0) {
      // No UID - skip
      return null;
    }

    Entity entity = Engine.getStore().getEntity(uid);
    if (entity == null) {
      System.err.println("Warning: Entity not found for UID: " + uid);
      return null;
    }

    // Check entity type and create appropriate FX render component
    // Check Player first (more specific than Creature)
    if (entity instanceof Player) {
      return new FXPlayerRenderComponent((Player) entity);
    }

    // Check Door (more specific than Item)
    if (entity instanceof Door) {
      return new FXDoorRenderComponent((Door) entity);
    }

    // Check Item
    if (entity instanceof Item) {
      return new FXItemRenderComponent((Item) entity);
    }

    // Check Creature
    if (entity instanceof Creature) {
      return new FXCreatureRenderComponent((Creature) entity);
    }

    // Unknown entity type
    System.err.println("Warning: Unknown entity type: " + entity.getClass().getName());
    return null;
  }
}
