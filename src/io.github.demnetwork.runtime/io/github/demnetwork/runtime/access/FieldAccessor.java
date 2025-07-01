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

package io.github.demnetwork.runtime.access;

import java.util.*;
import java.lang.reflect.*;
import sun.misc.Unsafe;

public final class FieldAccessor extends java.lang.Object {
    @EnforceModifiers
    private final ClassLoader cl;
    @EnforceModifiers
    private final Set<Field> fields = new HashSet<>();
    @InvisibleField
    protected static final Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
            // test();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    };

    public FieldAccessor(ClassLoader cl) {
        if (cl == null)
            throw new NullPointerException("Null ClassLoader");
        this.cl = cl;
    }

    @SuppressWarnings({ "deprecation" })
    public void modifyField(Class<?> clazz, String name, Object instance, Object newValue)
            throws NoSuchFieldException, FieldWriteException {
        if (clazz == null) {
            if (instance == null)
                throw new NullPointerException("Null Class And Null Instance");
            clazz = instance.getClass();
        } else if (instance != null && !clazz.isInstance(instance))
            throw new ClassCastException("Invalid Instance");
        validateClass(clazz);
        if (name == null)
            throw new NullPointerException("Null Name");
        Field f = clazz.getDeclaredField(name);
        int mod = f.getModifiers();
        if (!fields.contains(f)) {
            if (f.isAnnotationPresent(EnforceModifiers.class)) {
                if ((mod & Modifier.FINAL) != 0)
                    throw new FieldWriteException(f, new RuntimeException("The field is final"));
            }
            if (f.isAnnotationPresent(InvisibleField.class)) {
                throw new NoSuchFieldException("The field \'" + name + "\' is invisible");
            }
            fields.add(f);
        }
        boolean isStatic = (mod & Modifier.STATIC) != 0;
        if (instance == null && !isStatic)
            throw new NullPointerException("Null Instance");
        long offset = 0;
        Object base = null;
        if (isStatic) {
            offset = UNSAFE.staticFieldOffset(f);
            base = UNSAFE.staticFieldBase(f);
        } else {
            base = instance;
            offset = UNSAFE.objectFieldOffset(f);
        }

        Class<?> type = f.getType();
        try {
            if (!type.isPrimitive()) {
                if (!type.isAssignableFrom(newValue.getClass()))
                    throw new ClassCastException("Type Mismatch");
                UNSAFE.putObject(base, offset, newValue);
            } else {
                if (type == double.class) {
                    UNSAFE.putDouble(base, offset, (double) newValue);
                } else if (type == float.class) {
                    UNSAFE.putFloat(base, offset, (float) newValue);
                } else if (type == long.class) {
                    UNSAFE.putLong(base, offset, (long) newValue);
                } else if (type == int.class) {
                    UNSAFE.putInt(base, offset, (int) newValue);
                } else if (type == char.class) {
                    UNSAFE.putChar(base, offset, (char) newValue);
                } else if (type == short.class) {
                    UNSAFE.putShort(base, offset, (short) newValue);
                } else if (type == byte.class) {
                    UNSAFE.putByte(base, offset, (byte) newValue);
                } else if (type == boolean.class) {
                    UNSAFE.putBoolean(base, offset, (boolean) newValue);
                } else
                    throw new AssertionError("Field type is \'void\'");
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Types do not match", e);
        }
    }

    protected final void validateClass(Class<?> clazz) {
        try {
            if (cl.loadClass(clazz.getName()) != clazz)
                throw new IllegalArgumentException("The Provided class is not in this ClassLoader");
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("ClassLoader was unable to find the provided class");
        }
    }
}
