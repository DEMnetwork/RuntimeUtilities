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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import static io.github.demnetwork.runtime.internal.inject.DependencyObject.Type.*;
import io.github.demnetwork.runtime.inject.InjectedClassLoader;
import io.github.demnetwork.runtime.inject.InjectionException;
import io.github.demnetwork.runtime.internal.secret.Checker;

/**
 * Used to make custom containers that do not subclass
 * {@link io.github.demnetwork.runtime.inject.DependencyInjectionContainer
 * DependencyInjectionContainer}
 */
public non-sealed abstract class AbstractLowLevelDependencyInjector extends DependencyObject {
    private final InjectedClassLoader cl;

    public AbstractLowLevelDependencyInjector(InjectedClassLoader cl) {
        super(INJECTOR);
        if (cl == null)
            throw new NullPointerException();
        this.cl = cl;
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

    public abstract DependencyObject resolve(Class<?> clazz);

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
        return this.createInstance(serviceObj, interfaceObj, ALLDIHandeler.class);
    }

    public abstract boolean createInstance(DependencyObject serviceObj, DependencyObject interfaceObj,
            Class<? extends InvocationHandler> hClass);

    protected static class ALLDIHandeler implements InvocationHandler {
        private final Object ServiceObj;

        public ALLDIHandeler(Object ServiceObj) {
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
    public abstract boolean createInstance(DependencyObject obj, Class<?>[] paramTypes, Object... params);

    public abstract Object injectClient(DependencyObject clientObj, DependencyObject interfaceObj,
            InjectionType injectionType,
            Class<?>[] additionalParamTypes, Object[] additionalParams, String methodName) throws InjectionException;

    abstract public Class<? extends Annotation>[] getAnnotations();
}
