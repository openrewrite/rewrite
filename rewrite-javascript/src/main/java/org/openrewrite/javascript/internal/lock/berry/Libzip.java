/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.berry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Runs the Java-8 AOT translation of Yarn's libzip/zlib-ng WASM, without a runtime dependency or subprocess. */
final class Libzip {
    private static final class Code extends ClassLoader {
        static final Class<?> TYPE = new Code().load();
        static final Map<String, Method> EXPORTS = new HashMap<>();
        static {
            for (Method method : TYPE.getMethods()) EXPORTS.put(method.getName(), method);
        }

        Code() {
            super(Libzip.class.getClassLoader());
        }

        Class<?> load() {
            try (InputStream in = new GZIPInputStream(Libzip.class.getResourceAsStream("libzip-3.0.0.class.gz"))) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                byte[] bytes = out.toByteArray();
                return defineClass("org.openrewrite.javascript.internal.lock.berry.LibzipWasm", bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private Object instance;
    private ByteBuffer memory;

    private void initialize(int capacity) {
        memory = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle[] imports = new MethodHandle[6];
            imports[0] = lookup.bind(this, "setTempRet0", MethodType.methodType(void.class, int.class));
            imports[1] = lookup.bind(this, "time", MethodType.methodType(int.class, int.class));
            imports[2] = lookup.bind(this, "memcpy", MethodType.methodType(int.class, int.class, int.class, int.class));
            imports[3] = lookup.bind(this, "resize", MethodType.methodType(int.class, int.class));
            imports[4] = lookup.bind(this, "gmtime", MethodType.methodType(int.class, int.class, int.class));
            imports[5] = lookup.bind(this, "timegm", MethodType.methodType(int.class, int.class));
            instance = Code.TYPE.getConstructor(ByteBuffer.class, MethodHandle.class, MethodHandle.class,
                    MethodHandle.class, MethodHandle.class, MethodHandle.class, MethodHandle.class)
                    .newInstance(memory, imports[0], imports[1], imports[2], imports[3], imports[4], imports[5]);
            call("h");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot initialize libzip JVM code", e);
        }
    }

    private void setTempRet0(int value) {
        // The exposed libzip APIs use the low word only; all archive sizes here fit i32.
    }

    private int time(int pointer) {
        if (pointer != 0) memory.putInt(pointer, 456789000);
        return 456789000;
    }

    private int memcpy(int destination, int source, int length) {
        System.arraycopy(memory.array(), source, memory.array(), destination, length);
        return destination;
    }

    private int resize(int requested) {
        if (requested < 0 || requested > memory.capacity()) return 0;
        memory.limit((requested + 65535) / 65536 * 65536);
        return 1;
    }

    private int gmtime(int pointer, int result) {
        LocalDateTime t = LocalDateTime.ofEpochSecond(memory.getInt(pointer), 0, ZoneOffset.UTC);
        int[] fields = {t.getSecond(), t.getMinute(), t.getHour(), t.getDayOfMonth(), t.getMonthValue() - 1,
                t.getYear() - 1900, t.getDayOfWeek().getValue() % 7, t.getDayOfYear() - 1, 0, 0, 0};
        for (int n = 0; n < fields.length; n++) memory.putInt(result + n * 4, fields[n]);
        return result;
    }

    private int timegm(int pointer) {
        LocalDateTime t = LocalDateTime.of(memory.getInt(pointer + 20) + 1900, 1, 1, 0, 0)
                .plusMonths(memory.getInt(pointer + 16)).plusDays(memory.getInt(pointer + 12) - 1)
                .plusHours(memory.getInt(pointer + 8)).plusMinutes(memory.getInt(pointer + 4))
                .plusSeconds(memory.getInt(pointer));
        return (int) t.toEpochSecond(ZoneOffset.UTC);
    }

    private int call(String export, long... args) {
        Object[] boxed = new Object[args.length];
        for (int n = 0; n < args.length; n++) boxed[n] = (int) args[n];
        try {
            Object result = Code.EXPORTS.get(export).invoke(instance, boxed);
            return result == null ? 0 : ((Number) result).intValue();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private int buffer(byte[] data) {
        int p = call("D", Math.max(1, data.length));
        if (p == 0) throw new IllegalStateException("libzip allocation failed");
        System.arraycopy(data, 0, memory.array(), p, data.length);
        return p;
    }

    byte[] archive(Map<String, ZipArchive.Entry> entries, Object level) {
        long bytes = 0;
        for (Map.Entry<String, ZipArchive.Entry> entry : entries.entrySet()) bytes += entry.getValue().data.length + 1024;
        long capacity = ((16L * 1024 * 1024 + bytes * 4 + 65535) / 65536) * 65536;
        if (capacity > Integer.MAX_VALUE - 65536) throw new IllegalArgumentException("Archive too large for libzip");
        initialize((int) capacity);
        // An empty EOCD, as ZipFS.makeEmptyArchive.
        byte[] empty = new byte[22];
        empty[0] = 0x50; empty[1] = 0x4b; empty[2] = 5; empty[3] = 6;
        int error = call("o");
        int source = call("R", buffer(empty), empty.length, 0, 1, error);
        int zip = call("O", source, 0, error);
        if (zip == 0) throw new IllegalStateException("libzip could not open empty archive");
        boolean closed = false;
        try {
            for (Map.Entry<String, ZipArchive.Entry> entry : entries.entrySet()) {
                byte[] name = (entry.getKey() + "\0").getBytes(StandardCharsets.UTF_8);
                int namePtr = buffer(name);
                int index;
                try {
                    if (entry.getKey().endsWith("/")) {
                        index = call("u", zip, namePtr, 0);
                    } else {
                        byte[] data = entry.getValue().data;
                        int content = call("Q", zip, buffer(data), data.length, 0, 1);
                        index = call("B", zip, namePtr, content, 8192);
                        if (!"mixed".equals(level)) check(call("P", zip, index, 0, 8, ((Number) level).intValue()));
                    }
                } finally {
                    call("C", namePtr);
                }
                check(index);
                check(call("G", zip, index, 0, 0, 3, ((long) entry.getValue().mode << 16) & 0xffffffffL));
                check(call("H", zip, index, 0, 456789000, 0));
            }
            call("U", source);
            check(call("s", zip));
            closed = true;
            check(call("V", source));
            check(call("F", source, 0, 0, 2));
            int length = call("X", source);
            check(call("F", source, 0, 0, 0));
            int result = call("D", length);
            try {
                if (call("M", source, result, length, 0) != length) throw new IllegalStateException("libzip short read");
                byte[] data = new byte[length];
                System.arraycopy(memory.array(), result, data, 0, length);
                return data;
            } finally {
                call("C", result);
                call("S", source);
                call("T", source);
            }
        } finally {
            if (!closed) call("v", zip);
        }
    }

    private static void check(int rc) {
        if (rc < 0) throw new IllegalStateException("libzip operation failed");
    }
}
