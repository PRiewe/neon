package neon.maps.mvstore;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import neon.maps.Zone;
import neon.maps.ZoneFactory;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

public class ZoneType implements DataType<Zone> {

  private final ZoneFactory zoneFactory;

  public ZoneType(ZoneFactory zoneFactory) {
    this.zoneFactory = zoneFactory;
  }

  /**
   * Compare two keys.
   *
   * @param a the first key
   * @param b the second key
   * @return -1 if the first key is smaller, 1 if larger, and 0 if equal
   * @throws UnsupportedOperationException if the type is not orderable
   */
  @Override
  public int compare(Zone a, Zone b) {
    return 0;
  }

  /**
   * Perform binary search for the key within the storage
   *
   * @param key to search for
   * @param storage to search within (an array of type T)
   * @param size number of data items in the storage
   * @param initialGuess for key position
   * @return index of the key , if found, - index of the insertion point, if not
   */
  @Override
  public int binarySearch(Zone key, Object storage, int size, int initialGuess) {
    return 0;
  }

  /**
   * Calculates the amount of used memory in bytes.
   *
   * @param obj the object
   * @return the used memory
   */
  @Override
  public int getMemory(Zone obj) {
    return obj.getEstimatedMemory();
  }

  /**
   * Whether memory estimation based on previously seen values is allowed/desirable
   *
   * @return true if memory estimation is allowed
   */
  @Override
  public boolean isMemoryEstimationAllowed() {
    return false;
  }

  /**
   * Write an object.
   *
   * @param out the target buffer
   * @param zone the value
   */
  @Override
  public void write(WriteBuffer out, Zone zone) {
    try {
      zoneFactory.writeZoneToWriteBuffer(out, zone);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write a list of objects.
   *
   * @param buff the target buffer
   * @param storage the objects
   * @param len the number of objects to write
   */
  @Override
  public void write(WriteBuffer buff, Object storage, int len) {
    throw new IllegalStateException("Not implemented");
  }

  /**
   * Read an object.
   *
   * @param in the source buffer
   * @return the object
   */
  @Override
  public Zone read(ByteBuffer in) {
    try {
      return zoneFactory.readZoneByteBuffer(in);
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Read a list of objects.
   *
   * @param buff the target buffer
   * @param storage the objects
   * @param len the number of objects to read
   */
  @Override
  public void read(ByteBuffer buff, Object storage, int len) {
    throw new IllegalStateException("Not implemented");
  }

  /**
   * Create storage object of array type to hold values
   *
   * @param size number of values to hold
   * @return storage object
   */
  @Override
  public Zone[] createStorage(int size) {
    return new Zone[size];
  }
}
