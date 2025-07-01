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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class OffHeapMemoryInstanceMonitor {
    final Set<AutoCloseable> streams;
    final WeakReference<OffHeapMemoryStorage> storage;

    OffHeapMemoryInstanceMonitor(OffHeapMemoryStorage storage) {
        this.storage = new WeakReference<OffHeapMemoryStorage>(storage);
        this.streams = Collections.newSetFromMap(new ConcurrentHashMap<AutoCloseable, Boolean>());
    }

    void addStream(AutoCloseable stream) {
        this.streams.add(stream);
    }

    synchronized void onClose() {
        for (AutoCloseable ac : Set.copyOf(streams)) {
            try {
                ac.close();
            } catch (Exception e) {
                // Ignored
            } finally {
                streams.remove(ac);
            }
        }
    }
}
