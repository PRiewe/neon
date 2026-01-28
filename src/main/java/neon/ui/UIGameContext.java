package neon.ui;

import neon.core.GameContext;
import neon.core.GameServices;
import neon.core.GameStore;

public class UIGameContext {
  private final GameServices gameServices;
  private final GameStore gameStore;
  private final GameContext GameContext;

  public UIGameContext(GameServices gameServices, GameStore gameStore, GameContext GameContext) {
    this.gameServices = gameServices;
    this.gameStore = gameStore;
    this.GameContext = GameContext;
  }
}
