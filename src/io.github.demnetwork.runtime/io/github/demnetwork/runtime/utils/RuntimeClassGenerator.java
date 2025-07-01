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

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.tools.ToolProvider;

import io.github.demnetwork.runtime.access.FieldAccessor;
import io.github.demnetwork.runtime.access.FieldWriteException;
import io.github.demnetwork.runtime.classloader.ExtendedClassLoader;
import io.github.demnetwork.runtime.misc.RuntimeResourceProvider;
import io.github.demnetwork.runtime.misc.RuntimeUtilsRuntime;
import io.github.demnetwork.runtime.utils.Table.IteratorStop;
import sun.misc.Unsafe;

public abstract class RuntimeClassGenerator extends Generator {
    protected URLClassLoader cl;
    protected final ArrayList<String> l;
    protected final Table methods;
    protected int nmid = 0;
    protected Class<?> generatedClass;
    protected static final ExtendedClassLoader ecl = new ExtendedClassLoader();
    protected static final Unsafe UNSAFE;
    protected static final ArrayList<Class<?>> generated = new ArrayList<Class<?>>();
    protected static final Map<Class<?>, RuntimeClassGenerator> data = new HashMap<>();
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected RuntimeClassGenerator(String target, String pkg, String className, int Modifiers) {
        this(target, pkg, className, Modifiers, new Class<?>[0]);
    }

    protected RuntimeClassGenerator(String target, String pkg, String className, int Modifiers,
            Class<?>... implementedInterfaces) {
        super(target);
        if (className == null)
            throw new NullPointerException("Null Class Name");
        if (pkg == null)
            throw new NullPointerException("Null Package");
        if (implementedInterfaces == null)
            throw new NullPointerException("Null Interface Array");
        String interfaces = "";
        if (implementedInterfaces.length != 0) {
            for (Class<?> c : implementedInterfaces) {
                if (!c.isInterface())
                    throw new IllegalArgumentException("Cannot implement an type that is not an interface");
                if (interfaces.length() != 0) {
                    interfaces += ", " + c.getName();
                } else {
                    interfaces += c.getName();
                }
            }
        }
        this.l = new ArrayList<>();
        l.add("package " + pkg + ";");
        if (interfaces.length() == 0) {
            l.add(Modifier.toString(Modifiers & Modifier.classModifiers()) + " class " + className + " {");
        } else {
            l.add(Modifier.toString(Modifiers & Modifier.classModifiers()) + " class " + className + " implements "
                    + interfaces + " {");
        }
        l.add("public static InteractionInterface interaction;");
        l.add("public static interface InteractionInterface { public abstract Object call(Class<?>[] argTypes, String m, Object... args); }");
        this.methods = new Table(4, String.class, Class[].class, MethodImpl.class, int.class);
    }

    @Override
    public abstract Class<?> build() throws Exception;

    public RuntimeClassGenerator addMethod(MethodImpl impl, int modifiers, Class<?> returnType, String methodName,
            Class<?>... paramTypes) {
        if (returnType == void.class)
            return addMethod(impl, modifiers, Void.class, methodName, paramTypes);
        int row = nmid++;
        methods.set(modifiers, 3, row);
        methods.set(impl, 2, row);
        methods.set(paramTypes.clone(), 1, row);
        methods.set(methodName, 0, row);
        l.add(Modifier.toString(modifiers & Modifier.methodModifiers()) + " " + returnType.getName() + " " + methodName
                + "(" + getArgs(paramTypes) + ")" + "{return (" + returnType.getName()
                + ") interaction.call(new Class<?>[] {" + argTypesGetter(paramTypes) + "} ,\"" + methodName + "\""
                + createCall(paramTypes.length, (modifiers & Modifier.STATIC) != 0) + ");}");
        return this;
    }

