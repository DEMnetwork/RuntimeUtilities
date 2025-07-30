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

package io.github.demnetwork.runtime.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.function.Supplier;

public abstract class LazyInit<T extends LazyInit.LazyInitializable> implements Supplier<T> {
    protected T instance = null;
    protected final Class<T> type;
    protected volatile boolean initialized = false;
    protected final Object lock = new Object();
    protected final Object[] initArgs;

    protected LazyInit(Object[] args, Class<T> clazz) {
        chkType(clazz, this.getClass());
        if (args == null)
            throw new NullPointerException("Null Arguments");
        this.initArgs = Arrays.copyOf(args, args.length);
        this.type = clazz;
    }

    public abstract T newInstance() throws Exception;

    /**
     * Used by {@link BasicLazyInit} as optimization
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static abstract @interface DefaultConstructor {
        /**
         * Method that returns the parameters of the Default Constructor
         * 
         * @return The default constructor arguments
         *
         */
        public Class<?>[] value() default {
        };
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static abstract @interface LazyInitializer {
        @SuppressWarnings("rawtypes")
        public Class<? extends LazyInit> value() default BasicLazyInit.class;
    }

    private static class BasicLazyInit<T extends LazyInitializable> extends LazyInit<T> {
        private Constructor<T> c;

        @SuppressWarnings("unchecked")
        private BasicLazyInit(Object[] args, Class<T> type, boolean lookupConstructors) {
            super(args, type);
            Constructor<T> cns = null;
            DefaultConstructor d = type.getAnnotation(DefaultConstructor.class);
            Class<?>[] cls = null;
            if (d != null) {
                try {
                    cls = d.value();
                    if (check(cls)) {
                        cns = type.getConstructor(cls);
                    }
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Invalid Default Constructor", e);
                }
            }
            if (lookupConstructors) {
                Constructor<T>[] cs = (Constructor<T>[]) type.getConstructors();
                Constructor<T> f = null;
                if (cs.length == 0) {
                    throw new IllegalArgumentException("The type has no public Constructors");
                }
                for (int i = 0; i < cs.length; i++) {
                    Constructor<T> c = cs[i];
                    Class<?>[] clazzes = c.getParameterTypes();
                    if (!java.util.Arrays.equals(clazzes, cls)) {
                        if (check(clazzes)) {
                            cns = c;
                            f = cns;
                            break;
                        }
                    }
                }
                if (f == null) {
                    throw new IllegalArgumentException("Invalid Constructor or invalid arguments");
                }
            }
            this.c = cns;
        }

        private BasicLazyInit(Object[] args, Class<?>[] argTypes, Class<T> type) {
            super(args, type);
            if (argTypes == null)
                throw new NullPointerException("Null Argument types");
            if (argTypes.length != args.length)
                throw new IllegalArgumentException("Argument Array Length does not match Argument Type Array Length");
            try {
                this.c = type.getConstructor(argTypes);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Invalid Constructor");
            }
        }

        @Override
        public T newInstance() throws Exception {
            if (!super.initialized) {
                synchronized (super.lock) {
                    if (super.instance == null) {
                        if (c == null) {
                            Constructor<T> c = type.getConstructor(getTypes());
                            super.instance = c.newInstance(super.initArgs);
                            super.initialized = true;
                        } else {
                            super.instance = this.c.newInstance(super.initArgs);
                            super.initialized = true;
                        }
                    }
                    return super.instance;
                }
            } else if (super.instance != null) {
                return super.instance;
            } else {
                throw new Error("The Current LazyInit had already initalized one instance of Type \'"
                        + type.getName() + "\' but its value is null");
            }
        }

        private final Class<?>[] getTypes() {
            Class<?>[] clazzes = new Class[super.initArgs.length];
            for (int i = 0; i < super.initArgs.length; i++) {
                if (super.initArgs[i] != null) {
                    clazzes[i] = super.initArgs[i].getClass();
                } else {
                    clazzes[i] = Object.class;
                }

            }
            return clazzes;
        }

        private final Class<?>[] getTypes2() {
            Class<?>[] clazzes = new Class[super.initArgs.length];
            for (int i = 0; i < super.initArgs.length; i++) {
                if (super.initArgs[i] != null) {
                    clazzes[i] = super.initArgs[i].getClass();
                } else {
                    clazzes[i] = null;
                }

            }
            return clazzes;
        }

