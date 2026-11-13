/*
 * Copyright 2026 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

/**
 * A type cache that keeps its types in a HashMap, which finds them faster than the radix tree but takes more memory,
 * as suits a cache that lives for one test.
 */
public class HashMapJavaTypeCache extends JavaTypeCache {
    private static volatile boolean forParsers;

    private Map<String, Object> types = new HashMap<>();

    public HashMapJavaTypeCache() {
    }

    /**
     * @param base A cache to look a type up in when this one doesn't hold it.
     */
    public HashMapJavaTypeCache(JavaTypeCache base) {
        this.base = base;
    }

    /**
     * Makes parser builders build their parsers with this cache, as {@link #forParser} describes. Test support calls it.
     */
    public static void useForParsers() {
        forParsers = true;
    }

    /**
     * @param configured Whether the parser builder's caller gave it this cache or a type factory.
     * @return The cache a parser builder builds its parsers with: once {@link #useForParsers()} has been called, one of
     * this kind that reads through to {@code cache}, unless the caller configured the builder's types.
     */
    public static JavaTypeCache forParser(JavaTypeCache cache, boolean configured) {
        return forParsers && !configured && !(cache instanceof HashMapJavaTypeCache) ?
                new HashMapJavaTypeCache(cache) : cache;
    }

    @Override
    public <T> @Nullable T get(String signature) {
        //noinspection unchecked
        T result = (T) types.get(signature);
        if (result == null && base != null) {
            return base.get(signature);
        }
        return result;
    }

    @Override
    public void put(String signature, Object o) {
        types.put(signature, o);
    }

    @Override
    public void clear() {
        types.clear();
    }

    @Override
    public HashMapJavaTypeCache clone() {
        HashMapJavaTypeCache clone = (HashMapJavaTypeCache) super.clone();
        clone.types = new HashMap<>();
        return clone;
    }
}
