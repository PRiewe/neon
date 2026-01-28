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

package neon.editor.services;

import java.util.Vector;
import neon.editor.Editor;
import neon.maps.services.ResourceProvider;
import neon.resources.Resource;

/**
 * Editor-specific implementation of ResourceProvider that delegates to Editor.resources. This
 * allows editor components to use map generators without depending on Engine static methods.
 *
 * @author mdriesen
 */
public class EditorResourceProvider implements ResourceProvider {
  @Override
  public Resource getResource(String id) {
    return Editor.resources.getResource(id);
  }

  @Override
  public Resource getResource(String id, String type) {
    return Editor.resources.getResource(id, type);
  }

  @Override
  public <T extends Resource> Vector<T> getResources(Class<T> rRecipeClass) {
    return Editor.resources.getResources(rRecipeClass);
  }
}
