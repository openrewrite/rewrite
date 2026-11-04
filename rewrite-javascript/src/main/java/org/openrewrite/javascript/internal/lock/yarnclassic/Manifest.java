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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of yarn src/types.js {@code Manifest} (yarn 1.22.22).
 */
final class Manifest {

    final Map<String, @Nullable Object> data;

    /**
     * {@code _remote}.
     */
    @Nullable
    PackageRemote remote;

    /**
     * {@code _reference}.
     */
    @Nullable
    PackageReference reference;

    boolean fresh;

    /**
     * The lock entry a reused pattern's manifest stands for.
     */
    @Nullable
    Map<String, @Nullable Object> locked;

    Manifest() {
        this(new LinkedHashMap<>());
    }

    Manifest(Map<String, @Nullable Object> data) {
        this.data = data;
    }

    /**
     * {@code Object.assign({}, manifest)}.
     */
    Manifest copy() {
        Manifest copy = new Manifest(new LinkedHashMap<>(data));
        copy.remote = remote;
        copy.reference = reference;
        copy.fresh = fresh;
        copy.locked = locked;
        return copy;
    }

    @Nullable
    Object get(String key) {
        return data.get(key);
    }

    void put(String key, @Nullable Object value) {
        data.put(key, value);
    }

    @Nullable
    Object getName() {
        return data.get("name");
    }

    @Nullable
    Object getVersion() {
        return data.get("version");
    }

    @Nullable
    Object getUid() {
        return data.get("_uid");
    }

    /**
     * A dependency map ({@code dependencies}, {@code optionalDependencies}, ...) when it is an object.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    Map<String, @Nullable Object> getMap(String key) {
        Object value = data.get(key);
        return value instanceof Map ? (Map<String, @Nullable Object>) value : null;
    }

    @Override
    public String toString() {
        return data.get("name") + "@" + data.get("version");
    }
}
