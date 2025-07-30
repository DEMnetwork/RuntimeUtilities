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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import io.github.demnetwork.runtime.misc.serial.FastDeserializer;
import io.github.demnetwork.runtime.misc.serial.FastDeserializer.DeSerializationProblem;
import io.github.demnetwork.runtime.utils.memory.OffHeapMemoryInputStream;
import io.github.demnetwork.runtime.utils.memory.OffHeapMemoryOutputStream;
import io.github.demnetwork.runtime.utils.memory.OffHeapMemoryStorage;
import io.github.demnetwork.runtime.utils.memory.OffHeapMemoryStoragePool;
import io.github.demnetwork.tests.Main.FSTest;
import io.github.demnetwork.tests.Main.FSTest.Deserializer;

import java.io.File;

public class Main2 {
    public static void main(String[] args) throws IOException {
        @SuppressWarnings("resource") // Shuts up the compiler(because ps.close() will close it)
        OffHeapMemoryStorage storage = new OffHeapMemoryStorage(256);
        OffHeapMemoryOutputStream os = storage.toOutputStream();
        PrintStream ps = new PrintStream(os);
        ps.println("Hello World");
        ps.flush();
        OffHeapMemoryInputStream is = storage.toInputStream();
        File f = new File("./myFile.txt");
        FileOutputStream fos = new FileOutputStream(f);
        byte[] arr = new byte[(int) os.getOffset()];
        System.out.println("Read: " + is.read(arr) + " bytes");
        fos.write(arr);
        fos.close();
        ps.close(); // Implicitly closes the storage
        @SuppressWarnings("resource") // Shuts up the compiler(because ps2.close() will close it)
        OffHeapMemoryStorage storage2 = new OffHeapMemoryStorage(0x2000);
        OffHeapMemoryOutputStream os2 = storage2.toOutputStream();
        PrintStream ps2 = new PrintStream(os2);
        ps2.println("Hello World.");
        ps2.println("We have 8KiB to write stuff and text.");
        ps2.println("Since we did not trim the bytes he bytes after this text will be 0x00(NULL)");
        ps2.println("Did you know PI is an Irrational number?");
        ps2.flush();
        storage2.toFile(new File("./anotherFile.txt"), 256, 0);
        ps2.close();
        System.out.println(storage2.isClosed()); // True
        @SuppressWarnings("resource")
        OffHeapMemoryStorage serializedObject = new OffHeapMemoryStorage(256);
        try {
            FastDeserializer<FSTest> d = FastDeserializer.newFastDerializer(FSTest.class);
            if (!(d instanceof Deserializer))
                System.out.println("Deserializer Test Worked");
            FSTest tO = new FSTest();
            tO.fastWriteObject(new ObjectOutputStream(serializedObject.toOutputStream()));
            ObjectInputStream ois = new ObjectInputStream(serializedObject.toInputStream());
            d.deserialize(ois);
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (DeSerializationProblem e) {
            e.printStackTrace();
        }
        OffHeapMemoryStoragePool pool = new OffHeapMemoryStoragePool(1024, 1);
        OffHeapMemoryStorage myStorage = pool.getInstance();
        OffHeapMemoryOutputStream os3 = myStorage.toOutputStream(false);
        PrintStream ps3 = new PrintStream(os3);
        ps3.println("Hello World.");
        ps3.println("We have 1KiB to write stuff and text.");
        ps3.println("Since we did not trim the bytes he bytes after this text will be 0x00(NULL)");
        ps3.println("Did you know PI is an Irrational number?");
        ps3.println("I like Java and hate Python.");
        ps3.flush();
        ps3.close();
        File f2 = new File("./myText.txt");
        f2.createNewFile();
        myStorage.toFile(f2);
        try {
            pool.getInstance(); // Should throw an exception
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        myStorage.close();
        OffHeapMemoryStorage myStorage2 = pool.getInstance(); // Should work
        if (myStorage == myStorage2)
            System.out.println("Worked. Instance are the same"); // Should say the following string
        try {
            pool.getInstance(); // Should throw an exception
        } catch (Exception e) {
            PrintStream psr = new PrintStream(myStorage2.toOutputStream(false));
            e.printStackTrace(psr);
            psr.flush();
            psr.close();
            File f3 = new File("./stackTrace.txt");
            f3.createNewFile();
            myStorage2.toFile(f3);
            myStorage2.close();
        }
        pool.getInstance(); // Not recommended, but works when you have only 1 instance
        if (myStorage2.getClass() != OffHeapMemoryStorage.class)
            System.out.println("Is a subtype of OffHeapMemoryStorage");
    }
}
