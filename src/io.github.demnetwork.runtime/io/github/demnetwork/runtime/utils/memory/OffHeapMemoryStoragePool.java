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

import io.github.demnetwork.runtime.internal.Placeholders;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class OffHeapMemoryStoragePool {
    private final ConcurrentHashMap<ReutilizableOffHeapMemoryStorage, Void> pool;
    public final int maxCapacity;
    private final long size;
    private volatile boolean disposed = false;
    private final Object LOCK = new Object();
    private static final Void VOID_PLACEHOLDER;

    static {
        VOID_PLACEHOLDER = (Void) Placeholders.getPlaceholder(Void.class);
        if (VOID_PLACEHOLDER == null)
            throw new ExceptionInInitializerError("Unable to instantiate a Placeholder");
    }

    public OffHeapMemoryStoragePool(long size, int maxCapacity) {
        if (maxCapacity > 0) {
            this.pool = new ConcurrentHashMap<>(maxCapacity);
        } else if (maxCapacity == 0) {
            this.pool = new ConcurrentHashMap<>();
        } else
            throw new IllegalArgumentException("Illegal Max Capacity");
        if (size < 1)
            throw new IllegalArgumentException("Illegal Size");
        this.size = size;
        this.maxCapacity = maxCapacity;
    }

    /**
     * Retrieves an unused {@link OffHeapMemoryStorage} instance from the pool.
     * If no reusable instance is available, it attempts to create a new one.
     * If creating a new instance would exceed the {@link #maxCapacity} of this
     * pool,
     * an {@link IllegalStateException} will be thrown.
     *
     * @return an available or newly created {@link OffHeapMemoryStorage} instance
     * @throws IllegalStateException if the pool has reached its maximum capacity
     *                               and no reusable instance is available
     *
     * @apiNote
     *          <b>DO NOT</b> call {@code pool.getInstance()} to "revive" or reuse a
     *          previously acquired instance,
     *          as this method may return a different object than the one you
     *          expect.
     *          <p>
     *          Always assign the result of this method to a variable when obtaining
     *          a new instance:
     *
     *          <pre>{@code
     * <modifiers> OffHeapMemoryStorage <variableName> = pool.getInstance();
     * }</pre>
     *
     *          Replace {@code <modifiers>} with appropriate modifiers (e.g.,
     *          {@code final}) or remove if unnecessary.
     *          Replace {@code <variableName>} with a meaningful identifier.
     *          Replace {@code pool} with the variable referencing your
     *          {@link OffHeapMemoryStoragePool}.
     */
    public OffHeapMemoryStorage getInstance() {
        synchronized (LOCK) {
            ensureValid();
            InstanceFinder instanceF = new InstanceFinder();
            try {
                pool.forEach(instanceF);
            } catch (IllegalArgumentException ignored) {
                // Ignored because exception used to break outside the loop
            }
            ReutilizableOffHeapMemoryStorage storage = instanceF.instance;
            if (storage == null) {
                if (pool.size() >= this.maxCapacity && maxCapacity != 0)
                    throw new IllegalStateException(
                            "Unable to find an available instance and there is no space to add the new instance to the pool.");
                storage = new ReutilizableOffHeapMemoryStorage(size, true);
                pool.put(storage, VOID_PLACEHOLDER);
                return storage;
            } else {
                return storage;
            }
        }
    }

    private final class InstanceFinder implements BiConsumer<ReutilizableOffHeapMemoryStorage, Void> {
        private ReutilizableOffHeapMemoryStorage instance = null;

        @Override
        public void accept(ReutilizableOffHeapMemoryStorage storage, Void ignored) {
            if (storage != null)
                if (!storage.isInUse()) {
                    storage.reutilize();
                    this.instance = storage;
                    throw new IllegalArgumentException("Ignore me, please"); // Break outside the loop
                }
        }
    }

    public void dispose() {
        synchronized (LOCK) {
            if (disposed)
                return;
            this.disposed = true;
            pool.forEach(new BiConsumer<>() {

                @Override
                public void accept(ReutilizableOffHeapMemoryStorage storage, Void unused) {
                    storage.dispose();
                }

            });
            pool.clear();
        }
    }

    private void ensureValid() {
        if (disposed)
            throw new IllegalStateException("Disposed Instance");
    }
}
