package neon.entities;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import java.io.Serializable;
import neon.entities.components.*;

public abstract class EntityBase implements Serializable {
  protected ClassToInstanceMap<Component> components = MutableClassToInstanceMap.create();

  public RenderComponent getRenderComponent() {
    return components.getInstance(RenderComponent.class);
  }

  public void setRenderComponent(RenderComponent renderer) {
    components.putInstance(RenderComponent.class, renderer);
  }

  public PhysicsComponent getPhysicsComponent() {
    return components.getInstance(PhysicsComponent.class);
  }

  public ScriptComponent getScriptComponent() {
    return components.getInstance(ScriptComponent.class);
  }
}
