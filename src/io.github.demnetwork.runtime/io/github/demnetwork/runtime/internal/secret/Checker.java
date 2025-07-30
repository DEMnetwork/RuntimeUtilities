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

package io.github.demnetwork.runtime.internal.secret;

public final class Checker {
    private Checker() {
        throw new SecurityException();
    }

    public static boolean chkClassLoader(Class<?> cls, ClassLoader cl) {
        if (cls == null)
            return false;
        if (cl == null)
            throw new UnsupportedOperationException(
                    "Cannot use null ClassLoader(Bootstrap ClassLoader) to load classes in Java, because it is null");
        try {
            if (cls.getClassLoader() == cl)
                return true;
            if (cl.loadClass(cls.getName()) != cls)
                return false;
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static <T> boolean chkArray(T[] arr) {
        for (T o : arr) {
            if (o == null)
                return false;
        }
        return true;
    }

    public static boolean chkArray(Class<?>[] arr, ClassLoader cl) {
        if (arr == null)
            return false;
        if (cl == null)
            throw new UnsupportedOperationException(
                    "Cannot use null ClassLoader(Bootstrap ClassLoader) to load classes in Java, because it is null");
        for (Class<?> o : arr) {
            if (!chkClassLoader(o, cl))
                return false;
        }
        return true;
    }

    public static boolean strictContainsObject(Object[] arr, Object obj) {
        if (arr == null)
            return false;
        if (!arr.getClass().getComponentType().isAssignableFrom(obj.getClass()))
            return false;
        for (Object o : arr) {
            if (o == obj)
                return true;
        }
        return false;
    }
}
