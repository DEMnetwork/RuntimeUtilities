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

package io.github.demnetwork.runtime.inject;

import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import io.github.demnetwork.runtime.access.EnforceModifiers;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class loader loads classes from a primary JAR and its dependencies.
 * It overrides the parent-first delegation model in favor of local lookup
 * first, this might allow the JARs to modify an loaded class, or might allow an
 * dependancy to get priority over another dependancy. Use with caution.
 * 
 */
public class InjectedClassLoader extends ClassLoader implements Closeable {
    @EnforceModifiers
    protected final JarFile jarFile;
    protected volatile boolean closed = false;
    protected Map<String, Class<?>> loadedClasses = new HashMap<>();
    @EnforceModifiers
    protected final JarFile[] arr;

    public InjectedClassLoader(ClassLoader parent, URL jar, URL... dependencyJars)
            throws IOException, URISyntaxException {
        super(parent);
        this.jarFile = new JarFile(new File(jar.toURI()));
        if (dependencyJars == null) {
            arr = new JarFile[0];
        } else {
            ArrayList<JarFile> list = new ArrayList<>();
            for (URL url : dependencyJars) {
                if (url != null) {
                    File f = new File(url.toURI());
                    if (f.exists() && !f.isDirectory())
                        list.add(new JarFile(f));
                }
            }
            this.arr = list.toArray(new JarFile[list.size()]);
        }
    }

    public InjectedClassLoader(URL jar, URL... dependencyJars) throws IOException, URISyntaxException {
        this(ClassLoader.getSystemClassLoader(), jar, dependencyJars);
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        closed = true;
        this.jarFile.close();
        for (JarFile f : arr) {
            f.close();
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        ensureOpen();
        Class<?> cls = findClass(name);
        if (resolve)
            super.resolveClass(cls);
        return cls;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        ensureOpen();
        if (className == null)
            throw new NullPointerException();
        if (this.loadedClasses.containsKey(className))
            return this.loadedClasses.get(className);
        final String ClassName = className.replace(".", "/") + ".class";
        ArrayList<Exception> exs = new ArrayList<>();
        ZipEntry e = jarFile.getEntry(ClassName);
        if (e != null) {
            if (e.isDirectory())
                throw new ClassNotFoundException("Unable to find class(The target class is a directory)");
            try {
                Class<?> cls = defCls(className, ClassName, e, this.jarFile);
                loadedClasses.put(className, cls);
                return cls;
            } catch (Exception ex) {
                exs.add(ex);
            }
        } else {
            for (JarFile f : arr) {
                e = f.getEntry(ClassName);
                if (e != null) {
                    if (e.isDirectory())
                        throw new ClassNotFoundException("Unable to find class(The target class is a directory)");
                    try {
                        Class<?> cls = defCls(className, ClassName, e, f);
                        loadedClasses.put(className, cls);
                        return cls;
                    } catch (Exception ex) {
                        exs.add(ex);
                    }
                } else {
                    exs.add(new ClassNotFoundException(
                            "Unable to find class \'" + className + "\' at \"" + f.getName() + "\""));
                }
            }
        }
        try {
            return super.getParent().loadClass(className);
        } catch (ClassNotFoundException ex) {
            exs.add(ex);
        }
        ClassNotFoundException ex = new ClassNotFoundException("Unable to find class \'" + className + "\'");
        for (Exception exception : exs)
            ex.addSuppressed(exception);
        throw ex;
    }

    protected final Class<?> defCls(String className, String ClassName, ZipEntry e, JarFile jarFile)
            throws ClassNotFoundException {
        try {
            InputStream is = jarFile.getInputStream(e);
            byte[] arr2 = is.readAllBytes();
            is.close();
            return super.defineClass(className, arr2, 0, arr2.length);
        } catch (Exception ex) {
            throw new ClassNotFoundException(
                    "Unable to find class \'" + className + "\' at \"" + jarFile.getName() + "\"",
                    ex);
        } catch (ClassFormatError ex2) {
            throw new RuntimeException(
                    "The class \'" + className + "\' at \"" + jarFile.getName() + "\" is an invalid class", ex2);
        }
    }

    public final boolean isClosed() {
        return this.closed;
    }

    protected final void ensureOpen() throws IllegalStateException {
        if (this.closed)
            throw new IllegalStateException("This ClassLoader is closed");
    }

    public boolean clearCache() {
        try {
            loadedClasses.clear();
            return (loadedClasses.isEmpty());
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    public Optional<Class<?>> optionalClass(String name) {
        return this.optionalClass(name, false);
    }

    public Optional<Class<?>> optionalClass(String name, boolean resolve) {
        try {
            return Optional.of(loadClass(name, resolve));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }
}
