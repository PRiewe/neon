/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
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

package neon.maps.services;

import java.util.Vector;
import neon.core.UIEngineContext;
import neon.resources.Resource;

/**
 * Adapter implementation of ResourceProvider that delegates to a GameContext. This class provides
 * proper dependency injection without relying on deprecated Engine static methods.
 *
 * @author mdriesen
 */
public class GameContextResourceProvider implements ResourceProvider {
  private final UIEngineContext context;

  public GameContextResourceProvider(UIEngineContext context) {
    this.context = context;
  }

  @Override
  public Resource getResource(String id) {
    return context.getResources().getResource(id);
  }

  @Override
  public Resource getResource(String id, String type) {
    return context.getResources().getResource(id, type);
  }

  @Override
  public <T extends Resource> Vector<T> getResources(Class<T> rRecipeClass) {
    return context.getResources().getResources(rRecipeClass);
  }
}
