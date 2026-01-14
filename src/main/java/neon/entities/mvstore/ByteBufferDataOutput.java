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

import java.io.DataOutput;
import java.io.IOException;
import neon.maps.mvstore.MVUtils;
import org.h2.mvstore.WriteBuffer;

/**
 * Adapter class that wraps MVStore's WriteBuffer to implement the DataOutput interface. This allows
 * existing serializers using DataOutput to work with MVStore's WriteBuffer.
 *
 * @author mdriesen
 */
public class ByteBufferDataOutput implements DataOutput {
  private final WriteBuffer buffer;

  public ByteBufferDataOutput(WriteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void write(int b) throws IOException {
    buffer.put((byte) b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    buffer.put(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    buffer.put(b, off, len);
  }

  @Override
  public void writeBoolean(boolean v) throws IOException {
    buffer.put((byte) (v ? 1 : 0));
  }

  @Override
  public void writeByte(int v) throws IOException {
    buffer.put((byte) v);
  }

  @Override
  public void writeShort(int v) throws IOException {
    buffer.putShort((short) v);
  }

  @Override
  public void writeChar(int v) throws IOException {
    buffer.putChar((char) v);
  }

  @Override
  public void writeInt(int v) throws IOException {
    buffer.putInt(v);
  }

  @Override
  public void writeLong(long v) throws IOException {
    buffer.putLong(v);
  }

  @Override
  public void writeFloat(float v) throws IOException {
    buffer.putFloat(v);
  }

  @Override
  public void writeDouble(double v) throws IOException {
    buffer.putDouble(v);
  }

  @Override
  public void writeBytes(String s) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      buffer.put((byte) s.charAt(i));
    }
  }

  @Override
  public void writeChars(String s) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      buffer.putChar(s.charAt(i));
    }
  }

  @Override
  public void writeUTF(String str) throws IOException {
    MVUtils.writeString(buffer, str);
  }
}
