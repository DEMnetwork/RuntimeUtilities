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
import java.io.IOException;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.IntWrapper;

/**
 * * <strong>Note:</strong> This class assumes that input streams
 * passed to
 * {@code readObj}
 * are trusted and well-formed. It does not perform validation or
 * enforce
 * structural
 * integrity of the input.
 * <p>
 * It is the caller's responsibility to ensure the stream is safe and
 * correctly
 * formatted.
 * Passing untrusted or malformed streams may lead to memory
 * corruption,
 * deserialization issues,
 * or security vulnerabilities.
 * 
 * @implNote Can only read enums within the System Class Loader
 * 
 */
public final class EnumWrapper<E extends Enum<E>> implements MemorySerializable, Wrapper<E> {
    private final E value;
    private final Class<E> cls;

    public EnumWrapper(E value, Class<E> cls) {
        if (cls == null || value == null)
            throw new NullPointerException();
        if (!cls.isEnum())
            throw new IllegalArgumentException("Not an enum");
        if (value.getClass() != cls)
            throw new IllegalArgumentException(
                    "The value is not apart of the enum class, because its runtime class is not the same as the provided class");
        this.value = value;
        this.cls = cls;
    }

    @Override
    public E getValue() {
        return value;
    }

    @Override
    public void writeObj(MemoryOutputStream mos) throws IOException {
        mos.writeObj(new StringWrapper(cls.getName())); // We cannot ensure class is MemorySerializable, so we have do
                                                        // to do this
        mos.writeObj(new IntWrapper(value.ordinal()));
    }

    @Override
    public MemorySerializable readObj(MemoryInputStream mis) throws IOException {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            Class<?> cls = cl.loadClass(((StringWrapper) mis.readObj()).getValue());
            Object[] arr = cls.getEnumConstants();
            if (arr == null)
                throw new IOException("The provided class is not an enum");
            UNSAFE.putObject(this, UNSAFE.objectFieldOffset(EnumWrapper.class.getDeclaredField("cls")), cls);
            UNSAFE.putObject(this, UNSAFE.objectFieldOffset(EnumWrapper.class.getDeclaredField("value")),
                    arr[((IntWrapper) mis.readObj()).getValue()]);
            return this;
        } catch (ClassCastException | ReflectiveOperationException e) {
            throw new IOException("De-Serialization failed", e);
        }
    }
}
