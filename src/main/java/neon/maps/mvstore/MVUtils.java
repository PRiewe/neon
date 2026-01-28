package neon.maps.mvstore;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.h2.mvstore.WriteBuffer;

public class MVUtils {
  public static void writeString(WriteBuffer buffer, String str) {
    if (str == null) {
      buffer.putInt(-1);
    } else {
      buffer.putInt(str.getBytes(StandardCharsets.UTF_8).length);
      buffer.put(str.getBytes(StandardCharsets.UTF_8));
    }
  }

  public static String readString(ByteBuffer buffer) {
    int length = buffer.getInt();
    if (length < 0) {
      return null;
    }
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
