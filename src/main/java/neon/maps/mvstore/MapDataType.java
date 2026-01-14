package neon.maps.mvstore;

import java.nio.ByteBuffer;
import neon.maps.Dungeon;
import neon.maps.Map;
import neon.maps.World;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

public class MapDataType extends BasicDataType<Map> {
  private final WorldDataType worldDataType;
  private final Dungeon.DungeonDataType dungeonDataType;

  final int WORLDTYPE = 1;
  final int DUNGEONTYPE = 2;

  public MapDataType(WorldDataType worldDataType, Dungeon.DungeonDataType dungeonDataType) {
    this.worldDataType = worldDataType;
    this.dungeonDataType = dungeonDataType;
  }

  @Override
  public int getMemory(Map obj) {
    if (obj instanceof World world) {
      return worldDataType.getMemory(world);
    } else if (obj instanceof Dungeon dungeon) {
      return dungeonDataType.getMemory(dungeon);
    } else throw new UnsupportedOperationException("MV DataType not found for " + obj);
  }

  @Override
  public void write(WriteBuffer buff, Map obj) {
    if (obj instanceof World world) {
      buff.putInt(WORLDTYPE);
      worldDataType.write(buff, world);
    } else if (obj instanceof Dungeon dungeon) {
      buff.putInt(DUNGEONTYPE);
      dungeonDataType.write(buff, dungeon);
    } else throw new UnsupportedOperationException("MV DataType not found for " + obj);
  }

  @Override
  public Map read(ByteBuffer buff) {
    int type = buff.getInt();
    if (type == WORLDTYPE) {
      return worldDataType.read(buff);
    } else if (type == DUNGEONTYPE) {
      return dungeonDataType.read(buff);
    } else throw new UnsupportedOperationException("Unrecognized map type " + type);
  }

  /**
   * Create storage object of array type to hold values
   *
   * @param size number of values to hold
   * @return storage object
   */
  @Override
  public Map[] createStorage(int size) {
    return new Map[size];
  }
}
