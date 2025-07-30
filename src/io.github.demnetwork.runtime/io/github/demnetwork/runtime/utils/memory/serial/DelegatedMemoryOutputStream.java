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

import java.io.IOException;
import java.io.OutputStream;

public final class DelegatedMemoryOutputStream extends MemoryOutputStream {
    private volatile boolean closed = false;
    private final OutputStream os;

    public DelegatedMemoryOutputStream(OutputStream os) {
        if (os == null)
            throw new NullPointerException();
        this.os = os;
    }

    @Override
    protected void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("This stream was closed");
    }

    @Override
    public void write(int b) throws IOException {
        ensureOpen();
        this.os.write(b);
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
        this.os.flush();
    }

    @Override
    public void write(byte[] buf) throws IOException {
        ensureOpen();
        if (buf == null)
            throw new NullPointerException();
        this.write(buf, 0, buf.length);
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        ensureOpen();
        if (buf == null)
            throw new NullPointerException();
        this.os.write(buf, off, len);
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        this.closed = true;
        this.os.close();
    }

    public OutputStream getDelegated() {
        return this.os;
    }

}
