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

import static io.github.demnetwork.runtime.utils.memory.OffHeapMemoryStorage.UNSAFE;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.demnetwork.runtime.utils.Table;
import io.github.demnetwork.runtime.utils.memory.serial.MemoryInputStream;
import io.github.demnetwork.runtime.utils.memory.serial.MemoryOutputStream;
import io.github.demnetwork.runtime.utils.memory.serial.MemorySerializable;
import io.github.demnetwork.runtime.utils.memory.serial.NullWrapper;
import io.github.demnetwork.runtime.utils.memory.serial.StringWrapper;
import sun.misc.Unsafe;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.IntWrapper;
import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.LongWrapper;

public abstract class MemoryObject implements Closeable, MemorySerializable {
    protected final OffHeapMemoryStorage storage;
    final Table fields;
    protected static final int FIELD_MODIFIERS = Modifier.fieldModifiers();

    /**
     * @deprecated This constructor allocates an {@link OffHeapMemoryStorage} on
     *             every invocation
     */
    @Deprecated
    protected MemoryObject(long size) {
        this(size, false);
    }

    /**
     * @deprecated This constructor allocates an {@link OffHeapMemoryStorage} on
     *             every invocation
     */
    @Deprecated
    protected MemoryObject(long size, boolean ingoreLimits) {
        this(new OffHeapMemoryStorage(size, ingoreLimits));
    }

    protected MemoryObject(OffHeapMemoryStorage storage) {
        this(storage, -1);
    }

    protected MemoryObject(OffHeapMemoryStorage storage, long size) {
        this(storage, size, false);
    }

    protected MemoryObject(OffHeapMemoryStorage storage, long size, boolean interpretData) {
        if (storage == null)
            throw new NullPointerException();
        if (size > 0 && storage.size != size)
            throw new IllegalArgumentException("Size does not match");
        if (!interpretData) {
            this.storage = storage;
            this.fields = new Table(4, String.class, MemorySerializable.class, int.class, long.class);
            this.setupSentinelRow();
        } else {
            try (OffHeapMemoryInputStream mis = storage.toInputStream(false);) {
                long esize = ((LongWrapper) mis.readObj()).longValue();
                if (size > 0 && esize != size)
                    throw new IllegalArgumentException("Size does not match the declared size");
                this.storage = storage;
                this.fields = new Table(4, String.class, MemorySerializable.class, int.class, long.class);
                this.interpretData(((IntWrapper) mis.readObj()).intValue(), this.fields);
            } catch (Exception e) {
                if (e instanceof RuntimeException)
                    throw (RuntimeException) e;
                throw new RuntimeException("Something went wrong while interpreting data", e);
            }
        }
    }

    void setupSentinelRow() {
        this.fields.set("null", 0, 0);
        this.fields.set(null, 1, 0);
        this.fields.set(0, 2, 0);
        this.fields.set(-1L, 3, 0);
    }

    protected final int addField(String name, MemorySerializable obj, int mod) throws IOException {
        ensureOpen();
        return this.addField(name, obj, mod, -1, false);
    }

    protected final int addField(String name, MemorySerializable obj, int mod, int offset, boolean deserializationMode)
            throws IOException {
        return this.addField0(name, obj, mod, offset, deserializationMode, !deserializationMode);
    }

    protected final int addField0(String name, MemorySerializable obj, int mod, int offset, boolean deserializationMode,
            boolean rtupdate) throws IOException {
        ensureOpen();
        if (name == null)
            throw new NullPointerException();
        if (this.fields.getLocationsOf(name).length != 0)
            throw new IllegalArgumentException("Duplicate Field: " + name);
        mod = mod & FIELD_MODIFIERS;
        if ((mod & (Modifier.STATIC | Modifier.TRANSIENT)) != 0)
            throw new IllegalArgumentException("Cannot have static nor Transient fields to be serialized to memory");
        int fieldID = this.fields.getRowCount();
        this.fields.set(name, 0, fieldID);
        this.fields.set(obj, 1, fieldID);
        this.fields.set(mod, 2, fieldID);
        if (deserializationMode && offset >= 0) {
            this.fields.set(offset, 3, fieldID);
        }
        if (rtupdate && !deserializationMode)
            this.pullMem(); // This should not be done during deserialization
        return fieldID;
    }

