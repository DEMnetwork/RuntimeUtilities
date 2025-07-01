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

package io.github.demnetwork.runtime.internal;

import sun.misc.Unsafe;
import java.lang.reflect.*;
import java.util.Map;
import java.util.HashMap;

public final class Placeholders {
    static final Unsafe UNSAFE;
    static final Map<Class<?>, Object> placeholders = new HashMap<>();
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Placeholders() {
    };

    public static Object getPlaceholder(Class<?> clazz) {
        if (clazz != null)
            try {
                if (placeholders.containsKey(clazz)) {
                    return placeholders.get(clazz);
                } else {
                    Object instance = UNSAFE.allocateInstance(clazz);
                    placeholders.put(clazz, instance);
                    return instance;
                }
            } catch (InstantiationException e) {
                return null;
            }
        return getPlaceholder(NullPlaceholder.class);
    }

    private static final class NullPlaceholder extends Object {

    }
}
