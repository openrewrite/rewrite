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

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.openrewrite.internal.lang.Nullable;

public class JavaTypeCache {
    PatriciaTrie<Object> typeCache = new PatriciaTrie<>();

    @Nullable
    public <T> T get(String signature) {
        //noinspection unchecked
        return (T) typeCache.get(signature);
    }

    public void put(String signature, Object o) {
        typeCache.put(signature, o);
    }

    public void clear() {
        typeCache.clear();
    }

    public int size() {
        return typeCache.size();
    }
}
