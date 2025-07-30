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

package io.github.demnetwork.runtime.utils.memory;

import sun.misc.Unsafe;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.lang.reflect.*;
import java.nio.ByteOrder;
import java.util.Optional;
import io.github.demnetwork.runtime.internal.BuildData;

public sealed class OffHeapMemoryStorage implements AutoCloseable
        permits ReutilizableOffHeapMemoryStorage, SlicedOffHeapMemoryStorage, FileMappedOffHeapMemoryStorage {
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
    public final long size;
    protected final long baseAddr;
    protected static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
    protected volatile boolean closed;
    private static final Cleaner CLEANER = Cleaner.create();
    protected final Cleanable cleanable;
    final OffHeapMemoryInstanceMonitor monitor = new OffHeapMemoryInstanceMonitor(this);
    /**
     * The maximum safe allocation size in bytes. Defaults to 2 GiB
     * (<code>2147483648L</code>),
     * but can be overridden via system property:
     * <p>
     * Use the following JVM Argument to modify the max offheap allocation:
     * 
     * <pre>
     * -Dio.github.demnetwork.runtime.utils.memory.maxOffHeapAllocation=<value>
     * </pre>
     * 
     * <p>
     * Replace <code>&lt;value&gt;</code> with the desired value
     */
    public static final long MAX_SAFE_ALLOCATION = Long
            .getLong("io.github.demnetwork.runtime.utils.memory.maxOffHeapAllocation", 2147483648L);

    /**
     * Creates a new OffHeapMemoryStorage allocating the amount bytes specified in
     * {@link #size} in the off-heap
     * 
     * @param size               The amount of Bytes to allocate
     * @param ignoreSafetyLimits This will ignore the limit stated by
     *                           {@link #MAX_SAFE_ALLOCATION}
     * @throws IllegalArgumentException If the amount of bytes to allocate is
     *                                  greater than {@link #MAX_SAFE_ALLOCATION}
     *                                  and the <code>ignoreSafetyLimits</code> is
     *                                  false;
     *                                  or if the size is less than 1
     * @apiNote If the requested number of bytes to allocate off-heap is too large,
     *          the behavior is undefined. This may result in a JVM crash (e.g.,
     *          segmentation fault)
     *          instead of throwing an exception. It is strongly recommended to stay
     *          under {@link #MAX_SAFE_ALLOCATION} unless absolutely necessary.
     *
     * @implNote This class uses {@link sun.misc.Unsafe} to allocate off-heap
     *           memory. Attempting to allocate extremely large amounts of memory
     *           (e.g., 1 Exabyte (2^60 bytes))
     *           may crash the JVM or even the operating system.
     */
    public OffHeapMemoryStorage(long size, boolean ignoreSafetyLimits) {
        this(size, ignoreSafetyLimits, true);
    }

    protected OffHeapMemoryStorage(long size, boolean ignoreSafetyLimits, boolean useCleaner) {
        if (size < 1)
            throw new IllegalArgumentException("Size too small");
        if (!ignoreSafetyLimits && size > MAX_SAFE_ALLOCATION)
            throw new IllegalArgumentException(
                    "Requested allocation size " + size + " exceeds MAX_SAFE_ALLOCATION (" + MAX_SAFE_ALLOCATION + ")");
        this.baseAddr = (UNSAFE.allocateMemory(size));
        this.closed = false;
        this.size = size;
        UNSAFE.setMemory(baseAddr, size, (byte) 0); // Set Memory to 0
        if (useCleaner) {
            this.cleanable = CLEANER.register(this, new Dealloc(baseAddr, size));
        } else {
            this.cleanable = null;
        }
    }

    OffHeapMemoryStorage(long baseAddr, long size) {
        this.cleanable = null;
        this.baseAddr = baseAddr;
        this.size = size;
    }

    /**
     * Creates a new OffHeapMemoryStorage allocating the amount bytes specified in
     * {@link #size} in the off-heap
     * 
     * @param size The amount of Bytes to allocate
     * @throws IllegalArgumentException If the amount of bytes to allocate is
     *                                  greater than {@link #MAX_SAFE_ALLOCATION};
     *                                  or if the size is less than 1.
     */
    public OffHeapMemoryStorage(long size) {
        this(size, false);
    }

    private final class Dealloc implements Runnable {
        private long address;
        private long size;
        private volatile boolean cleaned = false;

        private Dealloc(long a, long s) {
            this.address = a;
            this.size = s;
        }

        public void run() {
            if (cleaned == true)
                return;
            cleaned = true;
            OffHeapMemoryStorage.this.closed = true;
            UNSAFE.setMemory(address, size, (byte) 0); // Set Memory to 0
            UNSAFE.freeMemory(this.address);
            OffHeapMemoryStorage.this.monitor.onClose();
        }
    }

    /**
     * Deallocates the memory
     * 
     */
    public void close() {
        if (this.closed)
            return;
        this.closed = true;
        UNSAFE.setMemory(baseAddr, size, (byte) 0); // Clean memory on close, maybe the GC may not clean it.
        if (cleanable != null)
            cleanable.clean();
        this.monitor.onClose();
    }

    protected void ensureOpen() {
        if (closed)
            throw new IllegalStateException("The Memory Storage closed");
    }

    public void setByte(long offset, byte value) {
        ensureOpen();
        if (offset >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        UNSAFE.putByte(baseAddr + offset, value);
    }

    public byte getByte(long offset) {
        ensureOpen();
        if (offset >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        return UNSAFE.getByte(baseAddr + offset);
    }

    public byte[] getBytes() {
        return this.getBytes(0);
    }

    /**
     * Reads bytes starting from the given offset (inclusive) up to the end of the
     * allocated memory.
     *
     * @param offset the position to start reading from
     * @return a byte array containing the data from offset to the end of the memory
     * @throws IllegalArgumentException if the offset is invalid
     * @throws IllegalStateException    if the memory has already been closed
     */
    public byte[] getBytes(long offset) {
        if (closed)
            throw new IllegalStateException("Object already closed");
        if (offset >= size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        byte[] arr = new byte[(int) (size - offset)];
        long arrayBase = UNSAFE.arrayBaseOffset(byte[].class);
        UNSAFE.copyMemory(null, this.baseAddr + offset, arr, arrayBase, size - offset);
        return arr;
    }

    public void setShort(long offset, short value) {
        this.setShort(offset, value, NATIVE_ORDER);
    }

    public short getShort(long offset) {
        return this.getShort(offset, NATIVE_ORDER);
    }

    public void setInt(long offset, int value) {
        this.setInt(offset, value, NATIVE_ORDER);
    }

    public int getInt(long offset) {
        return this.getInt(offset, NATIVE_ORDER);
    }

    public void setChar(long offset, char value) {
        ensureOpen();
        if (offset + 1 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        UNSAFE.putChar(baseAddr + offset, value);
    }

    public char getChar(long offset) {
        ensureOpen();
        if (offset + 1 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        return UNSAFE.getChar(baseAddr + offset);
    }

    public void setLong(long offset, long value) {
        ensureOpen();
        if (offset + 7 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        UNSAFE.putLong(baseAddr + offset, value);
    }

    public long getLong(long offset) {
        ensureOpen();
        if (offset + 7 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        return UNSAFE.getLong(baseAddr + offset);
    }

    public void setFloat(long offset, float value) {
        ensureOpen();
        if (offset + 3 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        UNSAFE.putFloat(baseAddr + offset, value);
    }

    public float getFloat(long offset) {
        ensureOpen();
        if (offset + 3 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        return UNSAFE.getFloat(baseAddr + offset);
    }

    public void setDouble(long offset, double value) {
        ensureOpen();
        if (offset + 7 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        UNSAFE.putDouble(baseAddr + offset, value);
    }

    public double getDouble(long offset) {
        ensureOpen();
        if (offset + 7 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        return UNSAFE.getDouble(baseAddr + offset);
    }

    public void setShort(long offset, short value, ByteOrder order) {
        ensureOpen();
        if (offset + 1 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        if (order != NATIVE_ORDER)
            value = Short.reverseBytes(value);
        UNSAFE.putShort(baseAddr + offset, value);
    }

    public short getShort(long offset, ByteOrder order) {
        ensureOpen();
        if (offset + 1 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        short value = UNSAFE.getShort(baseAddr + offset);
        if (order != NATIVE_ORDER)
            value = Short.reverseBytes(value);
        return value;
    }

    public void setInt(long offset, int value, ByteOrder order) {
        ensureOpen();
        if (offset + 3 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        if (order != NATIVE_ORDER)
            value = Integer.reverseBytes(value);
        UNSAFE.putInt(baseAddr + offset, value);
    }

    public int getInt(long offset, ByteOrder order) {
        ensureOpen();
        if (offset + 3 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        int value = UNSAFE.getInt(baseAddr + offset);
        if (order != NATIVE_ORDER)
            value = Integer.reverseBytes(value);
        return value;
    }

    public void setLong(long offset, long value, ByteOrder order) {
        ensureOpen();
        if (offset + 7 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        if (order != NATIVE_ORDER)
            value = Long.reverseBytes(value);
        UNSAFE.putLong(baseAddr + offset, value);
    }

    public long getLong(long offset, ByteOrder order) {
        ensureOpen();
        if (offset + 7 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        long value = UNSAFE.getLong(baseAddr + offset);
        if (order != NATIVE_ORDER)
            value = Long.reverseBytes(value);
        return value;
    }

    public void setFloat(long offset, float value, ByteOrder order) {
        ensureOpen();
        if (offset + 3 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        if (order != NATIVE_ORDER)
            value = Float.intBitsToFloat(Integer.reverseBytes(Float.floatToIntBits(value)));
        UNSAFE.putFloat(baseAddr + offset, value);
    }

    public float getFloat(long offset, ByteOrder order) {
        ensureOpen();
        if (offset + 3 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        float value = UNSAFE.getFloat(baseAddr + offset);
        if (order != NATIVE_ORDER)
            value = Float.intBitsToFloat(Integer.reverseBytes(Float.floatToIntBits(value)));
        return value;
    }

    public void setDouble(long offset, double value, ByteOrder order) {
        ensureOpen();
        if (offset + 7 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        if (order != NATIVE_ORDER)
            value = Double.longBitsToDouble(Long.reverseBytes(Double.doubleToLongBits(value)));
        UNSAFE.putDouble(baseAddr + offset, value);
    }

    public double getDouble(long offset, ByteOrder order) {
        ensureOpen();
        if (offset + 7 >= this.size || offset < 0)
            throw new IllegalArgumentException("Illegal Offset");
        double value = UNSAFE.getDouble(baseAddr + offset);
        if (order != NATIVE_ORDER)
            value = Double.longBitsToDouble(Long.reverseBytes(Double.doubleToLongBits(value)));
        return value;
    }

    public boolean isClosed() {
        return closed;
    }

    @Deprecated
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!closed) {
                if (BuildData.CURRENT.getDebugStatus())
                    System.out.println("Finalizer got called");
                close();

            }
        } catch (Throwable t) {
            if (BuildData.CURRENT.getDebugStatus()) {
                System.out.println("close() threw an exception (Inside finalizer).");
                t.printStackTrace(System.out);
            }
        }
        super.finalize();
    }

    public OffHeapMemoryInputStream toInputStream() {
        return this.toInputStream(true);
    }

    public OffHeapMemoryInputStream toInputStream(boolean linked) {
        ensureOpen();
        return new OffHeapMemoryInputStream(this, linked);
    }

    public OffHeapMemoryOutputStream toOutputStream() {
        return this.toOutputStream(true);
    }

    public OffHeapMemoryOutputStream toOutputStream(boolean linked) {
        ensureOpen();
        return new OffHeapMemoryOutputStream(this, linked);
    }

    public void fill(byte value) {
        UNSAFE.setMemory(this.baseAddr, size, value);
    }

    public static Optional<OffHeapMemoryStorage> tryAllocate(long bytes) {
        try {
            return Optional.ofNullable(new OffHeapMemoryStorage(bytes));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public void toFile(File f) throws IOException {
        this.toFile(f, this.size, 0);
    }

    public void toFile(File f, long b, long off) throws IOException {
        ensureOpen();
        if (b == 0)
            return;
        if (b < 0)
            throw new IllegalArgumentException("Cannot write negative bytes to a file");
        if (off < 0)
            throw new IllegalArgumentException("Cannot retireve data from a negative offset");
        if (off >= this.size)
            throw new IllegalArgumentException("Out of bounds offset");
        if (f == null)
            throw new NullPointerException("Null File");
        if (!f.exists() || f.isDirectory())
            throw new IOException("The File does not exist or it is a directory");
        OffHeapMemoryInputStream is = this.toInputStream(false);
        FileOutputStream fos = new FileOutputStream(f);
        if (b >= 0x1000) {
            is.setOffset(off);
            byte[] buff = new byte[4096];
            int aval = is.available();
            long rb = b;
            while (aval != 0 && rb != 0) {
                int read = is.read(buff);
                long off2 = Math.min(rb, read);
                fos.write(buff, 0, (int) off2);
                aval = is.available();
                rb -= off2;
            }
        } else {
            is.setOffset(off);
            byte[] arr = new byte[(int) b];
            is.read(arr);
            fos.write(arr);
        }
        fos.close();
        is.close();
    }

    public void fromFile(File f, long srcBytes, long srcOffset, long destOffset) throws IOException {
        ensureOpen();
        OffHeapMemoryOutputStream os = this.toOutputStream(false);
        if (srcBytes == 0)
            return;
        if (srcBytes < 0)
            throw new IllegalArgumentException("Cannot read negative bytes");
        if (srcOffset < 0)
            throw new IllegalArgumentException("Cannot read at an negative offset");
        if (destOffset < 0)
            throw new IllegalArgumentException("Cannot write to an negative offset");
        if (f == null)
            throw new NullPointerException("Null File");
        if (!f.exists() || f.isDirectory())
            throw new IOException("The File does not exist or it is a directory");
        FileInputStream fis = new FileInputStream(f);
        if (srcBytes >= 0x1000L) {
            fis.getChannel().position(srcOffset);
            os.setOffset(destOffset);
            byte[] buff = new byte[4096];
            long rb = srcBytes;
            while (rb != 0) {
                int read = fis.read(buff);
                if (read == -1)
                    break;
                long off2 = Math.min(rb, read);
                os.write(buff, 0, (int) off2);
                rb -= off2;
            }
        } else {
            fis.getChannel().position(srcOffset);
            os.setOffset(destOffset);
            byte[] arr = new byte[(int) srcBytes];
            int read = fis.read(arr);
            if (read != -1)
                os.write(arr, 0, read);
        }
        fis.close();
        os.close();
    }

    public final SlicedOffHeapMemoryStorage slice(long offset, long size) {
        ensureOpen();
        if (this.size < offset + size)
            throw new IllegalArgumentException("Illegal Offset or size");
        if (offset < 0)
            throw new IllegalArgumentException("Negative Offset");
        if (size < 0)
            throw new IllegalArgumentException("Negative Size");
        return slice0(offset, size);
    }

    SlicedOffHeapMemoryStorage slice0(long offset, long size) {
        return new SlicedOffHeapMemoryStorage(this, size, offset);
    }

    public long getBaseAddress() {
        return this.baseAddr;
    }

    @Override
    public String toString() {
        return "OffHeapMemoryStorage{baseAddr=" + this.baseAddr + "; size=" + this.size + "; type="
                + super.getClass() + "}";
    }
}
