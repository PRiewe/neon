package neon.core;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public record ScriptEngine(Context context) {

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
