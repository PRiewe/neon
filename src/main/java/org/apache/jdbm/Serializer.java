package org.apache.jdbm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface Serializer<A> {
  void serialize(DataOutput var1, A var2) throws IOException;

  A deserialize(DataInput var1) throws IOException, ClassNotFoundException;
}
