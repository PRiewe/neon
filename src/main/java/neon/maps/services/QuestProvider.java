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

package neon.maps.services;

/**
 * Service interface for quest tracking. Provides abstraction over the quest system to reduce
 * coupling.
 *
 * @author mdriesen
 */
public interface QuestProvider {
  /**
   * Gets the next requested quest object that should be spawned.
   *
   * @return the ID of the next requested object, or null if none
   */
  String getNextRequestedObject();
}
