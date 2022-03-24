/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.marker.ci;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;

public final class POSIXUtil {
    private static final C c = Native.load("c", C.class);

    private interface C extends Library {
        @SuppressWarnings("UnusedReturnValue")
        int gethostname(byte[] name, int size_t) throws LastErrorException;
    }

    public static String getHostName() {
        byte[] hostname = new byte[256];
        c.gethostname(hostname, hostname.length);
        return Native.toString(hostname);
    }
}