    protected final int[] addFields(FieldData[] data) throws IOException {
        ensureOpen();
        if (data == null)
            throw new NullPointerException();
        RuntimeException ex = new RuntimeException("Failed to add all fields.");
        boolean throwEx = false;
        int[] ids = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            FieldData d = data[i];
            try {
                ids[i] = addField0(d.name, d.obj, d.mod, -1, false, false);
            } catch (RuntimeException e) {
                ids[i] = -1;
                ex.addSuppressed(e);
                if (!throwEx)
                    throwEx = true;
            }
        }
        if (throwEx)
            throw ex;
        return ids;
    }

    /**
     * Updates the OffHeapMemoryStorage
     * 
     * @throws IOException If an I/O Operation Went wrong
     */
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

    protected final void setField(MemorySerializable obj, int fieldID) throws IOException {
        ensureOpen();
        if (fieldID >= this.fields.getRowCount())
            throw new IllegalArgumentException("Non-existant field");
        int mod = (Integer) this.fields.get(2, fieldID);
        if ((mod & Modifier.FINAL) != 0)
            throw new IllegalArgumentException("Cannot modify field, because it is final");
        this.fields.set(obj, 1, fieldID);
        this.pullMem();
    }

    @Override
    public MemoryObject readObj(MemoryInputStream mis) throws IOException {
        // No ensure-open. because we may not rely on state
        try {
            OffHeapMemoryStorage storage = new OffHeapMemoryStorage(((LongWrapper) mis.readObj()).longValue(), true);
            int fc = ((IntWrapper) mis.readObj()).intValue() + 1;
            setupFields(storage);
            transferData(mis, fc);
            interpretData(fc, this.fields);
            return this;
        } catch (InstantiationException e) {
            throw new IOException("Reading failed due to an InstantiationException", e);
        } catch (ClassCastException e) {
            throw new IOException("A ClassCastException occurred while casting a type to read data properly", e);
        } catch (ReflectiveOperationException e) {
            throw new IOException("An unexpected exception occurred while reading", e);
        }
    }

    /** Called upon deserialization to setup fields */
    protected void setupFields(OffHeapMemoryStorage storage) throws NoSuchFieldException {
        setupFields(storage, MemoryObject.class.getDeclaredField("storage"),
                MemoryObject.class.getDeclaredField("fields"));
    }

    protected void setupFields(OffHeapMemoryStorage storage, Field storageField, Field tableField) {
        UNSAFE.putObject(this, UNSAFE.objectFieldOffset(storageField), storage);
        UNSAFE.putObject(this, UNSAFE.objectFieldOffset(tableField),
                new Table(4, String.class, MemorySerializable.class, int.class, long.class));
    }

    protected void interpretData(int fc, Table fields) throws IOException, InstantiationException {
        try (OffHeapMemoryInputStream is = storage.toInputStream(false)) {
            is.readObj(); // This is ignored
            is.readObj(); // Also ignored, because we already know that data
            for (int i = 1; i < fc; i++) {
                fields.set(((StringWrapper) is.readObj()).getValue(), 0, i);
                fields.set(is.readObj(), 1, i);
                fields.set(((IntWrapper) is.readObj()).getValue(), 2, i);
                fields.set(((LongWrapper) is.readObj()).getValue(), 3, i);
            }
        }
    }

    protected void transferData(MemoryInputStream mis, int fc) throws IOException {
        try (OffHeapMemoryOutputStream os = storage.toOutputStream(false)) {
            os.writeObj(new LongWrapper(storage.size));
            os.writeObj(new IntWrapper(fc));
            byte[] buf = new byte[Math.min(4096,
                    os.remaining() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) os.remaining())];
            while (os.remaining() > 0) {
                int r = mis.read(buf, 0, buf.length);
                if (r == -1)
                    throw new EOFException("Corrupt Data");
                os.write(buf, 0, r);
            }
        }
    }

    protected final boolean compareAndSwap(int fieldID, MemorySerializable compare, MemorySerializable swap)
            throws IOException {
        ensureOpen();
        if (fieldID <= 0)
            throw new IllegalArgumentException("Illegal FieldID");
        try {
            Object[][] arr0 = (Object[][]) UNSAFE.getObject(this.fields,
                    UNSAFE.objectFieldOffset(Table.class.getDeclaredField("data")));
            Object[] arr1 = arr0[1];
            boolean b = UNSAFE.compareAndSwapObject(arr1,
                    Unsafe.ARRAY_OBJECT_BASE_OFFSET + Unsafe.ARRAY_OBJECT_INDEX_SCALE * fieldID, compare, swap);
            if (b)
                pullMem();
            return b;
        } catch (NoSuchFieldException e) {
            throw new IOException("Something went wrong", e);
        }
    }

    /**
     * Writes the body of this object to the
     * {@link io.github.demnetwork.runtime.utils.memory.serial.MemoryOutputStream}
     * MemoryOutputStream.
     * 
     * <p>
     * <strong>Warning:</strong> This method does not write the object type ID.
     * If you want the object to be properly deserialized, you must call
     * {@link io.github.demnetwork.runtime.utils.memory.serial.MemoryOutputStream#writeObj(MemorySerializable)
     * MemoryOutputStream.writeObj(MemorySerializable obj)} instead.
     */
    @Override
    public void writeObj(MemoryOutputStream mos) throws IOException {
        ensureOpen();
        pullMem();
        pushMem(mos);
    }

    /**
     * Dumps the data from the {@link OffHeapMemoryStorage} into the
     * {@link io.github.demnetwork.runtime.utils.memory.serial.MemoryOutputStream
     * MemoryOutputStream}
     * 
     * 
     * @apiNote It is recommended to call {@link #pullMem(MemoryOutputStream)}
     *          before calling this method, to ensure that the storage is up-to-date
     * @param mos Derstination to where the dumped data is going
     * @throws IOException If an I/O Operation went wrong
     */
    protected void pushMem(MemoryOutputStream mos) throws IOException {
        try (OffHeapMemoryInputStream is = this.storage.toInputStream(false)) {
            is.transferTo(mos); // Simple
        }
    }

    protected final MemorySerializable getField(int fieldID) throws IOException {
        ensureOpen();
        if (fieldID >= this.fields.getRowCount() || fieldID < 0)
            throw new IllegalArgumentException("Illegal Field ID: " + fieldID);
        return (MemorySerializable) this.fields.get(1, fieldID);
    }

    protected final int getFieldID(String name) throws IOException, NoSuchFieldException {
        ensureOpen();
        if (name == null)
            throw new NullPointerException("Null field name");
        int[][] arr = this.fields.getLocationsOf(name);
        if (arr.length == 0)
            throw new NoSuchFieldException(name);
        return arr[0][1]; // Array is [[col, row], [col, row], ...]
    }

    protected boolean isAccessible(int fieldID) throws IOException {
        return (((Integer) this.fields.get(2, fieldID)).intValue() & Modifier.PUBLIC) != 0;
    }

    public Object get(String name) throws IOException, NoSuchFieldException, SecurityException {
        ensureOpen();
        int fieldID = this.getFieldID(name);
        if (!isAccessible(fieldID))
            throw new SecurityException("Inaccessible field: " + name);
        return this.getField(fieldID);
    }

    public void set(String name, Object obj) throws IOException, NoSuchFieldException, SecurityException {
        if (name == null)
            throw new NullPointerException("Null Name");
        if (!(obj instanceof MemorySerializable))
            throw new ClassCastException("Incompatible Object provided");
        int fieldID = this.getFieldID(name);
        if (!isAccessible(fieldID))
            throw new SecurityException("Inaccessible Field: " + name);
        this.setField((MemorySerializable) obj, fieldID);
    }

    protected final void setField(MemorySerializable obj, String name) throws IOException, NoSuchFieldException {
        this.setField(obj, this.getFieldID(name));
    }

    protected abstract void ensureOpen() throws IOException;

    public abstract boolean isClosed();

    /**
     * @implSpec The implementation should not close the {@link #storage
     *           MemoryObject.storage}
     */
    @Override
    public abstract void close() throws IOException;

    protected final int getModifiers(int fieldID) throws IOException {
        ensureOpen();
        return ((Integer) this.fields.get(2, fieldID)).intValue();
    }

    public OffHeapMemoryStorage getStorage() {
        return this.storage;
    }

    @Override
    public String toString() {
        return "MemoryObject{" +
                "storageSize=" + storage.size +
                ", fields=" + this.getFieldCount() + " fields, type= " + this.getClass().getName() + "}";
    }

    public int getFieldCount() {
        return this.fields.getRowCount() - 1; // Exclude sentinel row
    }

    public final Source getSource() {
        // Check Sentinel Row
        return "null".equals(fields.get(0, 0)) && fields.get(1, 0) == null
                && Integer.valueOf(0).equals(fields.get(2, 0)) && Long.valueOf(-1L).equals(fields.get(3, 0))
                        ? Source.CONSTRUCTOR
                        : Source.SERIALIZATION;
    }

    public static enum Source {
        SERIALIZATION, CONSTRUCTOR;
    }

    protected final boolean hasField(String name) throws IOException {
        try {
            getFieldID(name); // Throws if not found
            return true;
        } catch (NoSuchFieldException e) {
            return false; // Catch exception
        }
    }

    public static final class FieldMetadata {
        public final int fieldID;
        private final String name;
        private final int mod;
        private final long offset;

        private FieldMetadata(String name, int mod, long off, int id) {
            if (name == null)
                throw new NullPointerException();
            this.name = name;
            this.mod = mod;
            this.offset = off;
            this.fieldID = id;
        }

        public String getName() {
            return this.name;
        }

        /**
         * An getter for the field modifiers
         * 
         * @return Returns the modifiers of the field
         * @see java.lang.reflect.Modifier
         */
        public int getModifiers() {
            return this.mod;
        }

        /**
         * An getter that return the offset of the field
         * 
         * @return Returns the Storage offset of the field in the storage
         * @see MemoryObject#toStreamOffset(long)
         * @see MemoryObject#toStorageOffset(long)
         */
        public long getOffset() {
            return this.offset;
        }

        @Override
        public String toString() {
            return "FieldMetadata{\"" + this.name + "\" | @" + this.offset + " | mod:\"" + Modifier.toString(this.mod)
                    + "\"}";
        }
    }

    /** @return An list containing the field metadata */
    protected final List<FieldMetadata> getFieldMetadataAsList() {
        return java.util.Arrays.asList(getFieldMetadata());
    }

    public final FieldMetadata[] getFieldMetadata() {
        int len = this.fields.getRowCount();
        FieldMetadata[] arr = new FieldMetadata[len - 1];
        for (int i = 1; i < len; i++) {
            arr[i - 1] = createMetadata(this.fields.get(0, i), this.fields.get(2, i), this.fields.get(3, i), i);
        }
        return arr;
    }

    protected final MemorySerializable getField(String name) throws IOException, NoSuchFieldException {
        ensureOpen();
        return getField(getFieldID(name));
    }

    private static FieldMetadata createMetadata(Object name, Object mod, Object offset, int id) {
        return new FieldMetadata((String) name, ((Integer) mod).intValue(), ((Long) offset).longValue(), id);
    }

    public static final long toStreamOffset(long offsetInStorage) {
        return offsetInStorage + 8;
    }

    public static final long toStorageOffset(long offsetInStream) {
        return offsetInStream - 8;
    }

    protected static final class FieldData {
        public final String name;
        public final int mod;
        MemorySerializable obj;

        public FieldData(String name, int mod) {
            this(name, mod, NullWrapper.NULL);
        }

        public FieldData(String name, int mod, MemorySerializable obj) {
            if (name == null)
                throw new NullPointerException();
            this.name = name;
            this.mod = mod;
            this.obj = obj;
        }

        public MemorySerializable getObj() {
            return this.obj;
        }

        public void setObj(MemorySerializable obj) {
            if (obj == null)
                throw new NullPointerException();
            this.obj = obj;
        }
    }

    public Map<String, FieldMetadata> getFieldMetadataAsMap() {
        FieldMetadata[] arr = this.getFieldMetadata();
        Map<String, FieldMetadata> map = new HashMap<>(arr.length);
        for (int i = 0; i < arr.length; i++) {
            FieldMetadata meta = arr[i];
            map.put(meta.name, meta);
        }
        return map;
    }

}
