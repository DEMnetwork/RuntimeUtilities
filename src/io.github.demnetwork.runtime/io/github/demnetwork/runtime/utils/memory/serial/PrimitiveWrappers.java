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

package io.github.demnetwork.runtime.utils.memory.serial;

import static io.github.demnetwork.runtime.utils.memory.serial.MemorySerializableRegistry.UNSAFE;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;

public final class PrimitiveWrappers {
    public static final class DoubleWrapper extends Number implements MemorySerializable, PrimitiveWrapper<Double> {
        private final double d;

        public DoubleWrapper(double d) {
            this.d = d;
        }

        @Override
        public void writeObj(MemoryOutputStream mos) throws IOException {
            mos.write(MemoryUtils.toBytes(Double.doubleToRawLongBits(this.d)), 0, 8);
        }

        /** This method writes values to this instance {@inheitDoc} */
        @Override
        public DoubleWrapper readObj(MemoryInputStream mis) throws IOException {
            try {
                Field f = DoubleWrapper.class.getDeclaredField("d");
                byte[] val = new byte[8];
                int r = mis.read(val, 0, 8);
                if (r == -1)
                    throw new EOFException("Reached EOF");
                UNSAFE.putDouble(this, UNSAFE.objectFieldOffset(f), Double.longBitsToDouble(MemoryUtils.toLong(val)));
                return this;
            } catch (ReflectiveOperationException e) {
                throw new IOException("Unable to read Object");
            }
        }

        @Override
        public int intValue() {
            return (int) d;
        }

        @Override
        public long longValue() {
            return (long) d;
        }

        @Override
        public float floatValue() {
            return (float) d;
        }

        @Override
        public double doubleValue() {
            return this.d;
        }

        @Override
        public Double getValue() {
            return Double.valueOf(this.d);
        }

    }

    public static final class LongWrapper extends Number implements MemorySerializable, PrimitiveWrapper<Long> {
        private final long v;
        public static final long SIZE = 8;

        public LongWrapper(long v) {
            this.v = v;
        }

        @Override
        public void writeObj(MemoryOutputStream mos) throws IOException {
            mos.write(MemoryUtils.toBytes(this.v), 0, 8);
        }

        /** This method writes values to this instance {@inheitDoc} */
        @Override
        public LongWrapper readObj(MemoryInputStream mis) throws IOException {
            try {
                Field f = LongWrapper.class.getDeclaredField("v");
                byte[] val = new byte[8];
                int r = mis.read(val, 0, 8);
                if (r == -1)
                    throw new EOFException("Reached EOF");
                if (r < 8)
                    throw new EOFException("Currupted or Incomplete data");
                UNSAFE.putLong(this, UNSAFE.objectFieldOffset(f), MemoryUtils.toLong(val));
                return this;
            } catch (ReflectiveOperationException e) {
                throw new IOException("Unable to read Object");
            }
        }

        @Override
        public int intValue() {
            return (int) this.v;
        }

        @Override
        public long longValue() {
            return this.v;
        }

        @Override
        public float floatValue() {
            return this.v;
        }

        @Override
        public double doubleValue() {
            return this.v;
        }

        @Override
        public Long getValue() {
            return Long.valueOf(this.v);
        }

    }

    public abstract interface PrimitiveWrapper<T> extends Wrapper<T> {

    }

    public static final class CharacterWrapper implements MemorySerializable, PrimitiveWrapper<Character> {
        private final char c;

        public CharacterWrapper(char c) {
            this.c = c;
        }

        @Override
        public Character getValue() {
            return Character.valueOf(this.c);
        }

        @Override
        public void writeObj(MemoryOutputStream mos) throws IOException {
            mos.write(MemoryUtils.toBytes(this.c), 6, 2);
        }

        @Override
        public MemorySerializable readObj(MemoryInputStream mis) throws IOException {
            try {
                Field f = CharacterWrapper.class.getDeclaredField("c");
                byte[] val = new byte[8];
                int r = mis.read(val, 6, 2); // Use proper Offset
                if (r == -1)
                    throw new EOFException("Reached EOF");
                if (r < 2)
                    throw new EOFException("Currupted or Incomplete data");
                UNSAFE.putChar(this, UNSAFE.objectFieldOffset(f), (char) MemoryUtils.toLong(val));
                return this;
            } catch (ReflectiveOperationException e) {
                throw new IOException("Unable to read Object");
            }
        }

        public char charValue() {
            return this.c;
        }
    }

    public static final class BooleanWrapper implements MemorySerializable, PrimitiveWrapper<Boolean> {
        private final boolean b;
        public static final BooleanWrapper TRUE = new BooleanWrapper(true);
        public static final BooleanWrapper FALSE = new BooleanWrapper(false);

        private BooleanWrapper(boolean b) {
            this.b = b;
        }

        @Override
        public Boolean getValue() {
            return Boolean.valueOf(this.b);
        }

        @Override
        public void writeObj(MemoryOutputStream mos) throws IOException {
            mos.write(this.b ? 1 : 0); // Just to keep things compact
        }

        /** This method returns constants because there are only two possible states */
        @Override
        public MemorySerializable readObj(MemoryInputStream mis) throws IOException {
            int b = mis.read();
            if (b == -1)
                throw new EOFException("Reached EOF");
            if (b != 0 && b != 1)
                throw new IOException("Corrupt Data found");
            if (b == 0)
                return BooleanWrapper.TRUE;
            if (b == 1)
                return BooleanWrapper.FALSE;
            throw new AssertionError("Something went wrong with checks");
        }

        public boolean booleanValue() {
            return this.b;
        }

    }

    public static final class IntWrapper extends Number implements MemorySerializable, PrimitiveWrapper<Integer> {
        private final int i;
        public static final long SIZE = 4;

        public IntWrapper(int i) {
            this.i = i;
        }

        @Override
        public void writeObj(MemoryOutputStream mos) throws IOException {
            mos.write(MemoryUtils.toBytes(this.i), 4, 4);
        }

        /** This method writes values to this instance {@inheitDoc} */
        @Override
        public IntWrapper readObj(MemoryInputStream mis) throws IOException {
            try {
                Field f = IntWrapper.class.getDeclaredField("i");
                byte[] val = new byte[8];
                int r = mis.read(val, 4, 4); // Write to index 4-7 to ensure proper interpretation of MemoryUtils
                if (r == -1)
                    throw new EOFException("Reached EOF");
                if (r < 4)
                    throw new EOFException("Currupted or Incomplete data");
                UNSAFE.putInt(this, UNSAFE.objectFieldOffset(f), (int) MemoryUtils.toLong(val));
                return this;
            } catch (ReflectiveOperationException e) {
                throw new IOException("Unable to read Object");
            }
        }

        @Override
        public int intValue() {
            return this.i;
        }

        @Override
        public long longValue() {
            return this.i;
        }

        @Override
        public float floatValue() {
            return this.i;
        }

        @Override
        public double doubleValue() {
            return this.i;
        }

        @Override
        public Integer getValue() {
            return Integer.valueOf(this.i);
        }

    }

    private PrimitiveWrappers() {
    }

    public static BooleanWrapper of(boolean b) {
        return b ? BooleanWrapper.TRUE : BooleanWrapper.FALSE;
    }

    // TODO: Add more wrappers, and getters to get a Wrapper Object
}
