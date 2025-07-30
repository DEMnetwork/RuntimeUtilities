/*
 *   Copyright (c) 2025 DEMnetwork
 *   All rights reserved.

 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 */

package io.github.demnetwork.runtime.utils.memory.serial;

import java.io.Closeable;
import java.io.IOException;

/** Not a Stream. It is a read/writter Abstract Class */
public abstract class MemoryChannel implements MemoryReaderWriter, Closeable {
    public abstract double readDouble() throws IOException;

    public abstract float readFloat() throws IOException;

    public abstract long readLong() throws IOException;

    public abstract int readInt() throws IOException;

    public abstract char readChar() throws IOException;

    public abstract short readShort() throws IOException;

    public abstract byte readByte() throws IOException;

    public abstract int read(byte[] buf, int off, int len) throws IOException;

    public int read(byte[] arr) throws IOException {
        ensureOpen();
        if (arr == null)
            throw new NullPointerException("Buffer cannot be null");
        return this.read(arr, 0, arr.length);
    }

    public abstract void ensureOpen() throws IOException;

    public void writeDouble(double d) throws IOException {
        this.write(d);
    }

    public void writeFloat(float f) throws IOException {
        this.write(f);
    }

    public void writeLong(long l) throws IOException {
        this.write(l);
    }

    public void writeInt(int i) throws IOException {
        this.write(i);
    }

    public void writeChar(char c) throws IOException {
        this.write(c);
    }

    public void writeShort(short s) throws IOException {
        this.write(s);
    }

    public void writeByte(byte b) throws IOException {
        this.write(b);
    }

    public abstract void write(double d) throws IOException;

    public abstract void write(float f) throws IOException;

    public abstract void write(long l) throws IOException;

    public abstract void write(int i) throws IOException;

    public abstract void write(char c) throws IOException;

    public abstract void write(short s) throws IOException;

    public abstract void write(byte b) throws IOException;

    public abstract void write(MemorySerializable obj) throws IOException;

    @Override
    public void writeObj(MemorySerializable obj) throws IOException {
        this.write(obj);
    }
}
