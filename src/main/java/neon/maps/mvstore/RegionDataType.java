package neon.maps.mvstore;

import static neon.maps.mvstore.MVUtils.readString;

import java.nio.ByteBuffer;
import neon.maps.Region;
import neon.maps.services.ResourceProvider;
import neon.resources.RRegionTheme;
import neon.resources.RTerrain;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.h2.mvstore.type.DataType;

public class RegionDataType extends BasicDataType<Region> implements DataType<Region> {
  private final ResourceProvider resourceProvider;

  public RegionDataType(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  @Override
  public int getMemory(Region obj) {
    return 0;
  }

  @Override
  public void write(WriteBuffer output, Region obj) {
    MVUtils.writeString(output, obj.getTheme() != null ? obj.getTheme().id : "");
    MVUtils.writeString(output, obj.getLabel());
    MVUtils.writeString(output, obj.getTextureType());
    output.putInt(obj.getX());
    output.putInt(obj.getY());
    output.putInt(obj.getZ());
    output.putInt(obj.getWidth());
    output.putInt(obj.getHeight());
    output.putInt(obj.getScripts().size());
    for (String script : obj.getScripts()) {
      MVUtils.writeString(output, script);
    }
  }

  @Override
  public Region read(ByteBuffer buff) {
    var builder = Region.builder();
    RRegionTheme theme = (RRegionTheme) resourceProvider.getResource(readString(buff), "theme");
    builder.theme(theme);
    builder.label(readString(buff));
    builder.terrain((RTerrain) resourceProvider.getResource(readString(buff), "terrain"));
    builder
        .x(buff.getInt())
        .y(buff.getInt())
        .z(buff.getInt())
        .width(buff.getInt())
        .height(buff.getInt());
    Region region = builder.build();
    int size = buff.getInt();
    for (int i = 0; i < size; i++) {
      region.addScript(readString(buff), false);
    }
    return region;
  }

  /**
   * Create storage object of array type to hold values
   *
   * @param size number of values to hold
   * @return storage object
   */
  @Override
  public Region[] createStorage(int size) {
    return new Region[size];
  }
}
