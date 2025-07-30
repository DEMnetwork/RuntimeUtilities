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

import static io.github.demnetwork.runtime.utils.memory.serial.MemorySerializableRegistry.UNSAFE;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;

import io.github.demnetwork.runtime.internal.BuildData;

public abstract class MemoryInputStream extends InputStream implements MemoryReader {

    /**
     * 
     * @return
     * @throws IOException            If an I/O operation went wrong
     * @throws InstantiationException If {@link MemoryInputStream#readObj()
     *                                MemoryInputStream.readObj()} was unable to
     *                                create an instance of it
     */
    public MemorySerializable readObj() throws IOException, InstantiationException {
        ensureOpen();
        byte[] longVal = new byte[8];
        int r = this.read(longVal, 0, 8);
        if (r == -1)
            throw new EOFException("Unexpected EOF");
        if (r < 8)
            throw new IOException("Corrupt or Incomplete data");
        long id = MemoryUtils.toLong(longVal);
        Class<? extends MemorySerializable> cls = MemorySerializableRegistry.getRegisteredClass(id);
        if (cls == MemorySerializable.class)
            throw new IOException("ID was not registered properly");
        MemorySerializable obj = (MemorySerializable) UNSAFE.allocateInstance(cls);
        return obj.readObj(this);
    }

    /**
     * <strong>WARNING: Interpret Data that does not match the expected structure
     * of the type,
     * causes undefined behaviour</strong>
     * <p>
     * This method interprets the read data into the type, instead of the found type
     * by ID
     */
    @SuppressWarnings("unchecked")
    public <T extends MemorySerializable> T readAndInterpretAs(Class<T> cls)
            throws IOException, InstantiationException {
        if (cls == null)
            throw new NullPointerException();
        if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()))
            throw new InstantiationException("Abstract Classes cannot be instantiated");
        byte[] buf = new byte[8];
        int r = this.read(buf, 0, 8); // Skip ID
        if (BuildData.CURRENT.getDebugStatus()) {
            long tid = MemorySerializableRegistry.getIdOfClass(cls);
            long id = MemoryUtils.toLong(buf);
            Class<?> srcCls = MemorySerializableRegistry.getRegisteredClass(id);
            System.out.println("[MemoryInputStream] Conversion: "
                    + (srcCls == MemorySerializable.class ? "UnknownClass" : srcCls.getName())
                    + "(" + (id == 0 ? "NoID" : id) + ") >>> " + cls.getName() + "(" + (tid == 0 ? "NoID" : tid)
                    + ")");
        }
        if (r == -1)
            throw new EOFException("Unexpected EOF");
        if (r < 8)
            throw new IOException("Corrupt or Incomplete data");
        T obj = (T) UNSAFE.allocateInstance(cls);
        return (T) obj.readObj(this);
    }

    protected abstract void ensureOpen() throws IOException;
}
