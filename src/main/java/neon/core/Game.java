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
import neon.systems.timing.Timer;

@Getter
public class Game implements Closeable {
  private final Timer timer = new Timer();
  private final GameStore gameStore;
  private final GameContext gameContext;
  private final Atlas atlas;

  public Game(GameStore gameStore, GameContext gameContext, Atlas atlas) {
    this.gameStore = gameStore;
    this.gameContext = gameContext;
    this.atlas = atlas;
  }

  public Player getPlayer() {
    return gameStore.getPlayer();
  }

  public UIDStore getStore() {
    return gameStore.getUidStore();
  }

  public Atlas getAtlas() {
    return atlas;
  }

  @Override
  public void close() throws IOException {
    gameStore.close();
    getGameContext().getAtlas().close();
  }
}
