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

package io.github.demnetwork.runtime.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class Table implements Iterable<Object> {
    private final Class<?>[] types;
    protected Object[][] data;

    public Table(int colCount, Class<?>... types) {
        this(colCount, 1, types);
    }

    public Table(int colCount, int rowCount, Class<?>... types) {
        if (rowCount < 1 || colCount < 2) {
            throw new IllegalArgumentException("Illegal Column Count and/or Row Count");
        }
        this.types = chkArray(types, colCount);
        this.data = new Object[colCount][rowCount];
    }

    protected final void expand(int rows) {
        Object[][] arr = data;
        int l = this.data[0].length + rows;
        for (int i = 0; i < arr.length; i++) {
            this.data[i] = java.util.Arrays.copyOf(arr[i], l);
        }
    }

    protected static final void throwNullPointerException(String msg) throws NullPointerException {
        if (msg == null)
            throwNullPointerException("Null message");
        throw new NullPointerException(msg);
    }

    protected static final Class<?> convertClass(Class<?> c) {
        if (c == boolean.class) {
            return Boolean.class;
        } else if (c == byte.class) {
            return Byte.class;
        } else if (c == short.class) {
            return Short.class;
        } else if (c == char.class) {
            return Character.class;
        } else if (c == int.class) {
            return Integer.class;
        } else if (c == long.class) {
            return Long.class;
        } else if (c == float.class) {
            return Float.class;
        } else if (c == double.class) {
            return Double.class;
        } else {
            return c;
        }
    }

    protected static final <T> T[] chkArray(T[] arr, int expectedLength) {
        T[] a = arr.clone();
        if (a.length != expectedLength) {
            throw new IllegalArgumentException("Lenghts do not match");
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) {
                throwNullPointerException("Null Array Item at index " + i);
            }
        }
        return a;
    }

    protected static final Object getDef(Class<?> primType) throws ClassCastException {
        if (!primType.isPrimitive())
            throwClassCastException("Not a primitive type");
        Class<?> c = convertClass(primType);
        if (c == Boolean.class) {
            return false;
        } else if (c == Character.class) {
            return '\u0000';
        } else {
            return (byte) 0;
        }
    }

    /**
     * Helper Method for checking bounds
     * 
     * @param length Array length, if it is negative it will only check for
     *               negative index
     * @param index  Index
     * @throws ArrayIndexOutOfBoundsException If the index is out of bounds
     */
    protected static void chkBounds(int length, int index) throws ArrayIndexOutOfBoundsException {
        if (index < 0 || (index >= length && length > -1)) {
            if (length > -1) {
                throw new ArrayIndexOutOfBoundsException(
                        "Index " + index + " is out of bounds for length " + length + "!");
            } else {
                throw new ArrayIndexOutOfBoundsException(
                        "Index " + index + " is negative!");
            }
        }
    }

    protected static final void throwClassCastException(String msg) throws NullPointerException, ClassCastException {
        if (msg == null) {
            throwNullPointerException("Null Message");
        }
        throw new ClassCastException(msg);
    }

    public void set(Object o, int col, int row) throws ClassCastException, ArrayIndexOutOfBoundsException {
        chkBounds(this.data.length, col);
        chkBounds(-1, row);
        if (row >= this.data[0].length) {
            expand((row - this.data[0].length) + 1);
        }
        chkType(this.types[col], o);
        this.data[col][row] = o;
    }

    public Object get(int col, int row) throws RuntimeException {
        chkBounds(this.data.length, col);
        chkBounds(this.data[0].length, row);
        if (types[col].isPrimitive() && this.data[col][row] == null) {
            try {
                this.set((convertClass(types[col]).getMethod("valueOf", types[col]).invoke(null, getDef(types[col]))),
                        col, row);
            } catch (Exception e) {
                throw new RuntimeException("Unable to set Default Value", e);
            }
        }
        return this.data[col][row];
    }

    public final Class<?>[] getParameterTypes() {
        return types.clone();
    }

    public final int getColCount() {
        return this.data.length;
    }

    public final int getRowCount() {
        return this.data[0].length;
    }

    public final Class<?> getColumnType(int col) {
        chkBounds(this.data.length, col);
        return this.types[col];
    }

    public final Object[] getColumn(int col) {
        chkBounds(this.data.length, col);
        return this.getTableData0()[col].clone();
    }

    protected final Object[][] getTableData0() {
        return this.data.clone();
    }

    public final Object[][] getTableData() {
        Object[][] arr = getTableData0();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i].clone();
        }
        return arr;
    }

    /**
     * 
     * The Iterator iterates in the first column, then the second, then the third,
     * so on
     * 
     * @return An Iterator instance
     */
    @Override
    public TableIterator iterator() {
        return new TableIterator();
    }

    public final class TableIterator implements Iterator<Object> {
        private TableIterator() {
        };

        private int pc = 0;
        private int pr = 0;

        @Override
        public boolean hasNext() {
            if (pc < Table.this.getColCount()) {
                if (pr == (Table.this.getRowCount())) {
                    if (pc == (Table.this.getColCount() - 1)) {
                        return false;
                    }
                    pc++;
                    pr = 0;
                }
                return true;
            }
            return false;
        }

        @Override
        public Object next() {
            if (hasNext()) {
                return Table.this.get(pc, pr++);
            }
            throw new NoSuchElementException("No more elements");
        }

        @Override
        public void forEachRemaining(Consumer<Object> action) {
            if (action != null) {

                if (!(action instanceof TableConsumer)) {
                    Iterator.super.forEachRemaining(action);
                } else {
                    TableConsumer c = (TableConsumer) action;
                    try {
                        while (this.hasNext())
                            c.accept(this.next(), pc, pr - 1);
                    } catch (IteratorStop is) {
                        return;
                    }
                }
            }
            return;
        }

        /**
         * Performs the given action for each remaining element until all elements
         * have been processed or the action throws an exception. Actions are
         * performed in the order of iteration specified in
         * {@link io.github.demnetwork.runtime.io.github.demnetwork.runtime.utils.Table#iterator()
         * Table.iterator()}.
         * <p>
         * 
         * 
         * @param action Action to execute
         * @return <strong>true</strong> if the action completed without using {@link
         *         io.github.demnetwork.runtime.io.github.demnetwork.runtime.utils.Table.TableConsumer#stop()
         *         TableConsumer.stop()}, <strong>false</strong> if the
         *         <strong>action</strong> is null, if it completed using {@link
         *         io.github.demnetwork.runtime.io.github.demnetwork.runtime.utils.Table.TableConsumer#stop()
         *         TableConsumer.stop()} or if the <strong>action</strong> threw an
         *         exception
         */
        public boolean forEachRemaining(TableConsumer action) {
            if (action != null) {
                try {
                    while (this.hasNext())
                        action.accept(this.next(), pc, pr - 1);

                } catch (IteratorStop is) {
                    return false;
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
            return true; // Handle the case when the action does nothing
        }
    }

    @FunctionalInterface
    public static abstract interface TableConsumer extends Consumer<Object> {
        @Override
        public default void accept(Object t) {
            throw new UnsupportedOperationException("Invalid Consumer");
        }

        public abstract void accept(Object o, int col, int row) throws IteratorStop;

        public default void stop() throws IteratorStop {
            throw new IteratorStop();
        }
    }

    public static final class IteratorStop extends Throwable {
        public IteratorStop() {
            super("Iterator Stopped");
        }
    }

    public static final void copy(Table src, Table dest, int fromCol, int toCol, int fromRow, int toRow, int targetRow,
            int targetCol) {
        if (src == null)
            throwNullPointerException("Null Source!");
        if (dest == null)
            throwNullPointerException("Null Destination!");
        chkBounds(src.getColCount(), fromCol);
        chkBounds(src.getColCount(), toCol);
        chkBounds(src.getRowCount(), fromRow);
        chkBounds(src.getRowCount(), toRow);
        chkBounds(dest.getColCount(), targetCol);
        chkBounds(dest.getRowCount(), targetRow);
        chkBounds(dest.getColCount(), targetCol + (toCol - fromCol));
        chkBounds(dest.getRowCount(), targetRow + (toRow - fromRow));
        Class<?>[] destTypes = dest.getParameterTypes();
        Class<?>[] srcTypes = src.getParameterTypes();
        for (int i = fromCol; i < toCol; i++) {
            if (!destTypes[i].isAssignableFrom(srcTypes[i]))
                throwClassCastException("Column Type Mismatch: " + "Cannot cast \'" + srcTypes[i].getName()
                        + "\' to \'" + destTypes[i].getName() + "\'!");
        }
        Object[][] srcData = src.getTableData0();
        Object[][] destData = dest.data;
        for (int col = fromCol; col < toCol; col++)
            for (int row = fromRow; row < toRow; row++) {
                destData[targetCol + (col - fromCol)][targetRow + (row - fromRow)] = srcData[col][row];
            }
    }

    protected static final void chkType(Class<?> reqType, Object o) {
        if (o == null && reqType.isPrimitive()) {
            throwNullPointerException("\'null\' does not represent a primitive type");
        }
        if (o != null) {
            chkType0(reqType, o.getClass());
        }
    }

    protected static final void chkType0(Class<?> reqType, Class<?> type) {
        if (convertClass(reqType).isAssignableFrom(type)) {
            return;
        }

        throwClassCastException(
                "Cannot cast \'" + type.getName() + "\' to \'" + reqType.getName() + "\'!");
    }

    public final void castTo(Class<?>[] colTypes) {
        chkArray(colTypes, this.types.length);
        this.iterator().forEachRemaining(new TableConsumer() {
            @Override
            public void accept(Object o, int col, int row) {
                chkType(colTypes[col], o);
            }
        });
        System.arraycopy(colTypes, 0, types, 0, this.types.length);
    }

    public final void copyTo(Table dest, int fromCol, int toCol, int fromRow, int toRow, int targetCol, int targetRow) {
        copy(this, dest, fromCol, toCol, fromRow, toRow, targetRow, targetCol);
    }

    @Override
    public void forEach(Consumer<Object> action) {
        this.iterator().forEachRemaining(action);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] colToArray(int col, T[] arr) throws IllegalArgumentException {
        if (arr == null) {
            return (T[]) new Object[0];
        }
        if (arr.length < this.getRowCount())
            throw new IllegalArgumentException("This array cannot fit all elements in the column " + col);
        chkType0(arr.getClass().getComponentType(), this.types[col]);
        System.arraycopy(this.data[col], 0, arr, 0, this.data[0].length);
        return arr;
    }

    public int[][] getLocationsOf(Object o) {
        List<int[]> l = new ArrayList<>();
        this.forEach(new TableConsumer() {

            @Override
            public void accept(Object o2, int col, int row) throws IteratorStop {
                if (o != null) {
                    if (o.equals(o2))
                        l.add(new int[] { col, row });
                } else if (o2 != null) {
                } else
                    l.add(new int[] { col, row });
            }

        });
        int[][] arr = new int[l.size()][2];
        for (int p0 = 0; p0 < l.size(); p0++) {
            int[] l2 = l.get(p0);
            for (int p1 = 0; p1 < l2.length; p1++) {
                arr[p0][p1] = l2[p1];
            }
        }
        return arr;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof Table))
            return false;
        if (o == this)
            return true;
        Table t = (Table) o;
        if (t.getColCount() != this.getColCount())
            return false;
        if (t.getRowCount() != this.getRowCount())
            return false;
        return t.iterator().forEachRemaining(new TableConsumer() {

            @Override
            public void accept(Object o, int col, int row) throws IteratorStop {
                Object o2 = Table.this.get(col, row);
                if (o != null) {
                    if (!o.equals(o2))
                        TableConsumer.super.stop();
                } else if (o2 != null) {
                    TableConsumer.super.stop();
                }
            }

        });
    }
}
