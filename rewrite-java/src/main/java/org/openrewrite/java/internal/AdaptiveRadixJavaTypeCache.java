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

@Incubating(since = "8.38.0")
public class AdaptiveRadixJavaTypeCache extends JavaTypeCache {

    AdaptiveRadixTree<Object> typeCache = new AdaptiveRadixTree<>();

    @Override
    public <T> @Nullable T get(String signature) {
        //noinspection unchecked
        return (T) typeCache.search(signature);
    }

    @Override
    public void put(String signature, Object o) {
        typeCache.insert(signature, o);
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
}
