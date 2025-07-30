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
import io.github.demnetwork.runtime.utils.memory.MemoryObject;
import io.github.demnetwork.runtime.utils.memory.MemoryObject.FieldMetadata;

public final class MemoryLayout {
    final FieldMetadata[] meta;
    final Class<? extends MemoryObject> cls;

    public MemoryLayout(MemoryObject obj) {
        if (obj == null)
            throw new NullPointerException();
        this.meta = obj.getFieldMetadata();
        this.cls = obj.getClass();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MemoryLayout{cls=" + this.cls.getName() + "; fields=[");
        for (int i = 0; i < meta.length; i++) {
            sb.append(meta[i].toString());
            if ((i + 1) != meta.length)
                sb.append(", ");
        }
        sb.append("]}");
        return sb.toString();
    }

    public void print() {
        this.print(System.out);
    }

    public void print(PrintStream ps) {
        this.print(ps, PrintFormatter.nullPrintFormatter());
    }

    public void print(PrintStream ps, PrintFormatter fm) {
        if (ps == null)
            throw new NullPointerException();
        if (fm == null)
            throw new NullPointerException();
        ps.println(fm.formatClassName(this.cls.getName()));
        for (int i = 0; i < meta.length; i++) {
            FieldMetadata m = meta[i];
            ps.println(fm.formatField(m.getModifiers(), m.getName(), m.getOffset()));
        }
    }

    public void print(PrintFormatter fm) {
        this.print(System.out, fm);
    }

    public static abstract interface PrintFormatter {
        public abstract String formatClassName(String clsName);

        public abstract String formatField(int modifier, String name, long off);

        public static PrintFormatter nullPrintFormatter() {
            return new PrintFormatter() {

                @Override
                public String formatClassName(String clsName) {
                    return clsName;
                }

                @Override
                public String formatField(int m, String name, long off) {
                    return (Modifier.toString(m) + " \"" + name + "\" @" + off);
                }

            };
        }
    }

    public FieldMetadata[] getFieldMetadata() {
        return this.meta.clone();
    }
}
