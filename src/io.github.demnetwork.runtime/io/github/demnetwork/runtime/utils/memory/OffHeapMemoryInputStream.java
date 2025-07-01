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
import java.io.InputStream;
import java.lang.ref.WeakReference;

public final class OffHeapMemoryInputStream extends InputStream {
    private final WeakReference<OffHeapMemoryStorage> ref;
    private long offset = 0;
    private final boolean linked;
    private volatile boolean closed = false;

    public OffHeapMemoryInputStream(OffHeapMemoryStorage offHeapMemoryStorage, boolean linked) {
        if (offHeapMemoryStorage == null)
            throw new NullPointerException("Null OffHeapMemoryStorage");
        if (offHeapMemoryStorage.isClosed())
            throw new IllegalStateException("The OffHeapMemoryStorage is already closed");

        this.ref = new WeakReference<>(offHeapMemoryStorage);
        this.linked = linked;
        if (linked)
            offHeapMemoryStorage.monitor.addStream(this);
    }

    private void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("This InputStream is already closed");
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
    public int read() throws IOException {
        ensureOpen();
        OffHeapMemoryStorage storage = getStorageOrFail();
        if (offset >= storage.size)
            return -1;
        try {
            return Byte.toUnsignedInt(storage.getByte(offset++));
        } catch (IllegalArgumentException e) {
            return -1;
        } catch (IllegalStateException e) {
            if (linked)
                this.close();
            throw new IOException("OffHeapMemoryStorage was closed");
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > b.length)
            throw new IndexOutOfBoundsException();
        OffHeapMemoryStorage storage = getStorageOrFail();
        if (offset >= storage.size)
            return -1;

        int toRead = (int) Math.min(len, storage.size - offset);
        if (toRead == 0)
            return -1;

        if (storage.isClosed()) {
            if (linked)
                close();
            throw new IOException("OffHeapMemoryStorage was closed");
        }

        long arrayBase = UNSAFE.arrayBaseOffset(byte[].class);
        UNSAFE.copyMemory(null, storage.baseAddr + offset, b, arrayBase + off, toRead);
        offset += toRead;
        return toRead;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long newOffset) throws IOException {
        ensureOpen();
        OffHeapMemoryStorage storage = getStorageOrFail();
        if (newOffset < 0 || newOffset > storage.size)
            throw new IOException("Invalid offset: " + newOffset);
        this.offset = newOffset;
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        OffHeapMemoryStorage storage = getStorageOrFail();
        long skipped = Math.min(n, storage.size - offset);
        offset += skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        OffHeapMemoryStorage storage = getStorageOrFail();
        long remaining = storage.size - offset;
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        closed = true;
        OffHeapMemoryStorage storage = ref.get();
        if (storage != null && linked && !storage.isClosed()) {
            storage.close();
        }
        ref.clear();
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
