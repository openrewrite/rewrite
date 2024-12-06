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
import org.openrewrite.Incubating;
import org.openrewrite.internal.AdaptiveRadixTree;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

@Incubating(since = "8.38.0")
public class AdaptiveRadixJavaTypeCache extends JavaTypeCache {

    AdaptiveRadixTree<Object> typeCache = new AdaptiveRadixTree<>();

    @Override
    public <T> @Nullable T get(String signature) {
        //noinspection unchecked
        return (T) typeCache.search(getKeyBytes(signature));
    }

    @Override
    public void put(String signature, Object o) {
        typeCache.insert(getKeyBytes(signature), o);
    }

    @Override
    public void clear() {
        typeCache.clear();
    }

    @Override
    public AdaptiveRadixJavaTypeCache clone() {
        AdaptiveRadixJavaTypeCache clone = (AdaptiveRadixJavaTypeCache) super.clone();
        clone.typeCache = this.typeCache.copy();
        return clone;
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
