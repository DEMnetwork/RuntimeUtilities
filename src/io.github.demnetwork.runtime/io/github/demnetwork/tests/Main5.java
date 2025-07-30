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

package io.github.demnetwork.tests;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.github.demnetwork.runtime.internal.BuildData;
import io.github.demnetwork.runtime.utils.Table;
import io.github.demnetwork.runtime.utils.memory.*;
import io.github.demnetwork.runtime.utils.memory.MemoryStructBuilder.SyntheticMemoryStructConstructor;
import io.github.demnetwork.runtime.utils.memory.reflect.MemoryLayout;
import io.github.demnetwork.runtime.utils.memory.reflect.MemoryObjectField;
import io.github.demnetwork.runtime.utils.memory.reflect.MemoryObjectFieldInspector;
import io.github.demnetwork.runtime.utils.memory.serial.MemorySerializableRegistry;
import io.github.demnetwork.runtime.utils.memory.serial.NullWrapper;
import io.github.demnetwork.runtime.utils.memory.serial.StringWrapper;
import sun.misc.Unsafe;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.LongWrapper;

public class Main5 {
    static {
        BuildData.CURRENT.setDebug(true);
    }

    public static void main(String[] args) throws Exception {
        MemorySerializableRegistry.register(3L, User.class);
        OffHeapMemoryStorage storage = new OffHeapMemoryStorage(256);
        OffHeapMemoryStorage storage2 = new OffHeapMemoryStorage(1024);
        User usr0 = new User("Arthur", 0L, storage);
        Object id = usr0.get("id");
        Object name = usr0.get("name");
        System.out.println(name.toString() + " | ID: " + ((LongWrapper) id).longValue());
        MemoryLayout layout = new MemoryLayout(usr0);
        layout.print();
        OffHeapMemoryOutputStream os = new OffHeapMemoryOutputStream(storage2, false);
        os.writeObj(usr0);
        os.close();
        usr0.close();
        SyntheticMemoryStruct usr1 = new MemoryStructBuilder("User")
                .addField("id", Modifier.PUBLIC | Modifier.FINAL, NullWrapper.NULL)
                .addField("name", Modifier.PUBLIC | Modifier.FINAL, NullWrapper.NULL)
                .setConstructorLogic(new SyntheticMemoryStructConstructor() {

                    @Override
                    public void construct(SyntheticMemoryStruct struct, Object... args) throws IOException {
                        if (args == null)
                            throw new NullPointerException();
                        if (args.length != 2)
                            throw new IllegalArgumentException("Illegal Argument");
                        try {
                            // Using the following approach ensures struct values are set properly, because
                            // struct.setField throws upon modifying final fields
                            MemoryObjectFieldInspector mofi = new MemoryObjectFieldInspector(struct);
                            MemoryObjectField idf = mofi.getField("id");
                            MemoryObjectField namef = mofi.getField("name");
                            idf.forceSet(new LongWrapper((Long) args[0]));
                            namef.forceSet(new StringWrapper((String) args[1]));
                        } catch (ReflectiveOperationException e) {
                            throw new IOException("Exception occoured", e);
                        }
                    }

                }).build(storage, 0L, "");
        System.out.println(usr1.toString());
        MemoryLayout layout2 = new MemoryLayout(usr1);
        layout2.print();
        id = usr1.get("id");
        name = usr1.get("name");
        System.out.println("\"" + name.toString() + "\" | ID: " + ((LongWrapper) id).longValue());
        usr1.close();
        SyntheticMemoryStruct usr2 = usr1.createNew(storage, 1L, "Bob");
        id = usr2.get("id");
        name = usr2.get("name");
        System.out.println("\"" + name.toString() + "\" | ID: " + ((LongWrapper) id).longValue());
        OffHeapMemoryInputStream is = storage2.toInputStream(false);
        User usr3 = (User) is.readObj();
        id = usr3.get("id");
        name = usr3.get("name");
        System.out.println("\"" + name.toString() + "\" | ID: " + ((LongWrapper) id).longValue());

    }

    public static final class User extends MemoryStruct {
        @MemoryStructField
        public final LongWrapper id;
        @MemoryStructField
        public final StringWrapper name;

        public User(String name, long id) throws IOException {
            this(name, id, new OffHeapMemoryStorage(256));
        }

        public User(String name, long id, OffHeapMemoryStorage storage) throws IOException {
            super(storage);
            if (name == null)
                throw new NullPointerException();
            this.id = new LongWrapper(id);
            this.name = new StringWrapper(name);
            super.flushFields();
        }
    }

    public static final class User2 extends MemoryObjectImpl {
        protected static final Unsafe UNSAFE;
        static {
            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                UNSAFE = (Unsafe) f.get(null);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        public final LongWrapper id;
        public final StringWrapper name;

        public User2(String name, long id, OffHeapMemoryStorage storage) throws IOException {
            super(storage);
            if (name == null)
                throw new NullPointerException();
            this.name = new StringWrapper(name);
            this.id = new LongWrapper(id);
            super.addFields(new FieldData[] { new FieldData("name", Modifier.PUBLIC, this.name),
                    new FieldData("id", Modifier.PUBLIC, this.id) });
        }

        @Override
        protected void interpretData(int fc, Table fields) throws IOException, InstantiationException {
            super.interpretData(fc, fields);
            // Hydrate data
            try {
                UNSAFE.putObject(this, UNSAFE.objectFieldOffset(User2.class.getDeclaredField("id")),
                        super.getField("id"));
                UNSAFE.putObject(this, UNSAFE.objectFieldOffset(User2.class.getDeclaredField("name")),
                        super.getField("name"));
            } catch (ReflectiveOperationException e) {
                throw new IOException("Something went wrong", e);
            }
        }
    }
}
