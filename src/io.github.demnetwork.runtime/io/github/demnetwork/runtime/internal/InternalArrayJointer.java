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

import io.github.demnetwork.runtime.internal.secret.ArrayJointer;

public final class InternalArrayJointer {
    private static final InternalArrayJointer INSTANCE = new InternalArrayJointer();

    private InternalArrayJointer() {
    }

    public static InternalArrayJointer getArrayJointer() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] jointArrays(T[] a, T[] b) {
        if (a == null) {
            if (b != null)
                return b.clone();
            return null;
        }
        if (b == null)
            return a.clone();
        Class<?> cA = a.getClass().getComponentType();
        Class<?> cB = b.getClass().getComponentType();
        // Clone Arrays
        a = a.clone();
        b = b.clone();
        if (cA.isAssignableFrom(cB)) {
            T[] arr = (T[]) java.lang.reflect.Array.newInstance(cA, a.length + b.length);
            System.arraycopy(a, 0, arr, 0, a.length);
            System.arraycopy(b, 0, arr, a.length, b.length);
            return arr;
        } else if (cB.isAssignableFrom(cA)) {
            T[] arr = (T[]) java.lang.reflect.Array.newInstance(cB, a.length + b.length);
            System.arraycopy(a, 0, arr, 0, a.length);
            System.arraycopy(b, 0, arr, a.length, b.length);
            return arr;
        } else
            return ArrayJointer.jointArrays(a, b);
    }
}
