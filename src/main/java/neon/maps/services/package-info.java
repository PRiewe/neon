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

/**
 * Service interfaces and implementations for dependency injection in the maps package.
 *
 * <p>This package provides abstractions over engine subsystems to reduce coupling and improve
 * testability. The interfaces define contracts for accessing resources, entities, physics, and
 * quests, while the implementations can delegate to the Engine singleton or provide test doubles.
 *
 * <h2>Service Interfaces:</h2>
 *
 * <ul>
 *   <li>{@link neon.maps.services.EntityStore} - Entity storage and retrieval
 *   <li>{@link neon.maps.services.ResourceProvider} - Resource access
 *   <li>{@link neon.maps.services.PhysicsManager} - Physics system management
 *   <li>{@link neon.maps.services.QuestProvider} - Quest tracking
 * </ul>
 *
 * @author mdriesen
 */
package neon.maps.services;
