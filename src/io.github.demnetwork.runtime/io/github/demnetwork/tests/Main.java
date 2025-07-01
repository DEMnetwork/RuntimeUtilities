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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import io.github.demnetwork.runtime.access.EnforceModifiers;
import io.github.demnetwork.runtime.access.FieldAccessor;
import io.github.demnetwork.runtime.math.ComplexNumber;
import io.github.demnetwork.runtime.misc.serial.FastDeserializer;
import io.github.demnetwork.runtime.misc.serial.FastSerializable;
import io.github.demnetwork.runtime.misc.serial.FastSerializableIDentity;
import io.github.demnetwork.runtime.misc.serial.IllegalObjectState;
import io.github.demnetwork.runtime.misc.serial.FastDeserializer.DeSerializationProblem;
import io.github.demnetwork.runtime.utils.GenericTypeWrapper;
import io.github.demnetwork.runtime.utils.RuntimeClassGenerator;
import io.github.demnetwork.runtime.utils.Table;
import io.github.demnetwork.runtime.utils.memory.OffHeapMemoryStorage;
import io.github.demnetwork.runtime.utils.wrapper.ListWrapper;
import io.github.demnetwork.tests.Main.FSTest.Deserializer;

@SuppressWarnings("unused")
public class Main {
    // @EnforceModifiers
    private final int myInt = (int) Math.pow(10, 1);

