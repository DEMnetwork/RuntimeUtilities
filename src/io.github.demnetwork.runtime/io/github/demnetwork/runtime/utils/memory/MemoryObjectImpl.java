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

/** A Simple implementation of {@link MemoryObject} */
public class MemoryObjectImpl extends MemoryObject {
    protected volatile boolean closed = false;

    /** @deprecated This creates a new instance on every invocation */
    @Deprecated
    public MemoryObjectImpl(long size) {
        this(size, false);
    }

    /** @deprecated This creates a new instance on every invocation */
    @Deprecated
    public MemoryObjectImpl(long size, boolean ignoreLimits) {
        this(new OffHeapMemoryStorage(size, ignoreLimits));
    }

    public MemoryObjectImpl(OffHeapMemoryStorage storage) {
        this(storage, -1);
    }

    public MemoryObjectImpl(OffHeapMemoryStorage storage, long size) {
        this(storage, size, false);
    }

    public MemoryObjectImpl(OffHeapMemoryStorage storage, long size, boolean interpretData) {
        super(storage, size, interpretData);
    }

    @Override
    protected void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("Closed MemoryObject");
        if (super.storage.isClosed())
            throw new IOException("Closed Storage");
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        this.closed = true;
    }

    @Override
    protected void setupFields(OffHeapMemoryStorage storage) throws NoSuchFieldException {
        super.setupFields(storage);
        this.closed = false;
    }
}
