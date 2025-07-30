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

public abstract class MemoryOutputStream extends OutputStream implements MemoryWriter {
    public static final long ID_SIZE = 8;

    public void writeObj(MemorySerializable obj) throws IOException {
        ensureOpen();
        if (obj == null) {
            // Handle null values
            this.writeObj(NullWrapper.NULL);
            return;
        }
        long id = MemorySerializableRegistry.getIdOfClass(obj.getClass());
        if (id == 0)
            throw new IllegalArgumentException("ID was not registered");
        byte[] bytes = MemoryUtils.toBytes(id);
        write(bytes, 0, 8);
        obj.writeObj(this);
    }

    protected abstract void ensureOpen() throws IOException;
}
