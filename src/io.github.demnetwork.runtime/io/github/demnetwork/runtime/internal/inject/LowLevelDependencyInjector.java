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

import io.github.demnetwork.runtime.inject.InjectedClassLoader;
import io.github.demnetwork.runtime.internal.InternalArrayJointer;
import io.github.demnetwork.runtime.internal.secret.ArrayJointer;
import io.github.demnetwork.runtime.internal.secret.Checker;
import io.github.demnetwork.runtime.inject.annotation.*;
import static io.github.demnetwork.runtime.internal.inject.DependencyObject.Type.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * Used to implement part of
 * {@link io.github.demnetwork.runtime.inject.DependencyInjectionContainer
 * DependancyInjectionContainer} DI
 * capabilities
 */
public non-sealed class LowLevelDependencyInjector extends DependencyObject {
    private final InjectedClassLoader cl;
    @SuppressWarnings("unchecked")
    static final Class<? extends Annotation>[] ANNOTATION_CLASSES = new Class[] { Client.class, Service.class,
            Interface.class, InjectLocation.class };
    private final Class<? extends Annotation>[] arr;

    public LowLevelDependencyInjector(InjectedClassLoader cl) {
        this(cl, ANNOTATION_CLASSES);
    }

    public LowLevelDependencyInjector(InjectedClassLoader cl, Class<? extends Annotation>[] annotations) {
        super(INJECTOR);
        if (cl == null)
            throw new NullPointerException("Null InjectedClassLoader");
        if (cl.isClosed())
            throw new IllegalArgumentException("Closed InjectedClassLoader");
        if (annotations == null)
            throw new NullPointerException("Null Annotation Array");
        if (annotations.length != 4)
            throw new IllegalArgumentException(
                    "The Annotation Array does not have all required annotations or has more annotations than the LowLevelDependencyInjector uses");
        this.cl = cl;
        Class<? extends Annotation>[] arr = annotations.clone();
        Checker.chkArray(arr, cl);
        this.arr = arr;
    }

    public final InjectedClassLoader getClassLoader() {
        return cl;
    }

    @Override
    public final Object getInstance() {
        return this;
    }

    @Override
    public final Class<?> getObjectType() {
        return super.getClass();
    }

    /**
     * No-OP method
     * 
     * @throws UnsupportedOperationException Because it is unsupported by this
     *                                       Injector
     */
    @Override
    final void setInstance(Object o) {
        throw new UnsupportedOperationException(
                "LowLevelDependencyInjector does not support \'setInstance(Object)\' method");
    }

    public DependencyObject resolve(Class<?> clazz) {
        if (!Checker.chkClassLoader(clazz, cl))
            throw new IllegalArgumentException(
                    "This class was not loaded by the InjectedClassLoader provided nor loaded by its ancestor(s)");
        if (clazz.isAnnotationPresent(arr[0])) {
            return new DependencyObject(CLIENT, clazz, null);
        }
        if (clazz.isAnnotationPresent(arr[1])) {
            return new DependencyObject(SERVICE, clazz, null);
        }
        if (clazz.isAnnotationPresent(arr[2])) {
            return new DependencyObject(INTERFACE, clazz, null);
        }
        return null;
    }

    public final boolean modifyDependencyObject(DependencyObject obj, Object newValue) {
        if (obj == null)
            return false;
        if (obj == this || obj.TYPE == INJECTOR)
            return false;
        Class<?> clazz = obj.getObjectType();
        if (!Checker.chkClassLoader(clazz, this.cl))
            return false;
        if (newValue == null)
            return false;
        obj.setInstance(newValue);
        return obj.getInstance() == newValue;
    }

    /**
     * Creates an instance of the interface that is bound to the service
     * 
     * @param serviceObj   Service to bind to interface
     * @param interfaceObj Interface to be bound to service, and instantiated
     * @return <code>true</code> if succeded, <code>false</code> otherwise
     * 
     * @throws NullPointerException     If any argument was null
     * @throws IllegalArgumentException If the service si nto a service or if the
     *                                  interface is not an interface
     * @throws RuntimeException         If the provided
     *                                  {@link io.github.demnetwork.runtime.internal.inject.DependencyObject
     *                                  DepedencyObject} was not instantiated and
     *                                  instatntiation failed
     */
    public boolean createInstance(DependencyObject serviceObj, DependencyObject interfaceObj) {
        return this.createInstance(serviceObj, interfaceObj, Handeler.class);
    }

    public boolean createInstance(DependencyObject serviceObj, DependencyObject interfaceObj,
            Class<? extends InvocationHandler> hClass) {
        if (serviceObj == null || interfaceObj == null)
            throw new NullPointerException("Null argument(s) were passed to this method");
        if (serviceObj.TYPE != SERVICE)
            throw new IllegalArgumentException("The service is not a service");
        if (interfaceObj.TYPE != INTERFACE)
            throw new IllegalArgumentException("The interface is not a interface");
        if (serviceObj.getInstance() == null) {
            if (!createInstance(serviceObj, new Class[0]))
                throw new RuntimeException("Service was not instantiated and instantiation failed");
        }
        Class<?> type = interfaceObj.getObjectType();
        Object srv = serviceObj.getInstance();
        if (type.isInstance(srv)) {
            return this.modifyDependencyObject(interfaceObj, srv);
        } else {
            try {
                // The caller must ensure that the serviceObj is compatible with the Interface
                // or undefined behaviour might occour
                return this.modifyDependencyObject(interfaceObj, Proxy.newProxyInstance(cl, new Class[] { type },
                        hClass.getConstructor(Object.class).newInstance(srv)));
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }
    }

    protected static class Handeler implements InvocationHandler {
        private final Object ServiceObj;

        public Handeler(Object ServiceObj) {
            this.ServiceObj = ServiceObj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                Method m = ServiceObj.getClass().getMethod(method.getName(), method.getParameterTypes());
                m.setAccessible(true);
                return m.invoke(ServiceObj, args);
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError("Unable to find method \'" + method.getName() + "\';");
            }
        }

    }

    /**
     * Creates an instance
     * 
     * @param obj        DepedencyObject wrapping the Object to be created
     * @param paramTypes Type of the parameters
     * @param params     Parameters
     * @return <code>true</code> if succeded, <code>false</code> otherwise
     */
    public boolean createInstance(DependencyObject obj, Class<?>[] paramTypes, Object... params) {
        if (obj == null || params == null || paramTypes == null)
            throw new NullPointerException("Null argument(s) were passed to this method");
        if (obj.TYPE == INJECTOR || obj.TYPE == INTERFACE)
            return false;
        if (paramTypes.length != params.length)
            throw new NullPointerException();
        try {
            return modifyDependencyObject(obj, obj.getObjectType().getConstructor(paramTypes).newInstance(params));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public Object injectClient(DependencyObject clientObj, DependencyObject interfaceObj, InjectionType injectionType,
            Class<?>[] additionalParamTypes, Object[] additionalParams, String methodName) throws BindException {
        if (clientObj == null || interfaceObj == null || injectionType == null || additionalParamTypes == null
                || additionalParams == null)
            throw new NullPointerException("Null argument(s) were passed to this method");
        if (clientObj.TYPE != CLIENT)
            throw new IllegalArgumentException("Client is not a Client");
        if (interfaceObj.TYPE != INTERFACE)
            throw new IllegalArgumentException("Interface is not an Interface");
        Class<?>[] paramTypes = additionalParamTypes.clone();
        if (!Checker.chkArray(paramTypes))
            throw new NullPointerException("Null Values in the \'additionalParamTypes\' array");
        Object[] params = additionalParams.clone();
        if (params.length != paramTypes.length)
            throw new IllegalArgumentException("Lenghts do not match");
        Class<?>[] argTypes = InternalArrayJointer.getArrayJointer().jointArrays(
                new Class<?>[] { interfaceObj.getObjectType() },
                paramTypes); // Preserve Type Safety
        Object[] args = ArrayJointer.jointArrays(new Object[] { interfaceObj.getInstance() }, params);
        if (injectionType == InjectionType.CONSTRUCTOR) {
            try {
                Constructor<?> cns = clientObj.getObjectType().getConstructor(argTypes);
                if (!cns.isAnnotationPresent(arr[3]))
                    throw new BindException("The method in the client does not support Injection");
                cns.setAccessible(true);
                Object obj = cns.newInstance(args);
                this.modifyDependencyObject(clientObj, obj);
                return obj;
            } catch (ReflectiveOperationException e) {
                throw new BindException("Something went wrong while instantiating client", e);
            }
        } else {
            if (methodName == null)
                throw new NullPointerException("Method Injection Type requires an method Name");
            try {
                Method m = clientObj.getObjectType().getMethod(methodName, argTypes);
                if (((m.getModifiers() & Modifier.STATIC) == 0) && clientObj.getInstance() == null)
                    throw new BindException("Client was not instantiated and the method is non-static");
                if (!m.isAnnotationPresent(arr[3]))
                    throw new BindException("The method in the client does not support Injection");
                m.setAccessible(true);
                return m.invoke(clientObj.getInstance(), args);
            } catch (ReflectiveOperationException e) {
                throw new BindException("Something went wrong while instantiating client", e);
            }
        }
    }

    public final Class<? extends Annotation>[] getAnnotations() {
        return arr.clone();
    }
}
