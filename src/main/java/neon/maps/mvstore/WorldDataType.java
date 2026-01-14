package neon.maps.mvstore;

import java.nio.ByteBuffer;
import neon.maps.World;
import neon.maps.Zone;
import neon.maps.ZoneFactory;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

public class WorldDataType extends BasicDataType<World> {
  private final ZoneFactory zoneFactory;
  private final ZoneType zoneType;

  public WorldDataType(ZoneFactory zoneFactory) {
    this.zoneFactory = zoneFactory;
    this.zoneType = new ZoneType(zoneFactory);
  }

  @Override
  public int getMemory(World obj) {
    return 0;
  }

  @Override
  public void write(WriteBuffer buff, World obj) {
    MVUtils.writeString(buff, obj.getName());
    buff.putInt(obj.getUID());
    zoneType.write(buff, obj.getZone());
  }

  @Override
  public World read(ByteBuffer buff) {
    String name = MVUtils.readString(buff);
    int uid = buff.getInt();
    Zone zone = zoneType.read(buff);
    return new World(name, uid, zone);
  }

  /**
   * Create storage object of array type to hold values
   *
   * @param size number of values to hold
   * @return storage object
   */
  @Override
  public World[] createStorage(int size) {
    return new World[size];
  }
}
