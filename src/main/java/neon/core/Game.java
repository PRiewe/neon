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

package neon.core;

import java.io.Closeable;
import java.io.IOException;
import lombok.Getter;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.maps.AtlasPosition;
import neon.maps.ZoneActivator;
import neon.maps.services.PhysicsManager;
import neon.narrative.QuestTracker;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.systems.timing.Timer;

@Getter
public class Game implements Closeable {
  private final UIDStore store;
  private final Player player;
  private final Timer timer = new Timer();
  private final Atlas atlas;
  private final AtlasPosition atlasPosition;

  public Game(
      Player player,
      FileSystem files,
      PhysicsManager physicsManager,
      ResourceManager resourceManager,
      QuestTracker questTracker) {
    store = new UIDStore(files.getFullPath("uidstore"));
    atlas = new Atlas(files, files.getFullPath("atlas"), store);
    this.player = player;
    this.atlasPosition =
        new AtlasPosition(
            atlas, new ZoneActivator(physicsManager), resourceManager, questTracker, store);
  }

  /**
   * Constructor with dependency injection for testing.
   *
   * @param player the player
   * @param atlas the atlas
   * @param store the UID store
   */
  public Game(Player player, Atlas atlas, UIDStore store, AtlasPosition atlasPosition) {
    this.player = player;
    this.atlas = atlas;
    this.store = store;
    this.atlasPosition = atlasPosition;
  }

  @Override
  public void close() throws IOException {
    store.close();
    atlas.close();
  }
}
