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

import io.github.demnetwork.runtime.utils.memory.serial.PrimitiveWrappers.*;
import static io.github.demnetwork.runtime.utils.memory.OffHeapMemoryStorage.UNSAFE;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import io.github.demnetwork.runtime.internal.BuildData;
import io.github.demnetwork.runtime.utils.Table;
import io.github.demnetwork.runtime.utils.memory.reflect.MemoryObjectField;
import io.github.demnetwork.runtime.utils.memory.reflect.MemoryObjectFieldInspector;
import io.github.demnetwork.runtime.utils.memory.serial.MemoryInputStream;
import io.github.demnetwork.runtime.utils.memory.serial.MemoryOutputStream;
import io.github.demnetwork.runtime.utils.memory.serial.MemorySerializable;

/**
 * MemoryStruct is a high-level, annotated alternative to {@link MemoryObject}.
 * 
 * Fields annotated with {@code @MemoryStructField} are automatically discovered
 * and registered into off-heap memory.
 *
 * <p>
 * Fields must be {@link MemorySerializable}.
 * <p>
 * Call {@code flushFields()} to write data to memory, and {@code loadFields()}
 * to hydrate memory values into local fields.
 * 
 * <p>
 * By convention, {@code MemoryStruct} classes should be {@code final} and
 * non-abstract.
 */

public class MemoryStruct extends MemoryObject {
    protected volatile boolean closed = false;
    public final boolean isSynthetic;

    protected MemoryStruct(OffHeapMemoryStorage storage) {
        this(storage, -1L);
    }

    protected MemoryStruct(OffHeapMemoryStorage storage, long size) {
        this(storage, size, false);
    }

    protected MemoryStruct(OffHeapMemoryStorage storage, long size, boolean interpret) {
        super(storage, size, interpret);
        if (this.getClass() != MemoryStruct.class && (this.getClass().getModifiers() & Modifier.FINAL) == 0
                && !BuildData.CURRENT.getDebugStatus())
            System.out.println("In a production environment, structs are recommended to be final, and non-abstract");
        if (!interpret)
            findFields();
        this.isSynthetic = false;
    }

    /** Creates an synthetic MemoryStruct */
    MemoryStruct(OffHeapMemoryStorage stroage, FieldData[] fields) throws IOException {
        super(stroage, -1, false);
        super.addFields(fields);
        this.isSynthetic = true;
    }

    protected final void findFields() {
        Field[] fs = this.getClass().getDeclaredFields();
        ArrayList<FieldData> dl = new ArrayList<>();
        for (Field f : fs) {
            if ((f.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0
                    && f.isAnnotationPresent(MemoryStructField.class)) {
                if (!MemorySerializable.class.isAssignableFrom(f.getType())) {
                    if (BuildData.CURRENT.getDebugStatus())
                        System.out.println(
                                "[MemoryStruct] Field \"" + f.getName() + "\" from \"" + this.getClass().getName()
                                        + "\" uses an incompatible type.");
                    continue;
                }
                dl.add(new FieldData(f.getName(), f.getModifiers()));
            }
        }
        try {
            super.addFields(dl.toArray(new FieldData[dl.size()]));
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception occoured", e);
        } catch (Exception e) {
            throw new RuntimeException("Unable to add fields", e);
        }
    }

    /**
     * Field that should be included in a {@link MemoryStruct} should be annotated
     * with this annotation. If the declaring class of a field does not subclass
     * {@link MemoryStruct} this annotation has no effect.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public static abstract @interface MemoryStructField {
    }

    @Override
    protected void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("Closed MemoryStruct");
        if (super.storage.isClosed())
            throw new IOException("Closed storage");
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        this.closed = true;
    }

    protected final void flushFields() throws IOException {
        MemoryObjectFieldInspector mofi = new MemoryObjectFieldInspector(this);
        MemoryObjectField[] arr = mofi.getFields();
        Class<? extends MemoryStruct> cls = this.getClass();
        for (int i = 0; i < arr.length; i++) {
            MemoryObjectField f0 = arr[i];
            try {
                Field f1 = cls.getDeclaredField(f0.getName());
                f0.forceSet((MemorySerializable) UNSAFE.getObject(this, UNSAFE.objectFieldOffset(f1)), false);
            } catch (ReflectiveOperationException e) {
                if (BuildData.CURRENT.getDebugStatus()) {
                    System.out.println("[MemoryStruct] Field not found in class");
                    e.printStackTrace(System.out);
                }
            }
            f0.close();
        }
        super.pullMem();
    }

    protected final void loadFields() throws IOException {
        MemoryObjectFieldInspector mofi = new MemoryObjectFieldInspector(this);
        MemoryObjectField[] arr = mofi.getFields();
        Class<? extends MemoryStruct> cls = this.getClass();
        for (int i = 0; i < arr.length; i++) {
            MemoryObjectField f0 = arr[i];
            try {
                MemorySerializable obj = f0.get();
                Field f1 = cls.getDeclaredField(f0.getName());
                if (obj == null || f1.getType().isAssignableFrom(obj.getClass())) {
                    UNSAFE.putObject(this, UNSAFE.objectFieldOffset(f1), obj);
                } else if (BuildData.CURRENT.getDebugStatus()) {
                    System.out.println("Type does not match. " + obj.getClass() + " cannot cast to" + f1.getType());
                }
            } catch (ReflectiveOperationException e) {
                if (BuildData.CURRENT.getDebugStatus()) {
                    System.out.println("[MemoryStruct] Field not found in class");
                    e.printStackTrace(System.out);
                }
            }
            f0.close();
        }
    }

    @Override
    protected void setupFields(OffHeapMemoryStorage storage) throws NoSuchFieldException {
        super.setupFields(storage); // Setup parent fields.
        this.closed = false; // Setup closed state
    }

    @Override
    protected void interpretData(int fc, Table fields) throws IOException, InstantiationException {
        super.interpretData(fc, fields);
        if (!this.isSynthetic)
            loadFields(); // Hydrate fields in struct
    }

    @Override
    public void writeObj(MemoryOutputStream mos) throws IOException {
        mos.writeObj(this.isSynthetic ? BooleanWrapper.TRUE : BooleanWrapper.FALSE);
        super.writeObj(mos);
    }

    @Override
    public MemoryStruct readObj(MemoryInputStream mis) throws IOException {
        try {
            UNSAFE.putBoolean(this, UNSAFE.objectFieldOffset(MemoryStruct.class.getDeclaredField("isSynthetic")),
                    ((BooleanWrapper) mis.readObj()).booleanValue());
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to read data", e);
        }
        return (MemoryStruct) super.readObj(mis);
    }

    public static void assertMemoryFieldEquals(MemoryStruct struct, String name, Object expected) throws Exception {
        Object actual = struct.getField(struct.getFieldID(name));
        if (!Objects.equals(expected, actual))
            throw new AssertionError("Expected " + expected + " but got " + actual);
    }

}
