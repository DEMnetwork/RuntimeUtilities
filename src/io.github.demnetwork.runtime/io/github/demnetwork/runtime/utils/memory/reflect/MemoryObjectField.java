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

package io.github.demnetwork.runtime.utils.memory.reflect;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import io.github.demnetwork.runtime.internal.BuildData;
import io.github.demnetwork.runtime.utils.Table;
import io.github.demnetwork.runtime.utils.memory.MemoryObject;
import io.github.demnetwork.runtime.utils.memory.OffHeapMemoryInputStream;
import io.github.demnetwork.runtime.utils.memory.OffHeapMemoryOutputStream;
import io.github.demnetwork.runtime.utils.memory.OffHeapMemoryStorage;
import io.github.demnetwork.runtime.utils.memory.MemoryObject.FieldMetadata;
import io.github.demnetwork.runtime.utils.memory.serial.MemorySerializable;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.IntWrapper;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.LongWrapper;
import io.github.demnetwork.runtime.utils.memory.serial.StringWrapper;
import sun.misc.Unsafe;

/**
 * Represents an field stored in memory used by
 * {@link io.github.demnetwork.runtime.utils.memory.MemoryObject MemoryObject}.
 * <p>
 * This is an snapshot and changes in layout of the Object may cause undefined
 * behvaiour
 */
public class MemoryObjectField implements Closeable {
    static final Unsafe UNSAFE;
    protected final MemoryObject obj;
    protected final FieldMetadata mData;
    protected final OffHeapMemoryInputStream mis;
    protected volatile boolean closed = false;
    protected final OffHeapMemoryStorage storage;
    protected final Table fields;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
            Field f2 = MemoryObject.class.getDeclaredField("fields");
            f2.setAccessible(true);
            FIELDS_TABLE_OFFSET = UNSAFE.objectFieldOffset(f2);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    static final long FIELDS_TABLE_OFFSET;

    public MemoryObjectField(MemoryObject obj, String name) {
        this(obj, obj.getFieldMetadataAsMap().get(name));
    }

    /**
     * @apiNote If the Metadata of the field was retrived by another MemroyObject
     *          that is not the one passed to this constructor undenfined behviour
     *          may occour
     */
    protected MemoryObjectField(MemoryObject obj, FieldMetadata mData) {
        if (obj == null)
            throw new NullPointerException();
        if (mData == null)
            throw new NullPointerException();
        this.obj = obj;
        this.mData = mData;
        this.storage = this.obj.getStorage();
        this.mis = this.obj.getStorage().toInputStream(false);
        this.fields = (Table) UNSAFE.getObject(obj, FIELDS_TABLE_OFFSET);
    }

    public MemorySerializable get() throws IOException {
        ensureOpen();
        return (MemorySerializable) this.fields.get(1, mData.fieldID); // No need to fr expensive de-serialization
    }

    public MemorySerializable getAtOffset(long off) throws IOException, InstantiationException {
        ensureOpen();
        mis.setOffset(off);
        return mis.readObj(); // required because it needs to read at an specific offset
    }

    /**
     * <strong>WARNING: Interpret Data that does not match the expected structure
     * of the type,
     * causes undefined behaviour</strong>
     * <p>
     */
    public <T extends MemorySerializable> T getAndInterpretAs(Class<T> cls) throws IOException, InstantiationException {
        ensureOpen();
        mis.setOffset(mData.getOffset());
        return mis.readAndInterpretAs(cls); // required because it uses re-interpretation
    }

    public void set(MemorySerializable object) throws IOException {
        this.set(object, true);
    }

    public void set(MemorySerializable object, boolean flush) throws IOException {
        ensureOpen();
        int mod = ((Integer) fields.get(2, mData.fieldID)).intValue();
        if ((mod & Modifier.FINAL) != 0)
            throw new IllegalArgumentException("Cannot modify final field: " + mData.getName());
        fields.set(object, 1, mData.fieldID);
        if (flush)
            pullMem();
    }

    protected void pullMem() throws IOException {
        ensureOpen();
        try (OffHeapMemoryOutputStream os = this.storage.toOutputStream(false)) {
            os.writeObj(new LongWrapper(this.storage.size));
            os.writeObj(new IntWrapper(this.fields.getRowCount() - 1));
            for (int i = 1; i < this.fields.getRowCount(); i++) {
                os.writeObj(new StringWrapper((String) this.fields.get(0, i)));
                long off = os.getOffset();
                this.fields.set(off, 3, i);
                os.writeObj((MemorySerializable) this.fields.get(1, i));
                os.writeObj(new IntWrapper(((Integer) this.fields.get(2, i)).intValue()));
                os.writeObj(new LongWrapper(off));
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        this.closed = true;
        this.mis.close();
    }

    protected void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("Closed MemoryObjectField");
        if (obj.isClosed()) {
            close();
            throw new IOException("Closed object");
        }
        if (this.storage.isClosed()) {
            close();
            throw new IOException("Closed Storage");
        }
    }

    @Override
    public String toString() {
        return "MemoryObjectField{" +
                "name=\"" + mData.getName() + "\", offset=" + mData.getOffset() +
                ", modifiers=" + java.lang.reflect.Modifier.toString(mData.getModifiers()) +
                "}";
    }

    public void forceSet(MemorySerializable object) throws IOException {
        this.forceSet(object, true);
    }

    public void forceSet(MemorySerializable object, boolean flush) throws IOException {
        ensureOpen();
        if ((mData.getModifiers() & Modifier.FINAL) != 0 && BuildData.CURRENT.getDebugStatus()) {
            System.out.println("[MemoryObjectField] Forcing set on final field: " + mData.getName());
        }
        fields.set(object, 1, mData.fieldID);
        if (flush)
            pullMem();
    }

    public int getModifiers() {
        return this.mData.getModifiers();
    }

    public String getName() {
        return this.mData.getName();
    }

    public FieldMetadata getMetadata() {
        return this.mData;
    }

}
