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

package io.github.demnetwork.runtime.utils.memory.serial;

import static io.github.demnetwork.runtime.utils.memory.serial.MemorySerializableRegistry.UNSAFE;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * <strong>Note:</strong> This class assumes that input streams passed to
 * {@code readObj}
 * are trusted and well-formed. It does not perform validation or enforce
 * structural
 * integrity of the input.
 * <p>
 * It is the caller's responsibility to ensure the stream is safe and correctly
 * formatted.
 * Passing untrusted or malformed streams may lead to memory corruption,
 * deserialization issues,
 * or security vulnerabilities.
 */
public final class StringWrapper implements MemorySerializable, Wrapper<String> {
    private final String str;

    public StringWrapper(String str) {
        if (str == null)
            throw new NullPointerException();
        this.str = str;
    }

    @Override
    public String getValue() {
        return this.str;
    }

    @Override
    public void writeObj(MemoryOutputStream mos) throws IOException {
        byte[] bytes = str.getBytes(Charset.forName("UTF-16BE"));
        int len = bytes.length;
        byte[] lenBytes = MemoryUtils.toBytes(len);
        mos.write(lenBytes, 4, 4);
        mos.write(bytes);
    }

    @Override
    public MemorySerializable readObj(MemoryInputStream mis) throws IOException {
        byte[] lenBytes = new byte[4];
        int readLen = mis.read(lenBytes, 0, 4);
        if (readLen != 4)
            throw new IOException("Reading length failed");
        int l = (int) MemoryUtils.toLong(new byte[] { 0, 0, 0, 0, lenBytes[0], lenBytes[1], lenBytes[2], lenBytes[3] });

        byte[] buf = new byte[Math.min(6144, l)];
        StringBuilder sb = new StringBuilder(l);
        while (l > 0) {
            int r = mis.read(buf, 0, Math.min(6144, l));
            if (r == -1)
                throw new IOException("Unexpected EOF during string read");
            sb.append(new String(buf, 0, r, "UTF-16BE"));
            l -= r;
        }

        String result = sb.toString();
        try {
            UNSAFE.putObject(this, UNSAFE.objectFieldOffset(StringWrapper.class.getDeclaredField("str")), result);
            return this;
        } catch (ReflectiveOperationException e) {
            return new StringWrapper(result); // Fallback
        }
    }

    @Override
    public String toString() {
        return this.str;
    }

    @Override
    public boolean equals(Object x) {
        if (x == null)
            return false;
        if (x == this)
            return true;
        if (!(x instanceof StringWrapper))
            return false;
        return ((StringWrapper) x).str.equals(this.str);
    }

}
