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

import io.github.demnetwork.runtime.internal.MeIDontLikeLambdas;
import io.github.demnetwork.runtime.utils.memory.reflect.MemoryObjectFieldInspector.FieldFilter;

@MeIDontLikeLambdas
public final class FieldFilterBuilder {
    private FieldFilter current;

    public FieldFilterBuilder(FieldFilter initial) {
        this.current = initial;
    }

    public FieldFilterBuilder and(FieldFilter next) {
        this.current = MemoryObjectFieldInspector.and(this.current, next);
        return this;
    }

    public FieldFilterBuilder nand(FieldFilter next) {
        this.current = MemoryObjectFieldInspector.nand(this.current, next);
        return this;
    }

    public FieldFilterBuilder or(FieldFilter next) {
        this.current = MemoryObjectFieldInspector.or(this.current, next);
        return this;
    }

    public FieldFilterBuilder nor(FieldFilter next) {
        this.current = MemoryObjectFieldInspector.nor(this.current, next);
        return this;
    }

    public FieldFilterBuilder xor(FieldFilter next) {
        this.current = MemoryObjectFieldInspector.xor(this.current, next);
        return this;
    }

    public FieldFilterBuilder xnor(FieldFilter next) {
        this.current = MemoryObjectFieldInspector.xnor(this.current, next);
        return this;
    }

    public FieldFilterBuilder not() {
        this.current = MemoryObjectFieldInspector.not(this.current);
        return this;
    }

    public FieldFilter build() {
        return this.current;
    }
}
