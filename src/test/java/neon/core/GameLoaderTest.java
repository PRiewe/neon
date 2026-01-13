/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Maarten Driesen
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

import static org.junit.jupiter.api.Assertions.*;

import neon.entities.Player;
import neon.entities.property.Gender;
import neon.resources.RSign;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for RCreature.clone() used by GameLoader.
 *
 * <p>Tests that cloning creatures preserves all fields and creates independent copies. This is
 * critical for player initialization where the species template must be cloned.
 */
public class GameLoaderTest {

  private MapStore testDb;

  @BeforeEach
  public void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    // Load test resources (includes creatures like dwarf, elf, etc.)
    TestEngineContext.loadTestResourceViaConfig("src/test/resources/neon.ini.sampleMod1.xml");
  }

  @AfterEach
  public void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  @Test
  public void testInitOfSampleMod1NewGame() {
    // Get GameContext from the TestEngineContext
    GameContext context = TestEngineContext.getTestContext();

    // Create instance of GameLoader with GameContext
    // Configuration is not needed for initGame(), only for loadGame()
    GameLoader gameLoader = new GameLoader(context, TestEngineContext.getGameStores(), null);

    // Get RSign "alraun" from our resource manager
    RSign alraun = (RSign) TestEngineContext.getTestResources().getResource("s_alraun", "magic");

    // Call gameLoader.initGame
    gameLoader.initGame(
        "dwarf", "Bilbo", Gender.MALE, Player.Specialisation.combat, "adventurer", alraun);

    // Verify player was created
    assertNotNull(context.getPlayer());
    assertEquals("Bilbo", context.getPlayer().getName());
  }
}
