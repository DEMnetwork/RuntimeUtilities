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
import java.lang.reflect.Array;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.IntWrapper;

public final class ArrayWrapper<T extends MemorySerializable> implements Wrapper<T[]>, MemorySerializable {
    final T[] arr;
    private final Class<T> cls;
    public final int length;

    public ArrayWrapper(T[] arr) {
        this(arr, (Class<T>) MemorySerializable.class);
    }

    public ArrayWrapper(T[] arr, Class<T> type) {
        if (arr == null)
            throw new NullPointerException();
        if (type == null)
            throw new NullPointerException();
        if (!MemorySerializable.class.isAssignableFrom(type))
            throw new IllegalArgumentException("\'type\' is not a subtype of MemorySerializable");
        if (!type.isAssignableFrom(arr.getClass().getComponentType()))
            throw new IllegalArgumentException(
                    "The type is not the superclass, superinterface or the same type of the Array Compontent type");
        this.arr = arr.clone();
        this.length = arr.length;
        this.cls = type;
    }

    @Override
    public void writeObj(MemoryOutputStream mos) throws IOException {
        mos.write(MemoryUtils.toBytes(MemorySerializableRegistry.getIdOfClass(this.cls)), 0, 8);
        mos.writeObj(new IntWrapper(this.arr.length));
        for (int i = 0; i < this.arr.length; i++)
            mos.writeObj(arr[i]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayWrapper<?> readObj(MemoryInputStream mis) throws IOException {
        try {
            byte[] arrctid = new byte[8];
            int r = mis.read(arrctid, 0, 8);
            if (r == -1)
                throw new EOFException("reached EOF");
            if (r < 8)
                throw new IOException("Incomplete or Corrupt data");
            Class<?> cls = MemorySerializableRegistry.getRegisteredClass(MemoryUtils.toLong(arrctid));
            int len = ((IntWrapper) mis.readObj()).intValue();
            UNSAFE.putObject(this, UNSAFE.objectFieldOffset(ArrayWrapper.class.getDeclaredField("arr")),
                    Array.newInstance(cls, len));
            UNSAFE.putObject(this, UNSAFE.objectFieldOffset(ArrayWrapper.class.getDeclaredField("cls")), cls);
            for (int i = 0; i < len; i++)
                this.arr[i] = (T) mis.readObj();
            return this;
        } catch (InstantiationException | ClassCastException e) {
            throw new IOException("Unable to deserialize", e);
        } catch (ReflectiveOperationException e) {
            throw new IOException("An unexpected exception while doing an Reflective Operation occurred!", e);
        }
    }

    @Override
    public T[] getValue() {
        return this.arr.clone();
    }

}
