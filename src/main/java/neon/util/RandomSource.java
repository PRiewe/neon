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

import java.util.Random;

/**
 * Interface for random number generation that can be implemented with deterministic behavior for
 * testing purposes.
 *
 * @author mdriesen
 */
public interface RandomSource {
  /**
   * Returns a random integer between min and max (inclusive).
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (inclusive)
   * @return a random integer in the range [min, max]
   */
  int nextInt(int min, int max);

  /**
   * Returns a random double between 0.0 (inclusive) and 1.0 (exclusive).
   *
   * @return a random double in the range [0.0, 1.0)
   */
  double nextDouble();

  Random getRandom();
}
