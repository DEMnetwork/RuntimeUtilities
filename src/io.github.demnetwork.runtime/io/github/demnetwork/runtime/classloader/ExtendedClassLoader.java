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

package io.github.demnetwork.runtime.classloader;

import java.util.*;
import java.util.function.Consumer;

import io.github.demnetwork.runtime.access.InvisibleField;

public final class ExtendedClassLoader extends ClassLoader {
    @InvisibleField
    private final List<ClassLoader> cls;

    public ExtendedClassLoader() {
        this(new ArrayList<>());
    }

    public ExtendedClassLoader(List<ClassLoader> loaders) {
        super();
        if (loaders == null) {
            throw new NullPointerException("Null Loaders");
        }
        if (!loaders.isEmpty()) {
            this.cls = new ArrayList<ClassLoader>();
            loaders.iterator().forEachRemaining((o) -> {
                this.cls.add(o);
            });
        } else {
            this.cls = new ArrayList<>();
        }
    }

    public ExtendedClassLoader(ClassLoader parent) {
        super(parent);
        this.cls = new ArrayList<>();
        this.addClassLoader(parent);
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassNotFoundException cce = new ClassNotFoundException(name);
        Class<?>[] cls = getClassesWithName(name);
        if (cls.length == 0) {
            lastSup.iterator().forEachRemaining(new Consumer<Throwable>() {

                @Override
                public void accept(Throwable t) {
                    cce.addSuppressed(t);
                }

            });
        }
        return cls[0];
    }

    private List<ClassNotFoundException> lastSup = new ArrayList<>();

    public Class<?>[] getClassesWithName(String name) {
        lastSup = new ArrayList<>();
        int p = 0;
        int l = 0;
        Class<?>[] arr = new Class[cls.size()];
        for (ClassLoader cl : cls) {
            try {
                arr[p++] = cl.loadClass(name);
                l++;
            } catch (ClassNotFoundException e) {
                lastSup.add(e);
            }
        }
        if (arr[0] == null)
            return new Class<?>[0];
        return Arrays.copyOf(arr, l);
    }

    public ExtendedClassLoader addClassLoader(ClassLoader cl) {
        if (cl == this || cl == null) {
            throw new IllegalArgumentException("The class loader is the same as this classloader or it is null");
        }
        this.cls.add(cl);
        return this;
    }

}
