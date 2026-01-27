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

package neon.util;

/**
 * The Dice class implements a set of polyhedral dice, as used in many pen-and-paper roleplaying
 * games. It accepts dice with any positive number of sides. Can be instantiated with a specific
 * {@link RandomSource} for deterministic testing.
 *
 * @author mdriesen
 */
public record Dice(RandomSource randomSource) {
  /** Shared default instance for static convenience methods. */
  private static final Dice DEFAULT_INSTANCE = new Dice();

  /** Creates a new Dice with a default (non-deterministic) random source. */
  public Dice() {
    this(new DefaultRandomSource());
  }

  /**
   * Creates a new Dice with a specific random source. Use this constructor for deterministic
   * testing.
   *
   * @param randomSource the random source to use
   */
  public Dice {}

  /**
   * Factory method to create a Dice with a seeded random source for reproducible results.
   *
   * @param seed the seed for the random number generator
   * @return a new Dice instance with seeded random behavior
   */
  public static Dice withSeed(long seed) {
    return new Dice(new DefaultRandomSource(seed));
  }

  /**
   * Static convenience method for rolling dice using the default instance.
   *
   * @param number the amount of dice to roll
   * @param dice the type of dice to roll
   * @param mod the modifier applied to the result of the roll
   * @return the result of (number)d(dice)+(mod)
   */
  public static int roll(int number, int dice, int mod) {
    return DEFAULT_INSTANCE.rollDice(number, dice, mod);
  }

  /**
   * Static convenience method for rolling dice using the default instance.
   *
   * @param roll the string representation of the roll
   * @return the result of the roll
   * @throws NumberFormatException if the string contains an unparsable part
   */
  public static int roll(String roll) {
    return DEFAULT_INSTANCE.rollDice(roll);
  }

  /**
   * Returns the result of a dice roll. The parameters <code>number</code> and <code>dice</code>
   * should be positive, <code>mod</code> can be any integer. If <code>number</code> or <code>dice
   * </code> are not positive, this method returns <code>mod</code>.
   *
   * @param number the amount of dice to roll
   * @param dice the type of dice to roll
   * @param mod the modifier applied to the result of the roll
   * @return the result of (number)d(dice)+(mod)
   */
  public int rollDice(int number, int dice, int mod) {
    if (number < 1 || dice < 1) {
      return mod;
    }
    int result = 0;

    for (int i = 0; i < number; i++) {
      result += randomSource.nextInt(1, dice);
    }

    return result + mod;
  }

  /**
   * Returns the result of a dice roll. The input string has the form 'xdy', 'xdy+z' or 'xdy-z',
   * with x, y and z positive integers.
   *
   * @param roll the string representation of the roll
   * @return the result of the roll
   * @throws NumberFormatException if the string contains an unparsable part
   */
  public int rollDice(String roll) {
    int index1 = roll.indexOf("d");
    int index2 = roll.indexOf("+");
    int index3 = roll.indexOf("-");
    int number = Integer.parseInt(roll.substring(0, index1));
    int dice = 0;
    int mod = 0;

    if (index2 > 0) { // -1 wilt zeggen dat er geen + is gevonden
      dice = Integer.parseInt(roll.substring(index1 + 1, index2));
      mod = Integer.parseInt(roll.substring(index2 + 1));
    } else if (index3 > 0) { // -1 wilt zeggen dat er geen - is gevonden
      dice = Integer.parseInt(roll.substring(index1 + 1, index3));
      mod = -Integer.parseInt(roll.substring(index3 + 1));
    } else {
      dice = Integer.parseInt(roll.substring(index1 + 1));
    }

    return rollDice(number, dice, mod);
  }

  /**
   * Returns the random source used by this Dice instance.
   *
   * @return the random source
   */
  @Override
  public RandomSource randomSource() {
    return randomSource;
  }
}
