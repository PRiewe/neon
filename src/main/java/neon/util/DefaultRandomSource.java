/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2017-2018 - Maarten Driesen
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

package neon.util;

import lombok.Getter;

import java.util.Random;

/**
 * Default implementation of {@link RandomSource} using {@link java.util.Random}. Can be
 * instantiated with a seed for reproducible random sequences.
 *
 * @author mdriesen
 */
@Getter
public class DefaultRandomSource implements RandomSource {
  private final Random random;

  /** Creates a new DefaultRandomSource with a random seed. */
  public DefaultRandomSource() {
    this.random = new Random();
  }

  /**
   * Creates a new DefaultRandomSource with a specific seed for reproducible random sequences.
   *
   * @param seed the seed for the random number generator
   */
  public DefaultRandomSource(long seed) {
    this.random = new Random(seed);
  }

  @Override
  public int nextInt(int min, int max) {
    if (min > max) {
      throw new IllegalArgumentException("min must be <= max");
    }
    return min + random.nextInt(max - min + 1);
  }

  @Override
  public double nextDouble() {
    return random.nextDouble();
  }
}
