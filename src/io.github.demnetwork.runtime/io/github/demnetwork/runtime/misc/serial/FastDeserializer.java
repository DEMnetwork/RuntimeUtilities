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

package io.github.demnetwork.runtime.misc.serial;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.*;
import sun.misc.Unsafe;

public abstract class FastDeserializer<T extends FastSerializable> {
    protected final Class<?> type;
    protected final long ClassID;
    protected final boolean validID;
    private final FastDeserializer<?> redirect;
    protected static final Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
            // test();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    };

    FastDeserializer(Class<T> type, boolean chkType) {
        // System.out.println("FastDeserializer Constructor Called");
        this.type = type;
        if (chkType) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                throw new IllegalArgumentException("You may not provide an Abstract Class");
            }
            if (type.isEnum()) {
                throw new IllegalArgumentException("An Enum Cannot be Deserialized");
            }
            FastSerializableIDentity id = type.getAnnotation(FastSerializableIDentity.class);
            if (id == null)
                throw new IllegalArgumentException("The Class is not annotated with FastSerializableIDentity");
            this.ClassID = id.value();
            this.validID = true;
            Class<?> clazz = id.deserializerClass();
            // System.out.println("Current FastDeserializer: " + this.getClass());
            if (clazz != this.getClass()
                    && clazz != FastDeserializer.class) {
                // System.out.println("Redirect " + clazz.getName() + " <<< " +
                // this.getClass().getName());
                try {
                    // System.out.println(type.getName());
                    this.redirect = ((FastSerializable) UNSAFE.allocateInstance(type)).getDeserializer();
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Something Went Wrong, when attemping to retrive the class de-serializer");
                }

            } else
                this.redirect = null;
        } else {
            this.validID = false;
            this.ClassID = 0L;
            this.redirect = null;
        }
    }

    protected FastDeserializer(Class<T> type) {
        this(type, true);
    }

    public final T deserialize(ObjectInputStream ois) throws DeSerializationProblem {
        if (validID)
            try {
                long id = ois.readLong();
                if (this.ClassID != id)
                    throw new ClassCastException("Types are not the same");
            } catch (Exception e) {
                throw new DeSerializationProblem("Problem During Pre-Deserialization Check", e);
            }
        if (redirect == null)
            return this.deserialize0(ois);
        return deserialize1(ois);
    }

    protected abstract T deserialize0(ObjectInputStream ois) throws DeSerializationProblem;

    @SuppressWarnings("unchecked")
    private T deserialize1(ObjectInputStream ois) throws DeSerializationProblem {
        return (T) redirect.deserialize0(ois);
    }

    public static final class DeSerializationProblem extends Throwable {
        private DeSerializationProblem(String msg, Throwable cause) {
            super(msg, cause);
        }

        private DeSerializationProblem(String msg) {
            super(msg);
        }
    }

    protected static final DeSerializationProblem newDeserializationProblem(String msg) {
        return new DeSerializationProblem(msg);
    }

    protected static final DeSerializationProblem newDeserializationProblem(String msg, Throwable cause) {
        return new DeSerializationProblem(msg, cause);
    }

    public static <T extends FastSerializable> FastDeserializer<T> newFastDerializer(Class<T> type) {
        return new FastDeserializer<T>(type, true) {

            @SuppressWarnings({ "deprecation", "unchecked" })
            @Override
            public T deserialize0(ObjectInputStream ois) throws DeSerializationProblem {
                try {
                    FastSerializable o = (FastSerializable) UNSAFE.allocateInstance(type);
                    Field[] fields = o.getClass().getDeclaredFields();
                    int tfi = 0;
                    Object[] tfields = o.getInitializer().initialize();
                    for (Field f : fields) {
                        Class<?> type = f.getType();
                        int mod = f.getModifiers();
                        long offset = UNSAFE.objectFieldOffset(f);
                        if ((mod & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
                            if (!type.isPrimitive()) {
                                UNSAFE.putObject(o, offset, ois.readObject());
                            } else {
                                if (type == double.class) {
                                    UNSAFE.putDouble(o, offset, ois.readDouble());
                                } else if (type == float.class) {
                                    UNSAFE.putFloat(o, offset, ois.readFloat());
                                } else if (type == long.class) {
                                    UNSAFE.putLong(o, offset, ois.readLong());
                                } else if (type == int.class) {
                                    UNSAFE.putInt(o, offset, ois.readInt());
                                } else if (type == char.class) {
                                    UNSAFE.putChar(o, offset, ois.readChar());
                                } else if (type == short.class) {
                                    UNSAFE.putShort(o, offset, ois.readShort());
                                } else if (type == byte.class) {
                                    UNSAFE.putByte(o, offset, ois.readByte());
                                } else if (type == boolean.class) {
                                    UNSAFE.putBoolean(o, offset, ois.readBoolean());
                                } else
                                    throw new AssertionError("Field type is \'void\'");
                            }
                        } else if ((mod & Modifier.STATIC) == 0 && (mod & Modifier.TRANSIENT) != 0) {
                            if (!type.isPrimitive()) {
                                UNSAFE.putObject(o, offset, tfields[tfi++]);
                            } else {
                                if (type == double.class) {
                                    UNSAFE.putDouble(o, offset, (Double) tfields[tfi++]);
                                } else if (type == float.class) {
                                    UNSAFE.putFloat(o, offset, (Float) tfields[tfi++]);
                                } else if (type == long.class) {
                                    UNSAFE.putLong(o, offset, (Long) tfields[tfi++]);
                                } else if (type == int.class) {
                                    UNSAFE.putInt(o, offset, (Integer) tfields[tfi++]);
                                } else if (type == char.class) {
                                    UNSAFE.putChar(o, offset, (Character) tfields[tfi++]);
                                } else if (type == short.class) {
                                    UNSAFE.putShort(o, offset, (Short) tfields[tfi++]);
                                } else if (type == byte.class) {
                                    UNSAFE.putByte(o, offset, (Byte) tfields[tfi++]);
                                } else if (type == boolean.class) {
                                    UNSAFE.putBoolean(o, offset, (Boolean) tfields[tfi++]);
                                } else
                                    throw new AssertionError("Field type is \'void\'");
                            }
                        }
                    }
                    o.validate();
                    return (T) o;
                } catch (Exception e) {
                    throw new DeSerializationProblem("Unable to desrialize Object", e);
                } catch (IllegalObjectState e0) {
                    throw new DeSerializationProblem("Object Validation Failed", e0);
                }
            }
        };
    }

    static void test() {
        try {
            SerializerTest t = new SerializerTest("Test String");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("./Test.java_object"));
            t.fastWriteObject(oos);
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("./Test.java_object"));
            SerializerTest t2 = t.getDeserializer().deserialize(ois);
            ois.close();
            if (t2.getBoolean() == t.getBoolean()) {
                System.out.println("Boolean copy worked");
            }
            if (t2.getMyString().equals(t.getMyString())) {
                System.out.println("MyString copy worked");
            }
            if (t2.getDouble() == t2.getDouble()) {
                System.out.println("Double copy worked");
            }
            if (t2.getStringS().equals(t.getStringS())) {
                System.out.println("String S copy worked");
            }
        } catch (Exception | DeSerializationProblem e) {
            throw new Error("Test failed", e);
        }
    }

    /**
     * 
     * @return The Underlying FastDeserializer Instance, if it does not use it null.
     */
    public final FastDeserializer<?> getWrappedFastDeserializer() {
        return this.redirect;
    }
}
