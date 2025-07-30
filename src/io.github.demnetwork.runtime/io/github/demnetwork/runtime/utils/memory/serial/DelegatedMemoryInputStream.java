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

import java.io.*;

public final class DelegatedMemoryInputStream extends MemoryInputStream {
    private volatile boolean closed = false;
    private final InputStream is;

    public DelegatedMemoryInputStream(InputStream is) {
        if (is == null)
            throw new NullPointerException();
        this.is = is;
    }

    @Override
    protected void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("Stream is closed");
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        return is.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        ensureOpen();
        if (buf == null)
            throw new NullPointerException();
        return this.read(buf, 0, buf.length);
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        ensureOpen();
        if (buf == null)
            throw new NullPointerException();
        return this.is.read(buf, off, len);
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        this.closed = true;
        this.is.close();
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return is.available();
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        return is.skip(n);
    }

    public InputStream getDelegated() {
        return this.is;
    }

}
