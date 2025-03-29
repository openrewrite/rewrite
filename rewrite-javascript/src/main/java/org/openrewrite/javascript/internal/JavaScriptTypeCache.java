/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.javascript.internal;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class JavaScriptTypeCache implements Cloneable {

    Map<Object, Object> typeCache = new HashMap<>();

    public <T> @Nullable T get(String signature) {
        //noinspection unchecked
        return (T) typeCache.get(key(signature));
    }

    public void put(String signature, Object o) {
        typeCache.put(key(signature), o);
    }

    private Object key(String signature) {
        return signature;
    }

    public void clear() {
        typeCache.clear();
    }

    public int size() {
        return typeCache.size();
    }

    @Override
    public JavaScriptTypeCache clone() {
        try {
            JavaScriptTypeCache clone = (JavaScriptTypeCache) super.clone();
            clone.typeCache = new HashMap<>(this.typeCache);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
