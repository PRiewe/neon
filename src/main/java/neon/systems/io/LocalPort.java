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

package neon.systems.io;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;

import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.IBusConfiguration;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

/**
 * A {@code Port} to connect client and server running locally.
 *
 * @author mdriesen
 */
@Listener(references = References.Strong) // strong, to avoid gc
@Slf4j
public class LocalPort extends Port {
  private Collection<EventObject> buffer =
      Collections.synchronizedCollection(new ArrayDeque<EventObject>());
  private LocalPort peer;
  private final String name;

  public LocalPort(String name) {
    super(new BusConfiguration().setProperty(IBusConfiguration.Properties.BusId, name));
    this.name = name;
    bus.subscribe(this);
  }

  /**
   * Connects this {@code LocalPort} to another {@code LocalPort}.
   *
   * @param peer another {@code LocalPort}
   */
  public void connect(LocalPort peer) {
    this.peer = peer;
  }

  @Override
  @Handler
  public void receive(EventObject event) {
    log.trace("{} received {}",this.name,event);
    // ensure that already processed events are not sent back again
    if (!buffer.remove(event)) {
      peer.write(event);
    }
  }

  private void write(EventObject event) {
    buffer.add(event);
    // no async, otherwise save and quit won't work anymore
    bus.publish(event);
  }
}
