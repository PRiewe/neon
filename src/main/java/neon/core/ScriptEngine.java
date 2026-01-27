package neon.core;

import lombok.Getter;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class ScriptEngine {
  @Getter private final Context context;

  public ScriptEngine(Context context) {
    this.context = context;
  }

  public Object execute(String script) {
    try {
      return context.eval("js", script);
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public Value getBindings() {
    return context.getBindings("js");
  }
}
