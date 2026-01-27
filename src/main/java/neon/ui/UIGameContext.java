package neon.ui;

import neon.core.GameServices;
import neon.core.GameStore;
import neon.core.UIEngineContext;

public class UIGameContext {
  private final GameServices gameServices;
  private final GameStore gameStore;
  private final UIEngineContext UIEngineContext;

  public UIGameContext(
      GameServices gameServices, GameStore gameStore, UIEngineContext UIEngineContext) {
    this.gameServices = gameServices;
    this.gameStore = gameStore;
    this.UIEngineContext = UIEngineContext;
  }
}
