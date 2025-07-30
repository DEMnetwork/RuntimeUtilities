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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import io.github.demnetwork.runtime.internal.BuildData;

public non-sealed class FileMappedOffHeapMemoryStorage extends OffHeapMemoryStorage {
    protected final File file;
    protected final RandomAccessFile raf;
    protected final FileChannel channel;
    protected final MappedByteBuffer mapped;

    public FileMappedOffHeapMemoryStorage(File file, long size) throws IOException {
        super(mapBaseAddress(file, size), size);
        this.file = file;
        this.raf = new RandomAccessFile(file, "rw");
        this.channel = raf.getChannel();
        this.raf.setLength(size);
        this.mapped = channel.map(
                FileChannel.MapMode.READ_WRITE, 0, size);
    }

    protected static long mapBaseAddress(File file, long size) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(size);
            FileChannel ch = raf.getChannel();
            MappedByteBuffer mbb = ch.map(
                    FileChannel.MapMode.READ_WRITE, 0, size);
            Field addressField = Buffer.class.getDeclaredField("address");
            return UNSAFE.getLong(mbb, UNSAFE.objectFieldOffset(addressField)); // Avoid expensive reflection
        } catch (Exception e) {
            throw new IOException("Failed to map file", e);
        }
    }

    @Override
    public void close() {
        if (super.closed)
            return;
        try {
            UNSAFE.invokeCleaner(this.mapped);
        } catch (RuntimeException e) {
            if (BuildData.CURRENT.getDebugStatus()) {
                System.out.println("Exception occured");
                e.printStackTrace(System.out);
            }
        }
        try {
            this.channel.close();
            this.raf.close();
        } catch (IOException e) {
            if (BuildData.CURRENT.getDebugStatus()) {
                System.out.println("Exception occured");
                e.printStackTrace(System.out);
            }
        }
        super.closed = true;
        super.monitor.onClose();
    }

    public File getFile() {
        this.ensureOpen();
        return this.file;
    }
}
