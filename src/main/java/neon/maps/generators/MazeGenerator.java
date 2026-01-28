/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2010 - Maarten Driesen
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

package neon.maps.generators;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import neon.util.Dice;

/**
 * Generator for maze layouts using various algorithms.
 *
 * @author mdriesen
 */
public class MazeGenerator {
  private final Dice dice;

  /** Creates a new MazeGenerator with default (non-deterministic) random behavior. */
  public MazeGenerator() {
    this(new Dice());
  }

  /**
   * Creates a new MazeGenerator with a specific Dice instance for deterministic testing.
   *
   * @param dice the Dice instance to use for random operations
   */
  public MazeGenerator(Dice dice) {
    this.dice = dice;
  }

  /**
   * Generates a maze with the given dimensions and parameters.
   *
   * @param width maze width
   * @param height maze height
   * @param sparse sparseness factor
   * @param randomness randomness factor
   * @return an Area representing the maze
   */
  public Area generateMaze(int width, int height, int sparse, int randomness) {
    Block[][] blocks = generateBlock(width / 2 - 1, height / 2 - 1, sparse, 50, 70);

    Area maze = new Area();
    for (int j = 0; j < height - 2; j += 2) {
      for (int i = 0; i < width - 2; i += 2) {
        if (blocks[i / 2][j / 2].visited) {
          maze.add(new Area(new Rectangle(i + 1, j + 1, 1, 1)));
        }
      }
    }

    for (int j = 1; j < height - 1; j++) {
      for (int i = 1; i < width - 1; i++) {
        if ((i + j) % 2 != 0) {
          if (i % 2 != 0 && blocks[((i - 1) / 2)][j / 2].up) {
            maze.add(new Area(new Rectangle(i, j, 1, 1)));
          } else if (i % 2 != 0 && blocks[((i - 1) / 2)][j / 2 - 1].down) {
            maze.add(new Area(new Rectangle(i, j, 1, 1)));
          } else if (j % 2 != 0 && blocks[i / 2][(j - 1) / 2].left) {
            maze.add(new Area(new Rectangle(i, j, 1, 1)));
          } else if (j % 2 != 0 && blocks[(i / 2 - 1)][(j - 1) / 2].right) {
            maze.add(new Area(new Rectangle(i, j, 1, 1)));
          }
        }
      }
    }
    return maze;
  }

  /**
   * Generates a squashed maze with the given dimensions.
   *
   * @param width maze width
   * @param height maze height
   * @param sparse sparseness factor
   * @return an Area representing the squashed maze
   */
  public Area generateSquashedMaze(int width, int height, int sparse) {
    Block[][] blocks = generateBlock(width - 2, height - 2, sparse, 100, 0);

    Path2D maze = new Path2D.Float();
    for (int j = 0; j < height - 2; j++) {
      for (int i = 0; i < width - 2; i++) {
        if (blocks[i][j].visited) {
          maze.append(new Area(new Rectangle(i + 1, j + 1, 1, 1)), false);
        }
      }
    }
    return new Area(maze);
  }