    public static final RuntimeClassGenerator newGenerator(String pkg, String className, int Modifiers) {
        File f1 = new File(System.getProperty("java.io.tmpdir") + "rc" + 0);
        for (int i = 0; f1.exists(); i++) {
            try {
                RuntimeUtilsRuntime.deleteFile(f1); // Attemp to delete unused file
            } catch (IOException e) {
            }
            f1 = new File(System.getProperty("java.io.tmpdir") + "rc" + i);
        }
        final File f = f1;
        return new RuntimeClassGenerator(f.getAbsolutePath(), pkg, className, Modifiers) {

            @Override
            public Class<?> build() throws Exception {
                super.l.add("}");
                String s = f.getAbsolutePath() + "/" + pkg.replace(".", "/");
                new File(s).mkdirs();
                File target = new File(s + "/" + className + ".java");
                target.createNewFile();
                FileWriter fw = new FileWriter(target);
                for (int i = 0; i < super.l.size(); i++) {
                    fw.write(super.l.get(i));
                }
                fw.close();
                boolean b = ToolProvider.getSystemJavaCompiler().run(System.in, System.out, System.err, "-cp", s,
                        target.getAbsolutePath()) == 0;
                if (b) {
                    Process p = new ProcessBuilder(
                            new String[] { "jar", "--create",
                                    "--file=src.jar",
                                    pkg.split("\\.")[0] })
                            .directory(f)
                            .inheritIO().start();
                    while (p.isAlive()) {

                    }
                    if (p.exitValue() != 0) {
                        throw new RuntimeException("The JAR file creation did not go with success");
                    }
                    super.cl = new URLClassLoader(
                            new URL[] {
                                    new File(f.getAbsolutePath() + File.separatorChar + "src.jar").toURI().toURL() });
                    final String cn = pkg + "." + className;
                    Class<?> ii = super.cl.loadClass(cn + "$InteractionInterface");
                    Class<?> clazz = super.cl.loadClass(cn);
                    Field f = clazz.getField("interaction");
                    f.setAccessible(true);
                    f.set(null, Proxy.newProxyInstance(super.cl, new Class<?>[] { ii }, new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            return this.handle(method.getName(), method.getParameterTypes(), args,
                                    method.getModifiers());
                        }

                        private Object handle(String methodName, Class<?>[] argTypes, Object[] args, int mod) {
                            switch (methodName) {
                                case "call":

                                    int[] rows = new int[methods.getRowCount()];
                                    // System.out.println(rows.length);

                                    methods.forEach(new Table.TableConsumer() {
                                        int ci = 0;

                                        @Override
                                        public void accept(Object o, int col, int row) throws IteratorStop {
                                            if (col != 0) {
                                                Table.TableConsumer.super.stop();
                                            }
                                            // System.out.println(o + " | " + col + ", " + row);
                                            if (o.equals(args[1])) {
                                                rows[ci++] = row;
                                            }
                                        }
                                    });
                                    // System.out.println(Arrays.toString(rows));
                                    NoSuchMethodError error = new NoSuchMethodError("Unable to find method");
                                    for (int i : rows) {
                                        // System.out.println(Arrays.toString((Class[]) methods.get(1, i)));
                                        // System.out.println(Arrays.toString((Class[]) args[0]));
                                        if (Arrays.equals((Class[]) methods.get(1, i), (Class[]) args[0])) {
                                            try {
                                                Object o = methods.get(2, i);
                                                Method m = o.getClass().getMethod("onInvoke",
                                                        Object[].class);
                                                m.setAccessible(true);
                                                return m.invoke(o,
                                                        java.util.Arrays.copyOfRange(args, 2, args.length));
                                            } catch (Exception e) {
                                                if (e instanceof InvocationTargetException) {
                                                    Throwable cause = e.getCause();
                                                    if (cause instanceof Exception) {
                                                        throw new RuntimeException(
                                                                "The method implementation threw an exception", e);
                                                    } else if (cause instanceof Error) {
                                                        throw new Error("The method implementation threw an error", e);
                                                    } else {
                                                        throw new RuntimeException(
                                                                "The method implementation threw an throwable", e);
                                                    }
                                                }
                                                error.addSuppressed(e);
                                            }
                                        }
                                    }
                                    if (error.getSuppressed().length >= 1) {
                                        throw error;
                                    }

                                default:
                                    throw new AbstractMethodError("Unimplemented Method");
                            }
                        }

                    }));
                    generated.add(clazz);
                    ecl.addClassLoader(super.cl);
                    data.put(clazz, this);
                    this.generatedClass = clazz;
                    return clazz;
                } else {
                    throw new RuntimeException("Something went wrong");
                }
            }

        };
    }

    public static RuntimeClassGenerator getProviderGeneratorImplementation(String pkg, String className,
            int Modifiers) {
        RuntimeResourceProvider p = RuntimeUtilsRuntime.getDefaultProvider();
        try {
            return (RuntimeClassGenerator) p.getInstance(p.getImpl(104745), new Object[] { pkg, className, Modifiers });
        } catch (InstantiationException e) {
            throw new RuntimeException("Provider does not implement an RuntimeClassGenerator", e);
        }
    }

    @FunctionalInterface
    public static interface MethodImpl {
        /**
         * @apiNote The Last Index of the args array is the instance if it is non-static
         *          method
         */
        public Object onInvoke(Object[] args) throws Throwable;

        public default Object getFieldValue(Object instance, String fieldName, Class<?> type)
                throws NoSuchFieldException {
            if (instance != null) {
                try {
                    Field f = instance.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(instance);
                } catch (InaccessibleObjectException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (type == null) {
                throw new NullPointerException();
            } else {
                try {
                    Field f = type.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(null);
                } catch (InaccessibleObjectException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public default void setFieldValue(Object instance, String fieldName, Object fieldValue, Class<?> type)
                throws NoSuchFieldException {
            if (instance != null) {
                try {
                    Field f = instance.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(instance, fieldValue);
                } catch (Exception e) {
                    if (e instanceof NoSuchFieldException) {
                        throw new NoSuchFieldException(e.getMessage());
                    }
                    throw new RuntimeException(e);
                }
            } else if (type == null) {
                throw new NullPointerException("Null parameters");
            } else {
                try {
                    Field f = type.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(null, fieldValue);
                } catch (Exception e) {
                    if (e instanceof NoSuchFieldException) {
                        throw new NoSuchFieldException(e.getMessage());
                    }
                    throw new RuntimeException(e);
                }
            }
        }

        public default Object call(Object instance, Class<?> type, final int methodID, Object... args)
                throws InvocationTargetException {
            if (args == null)
                throw new NullPointerException("Null Method Arguments");
            if (instance == null && type == null)
                throw new NullPointerException("Null Arguments");
            if (type == null)
                type = instance.getClass();
            try {
                RuntimeClassGenerator rcg = data.get(type);
                if (rcg != null) {
                    if ((((Integer) rcg.methods.get(3, methodID)).intValue() & Modifier.STATIC) == 0
                            && instance == null) {
                        throw new NullPointerException("Cannot invoke non-static method on null instance");
                    }
                    try {

                        if (instance == null) {
                            MethodImpl impl = ((MethodImpl) rcg.methods.get(2, methodID));
                            chkTypeCompat((Class[]) rcg.methods.get(1, methodID), args);
                            return impl.onInvoke(args);
                        }
                        MethodImpl impl = ((MethodImpl) rcg.methods.get(2, methodID));
                        chkTypeCompat((Class[]) rcg.methods.get(1, methodID), args);
                        return impl.onInvoke(arrAdd(args, instance));

                    } catch (Throwable t) {
                        throw new InvocationTargetException(t, "The method did not execute properly");
                    }
                }
                throw new AssertionError("The class and its RuntimeClassGenerator are not registered");
                // This wont happen except if the Generator did not register its generated class
                // and it self
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static Object[] arrAdd(Object[] arr, Object obj) {
            Object[] narr = new Object[arr.length + 1];
            narr[arr.length] = obj;
            for (int i = 0; i < arr.length; i++) {
                narr[i] = arr[i];
            }
            return narr;
        }

        private static void chkTypeCompat(Class<?>[] a1, Object[] a2) {
            if (a1 == null)
                throw new AssertionError();
            if (a2 == null)
                throw new NullPointerException("Null Argument Array");
            if (a1.length != a2.length)
                throw new IllegalArgumentException("Argument length mismatch");
            try {
                for (int i = 0; i < a1.length; i++)
                    Table.chkType(a1[i], a2[i]);
                return;
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Argument type mismatch", e);
            }
        }
    }

    @FunctionalInterface
    public static interface ConstructorImpl {
        public abstract void newInstance(Object[] args, Object thisObject) throws Throwable;

        public default Object getFieldValue(Object instance, String fieldName, Class<?> type)
                throws NoSuchFieldException {
            if (instance != null) {
                try {
                    Field f = instance.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(instance);
                } catch (InaccessibleObjectException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (type == null) {
                throw new NullPointerException();
            } else {
                try {
                    Field f = type.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(null);
                } catch (InaccessibleObjectException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @SuppressWarnings("deprecation")
        public default void setFieldValue(Object instance, String fieldName, Object fieldValue, Class<?> type)
                throws NoSuchFieldException {
            if (instance == null && type == null)
                throw new NullPointerException();
            if (instance != null && type == null) {
                type = instance.getClass();
            } else if (!type.isInstance(instance))
                throw new ClassCastException("Type mismatch");
            try {
                new FieldAccessor(type.getClassLoader()).modifyField(type, fieldName, instance, fieldValue);
            } catch (FieldWriteException e) {
                throw new RuntimeException("Unable to modify field", e);
            }
        }
    }

    public RuntimeClassGenerator addConstructor(int mod, ConstructorImpl impl, Class<?>... argTypes) {
        // System.out.println(Arrays.toString(argTypes));
        return this.addMethod(new MethodImpl() {

            @Override
            public Object onInvoke(Object[] args) throws Throwable {
                Object instance = UNSAFE.allocateInstance(RuntimeClassGenerator.this.generatedClass);
                impl.newInstance(args, instance);
                return instance;
            }

        }, (mod & Modifier.constructorModifiers()) | Modifier.STATIC, Object.class,
                "newInstance", argTypes);
    }

    public RuntimeClassGenerator addField(Class<?> type, int mod, String fieldName) {
        if (type.isPrimitive()) {
            return this.addField(Table.convertClass(type), mod, fieldName);
        } else
            this.l.add(Modifier.toString(mod) + " " + type.getName() + " " + fieldName + "= null;");
        return this;
    }

    protected static final String getArgs(Class<?>[] argsTypes) {
        String s = "";
        for (int i = 0; i < argsTypes.length; i++) {
            if (i == 0) {
                s = s + argsTypes[i].getName() + " arg" + i;
            } else {
                s = s + ", " + argsTypes[i].getName() + " arg" + i;
            }
        }
        return s;
    }

    protected static final String createCall(int count, boolean isStatic) {
        String s = "";
        if (count != 0) {
            for (int i = 0; i < count; i++)
                s = s + ", arg" + i;
        }
        if (!isStatic) {
            s += ", this";
        }
        return s;
    }

    public static Class<?> loadGeneratedClass(String className) throws ClassNotFoundException {
        return ecl.loadClass(className);
    }

    protected static final String argTypesGetter(Class<?>[] args) {
        String s = "";
        for (Class<?> c : args) {
            String s2 = c.getName() + ".class";
            if (s.length() == 0) {
                s += s2;
            } else
                s += ", " + s2;
        }
        return s;
    }

}
