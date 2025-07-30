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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;

import io.github.demnetwork.runtime.internal.BuildData;
import io.github.demnetwork.runtime.utils.memory.*;
import io.github.demnetwork.runtime.utils.memory.serial.ArrayWrapper;
import io.github.demnetwork.runtime.utils.memory.serial.MemorySerializable;
import io.github.demnetwork.runtime.utils.memory.serial.MemorySerializableRegistry;
import io.github.demnetwork.runtime.utils.memory.serial.NullWrapper;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.LongWrapper;

public class Main4 {
    static {
        BuildData.CURRENT.setDebug(true);
    }

    public static void main(String[] args) throws Exception {
        MemorySerializableRegistry.register(2L, MyMemoryObj.class);
        OffHeapMemoryStorage storage = new OffHeapMemoryStorage(1024);
        MyMemoryObj obj = new MyMemoryObj(storage);
        if (obj.get("test") == null)
            System.out.println("Test 1: OK");
        OffHeapMemoryStorage storage2 = new OffHeapMemoryStorage(2048);
        OffHeapMemoryOutputStream os = storage2.toOutputStream(false);
        os.writeObj(obj);
        os.close();
        storage2.toFile(new File("./file.txt"));
        OffHeapMemoryInputStream is = storage2.toInputStream(false);
        MemorySerializable obj2 = is.readObj();
        if (obj2 instanceof MyMemoryObj)
            System.out.println("Test 2: OK");
        if (obj2 instanceof LongWrapper) {
            // If you directly run obj.writeObj directly you mihgt get this, because ID was
            // not writtem
            LongWrapper lw = (LongWrapper) obj2;
            System.out.println("Value: " + lw.longValue());
        }
        if (obj != obj2)
            System.out.println("Test 3: OK");
        is.close();
        storage.toFile(new File("./anotherFile.txt"));
        System.out.println(obj.test().toString());
        MyMemoryObj memoryObj = (MyMemoryObj) obj2;
        ArrayWrapper<?> arr = (ArrayWrapper<?>) memoryObj.get("mArr");
        ArrayWrapper<?> arr2 = (ArrayWrapper<?>) obj.get("mArr");
        System.out.println(arr2 == arr);
        System.out.println(arr.getValue()[0] == NullWrapper.NULL);
        storage.close();
        storage2.close();
        obj.close();
        memoryObj.close();
        // Does not follows closing storage when not needed, nor streams, but it is
        // testing nothing that bad
        OffHeapMemoryStorage storage3 = new OffHeapMemoryStorage(1024);
        FileMappedOffHeapMemoryStorage fmohmst = new FileMappedOffHeapMemoryStorage(new File("./file2.txt"), 2048);
        fmohmst.toOutputStream(false).writeObj(new MyMemoryObj(storage3));
        MyMemoryObj obj3 = fmohmst.toInputStream(false).readAndInterpretAs(MyMemoryObj.class);
        System.out.println(obj3.get("test") == NullWrapper.NULL); // True
        fmohmst.close();
    }

    static class MyMemoryObj extends MemoryObjectImpl {
        public MyMemoryObj(OffHeapMemoryStorage storage) throws IOException {
            super(storage);
            super.addField("test", null, Modifier.PUBLIC);
            super.addField("mArr",
                    new ArrayWrapper<>(new MemorySerializable[] { NullWrapper.NULL }, MemorySerializable.class),
                    Modifier.PUBLIC);
        }

        @Override
        protected boolean isAccessible(int fieldID) {
            return true; // To Ensure Access
        }

        public FieldMetadata test() {
            return super.getFieldMetadataAsList().get(1);
        }
    }
}
