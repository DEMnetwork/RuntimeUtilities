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

package io.github.demnetwork.runtime.misc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import javax.tools.ToolProvider;

import io.github.demnetwork.runtime.utils.Table;
import io.github.demnetwork.runtime.utils.Table.IteratorStop;

public final class RuntimeUtilsRuntime {
    private static final List<RuntimeResourceProvider> providers = new ArrayList<>();
    private static final int RCG_DEFAULT_IMPL = RuntimeResourceProvider.RCG_IMPL_ID;
    private static int dp = 0;
    public static final int BUILTIN_PROVIDER_INDEX = 0;

    static {
        providers.add(new BuiltinProvider());
    }

    public synchronized static RuntimeResourceProvider getProvider(int index) {
        return providers.get(index);
    }

    private RuntimeUtilsRuntime() {
        throw new IllegalAccessError("You may not access this");
    }

    private static final class BuiltinProvider extends RuntimeResourceProvider {
        // private static final File PATH = new File("./");

        @Override
        public Class<? extends Implentation> getImpl(int id) {
            switch (id) {
                case RCG_DEFAULT_IMPL:
                    return RCGDefaultImpl.class;
            }
            throw new RuntimeException("Invalid id");
        }

        @Override
        public Implentation getInstance(Class<? extends Implentation> c, Object[] args) throws InstantiationException {
            if (args == null) {
                throw new NullPointerException("Null Array");
            }
            if (c == RCGDefaultImpl.class) {
                if (args.length != 3) {
                    throw new IllegalArgumentException("The array lacks arguments or has more than the expected");
                }
                try {
                    return new RCGDefaultImpl((String) args[0], (String) args[1], (Integer) args[2]);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Argument type mismatch", e);
                }
            } else {
                if (c == null) {
                    throw new InstantiationException("Cannot instantiate null class");
                } else if ((c.getModifiers() & Modifier.ABSTRACT) != 0) {
                    throw new InstantiationException("Cannot instantiate abstract class");
                } else {
                    throw new InstantiationException("This provider does not know this class");
                }
            }
        }

        // @Override
        // public Object[] resolveDependancy(String name) {
        // return new Object[] { name, lookup(PATH, name), this };
        // }

        // private File lookup(File path, String name) {
        // if (name.contains("/") || name.contains("\\"))
        // throw new IllegalArgumentException("Name Cannot have directory seperators");
        // File[] fls = path.listFiles();
        // for (File f : fls) {
        // if (f.isDirectory()) {
        // return this.lookup(f, name);
        // } else {
        // if (f.getName().equals(name)) {
        // return f;
        // }
        // }
        // }
        // throw new NoSuchElementException("Unable to find dependancy");
        // }

    }

    private static final class RCGDefaultImpl extends RuntimeResourceProvider.RCGImpl {
        private final String pkg;
        private final String className;
        private final File f;

        public RCGDefaultImpl(String pkg, String className, int modifiers) {
            super(generateTarget().getAbsolutePath(), pkg, className, modifiers);
            this.f = new File(super.targetLoaction);
            this.className = className;
            this.pkg = pkg;
        }

        private static File generateTarget() {
            File f1 = new File(System.getProperty("java.io.tmpdir") + "rc" + 0);
            for (int i = 0; f1.exists(); i++) {
                try {
                    deleteFile(f1); // Attemp to delete unused file
                } catch (IOException e) {
                }
                f1 = new File(System.getProperty("java.io.tmpdir") + "rc" + i);
            }
            return f1;
        }

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
                return clazz;
            } else {
                throw new RuntimeException("Something went wrong");
            }
        }

        @Override
        public String getName() {
            return "RCGDefaultImpl";
        }

    }

    public static void deleteFile(File f) throws IOException {
        if (!f.exists()) {
            throw new IOException("File does not exist");
        }
        if (!f.isDirectory()) {
            java.nio.file.Files.delete(f.toPath());
            return;
        }
        File[] fls = f.listFiles();
        for (int i = 0; i < fls.length; i++) {
            File f2 = fls[i];
            deleteFile(f2);
        }
        java.nio.file.Files.delete(f.toPath());
    }

    public static RuntimeResourceProvider getDefaultProvider() {
        return getProvider(dp);
    }

    public static synchronized void setDefaultProvider(RuntimeResourceProvider p) {
        if (providers.contains(p)) {
            dp = providers.indexOf(p);
        } else {
            addProvider(p);
            setDefaultProvider(p);
        }
    }

    public static synchronized void addProvider(RuntimeResourceProvider p) {
        if (p == null)
            throw new NullPointerException();
        providers.add(p);
    }
}
