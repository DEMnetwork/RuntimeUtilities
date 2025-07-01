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

package io.github.demnetwork.runtime.misc;

import io.github.demnetwork.runtime.utils.RuntimeClassGenerator;

public abstract class RuntimeResourceProvider {

    protected static final int RCG_IMPL_ID = 104745;

    public abstract Class<? extends Implentation> getImpl(int id);

    public static abstract interface Implentation {
        public abstract int getID();

        public abstract String getName();
    }

    protected static abstract class RCGImpl extends RuntimeClassGenerator implements Implentation {

        protected RCGImpl(String target, String pkg, String className, int Modifiers) {
            super(target, pkg, className, Modifiers);
        }

        @Override
        public final int getID() {
            return RCG_IMPL_ID;
        }

    }

    public abstract Implentation getInstance(Class<? extends Implentation> c, Object[] args)
            throws InstantiationException;

    public abstract Object[] resolveDependancy(String name);
}
