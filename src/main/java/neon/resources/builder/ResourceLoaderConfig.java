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

package neon.resources.builder;

import java.util.HashSet;
import java.util.Set;

/**
 * Feature flag configuration for controlling which resources use Jackson XML parsing vs JDOM2.
 * During the migration from JDOM2 to Jackson, this allows gradual rollout and easy rollback.
 *
 * <p>Usage: Add resource types to JACKSON_ENABLED_RESOURCES as they are migrated. To disable
 * Jackson for a resource type, simply remove it from the set.
 *
 * @author mdriesen
 */
public class ResourceLoaderConfig {
  private static final Set<String> JACKSON_ENABLED_RESOURCES = new HashSet<>();

  static {
    // Add resource types as we migrate them to Jackson
    // Example: JACKSON_ENABLED_RESOURCES.add("terrain");
    // Example: JACKSON_ENABLED_RESOURCES.add("sign");
    // Example: JACKSON_ENABLED_RESOURCES.add("creature");
  }

  /**
   * Check if a resource type should use Jackson XML parsing.
   *
   * @param resourceType the type of resource (e.g., "terrain", "creature", "item")
   * @return true if Jackson should be used, false to use JDOM2
   */
  public static boolean useJackson(String resourceType) {
    return JACKSON_ENABLED_RESOURCES.contains(resourceType);
  }

  /**
   * Enable Jackson parsing for a specific resource type.
   *
   * @param resourceType the type of resource to enable
   */
  public static void enableJackson(String resourceType) {
    JACKSON_ENABLED_RESOURCES.add(resourceType);
  }

  /**
   * Disable Jackson parsing for a specific resource type (fallback to JDOM2).
   *
   * @param resourceType the type of resource to disable
   */
  public static void disableJackson(String resourceType) {
    JACKSON_ENABLED_RESOURCES.remove(resourceType);
  }

  /**
   * Get all resource types currently using Jackson.
   *
   * @return set of enabled resource types
   */
  public static Set<String> getEnabledResources() {
    return new HashSet<>(JACKSON_ENABLED_RESOURCES);
  }
}
