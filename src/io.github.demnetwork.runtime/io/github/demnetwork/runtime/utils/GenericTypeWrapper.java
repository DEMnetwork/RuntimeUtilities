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

import java.util.*;

import io.github.demnetwork.runtime.utils.RuntimeClassGenerator.*;

import java.lang.reflect.*;
import static java.lang.reflect.Modifier.*;

public final class GenericTypeWrapper {
    private final Object o;
    private final Object actualObject;
    private final String objCode;
    private static final Map<String, Class<?>> types = new HashMap<>();
    private static final Table wrappers = new Table(3, GenericTypeWrapper.class, Object.class, String.class);
    private static int nrn = 0;

    public GenericTypeWrapper(Object o) throws Exception {
        GenericType gt = null;
        if (!(o instanceof GenericType)) {
            throw new IllegalArgumentException("No Generic Data");
        }
        gt = (GenericType) o;
        String code = o.getClass().getName();
        Type[] arr = gt.getTypeArgs();
        // System.out.println(Arrays.toString(arr));
        for (int i = 0; i < arr.length; i++) {
            code += arr[i].getTypeName();
        }
        // System.out.println(code);
        if (!types.containsKey(code)) {
            Class<?> clazz = RuntimeClassGenerator.newGenerator("io.github.demnetwork.runtime.gen",
                    o.getClass().getSimpleName() + "Wrapper", PUBLIC | FINAL)
                    .addConstructor(PUBLIC, new ConstructorImpl() {

                        @Override
                        public void newInstance(Object[] args, Object thisObject) throws Exception {
                            setFieldValue(thisObject, "obj", args[0], null);
                        }

                    }, Object.class).addField(Object.class, PRIVATE | FINAL, "obj").addMethod(new MethodImpl() {

                        @Override
                        public Object onInvoke(Object[] args) throws Exception {
                            return getFieldValue(args[0], "obj", null);
                        }

                    }, PUBLIC, Object.class, "getWrappedObject").build();
            types.put(code, clazz);
            Method cns = clazz.getMethod("newInstance", Object.class);
            cns.setAccessible(true);
            this.o = cns.invoke(null, o);
        } else {
            Class<?> clazz = types.get(code);
            Method cns = clazz.getMethod("newInstance", Object.class);
            cns.setAccessible(true);
            this.o = cns.invoke(null, o);
        }
        this.objCode = code;
        this.actualObject = o;
        int row = nrn++;
        wrappers.set(this.objCode, 2, row);
        wrappers.set(this.o, 1, row);
        wrappers.set(this, 0, row);
    }

    public static interface GenericType1<T0> extends GenericType {
    }

    public static interface GenericType2<T0, T1> extends GenericType {
    }

    public static interface GenericType3<T0, T1, T2> extends GenericType {
    }

    public static interface GenericType4<T0, T1, T2, T3> extends GenericType {
    }

    public static interface GenericType5<T0, T1, T2, T3, T4> extends GenericType {

    }

    static public interface GenericType {
        public abstract Type[] getTypeArgs();
    }

    public Object getActualWrapperObject() {
        return this.o;
    }

    public Class<?> getActualWrapperType() {
        return this.o.getClass();
    }

    public Object getActualWrappedObject() {
        return this.actualObject;
    }

    public String getCode() {
        return this.objCode;
    }

    public static synchronized GenericTypeWrapper getWrapper(Object o) {
        int rc = wrappers.getRowCount();
        for (int i = 0; i < rc; i++) {
            if (wrappers.get(1, i) == o) {
                return (GenericTypeWrapper) wrappers.get(0, i);
            }
        }
        return null;
    }
}
