/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.entities.mvstore;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import neon.maps.mvstore.MVUtils;

/**
 * Adapter class that wraps a ByteBuffer to implement the DataInput interface. This allows existing
 * serializers using DataInput to work with MVStore's ByteBuffer.
 *
 * @author mdriesen
 */
public class ByteBufferDataInput implements DataInput {
  private final ByteBuffer buffer;

  public ByteBufferDataInput(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void readFully(byte[] b) throws IOException {
    buffer.get(b);
  }

  @Override
  public void readFully(byte[] b, int off, int len) throws IOException {
    buffer.get(b, off, len);
  }

  @Override
  public int skipBytes(int n) throws IOException {
    int remaining = buffer.remaining();
    int toSkip = Math.min(n, remaining);
    buffer.position(buffer.position() + toSkip);
    return toSkip;
  }

  @Override
  public boolean readBoolean() throws IOException {
    return buffer.get() != 0;
  }

  @Override
  public byte readByte() throws IOException {
    return buffer.get();
  }

  @Override
  public int readUnsignedByte() throws IOException {
    return buffer.get() & 0xFF;
  }

  @Override
  public short readShort() throws IOException {
    return buffer.getShort();
  }

  @Override
  public int readUnsignedShort() throws IOException {
    return buffer.getShort() & 0xFFFF;
  }

  @Override
  public char readChar() throws IOException {
    return buffer.getChar();
  }

  @Override
  public int readInt() throws IOException {
    return buffer.getInt();
  }

  @Override
  public long readLong() throws IOException {
    return buffer.getLong();
  }

  @Override
  public float readFloat() throws IOException {
    return buffer.getFloat();
  }

  @Override
  public double readDouble() throws IOException {
    return buffer.getDouble();
  }

  @Override
  public String readLine() throws IOException {
    throw new UnsupportedOperationException("readLine() is not supported");
  }

  @Override
  public String readUTF() throws IOException {
    return MVUtils.readString(buffer);
  }
}
