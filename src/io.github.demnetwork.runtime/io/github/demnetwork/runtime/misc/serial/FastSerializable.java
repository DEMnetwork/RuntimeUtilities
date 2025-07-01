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

package io.github.demnetwork.runtime.misc.serial;

import static io.github.demnetwork.runtime.misc.serial.FastDeserializer.UNSAFE;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.*;

public abstract interface FastSerializable extends Serializable {

    public abstract FastDeserializer<?> getDeserializer();

    @SuppressWarnings("deprecation")
    public default void fastWriteObject(ObjectOutputStream oos) throws IOException {
        FastSerializableIDentity id = this.getClass().getAnnotation(FastSerializableIDentity.class);
        if (id != null) {
            oos.writeLong(id.value());
            // System.out.println(id.value());
        } else
            throw new IllegalStateException("Missing @FastSerializableIDentity annotation");

        Field[] fields = this.getClass().getDeclaredFields();
        for (Field f : fields) {
            Class<?> type = f.getType();
            long offset = UNSAFE.objectFieldOffset(f);
            if (!(Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers()))) {
                if (!type.isPrimitive()) {
                    oos.writeObject(UNSAFE.getObject(this, offset));
                    // System.out.println(offset + " | " + UNSAFE.getObject(this, offset));
                } else {
                    if (type == double.class) {
                        oos.writeDouble(UNSAFE.getDouble(this, offset));
                    } else if (type == float.class) {
                        oos.writeFloat(UNSAFE.getFloat(this, offset));
                    } else if (type == long.class) {
                        oos.writeLong(UNSAFE.getLong(this, offset));
                    } else if (type == int.class) {
                        oos.writeInt(UNSAFE.getInt(this, offset));
                    } else if (type == char.class) {
                        oos.writeChar(UNSAFE.getChar(this, offset));
                    } else if (type == short.class) {
                        oos.writeShort(UNSAFE.getShort(this, offset));
                    } else if (type == byte.class) {
                        oos.writeByte(UNSAFE.getByte(this, offset));
                    } else if (type == boolean.class) {
                        oos.writeBoolean(UNSAFE.getBoolean(this, offset));
                    } else
                        throw new AssertionError("Field type is \'void\'");
                }
            }
            // System.out.println(f.getName());
        }
    }

    public default void validate() throws IllegalObjectState {
        return;
    }

    public default TransientInitializer getInitializer() {
        return TransientInitializer.nullInitializer();
    }
}
