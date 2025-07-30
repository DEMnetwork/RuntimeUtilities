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

public final class SlicedOffHeapMemoryStorage extends OffHeapMemoryStorage {
    private final OffHeapMemoryStorage parent;

    SlicedOffHeapMemoryStorage(OffHeapMemoryStorage parent, long size, long offset) {
        super(parent.baseAddr + offset, size);
        this.parent = parent;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("This cannot be closed");
    }

    @Override
    protected final void ensureOpen() {
        if (this.parent.isClosed())
            throw new IllegalStateException("Parent storage is closed");
    }

    @Override
    public boolean isClosed() {
        return this.parent.isClosed();
    }

    @Override
    SlicedOffHeapMemoryStorage slice0(long offset, long size) {
        return this.parent.slice((super.baseAddr - parent.baseAddr) + offset, size);
    }

    public OffHeapMemoryStorage getParent() {
        ensureOpen();
        return this.parent;
    }

    @Deprecated
    @Override
    protected void finalize() throws Throwable {
    }

    @Override
    public long getBaseAddress() {
        return this.parent.getBaseAddress();
    }

    public long getSliceBaseAddress() {
        return super.baseAddr;
    }

}
