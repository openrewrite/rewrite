/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.AdaptiveRadixTree;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

public class JavaTypeCache implements Cloneable {

    AdaptiveRadixTree<Object> typeCache = new AdaptiveRadixTree<>();

    @Nullable JavaTypeCache base;

    public <T> @Nullable T get(String signature) {
        //noinspection unchecked
        T result = (T) typeCache.search(getKeyBytes(signature));
        if (result == null && base != null) {
            return base.get(signature);
        }
        return result;
    }

    public void put(String signature, Object o) {
        typeCache.insert(getKeyBytes(signature), o);
    }

    public void clear() {
        typeCache.clear();
        // base is intentionally NOT cleared — it may be shared
    }

    @Override
    public JavaTypeCache clone() {
        try {
            JavaTypeCache clone = (JavaTypeCache) super.clone();
            clone.base = this;
            clone.typeCache = new AdaptiveRadixTree<>();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a new cache with the given base for read-through lookups.
     * Writes go to the new cache's overlay; reads fall through to the base on miss.
     */
    public static JavaTypeCache withBase(JavaTypeCache base) {
        JavaTypeCache cache = new JavaTypeCache();
        cache.base = base;
        return cache;
    }

    private static final @Nullable Field STRING_VALUE;
    private static final @Nullable Field STRING_CODER;
    private static final boolean USE_REFLECTION;

    static {
        Field value;
        Field coder;
        boolean hasCompactStrings = false;

        try {
            // requires: --add-opens java.base/java.lang=ALL-UNNAMED
            value = String.class.getDeclaredField("value");
            value.setAccessible(true);

            try {
                coder = String.class.getDeclaredField("coder");
                coder.setAccessible(true);
                Field compactStrings = String.class.getDeclaredField("COMPACT_STRINGS");
                compactStrings.setAccessible(true);
                hasCompactStrings = compactStrings.getBoolean(null);
            } catch (NoSuchFieldException e) {
                // Java 8 - field doesn't exist
                coder = null;
            }
        } catch (Exception e) {
            value = null;
            coder = null;
        }

        STRING_VALUE = value;
        STRING_CODER = coder;
        USE_REFLECTION = STRING_VALUE != null && STRING_CODER != null && hasCompactStrings;
    }

    /**
     * For ASCII and Latin-1 strings this operation is allocation-free.
     */
    static byte[] getKeyBytes(String s) {
        // Try to get internal representation first
        if (USE_REFLECTION) {
            try {
                //noinspection DataFlowIssue
                byte[] bytes = (byte[]) STRING_VALUE.get(s);
                //noinspection DataFlowIssue
                byte coder = (byte) STRING_CODER.get(s);
                if (coder == 0) {
                    // Latin1, use directly
                    return bytes;
                } else {
                    // UTF-8: append NUL byte to avoid collisions
                    byte[] prefixed = new byte[bytes.length + 1];
                    System.arraycopy(bytes, 0, prefixed, 0, bytes.length);
                    prefixed[bytes.length] = 0;
                    return prefixed;
                }
            } catch (Exception ignored) {
            }
        }

        return s.getBytes(StandardCharsets.UTF_8);
    }
}
