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

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import sun.misc.Unsafe;
import io.github.demnetwork.runtime.internal.secret.Checker;
import io.github.demnetwork.runtime.utils.memory.SyntheticMemoryStruct;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.*;

public final class MemorySerializableRegistry {
    static final Unsafe UNSAFE;
    static final ConcurrentHashMap<Long, Class<? extends MemorySerializable>> registry = new ConcurrentHashMap<>(); // Registry
    static final ConcurrentHashMap<Class<? extends MemorySerializable>, Long> cRegistry = new ConcurrentHashMap<>(); // Counter_Registry
    public static final Class<?> NULL_WRAPPER = register(1L, NullWrapper.class);
    public static final Class<?> DOUBLE_WRAPPER = register(-1L, DoubleWrapper.class);
    public static final Class<?> LONG_WRAPPER = register(-2L, LongWrapper.class);
    public static final Class<?> INT_WRAPPER = register(-3L, IntWrapper.class);
    public static final Class<?> CHAR_WRAPPER = register(-4L, CharacterWrapper.class);
    public static final Class<?> STRING_WRAPPER = register(-5L, StringWrapper.class);
    public static final Class<?> ARRAY_WRAPPER = register(-6L, ArrayWrapper.class);
    public static final Class<?> ENUM_WRAPPER = register(-7L, EnumWrapper.class);
    public static final Class<?> BOOLEAN_WRAPPER = register(-8L, BooleanWrapper.class);
    public static final Class<?> SYNTHETIC_MEMORY_STRUCT = register(Long.MAX_VALUE, SyntheticMemoryStruct.class);
    private static final Class<?>[] NON_UNREGISTRABLE_CLASSES = new Class[] { NULL_WRAPPER, DOUBLE_WRAPPER,
            LONG_WRAPPER, INT_WRAPPER, CHAR_WRAPPER, STRING_WRAPPER, ARRAY_WRAPPER, ENUM_WRAPPER, BOOLEAN_WRAPPER,
            SYNTHETIC_MEMORY_STRUCT, MemorySerializable.class };

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        onInit();
    }

    private static final void onInit() {
        registry.put(Long.valueOf(0L), MemorySerializable.class);
        cRegistry.put(MemorySerializable.class, Long.valueOf(0L));
    }

    public static <T extends MemorySerializable> Class<T> register(long id, Class<T> clazz) {
        if (clazz == null)
            throw new NullPointerException();
        if (clazz == MemorySerializable.class)
            throw new IllegalArgumentException("Illegal Class");
        if (registry.containsKey(id))
            throw new IllegalStateException("Duplicate ID: " + id);
        if (cRegistry.containsKey(clazz))
            throw new IllegalStateException("Class already registered: " + clazz);
        registry.put(Long.valueOf(id), clazz);
        cRegistry.put(clazz, Long.valueOf(id));
        return clazz;
    }

    public static Class<? extends MemorySerializable> getRegisteredClass(long id) {
        return registry.getOrDefault(Long.valueOf(id), MemorySerializable.class);
    }

    public static long getIdOfClass(Class<? extends MemorySerializable> cls) {
        return cRegistry.getOrDefault(cls, 0L).longValue();
    }

    public static boolean unregister(Class<? extends MemorySerializable> clazz) {
        if (clazz == null)
            throw new NullPointerException("Null Class");
        if (Checker.strictContainsObject(NON_UNREGISTRABLE_CLASSES, clazz))
            throw new IllegalArgumentException("Cannot Unregister the class: " + clazz.getName());
        Long id = cRegistry.remove(clazz);
        if (id != null) {
            registry.remove(id);
            return true;
        }
        return false;
    }

    public static Class<?>[] getNonUnregistrableClasses() {
        return NON_UNREGISTRABLE_CLASSES.clone(); // Class Objects are immutable
    }
}
