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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import io.github.demnetwork.runtime.utils.memory.MemoryObject.FieldData;
import io.github.demnetwork.runtime.utils.memory.serial.MemorySerializable;
import io.github.demnetwork.runtime.utils.memory.serial.NullWrapper;

public final class MemoryStructBuilder {
    private final ArrayList<FieldData> fields = new ArrayList<>();
    private final HashSet<String> seenFields = new HashSet<>();
    private SyntheticMemoryStructConstructor logic = null;
    private final String name;

    public MemoryStructBuilder() {
        this("null");
    }

    public MemoryStructBuilder(String name) {
        if (name == null)
            throw new NullPointerException();
        this.name = name;
    }

    public MemoryStructBuilder addField(String name, int mod) {
        return this.addField(name, mod, NullWrapper.NULL);
    }

    public MemoryStructBuilder addField(String name, int mod, MemorySerializable obj) {
        if (seenFields.contains(name))
            throw new IllegalArgumentException("Cannot add two fields with same name.");
        fields.add(new FieldData(name, mod, obj)); // 'new FieldData(...)' might throw an exception
        seenFields.add(name); // to ensrue no fake fields are there
        return this;
    }

    public MemoryStructBuilder removeField(String name) {
        if (name == null)
            throw new NullPointerException();
        if (!seenFields.contains(name))
            throw new IllegalArgumentException("Cannot remove an field that was not added");
        Iterator<FieldData> i = fields.iterator();
        while (i.hasNext()) {
            FieldData fd = i.next();
            if (fd.name.equals(name)) {
                i.remove();
                seenFields.remove(name);
                break;
            }
        }
        return this;
    }

    public MemoryStructBuilder setConstructorLogic(SyntheticMemoryStructConstructor c) {
        if (c == null)
            throw new NullPointerException();
        this.logic = c;
        return this;
    }

    public static abstract interface SyntheticMemoryStructConstructor {
        public abstract void construct(SyntheticMemoryStruct struct, Object... args) throws IOException;

        public static int addField(SyntheticMemoryStruct struct, String name, int mod, MemorySerializable obj)
                throws IOException {
            return struct.addField(name, obj, mod);
        }

        public static void setField(SyntheticMemoryStruct struct, String name, MemorySerializable obj)
                throws NoSuchFieldException, IOException {
            struct.setField(obj, struct.getFieldID(name));
        }

        public static MemorySerializable getField(SyntheticMemoryStruct struct, String name)
                throws NoSuchFieldException, IOException {
            return struct.getField(struct.getFieldID(name));
        }
    }

    public SyntheticMemoryStruct build(OffHeapMemoryStorage storage, Object... args) throws IOException {
        return new SyntheticMemoryStruct(this.name, storage, this.fields.toArray(new FieldData[this.fields.size()]),
                this.logic, args);
    }
}
