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

/**
 * A test implementation of {@link RandomSource} that returns values from a predetermined sequence.
 * This allows for fully deterministic testing of random-dependent code.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SequentialRandomSource random = new SequentialRandomSource(5, 10, 15);
 * random.nextInt(0, 100); // returns 5
 * random.nextInt(0, 100); // returns 10
 * random.nextInt(0, 100); // returns 15
 * random.nextInt(0, 100); // returns 5 (wraps around)
 * }</pre>
 *
 * @author mdriesen
 */
public class SequentialRandomSource implements RandomSource {
  private final int[] sequence;
  private int index = 0;

  /**
   * Creates a new SequentialRandomSource with the given sequence of values.
   *
   * @param values the sequence of values to return (will cycle when exhausted)
   * @throws IllegalArgumentException if no values are provided
   */
  public SequentialRandomSource(int... values) {
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("At least one value must be provided");
    }
    this.sequence = values.clone();
  }

  @Override
  public int nextInt(int min, int max) {
    int value = sequence[index];
    index = (index + 1) % sequence.length;
    // Clamp the value to the requested range
    return Math.max(min, Math.min(max, value));
  }

  @Override
  public double nextDouble() {
    int value = sequence[index];
    index = (index + 1) % sequence.length;
    // Convert to a double in range [0.0, 1.0)
    return Math.abs(value % 100) / 100.0;
  }

  /** Resets the sequence index to the beginning. */
  public void reset() {
    index = 0;
  }

  /**
   * Returns the current position in the sequence.
   *
   * @return the current index
   */
  public int getIndex() {
    return index;
  }
}