    public static void main(String[] args) throws InterruptedException {
        Table t = new Table(2, int.class, String.class);
        int i = ((Integer) t.get(0, 0)).intValue();
        System.out.println("Result: " + i);
        System.out.println("Result: " + t.get(1, 0));
        t.set("Test String", 1, 0);
        System.out.println("Result: " + t.get(1, 0));
        System.out.println("Row Count: " + t.getRowCount());
        try {
            t.set("Test String", 0, 0);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        t.set("My String", 1, 1);
        System.out.println("Result: " + t.get(1, 1));
        System.out.println("Row Count: " + t.getRowCount());
        try {
            t.set((byte) 0, 0, 1);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        for (Object o : t) {
            System.out.println("Object: " + o.toString());
        }
        System.out.println();
        t.iterator().forEachRemaining(new Consumer<Object>() {

            @Override
            public void accept(Object o) {
                System.out.println("Object: " + o.toString());
            }

        });
        try {
            Table myTable = new Table(3, String.class, int.class, Class.class);
            Table myTable2 = new Table(2, 2, String.class, int.class);
            myTable.set("MyString", 0, 0);
            myTable.set(0, 1, 0);
            myTable.set(String.class, 2, 0);
            myTable.set("10", 0, 1);
            myTable.set(1, 1, 1);
            myTable.set(Long.class, 2, 1);
            Table.copy(myTable, myTable2, 0, 1, 0, 1, 0, 0);
            System.out.println("myTable2(0,0): " + myTable2.get(0, 0));
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        try {
            Table myTable = new Table(3, CharSequence.class, int.class, Class.class);
            Table myTable2 = new Table(2, 2, String.class, int.class);
            myTable.set("MyString", 0, 0);
            myTable.set(0, 1, 0);
            myTable.set(String.class, 2, 0);
            myTable.set("10", 0, 1);
            myTable.set(1, 1, 1);
            myTable.set(Long.class, 2, 1);
            myTable.castTo(new Class<?>[] { String.class, int.class, Class.class });
            Table.copy(myTable, myTable2, 0, 1, 0, 1, 0, 0);
            System.out.println("myTable2(0,0): " + myTable2.get(0, 0));
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        try {
            Class<?> clazz = RuntimeClassGenerator.newGenerator("io.github.demnetwork.test", "MyTest", Modifier.PUBLIC)
                    .addMethod(new RuntimeClassGenerator.MethodImpl() {

                        @Override
                        public Object onInvoke(Object[] args) throws Exception {
                            if (args[0] instanceof Table || args[0] == null) {
                                System.out.println("Argument 1 is a Table or null");
                                setFieldValue(args[1], "table", args[0], null);
                            }
                            return null;
                        }

                    }, Modifier.PUBLIC, void.class, "setTable", Object.class)
                    .addField(Object.class, Modifier.PRIVATE, "table")
                    .addMethod(new RuntimeClassGenerator.MethodImpl() {

                        @Override
                        public Object onInvoke(Object[] args) throws Exception {
                            return getFieldValue(args[0], "table", null);
                        }

                    }, Modifier.PUBLIC, Object.class, "getTable").addMethod(new RuntimeClassGenerator.MethodImpl() {

                        @Override
                        public Object onInvoke(Object[] args) throws Throwable {
                            call(args[1], null, 0, args[0]);
                            return call(args[1], null, 1);
                        }

                    }, Modifier.PUBLIC, Object.class, "setAndGetTable", Object.class).build();
            Constructor<?> c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            Object o = c.newInstance();
            Method m = clazz.getDeclaredMethod("setTable", Object.class);
            m.setAccessible(true);
            m.invoke(o, t);
            Method m2 = clazz.getDeclaredMethod("getTable");
            m2.setAccessible(true);
            if (m2.invoke(o) == t) {
                System.out.println("Test 1 worked");
            }
            m.invoke(o, new Object[] { null });
            if (m2.invoke(o) == null) {
                System.out.println("Test 2 worked");
            }
            Method m3 = clazz.getDeclaredMethod("setAndGetTable", Object.class);
            m3.setAccessible(true);
            if (m3.invoke(o, t) == t) {
                System.out.println("Test 3 worked");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Thread.sleep(20000);
        try {
            Table myTable = new Table(2, String.class, int.class);
            myTable.set("MyString", 0, 0);
            CharSequence[] arr = myTable.colToArray(0, new CharSequence[1]);
            System.out.println(arr[0].toString());
            Table myTable2 = new Table(2, CharSequence.class, int.class);
            myTable2.set("MyString", 0, 0);
            String[] arr2 = myTable2.colToArray(0, new String[1]);
            System.out.println(arr2[0].toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            FastDeserializer<FSTest> d = FastDeserializer.newFastDerializer(FSTest.class);
            if (!(d instanceof Deserializer))
                System.out.println("Deserializer Test Worked");
            FSTest tO = new FSTest();
            tO.fastWriteObject(new ObjectOutputStream(new FileOutputStream("FSTest.java_object")));
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("FSTest.java_object"));
            d.deserialize(ois);
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (DeSerializationProblem e) {
            e.printStackTrace();
        }
        System.out.println(Arrays.toString(t.getLocationsOf("Test String")[0]));
        OffHeapMemoryStorage ohmst = new OffHeapMemoryStorage(1024);
        System.out.println(ohmst.getByte(0));
        ohmst.setByte(0, (byte) 10);
        System.out.println(ohmst.getByte(0));
        ohmst.close();
        try {
            Main o = new Main();
            FieldAccessor fa = new FieldAccessor(Main.class.getClassLoader());
            System.out.println(o.myInt);
            fa.modifyField(Main.class, "myInt", o, 0);
            System.out.println(o.myInt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            GenericTypeWrapper wpr = new GenericTypeWrapper(
                    new ListWrapper<String>(Arrays.asList("My String", "Your String")));
            Class<?> cls1 = wpr.getActualWrapperType();
            GenericTypeWrapper wpr2 = new GenericTypeWrapper(
                    new ListWrapper<String>(Arrays.asList("Their String", "Its String")));
            GenericTypeWrapper wpr3 = new GenericTypeWrapper(
                    new ListWrapper<Integer>(Arrays.asList(2, 3, 5, 7, 11)));
            System.out.println(wpr3.getCode());
            if (cls1 == wpr2.getActualWrapperType())
                System.out.println("Wrapper Class Generation OK");
            Table myTable = new Table(2, int.class, cls1);
            myTable.set(1, 0, 0);
            myTable.set(wpr.getActualWrapperObject(), 1, 0);
            myTable.set(2, 0, 1);
            myTable.set(wpr2.getActualWrapperObject(), 1, 1);
            myTable.set(3, 0, 2);
            myTable.set(wpr3.getActualWrapperObject(), 1, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ComplexNumber n = ComplexNumber.getInstance(2, 3);
        ComplexNumber n2 = ComplexNumber.getInstance(4, 5);
        System.out.println(n.multiply(n2).toString());
        System.out.println(n.raiseToPower(3).toString());
        System.out.println(ComplexNumber.getInstance(8, 0).raiseToPower(3).toString());
        System.out.println(n.hashCode());
        System.out.println(n2.hashCode());
        System.out.println(ComplexNumber.getInstance(20, 50.2).hashCode());
        int collisions = 0;
        Random r0 = new Random();
        Random r1 = new Random();
        Map<Integer, ComplexNumber> hashMap = new HashMap<>();
        for (long l = 0; l < 1000000000; l++) {
            ComplexNumber cn = ComplexNumber.getInstance(r0.nextDouble() * 100000000, r1.nextDouble() * 100000000);
            int hash = cn.hashCode();

            if (!hashMap.containsKey(hash)) {
                // No collision, insert it.
                hashMap.put(hash, cn);
            } else {
                // Collision detected.
                ComplexNumber existingCn = hashMap.get(hash);
                System.out.println("Current Hash: " + hash + " | Collisions: " + ++collisions + " | " + cn.toString()
                        + " | " + existingCn.toString() + " | Iteration: " + l);
            }
        }
    }

    @FastSerializableIDentity(value = 20243645L, deserializerClass = io.github.demnetwork.tests.Main.FSTest.Deserializer.class)
    static class FSTest implements FastSerializable {
        private String helloWorld = "Hello World";

        @Override
        public FastDeserializer<?> getDeserializer() {
            return new Deserializer();
        }

        @Override
        public void validate() throws IllegalObjectState {
            if (!this.helloWorld.equals("Hello World"))
                throw new IllegalObjectState("Corrupted Data found", null);
        }

        static final class Deserializer extends FastDeserializer<FSTest> {

            private Deserializer() {

                super(FSTest.class);
                System.out.println("Constructor Called");
            }

            @Override
            protected FSTest deserialize0(ObjectInputStream ois) throws DeSerializationProblem {
                try {
                    System.out.println("De-Serializing");
                    FSTest o = (FSTest) UNSAFE.allocateInstance(super.type);
                    o.helloWorld = (String) ois.readObject();
                    o.validate();
                    return o;
                } catch (Exception e) {
                    throw super.newDeserializationProblem("Unable to deserialize Data", e);
                } catch (IllegalObjectState e) {
                    throw super.newDeserializationProblem("Validation Failed", e);
                }
            }

        }

    }
}