        private final boolean check(Class<?>[] types) {
            Class<?>[] thisTypes = getTypes2();
            if (thisTypes.length != types.length)
                return false;
            if (thisTypes == types)
                return true;
            for (int i = 0; i < types.length; i++) {
                /*
                 * System.out.println("thisTypes[" + i + "]: " + thisTypes[i].getName() + ";
                 * // types[" + i + "]: "
                 * + types[i].getName() + ";");
                 */
                if (thisTypes[i] == null) {
                    if (types[i].isPrimitive()) {
                        return false;
                    }
                } else if (!convertClass(types[i]).isAssignableFrom(thisTypes[i])) {
                    return false;
                }
            }
            return true;
        }

        private static Class<?> convertClass(Class<?> clazz) {
            if (clazz == int.class) {
                return Integer.class;
            } else if (clazz == long.class) {
                return Long.class;
            } else if (clazz == double.class) {
                return Double.class;
            } else if (clazz == float.class) {
                return Float.class;
            } else if (clazz == boolean.class) {
                return Boolean.class;
            } else if (clazz == char.class) {
                return Character.class;
            } else if (clazz == byte.class) {
                return Byte.class;
            } else if (clazz == short.class) {
                return Short.class;
            } else {
                return clazz;
            }
        }

    }

    public final static <T extends LazyInitializable> LazyInit<T> getLazyInit(Class<T> type, boolean lookupConstructors,
            Object... args) {
        chkType(type, BasicLazyInit.class);
        return new BasicLazyInit<T>(args, type, lookupConstructors);
    }

    public static final <T extends LazyInitializable> LazyInit<T> getLazyInit(Class<T> type, Object... args) {
        return getLazyInit(type, true, args);
    }

    public static final <T extends LazyInitializable> LazyInit<T> getLazyInit(Class<T> type, Object[] args,
            Class<?>[] types) {
        chkType(type, BasicLazyInit.class);
        return new BasicLazyInit<>(args, types, type);
    }

    public static final <T extends LazyInitializable> SupplierLazyInit<T> getLazyInit(Class<T> type,
            Supplier<T> supplier) {
        chkType(type, SupplierLazyInit.class);
        return new SupplierLazyInit<T>(supplier, type);
    }

    @SuppressWarnings("rawtypes")
    private static void chkType(Class<?> type, Class<? extends LazyInit> lazyInitType) {
        if (type == null)
            throw new NullPointerException("Null Type");
        LazyInitializer li = type.getDeclaredAnnotation(LazyInitializer.class);
        if (li != null) {
            Class<?> lic = li.value();
            if (lic != null && lic != lazyInitType && lic != LazyInit.class)
                throw new IllegalArgumentException(
                        "The class \"" + type.getName() + "\" uses a different LazyInit(\"" + lic.getName() + "\")");
        }
    }

    public final boolean isInitialized() {
        return this.initialized;
    }

    public static final class SupplierLazyInit<T extends LazyInitializable> extends LazyInit<T> {
        private static final Object[] DUMMY_ARGUMENTS = new Object[0];
        private final Supplier<T> supplier;

        protected SupplierLazyInit(Supplier<T> supplier, Class<T> type) {
            super(DUMMY_ARGUMENTS, type);
            if (supplier == null)
                throw new NullPointerException("Null Supplier");
            this.supplier = supplier;
        }

        @Override
        public T newInstance() {
            if (!super.initialized) {
                synchronized (super.lock) {
                    if (super.instance == null) {
                        T i = supplier.get();
                        if (i == null)
                            throw new NullPointerException("Supplier returned null");
                        super.instance = i;
                        super.initialized = true;
                    }
                    return super.instance;
                }
            } else if (super.instance != null) {
                return super.instance;
            } else {
                throw new Error("The Current LazyInit had already initalized one instance of Type \'"
                        + type.getName() + "\' but its value is null");
            }
        }

    }

    /**
     * This method will return an instance of type T, the instance will be created
     * if it was not created, otherwise it will return the cached instance
     * 
     * @return The instance
     */
    @Override
    public final T get() {
        try {
            return newInstance();
        } catch (Exception e) {
            throw new RuntimeException("LazyInit failed to instantiate for type: " + type.getName(), e);
        }
    }

    @Override
    public String toString() {
        return "LazyInit<? extends " + type.getName() + ">(" + (initialized ? "initialized" : "uninitialized") + ")";
    }

    public static abstract interface LazyInitializable {
        public static LazyInit<?> getLazyInit() {
            throw new IllegalStateException("This method was not implemented; A implementer of it must implement it");
        }
    }
}
