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

package io.github.demnetwork.runtime.internal;

public final class BuildData {
    public final int versionID;
    public final String versionName;
    private volatile boolean debugEnabled = false;
    private final boolean debugAllowed;
    public static final BuildData CURRENT = new BuildData();
    public static final BuildData PREVIOUS = new BuildData(1, "1.0.0", false); // Past Versions don't support debug

    private BuildData() {
        this(2, "1.1.0");
    }

    private BuildData(int versionID, String versionName) {
        this(versionID, versionName, true);
    }

    private BuildData(int versionID, String versionName, boolean debugAllowed) {
        this.versionID = versionID;
        this.versionName = versionName;
        this.debugAllowed = debugAllowed;
    }

    public boolean setDebug(boolean newValue) {
        if (!debugAllowed)
            throw new IllegalStateException("Cannot change debug state for builds that does not support it");
        if (newValue == this.debugEnabled)
            return this.debugEnabled;
        boolean old = debugEnabled;
        debugEnabled = newValue;
        return old;
    }

    public boolean getDebugStatus() {
        if (this != CURRENT)
            throw new IllegalStateException(
                    "Cannot read data about debug on non-current versions; Because they do not affect the current version");
        return this.debugEnabled;
    }

    public static BuildData getPrevious() {
        return PREVIOUS;
    }

    public static BuildData getCurrent() {
        return CURRENT;
    }

    @Override
    public String toString() {
        return "BuildData{version=" + versionID +
                ", name='" + versionName + '\'' +
                ", debugAllowed=" + debugAllowed +
                ", debugEnabled=" + debugEnabled + '}';
    }

    public boolean isDebugAllowed() {
        return debugAllowed;
    }

}
