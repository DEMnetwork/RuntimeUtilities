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

package io.github.demnetwork.runtime.utils.memory;

import java.io.IOException;
import io.github.demnetwork.runtime.utils.memory.serial.MemoryChannel;
import io.github.demnetwork.runtime.utils.memory.serial.MemorySerializable;

public final class OffHeapMemoryStorageChannel extends MemoryChannel {
    private final OffHeapMemoryStorage storage;
    volatile boolean closed = false;
    private long off = 0L;

    public OffHeapMemoryStorageChannel(OffHeapMemoryStorage storage) {
        if (storage == null)
            throw new NullPointerException();
        if (storage.isClosed())
            throw new IllegalArgumentException("Closed storage");
        this.storage = storage;
    }

    @Override
    public MemorySerializable readObj() throws IOException, InstantiationException {
        ensureOpen();
        try (OffHeapMemoryInputStream is = this.storage.toInputStream(false)) {
            is.setOffset(off);
            MemorySerializable v = is.readObj();
            this.off = is.getOffset();
            return v;
        }
    }

    @Override
    public <T extends MemorySerializable> T readAndInterpretAs(Class<T> cls)
            throws IOException, InstantiationException {
        ensureOpen();
        try (OffHeapMemoryInputStream is = this.storage.toInputStream(false)) {
            is.setOffset(off);
            T v = is.readAndInterpretAs(cls);
            this.off = is.getOffset();
            return v;
        }
    }

    @Override
    public void close() {
        if (closed)
            return;
        this.closed = true;
    }

    @Override
    public double readDouble() throws IOException {
        ensureOpen();
        double d = this.storage.getDouble(off);
        this.off += 8;
        return d;
    }

    @Override
    public float readFloat() throws IOException {
        ensureOpen();
        float f = this.storage.getFloat(off);
        this.off += 4;
        return f;
    }

    @Override
    public long readLong() throws IOException {
        ensureOpen();
        long l = this.storage.getLong(off);
        this.off += 8;
        return l;
    }

    @Override
    public int readInt() throws IOException {
        ensureOpen();
        int i = this.storage.getInt(off);
        this.off += 4;
        return i;
    }

    @Override
    public char readChar() throws IOException {
        ensureOpen();
        char c = this.storage.getChar(off);
        this.off += 2;
        return c;
    }

    @Override
    public short readShort() throws IOException {
        ensureOpen();
        short s = this.storage.getShort(off);
        this.off += 2;
        return s;
    }

    @Override
    public byte readByte() throws IOException {
        ensureOpen();
        byte b = this.storage.getByte(off);
        this.off += 1;
        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        ensureOpen();
        try (OffHeapMemoryInputStream is = this.storage.toInputStream(false)) {
            is.setOffset(this.off);
            int r = is.read(buf, off, len);
            this.off = is.getOffset();
            return r;
        }
    }

    @Override
    public void ensureOpen() throws IOException {
        if (this.closed)
            throw new IOException("Closed OffHeapmemoryChannel");
        if (this.storage.isClosed())
            throw new IOException("Closed Storage");
    }

    @Override
    public void write(double d) throws IOException {
        ensureOpen();
        this.storage.setDouble(off, d);
        this.off += 8;
    }

    @Override
    public void write(float f) throws IOException {
        ensureOpen();
        this.storage.setFloat(off, f);
        this.off += 4;
    }

    @Override
    public void write(long l) throws IOException {
        ensureOpen();
        this.storage.setLong(off, l);
        this.off += 8;
    }

    @Override
    public void write(int i) throws IOException {
        ensureOpen();
        this.storage.setInt(off, i);
        this.off += 4;
    }

    @Override
    public void write(char c) throws IOException {
        ensureOpen();
        this.storage.setChar(off, c);
        this.off += 2;
    }

    @Override
    public void write(short s) throws IOException {
        ensureOpen();
        this.storage.setShort(off, s);
        this.off += 2;
    }

    @Override
    public void write(byte b) throws IOException {
        ensureOpen();
        this.storage.setByte(off, b);
        this.off += 1;
    }

    @Override
    public void write(MemorySerializable obj) throws IOException {
        ensureOpen();
        try (OffHeapMemoryOutputStream os = this.storage.toOutputStream(false)) {
            os.setOffset(off);
            os.writeObj(obj);
            this.off = os.getOffset();
        }
    }
}
