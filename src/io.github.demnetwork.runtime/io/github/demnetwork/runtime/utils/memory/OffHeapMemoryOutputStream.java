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

import static io.github.demnetwork.runtime.utils.memory.OffHeapMemoryStorage.UNSAFE;

import java.io.IOException;
import java.lang.ref.WeakReference;
import io.github.demnetwork.runtime.utils.memory.serial.MemoryOutputStream;

public final class OffHeapMemoryOutputStream extends MemoryOutputStream {
    static final int BYTE_MASK = 0xFF;
    private final WeakReference<OffHeapMemoryStorage> ref;
    private long offset = 0;
    private final boolean linked;
    private volatile boolean closed = false;

    public OffHeapMemoryOutputStream(OffHeapMemoryStorage offHeapMemoryStorage, boolean linked) {
        if (offHeapMemoryStorage == null)
            throw new NullPointerException("Null OffHeapMemoryStorage");
        if (offHeapMemoryStorage.isClosed())
            throw new IllegalStateException("The OffHeapMemoryStorage is already closed");
        this.ref = new WeakReference<OffHeapMemoryStorage>(offHeapMemoryStorage);
        this.linked = linked;
        if (linked)
            offHeapMemoryStorage.monitor.addStream(this);
    }

    private OffHeapMemoryStorage getStorageOrFail() throws IOException {
        OffHeapMemoryStorage storage = ref.get();
        if (storage == null) {
            if (linked)
                closed = true;
            throw new IOException("The underlying OffHeapMemoryStorage was garbage collected");
        }
        return storage;
    }

    @Override
    public void write(int b) throws IOException {
        ensureOpen();
        OffHeapMemoryStorage offHeapMemoryStorage = getStorageOrFail();
        byte data = (byte) (b & BYTE_MASK);
        if (offset >= offHeapMemoryStorage.size)
            throw new IOException("This OutputStream reached its end.");
        try {
            offHeapMemoryStorage.setByte(offset++, data);
        } catch (IllegalStateException e) {
            if (this.linked)
                this.close();
            throw new IOException("The OffHeapMemoryStorage is closed");

        }
    }

    protected void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("This OutputStream is already closed");
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        this.closed = true;
        OffHeapMemoryStorage offHeapMemoryStorage = getStorageOrFail();
        if (offHeapMemoryStorage != null)
            if (!offHeapMemoryStorage.isClosed() && this.linked) {
                offHeapMemoryStorage.close();
            }
        ref.clear();
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
    }

    public void setOffset(long newOffset) throws IOException {
        ensureOpen();
        OffHeapMemoryStorage offHeapMemoryStorage = getStorageOrFail();
        if (newOffset < 0 || newOffset > offHeapMemoryStorage.size)
            throw new IOException("Invalid offset: " + newOffset);
        this.offset = newOffset;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > b.length)
            throw new IndexOutOfBoundsException();
        OffHeapMemoryStorage offHeapMemoryStorage = getStorageOrFail();
        if (offset + len > offHeapMemoryStorage.size)
            throw new IOException("Not enough space in OffHeapMemoryStorage");
        if (offHeapMemoryStorage.isClosed()) {
            if (this.linked)
                this.close();
            throw new IOException("The OffHeapMemoryStorage is closed");
        }
        long arrayBase = UNSAFE.arrayBaseOffset(byte[].class);
        UNSAFE.copyMemory(b, arrayBase + off, null, offHeapMemoryStorage.baseAddr + offset, len);
        offset += len;
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b == null)
            throw new NullPointerException();
        write(b, 0, b.length);
    }

    public long remaining() throws IOException {
        ensureOpen();
        OffHeapMemoryStorage offHeapMemoryStorage = getStorageOrFail();
        return offHeapMemoryStorage.size - offset;
    }

    @Deprecated
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!closed)
                close();
        } catch (Throwable t) {

        }
        super.finalize();
    }
}
