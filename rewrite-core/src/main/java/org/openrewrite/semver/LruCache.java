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
package org.openrewrite.semver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory for the bounded, access-order LRU maps used to memoize the pure (but regex-heavy) results
 * of semver parsing and selector validation. Recipe runs evaluate a recipe-constant version selector
 * against the same recurring version strings across thousands of dependencies and visits, so caching
 * collapses that work to a handful of computations.
 * <p>
 * The returned map is {@code synchronized} (recipes run on a {@link java.util.concurrent.ForkJoinPool})
 * and evicts the least-recently-used entry once {@code maxSize} is exceeded, capping the retained
 * footprint in a long-lived process.
 */
final class LruCache {

    private LruCache() {
    }

    static <K, V> Map<K, V> bounded(int maxSize) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        });
    }
}
