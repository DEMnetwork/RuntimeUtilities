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

package io.github.demnetwork.runtime.internal.inject;

/**
 * Represents any Object that is part of the Dependency Injection roles
 */
public sealed class DependencyObject permits AbstractLowLevelDependencyInjector, LowLevelDependencyInjector {
    public static enum Type {
        CLIENT, SERVICE, INTERFACE, INJECTOR;
    }

    public final Type TYPE;
    private final Class<?> clazz;
    private Object instance = null;

    /** Used internally */
    DependencyObject(Type t) {
        if (t == null)
            throw new NullPointerException();
        this.TYPE = t;
        this.clazz = null;
    }

    /**
     * Defines a new DependencyObject
     * 
     * @param type     Role in the Dependency Injection
     * @param clazz    Type that the instance should hold
     * @param instance Instance of the type <code>clazz</code> or null
     */
    public DependencyObject(Type type, Class<?> clazz, Object instance) {
        if (type == null)
            throw new NullPointerException();
        if (null == clazz)
            throw new NullPointerException();
        if (instance != null)
            if (!clazz.isInstance(instance))
                throw new IllegalArgumentException("Illegal Type for Instance");
        this.TYPE = type;
        this.clazz = clazz;
        this.instance = instance;
    }

    /**
     * An method to retrieve the instance
     * 
     * @return The instance, of which can be <code>null</code>
     */
    public Object getInstance() {
        return instance;
    }

    /**
     * 
     * @return The Type that instance stored in the DependencyObject should be;
     */
    public Class<?> getObjectType() {
        return clazz;
    }

    /**
     * This method is used to change the instance of the DependencyObject holds
     * <p>
     * Callers <strong>MUST</strong> ensure that the instance is non-null and it
     * matches the type
     * 
     * @param o The new Value
     */
    void setInstance(Object o) {
        if (o == null)
            throw new NullPointerException();
        if (clazz.isInstance(o))
            this.instance = o;
    }
}
