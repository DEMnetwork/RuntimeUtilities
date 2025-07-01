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

package io.github.demnetwork.runtime.math;

import java.util.Arrays;

import io.github.demnetwork.runtime.access.EnforceModifiers;
import io.github.demnetwork.runtime.access.InvisibleField;
import io.github.demnetwork.runtime.utils.Table;

public final class ComplexNumber implements Comparable<ComplexNumber> {
    @EnforceModifiers
    private final double a;
    @EnforceModifiers
    private final double b;
    @InvisibleField
    private static final Table INSTANCES = new Table(3, double.class, double.class, ComplexNumber.class);
    @InvisibleField
    private static final Object LOCK = new Object();
    @InvisibleField
    private static int nrow;
    public static final ComplexNumber I = new ComplexNumber(0, 1);
    public static final ComplexNumber ONE = new ComplexNumber(1, 0);
    public static final ComplexNumber TWO = new ComplexNumber(2, 0);
    public static final ComplexNumber THREE = new ComplexNumber(3, 0);
    public static final ComplexNumber FOUR = new ComplexNumber(4, 0);
    public static final ComplexNumber FIVE = new ComplexNumber(5, 0);
    public static final ComplexNumber SIX = new ComplexNumber(6, 0);
    public static final ComplexNumber SEVEN = new ComplexNumber(7, 0);
    public static final ComplexNumber EIGHT = new ComplexNumber(8, 0);
    public static final ComplexNumber NINE = new ComplexNumber(9, 0);
    public static final ComplexNumber ZERO = new ComplexNumber(0, 0);

    private ComplexNumber(double real, double imaginary) {
        this.a = real;
        this.b = imaginary;
        synchronized (LOCK) {
            int r = nrow++;
            INSTANCES.set(real, 0, r);
            INSTANCES.set(imaginary, 1, r);
            INSTANCES.set(this, 2, r);
        }
    }

    @Override
    public String toString() {
        return a + "+(" + b + "i)";
    }

    public ComplexNumber add(double d) {
        return getInstance(this.a + d, this.b);
    }

    public boolean isReal() {
        return (this.b == 0);
    }

    public double toRealDouble() throws IllegalStateException {
        if (!isReal())
            throw new IllegalStateException("This ComplexNumber is not real");
        return this.a;
    }

    public ComplexNumber subtract(double d) {
        return add(-d);
    }

    public ComplexNumber multiply(double d) {
        return getInstance(this.a * d, this.b * d);
    }

    public ComplexNumber divide(double d) {
        if (d == 0)
            throw new ArithmeticException("Cannot divide by 0");
        return getInstance(this.a / d, this.b / d);
    }

    public ComplexNumber add(ComplexNumber b) {
        if (b == null)
            throw new NullPointerException();
        return getInstance(this.a + b.a, this.b + b.b);
    }

    public ComplexNumber subtract(ComplexNumber b) {
        if (b == null)
            throw new NullPointerException();
        return getInstance(this.a - b.a, this.b - b.b);
    }

    public ComplexNumber multiply(ComplexNumber b) {
        if (b == null)
            throw new NullPointerException();
        return getInstance((this.a * b.a - this.b * b.b), (this.a * b.b + this.b * b.a));
    }

    public ComplexNumber divide(ComplexNumber b) {
        if (b == null)
            throw new NullPointerException();
        if (b.isReal() && b.toRealDouble() == 0.0)
            throw new ArithmeticException("Cannot divide by " + b.toString());
        return getInstance((this.a * b.a + this.b * b.b) / (Math.pow(b.a, 2) + Math.pow(b.b, 2)),
                (this.b * b.a - this.a * b.b) / (Math.pow(b.a, 2) + Math.pow(b.b, 2)));
    }

    public ComplexNumber raiseToPower(long i) {
        if (this.isReal()) {
            if (this.toRealDouble() == 0 && i < 0)
                throw new ArithmeticException("Cannot raise 0 to a negative number");
            return getInstance(Math.pow(this.a, i), 0);
        } else {
            double r = Math.sqrt(Math.pow(this.a, 2) + Math.pow(this.b, 2));
            double arg = Math.atan2(this.b, this.a);
            double ri = Math.pow(r, i);
            return getInstance(ri * Math.cos(i * arg), ri * Math.sin(i * arg));
        }
    }

    public ComplexNumber raiseToPower(double d) {
        if (this.isReal()) {
            if (this.toRealDouble() == 0 && d < 0)
                throw new ArithmeticException("Cannot raise 0 to a negative number");
            return getInstance(Math.pow(this.a, d), 0);
        } else {
            return this.raiseToPower0(d, 0);
        }
    }

    private ComplexNumber raiseToPower0(double d, long k) {
        double r = Math.sqrt(Math.pow(this.a, 2) + Math.pow(this.b, 2));
        double arg = Math.atan2(this.b, this.a) + (2 * Math.PI * k);
        double rd = Math.pow(r, d);
        return getInstance(rd * Math.cos(d * arg), rd * Math.sin(d * arg));
    }

    public ComplexNumber raiseToPower(double d, long k) {
        if (Double.isFinite(d))
            return this.raiseToPower0(d, k);
        throw new ArithmeticException("Cannot raise to " + d);
    }

    public static ComplexNumber getInstance(double a, double b) {
        int[][] arr = INSTANCES.getLocationsOf(a);
        int[][] arr2 = INSTANCES.getLocationsOf(b);
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr2.length; j++) {
                int[] arr3 = arr2[j];
                if (Arrays.equals(arr[i], arr3))
                    return (ComplexNumber) INSTANCES.get(2, arr3[1]);
            }
        }
        return new ComplexNumber(a, b);
    }

    @Override
    public boolean equals(Object anObject) {
        if (anObject == this)
            return true;
        if (!(anObject instanceof ComplexNumber)) // null instanceof ComplexNumber is always false
            return false;
        ComplexNumber cn = (ComplexNumber) anObject;
        return (cn.a == this.a && cn.b == this.b);
    }

    @Override
    public int hashCode() {
        int hash = 499;
        hash = hash * (hash + Double.hashCode(this.a));
        hash = hash * (hash + Double.hashCode(this.b));
        return hash;
    }

    @Override
    public int compareTo(ComplexNumber cn) {
        if (this.equals(cn))
            return 0;
        double m0 = Math.pow(this.a, 2) + Math.pow(this.b, 2);
        double m1 = Math.pow(cn.a, 2) + Math.pow(cn.b, 2);
        if (m0 > m1) {
            return 1;
        } else
            return -1;
    }
}