  private Block[][] generateBlock(int width, int height, int sparse, int randomness, int remove) {
    Block[][] blocks = new Block[width + 1][height + 1];
    for (int i = 0; i < width + 1; i++) {
      for (int j = 0; j < height + 1; j++) {
        blocks[i][j] = new Block();
      }
    }

    int count = 0;
    int x = 0;
    int y = 0;
    // start position for maze
    Block start = blocks[x][y];
    start.visited = true;

    // step 1: generate maze
    int roll = dice.rollDice(1, 4, 0);
    while (count < width * height - 1) {
      if (dice.rollDice(1, 100, 0) < randomness) {
        roll = dice.rollDice(1, 4, 0);
      }

      // from start position choose direction and make passage to next
      // cell. The switch continues until a valid direction is found,
      // or a new cell is chosen (case 0).
      switch (roll) {
        case 1:
          if (y - 1 >= 0 && !blocks[x][y - 1].visited) {
            blocks[x][y].up = true;
            y--;
            blocks[x][y].visited = true;
            blocks[x][y].down = true;
            count++;
            break;
          }
        case 2:
          if (x + 1 < width && !blocks[x + 1][y].visited) {
            blocks[x][y].right = true;
            x++;
            blocks[x][y].visited = true;
            blocks[x][y].left = true;
            count++;
            break;
          }
        case 3:
          if (y + 1 < height && !blocks[x][y + 1].visited) {
            blocks[x][y].down = true;
            y++;
            blocks[x][y].visited = true;
            blocks[x][y].up = true;
            count++;
            break;
          }
        case 4:
          if (x - 1 >= 0 && !blocks[x - 1][y].visited) {
            blocks[x][y].left = true;
            x--;
            blocks[x][y].visited = true;
            blocks[x][y].right = true;
            count++;
            break;
          }
        default:
          do {
            x = dice.rollDice(1, width, -1);
            y = dice.rollDice(1, height, -1);
          } while (!blocks[x][y].visited);
      }
    }

    // step 2: remove dead ends
    while (sparse > 0) {
      Block[][] temp = blocks.clone();
      sparse--;
      for (int i = 0; i < width; i++) {
        for (int j = 0; j < height; j++) {
          if (blocks[i][j].dead()) {
            temp[i][j].visited = false;
            if (blocks[i][j].up) {
              temp[i][j - 1].down = false;
              temp[i][j].up = false;
            } else if (blocks[i][j].right) {
              temp[i][j].right = false;
              temp[i + 1][j].left = false;
            } else if (blocks[i][j].down) {
              temp[i][j + 1].up = false;
              temp[i][j].down = false;
            } else if (blocks[i][j].left) {
              temp[i][j].left = false;
              temp[i - 1][j].right = false;
            }
          }
        }
      }
      blocks = temp;
    }

    // step 3: create loops
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        if (blocks[i][j].dead() && dice.rollDice(1, 100, 0) < remove) {
          roll = dice.rollDice(1, 4, 0);
          x = i;
          y = j;
          boolean loop = true;
          do {
            if (dice.rollDice(1, 100, 0) < randomness) {
              roll = dice.rollDice(1, 4, 0);
            }
            // from start position choose direction and make passage to next
            // cell. The switch continues until a valid direction is found,
            // or a new cell is chosen (case 0).
            switch (roll) {
              case 1:
                if (y - 1 >= 0) {
                  if (blocks[x][y - 1].visited) {
                    loop = false;
                  }
                  blocks[x][y].up = true;
                  y--;
                  blocks[x][y].visited = true;
                  blocks[x][y].down = true;
                  break;
                }
              case 2:
                if (x + 1 < width) {
                  if (blocks[x + 1][y].visited) {
                    loop = false;
                  }
                  blocks[x][y].right = true;
                  x++;
                  blocks[x][y].visited = true;
                  blocks[x][y].left = true;
                  break;
                }
              case 3:
                if (y + 1 < height) {
                  if (blocks[x][y + 1].visited) {
                    loop = false;
                  }
                  blocks[x][y].down = true;
                  y++;
                  blocks[x][y].visited = true;
                  blocks[x][y].up = true;
                  break;
                }
              case 4:
                if (x - 1 >= 0) {
                  if (blocks[x - 1][y].visited) {
                    loop = false;
                  }
                  blocks[x][y].left = true;
                  x--;
                  blocks[x][y].visited = true;
                  blocks[x][y].right = true;
                  break;
                }
            }
          } while (loop);
        }
      }
    }

    return blocks;
  }

  private static class Block {
    public boolean visited;
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;

    public Block() {
      visited = false;
      up = false;
      down = false;
      left = false;
      right = false;
    }

    public boolean dead() {
      if (up && !down && !left && !right) {
        return true;
      }
      if (!up && down && !left && !right) {
        return true;
      }
      if (!up && !down && left && !right) {
        return true;
      }
      return !up && !down && !left && right;
    }
  }
}
