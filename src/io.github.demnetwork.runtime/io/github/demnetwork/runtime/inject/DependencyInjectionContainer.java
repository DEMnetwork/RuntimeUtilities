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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import io.github.demnetwork.runtime.inject.annotation.lifecycle.*;
import io.github.demnetwork.runtime.internal.inject.DependencyObject;
import io.github.demnetwork.runtime.internal.inject.InjectionType;
import io.github.demnetwork.runtime.internal.inject.LowLevelDependencyInjector;
import io.github.demnetwork.runtime.internal.secret.Checker;
import io.github.demnetwork.runtime.internal.inject.BindException;

public class DependencyInjectionContainer {
    protected final LowLevelDependencyInjector lldi;
    private final Class<? extends Annotation>[] lifecycleAnnotations;
    private Class<? extends Annotation> defaultLifecycleAnnotation = Singleton.class; // Treat as singleton
    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] DEFAULT_LIFECYCLE = (Class<? extends Annotation>[]) new Class<?>[] {
            Singleton.class, Prototype.class };
    private final Map<Class<?>, DependencyObject> registrations = new ConcurrentHashMap<>();

    public DependencyInjectionContainer(URL mainJar, URL... depdendencyJars) throws IOException, URISyntaxException {
        this(ClassLoader.getSystemClassLoader(), mainJar, depdendencyJars);
    }

    public DependencyInjectionContainer(ClassLoader parent, URL mainJar, URL... depdendencyJars)
            throws IOException, URISyntaxException {
        this(new InjectedClassLoader(parent, mainJar, depdendencyJars));
    }

    public DependencyInjectionContainer(InjectedClassLoader classLoader) {
        this(new LowLevelDependencyInjector(classLoader));
    }

    protected DependencyInjectionContainer(LowLevelDependencyInjector injector) {
        this(injector, DEFAULT_LIFECYCLE);
    }

    protected DependencyInjectionContainer(LowLevelDependencyInjector injector,
            Class<? extends Annotation>[] lifecycleAnnotations) {
        if (injector == null)
            throw new NullPointerException("Null Injector");
        this.lldi = injector;
        if (lifecycleAnnotations == null)
            throw new NullPointerException("Null lifecycleAnnotations array");
        if (lifecycleAnnotations.length < 2)
            throw new IllegalArgumentException("Array lacks the minimum amount of annotation classes");
        Class<? extends Annotation>[] arr = lifecycleAnnotations.clone();
        if (!Checker.chkArray(arr, this.lldi.getClassLoader()))
            throw new IllegalArgumentException(
                    "At least one class was not loaded by the InjectedClassLoader from the LowLevelDependencyInjector nor by its ancestors;");
        this.lifecycleAnnotations = arr;
    }

    public synchronized void setDefaultAnnotation(Class<? extends Annotation> annotation) {
        if (annotation == null)
            throw new NullPointerException();
        if (annotation == defaultLifecycleAnnotation)
            return;
        if (!Checker.strictContainsObject(this.lifecycleAnnotations, annotation))
            throw new IllegalArgumentException("Annotation is not in the annotations used to manage lifecycle");
        this.defaultLifecycleAnnotation = annotation;
    }

    public final synchronized Class<? extends Annotation> getDefaultLifecycleAnnotation() {
        return defaultLifecycleAnnotation;
    }

    public Class<? extends Annotation>[] getLifeCycleAnnotations() {
        return this.lifecycleAnnotations.clone();
    }

    public void register(Class<?> clazz) {
        if (isSingleton(clazz)) { // Avoid dual registration of Singleton, we don't register
            registrations.put(clazz, register0(clazz));
        } else
            throw new UnsupportedOperationException("You cannot register prototype types");
    }

    /*
     * Subclasses should override this if they use a different Lifecycle annotations
     */
    protected boolean isSingleton(Class<?> clazz) {
        return ((clazz.isAnnotationPresent(lifecycleAnnotations[0])
                && !clazz.isAnnotationPresent(lifecycleAnnotations[1]))
                || defaultLifecycleAnnotation == lifecycleAnnotations[0]);
    }

    protected DependencyObject register0(Class<?> clazz) {
        if (isSingleton(clazz) && registrations.containsKey(clazz))
            return registrations.get(clazz);
        DependencyObject dobj = lldi.resolve(clazz);
        if (dobj == null)
            throw new IllegalArgumentException("The class is not annotatated with a DI annotation");
        return dobj;
    }

    public Object instantiate(Class<?> cls, Class<?> optCls, Object... args) throws InjectionException {
        if (args == null)
            throw new NullPointerException();
        if (cls == null)
            throw new NullPointerException();
        if (!Checker.chkClassLoader(cls, lldi.getClassLoader()))
            throw new IllegalArgumentException("Class is not apart of the Provided InjectedClassLoader");
        args = args.clone();
        DependencyObject dobj = getPrototypeOrGetAndRegSingleton(cls);
        if (dobj.getInstance() != null && isSingleton(cls))
            return dobj.getInstance(); // Prevent instantiating a singleton two times
        Class<? extends Annotation> injectAnnot = lldi.getAnnotations()[3];
        Constructor<?>[] arr = cls.getConstructors();
        if (arr.length == 0)
            throw new InjectionException("The provided Class does not have a public constructor");
        for (int i = 0; i < arr.length; i++) {
            Constructor<?> cns = arr[i];
            Class<?>[] argTypes = cns.getParameterTypes();
            if (!cns.isAnnotationPresent(injectAnnot) || args.length != argTypes.length)
                continue;
            switch (dobj.TYPE) {
                case CLIENT:
                    return lldi.injectClient(dobj, (DependencyObject) args[0], InjectionType.CONSTRUCTOR,
                            Arrays.copyOfRange(argTypes, 1, argTypes.length),
                            Arrays.copyOfRange(args, 1, args.length), null);
                case SERVICE:
                    if (lldi.createInstance(dobj, argTypes, args))
                        return dobj.getInstance();
                    throw new BindException("Binding Failed");
                case INTERFACE:
                    if (optCls == null)
                        throw new InjectionException("Unable to Create interface Instance",
                                new NullPointerException("Optional Class Argumnet is required to be non-null"));
                    try {
                        DependencyObject obj = getPrototypeOrGetAndRegSingleton(optCls);
                        if (!lldi.createInstance(obj, dobj))
                            throw new BindException("Something went wrong while instantiating and binding Interface");
                        return dobj.getInstance();
                    } catch (RuntimeException e) {
                        throw new InjectionException("Something went wrong", e);
                    }
                case INJECTOR:
                    throw new InjectionException("Cannot Instatitate an Injector here");
            }
        }
        throw new InjectionException("Injection Failed");
    }

    protected DependencyObject getPrototypeOrGetAndRegSingleton(Class<?> cls) {
        if (cls == null)
            throw new NullPointerException();
        return isSingleton(cls) ? registrations.getOrDefault(cls, (DependencyObject) new Supplier<>() {

            @Override
            public DependencyObject get() {
                DependencyObject obj = register0(cls);
                registrations.put(cls, obj);
                return obj;
            }

        }.get()) : register0(cls);
    }

    public Object exec(Class<?> clientClass, Method m, Class<?> interfaceClass, Object... args)
            throws InjectionException {
        if (args == null)
            throw new NullPointerException();
        if (!Checker.chkClassLoader(clientClass, this.lldi.getClassLoader()))
            throw new IllegalArgumentException("clientClass is not an Valid Class");
        if (!Checker.chkClassLoader(interfaceClass, this.lldi.getClassLoader()))
            throw new IllegalArgumentException("interfaceClass is not an Valid Class");
        if (!clientClass.isAssignableFrom(m.getDeclaringClass()) || (m.getModifiers() & Modifier.PUBLIC) == 0)
            throw new IllegalArgumentException("Method is not present in the class and/or it is non-public");
        DependencyObject dobj = getPrototypeOrGetAndRegSingleton(clientClass);
        DependencyObject idobj = getPrototypeOrGetAndRegSingleton(interfaceClass);
        return lldi.injectClient(dobj, idobj, InjectionType.METHOD, m.getParameterTypes(), args, m.getName());
    }

    public Object exec(String clientClassName, String method, String interfaceClassName, Object... args)
            throws InjectionException, ClassNotFoundException, NoSuchMethodException {
        if (args == null)
            throw new NullPointerException();
        if (!Checker.chkArray(new Object[] { clientClassName, method, interfaceClassName }))
            throw new NullPointerException("At least one argument is null");
        InjectedClassLoader icl = lldi.getClassLoader();
        Class<?> cc = icl.loadClass(clientClassName);
        return this.exec(cc, cc.getMethod(method), icl.loadClass(interfaceClassName), args);
    }

    public Object instantiate(String className, Optional<String> optionalClassName, Object... args)
            throws ClassNotFoundException, InjectionException {
        return this.instantiate(className, optionalClassName.orElse(null), args);
    }

    public Object instantiate(String className, String optionalClassName, Object... args)
            throws ClassNotFoundException, InjectionException {
        if (!Checker.chkArray(new Object[] { className, args }))
            throw new NullPointerException("At least one required argument is null");
        InjectedClassLoader icl = lldi.getClassLoader();
        Class<?> cls = icl.loadClass(className);
        return this.instantiate(cls, icl.optionalClass(optionalClassName).orElse(null), args);
    }
}
