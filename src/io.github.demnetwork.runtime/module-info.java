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

module io.github.demnetwork.runtime {
    exports io.github.demnetwork.runtime.access;
    exports io.github.demnetwork.runtime.classloader;
    exports io.github.demnetwork.runtime.utils;
    exports io.github.demnetwork.runtime.misc;
    exports io.github.demnetwork.runtime.utils.memory;
    exports io.github.demnetwork.runtime.utils.wrapper;
    exports io.github.demnetwork.runtime.misc.serial;
    exports io.github.demnetwork.runtime.math;
    exports io.github.demnetwork.runtime.internal;
    exports io.github.demnetwork.runtime.inject;

    opens io.github.demnetwork.runtime.misc.serial;
    opens io.github.demnetwork.runtime.misc;
    opens io.github.demnetwork.runtime.utils;
    opens io.github.demnetwork.runtime.utils.memory;
    opens io.github.demnetwork.runtime.utils.wrapper;
    opens io.github.demnetwork.runtime.access;
    opens io.github.demnetwork.runtime.classloader;
    opens io.github.demnetwork.runtime.math;

    requires java.base;
    requires jdk.unsupported;
    requires jdk.compiler;
}