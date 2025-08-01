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

// import java.io.IOException;
// import java.io.ObjectOutputStream;

@FastSerializableIDentity(20478612L)
final class SerializerTest implements FastSerializable {
    private String s = "Test";
    private final String myString;
    private boolean b = true;
    private final boolean b2 = true;
    private final double d = 1.24;

    public SerializerTest(String myString) {
        this.myString = myString;
    }

    public String getMyString() {
        return this.myString;
    }

    public String getStringS() {
        return this.s;
    }

    public boolean getBoolean() {
        return b && b2;
    }

    public double getDouble() {
        return this.d;
    }

    @Override
    public FastDeserializer<SerializerTest> getDeserializer() {
        return FastDeserializer.newFastDerializer(SerializerTest.class);
    }

    /*
     * BROKEN
     * 
     * @Override
     * public void fastWriteObject(ObjectOutputStream oos) throws IOException {
     * oos.writeObject(s);
     * oos.writeObject(myString);
     * oos.writeBoolean(b);
     * oos.writeBoolean(b2);
     * oos.writeDouble(d);
     * }
     */
}
