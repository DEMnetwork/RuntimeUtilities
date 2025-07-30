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

import java.io.IOException;
import io.github.demnetwork.runtime.utils.Table;
import io.github.demnetwork.runtime.utils.memory.MemoryStructBuilder.SyntheticMemoryStructConstructor;
import io.github.demnetwork.runtime.utils.memory.serial.MemoryInputStream;
import io.github.demnetwork.runtime.utils.memory.serial.MemoryOutputStream;
import io.github.demnetwork.runtime.utils.memory.serial.StringWrapper;
import static io.github.demnetwork.runtime.utils.memory.OffHeapMemoryStorage.UNSAFE;

/**
 * {@link SyntheticMemoryStruct} does not suport serializing constructor logic.
 * Meaning constructor logic cannot be used on serialized instances.
 * <p>
 * Serializing the constructor logic would allow for RCE(Remote Code Execution).
 * <p>
 * If you need to send constructor logic, it is better to rather make both
 * parties know ahead-of-time the constructor logic.
 * <p>
 * <strong>Note:</strong> <code>SyntheticMemoryStruct</code> assumes both
 * producer and consumer already agree on the constructor logic. This logic is
 * not serialized and must be registered or available in the environment where
 * it's used.
 */
public final class SyntheticMemoryStruct extends MemoryStruct {
    private final SyntheticMemoryStructConstructor logic;
    private final FieldData[] fData;
    public final String name;

    SyntheticMemoryStruct(String name, OffHeapMemoryStorage stroage, FieldData[] fields,
            SyntheticMemoryStructConstructor cnsLogic, Object... args) throws IOException {
        super(stroage, fields);
        if (name == null)
            throw new NullPointerException();
        if (cnsLogic != null)
            cnsLogic.construct(this, args);
        this.logic = cnsLogic;
        this.fData = fields;
        this.name = name;
    }

    public SyntheticMemoryStruct createNew(OffHeapMemoryStorage storage, Object... args) throws IOException {
        return new SyntheticMemoryStruct(this.name, storage, this.fData.clone(), this.logic, args);
    }

    public static SyntheticMemoryStruct createNew(String name, OffHeapMemoryStorage storage,
            SyntheticMemoryStructConstructor logic, Object... args) throws IOException {
        if (args == null)
            throw new NullPointerException();
        return new SyntheticMemoryStruct(name, storage, new FieldData[0], logic, args.clone());
    }

    public static SyntheticMemoryStruct createNew(SyntheticMemoryStruct template, OffHeapMemoryStorage storage,
            SyntheticMemoryStructConstructor logic, Object... args) throws IOException {
        if (template == null)
            throw new NullPointerException();
        return new SyntheticMemoryStruct(template.name, storage, template.fData.clone(), logic, args);
    }

    @Override
    public void writeObj(MemoryOutputStream mos) throws IOException {
        mos.writeObj(new StringWrapper(this.name));
        super.writeObj(mos);
    }

    @Override
    public SyntheticMemoryStruct readObj(MemoryInputStream mis) throws IOException {
        try {
            UNSAFE.putObject(this, UNSAFE.objectFieldOffset(SyntheticMemoryStruct.class.getDeclaredField("name")),
                    ((StringWrapper) mis.readObj()).getValue());
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to read data", e);
        }
        return (SyntheticMemoryStruct) super.readObj(mis);
    }

    @Override
    protected void interpretData(int fc, Table fields) throws IOException, InstantiationException {
        super.interpretData(fc, fields);
        try {
            FieldData[] arr = new FieldData[fc];
            for (int i = 0; i < fc; i++) {
                arr[i] = new FieldData((String) fields.get(0, i), ((Integer) fields.get(2, i)).intValue());
            }
            UNSAFE.putObject(this, UNSAFE.objectFieldOffset(SyntheticMemoryStruct.class.getDeclaredField("fData")),
                    arr);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to read data", e);
        }
    }

    @Override
    public String toString() {
        return this.name + "(SyntheticMemoryStruct){super=\"" + super.toString() + "\";}";
    }

    public String getName() {
        return this.name;
    }
}
