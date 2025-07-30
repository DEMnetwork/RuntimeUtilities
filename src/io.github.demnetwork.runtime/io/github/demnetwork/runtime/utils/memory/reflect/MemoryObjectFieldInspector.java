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

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import io.github.demnetwork.runtime.internal.*;
import io.github.demnetwork.runtime.utils.memory.MemoryObject;
import io.github.demnetwork.runtime.utils.memory.MemoryObject.FieldMetadata;

@MeIDontLikeLambdas
public class MemoryObjectFieldInspector {
    protected final MemoryObject obj;
    public static final FieldFilter PUBLIC_FIELD_FILTER = new PublicFieldFilter();
    public static final FieldFilter NON_PUBLIC_FIELD_FILTER = not(PUBLIC_FIELD_FILTER);
    public static final FieldFilter NON_FINAL_FIELD_FILTER = new NonFinalFieldFilter();
    public static final FieldFilter FINAL_FIELD_FILTER = not(NON_FINAL_FIELD_FILTER);
    /** Matches every Field */
    public static final FieldFilter WILDCARD_FIELD_FILTER = new FieldFilter() {

        @Override
        public boolean filter(FieldMetadata meta) {
            return true;
        }

    };

    public MemoryObjectFieldInspector(MemoryObject obj) {
        if (obj == null)
            throw new NullPointerException();
        this.obj = obj;
    }

    public MemoryObjectField[] getFields() {
        MemoryObjectField[] fields = new MemoryObjectField[this.obj.getFieldCount()];
        FieldMetadata[] fm = this.obj.getFieldMetadata();
        for (int i = 0; i < fields.length; i++) {
            fields[i] = new MemoryObjectField(this.obj, fm[i]);
        }
        return fields;
    }

    @Override
    public String toString() {
        return "MemoryObjectFieldInspector{obj=\'" + obj.toString() + "\'}";
    }

    public MemoryObjectField[] getFilteredFields(FieldFilter f) {
        if (f == null)
            throw new NullPointerException();
        ArrayList<MemoryObjectField> list = new ArrayList<>();
        FieldMetadata[] fm = obj.getFieldMetadata();
        try {
            for (int i = 0; i < fm.length; i++) {
                FieldMetadata meta = fm[i];
                if (f.filter(meta))
                    list.add(new MemoryObjectField(this.obj, meta));
            }
        } catch (Exception e) {
            if (BuildData.CURRENT.getDebugStatus()) {
                System.out.println("[MemoryObjectFieldInspector] An exception occured.");
                e.printStackTrace(System.out);
            }
        }
        return list.toArray(new MemoryObjectField[list.size()]);
    }

    @MeIDontLikeLambdas
    @FunctionalInterface
    public static abstract interface FieldFilter {
        public abstract boolean filter(FieldMetadata meta);
    }

    protected static final class PublicFieldFilter implements FieldFilter {

        @Override
        public boolean filter(FieldMetadata meta) {
            return (meta.getModifiers() & Modifier.PUBLIC) != 0;
        }

    }

    public MemoryObjectField[] getPublicFields() {
        return this.getFilteredFields(PUBLIC_FIELD_FILTER);
    }

    public void printFieldSummary() {
        this.printFieldSummary(System.out);
    }

    public void printFieldSummary(PrintStream ps) {
        for (MemoryObjectField field : getFields()) {
            ps.println(field.toString());
        }
    }

    public MemoryObjectField getField(String name) throws NoSuchFieldException {
        if (name == null)
            throw new NullPointerException();
        FieldMetadata meta = this.obj.getFieldMetadataAsMap().get(name); // Efficient lookup
        if (meta == null)
            throw new NoSuchFieldException(name);
        return new MemoryObjectField(obj, meta);
    }

    public static final class NameFieldFilter implements FieldFilter {
        public final String name;

        public NameFieldFilter(String name) {
            if (name == null)
                throw new NullPointerException();
            this.name = name;
        }

        @Override
        public boolean filter(FieldMetadata meta) {
            return meta.getName().equals(name);
        }

    }

    protected static final class NonFinalFieldFilter implements FieldFilter {

        @Override
        public boolean filter(FieldMetadata meta) {
            return (meta.getModifiers() & Modifier.FINAL) == 0;
        }

    }

    public int getFieldCount() {
        return obj.getFieldCount();
    }

    public FieldMetadata[] getFieldMetadata() {
        return obj.getFieldMetadata();
    }

    public static FieldFilter and(FieldFilter a, FieldFilter b) {
        if (a == null)
            throw new NullPointerException();
        if (b == null)
            throw new NullPointerException();
        return new FieldFilter() {

            @Override
            public boolean filter(FieldMetadata meta) {
                return a.filter(meta) && b.filter(meta);
            }

        };
    }

    public static FieldFilter or(FieldFilter a, FieldFilter b) {
        if (a == null)
            throw new NullPointerException();
        if (b == null)
            throw new NullPointerException();
        return new FieldFilter() {

            @Override
            public boolean filter(FieldMetadata meta) {
                return a.filter(meta) || b.filter(meta);
            }

        };
    }

    public static FieldFilter xor(FieldFilter a, FieldFilter b) {
        if (a == null)
            throw new NullPointerException();
        if (b == null)
            throw new NullPointerException();
        return new FieldFilter() {

            @Override
            public boolean filter(FieldMetadata meta) {
                boolean b0 = a.filter(meta);
                boolean b1 = b.filter(meta);
                return b0 && !b1 || !b0 && b1;
            }
        };
    }

    public static FieldFilter nor(FieldFilter a, FieldFilter b) {
        if (a == null)
            throw new NullPointerException();
        if (b == null)
            throw new NullPointerException();
        return new FieldFilter() {

            @Override
            public boolean filter(FieldMetadata meta) {
                return !a.filter(meta) && !b.filter(meta);
            }
        };
    }

    public static FieldFilter nand(FieldFilter a, FieldFilter b) {
        if (a == null)
            throw new NullPointerException();
        if (b == null)
            throw new NullPointerException();
        return new FieldFilter() {

            @Override
            public boolean filter(FieldMetadata meta) {
                return !a.filter(meta) || !b.filter(meta);
            }

        };
    }

    public static FieldFilter not(FieldFilter a) {
        if (a == null)
            throw new NullPointerException();
        return new FieldFilter() {
            @Override
            public boolean filter(FieldMetadata meta) {
                return !a.filter(meta);
            }
        };
    }

    public static FieldFilter xnor(FieldFilter a, FieldFilter b) {
        if (a == null)
            throw new NullPointerException();
        if (b == null)
            throw new NullPointerException();
        return new FieldFilter() {
            @Override
            public boolean filter(FieldMetadata meta) {
                boolean b0 = a.filter(meta);
                boolean b1 = b.filter(meta);
                return b0 && b1 || !b0 && !b1;
            }
        };
    }

    public MemoryObjectField getField(int index) {
        FieldMetadata[] meta = obj.getFieldMetadata();
        if (index < 0 || index >= meta.length)
            throw new IndexOutOfBoundsException();
        return new MemoryObjectField(obj, meta[index]);
    }

    public MemoryObjectField[] getNonFinalFields() {
        return this.getFilteredFields(NON_FINAL_FIELD_FILTER);
    }

    public MemoryObjectField[] getFinalFields() {
        return this.getFilteredFields(FINAL_FIELD_FILTER);
    }

    public MemoryObjectField[] getNonPublicFields() {
        return this.getFilteredFields(NON_PUBLIC_FIELD_FILTER);
    }

}
