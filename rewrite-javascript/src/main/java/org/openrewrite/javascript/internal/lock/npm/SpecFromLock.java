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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/spec-from-lock.js (npm 11.17.0).
 */
final class SpecFromLock {

    private SpecFromLock() {
    }

    /**
     * @return {@code null} where arborist returns an empty object, for an entry no spec can be read from.
     */
    static @Nullable Spec specFromLock(@Nullable String name, Map<String, Object> lock, @Nullable String where,
                                       Spec.Options npa) {
        try {
            if (truthy(lock.get("version"))) {
                Spec spec = Spec.resolve(name, string(lock.get("version")), where, npa);
                if (truthy(lock.get("integrity")) || spec.getType() == Spec.Type.GIT) {
                    return spec;
                }
            }
            if (truthy(lock.get("from"))) {
                Spec spec = Spec.resolve(name, string(lock.get("from")), where, npa);
                if (spec.isRegistry() && truthy(lock.get("version"))) {
                    return Spec.resolve(name, string(lock.get("version")), where, npa);
                } else if (!truthy(lock.get("resolved"))) {
                    return spec;
                }
            }
            if (truthy(lock.get("resolved"))) {
                return Spec.resolve(name, string(lock.get("resolved")), where, npa);
            }
        } catch (IllegalArgumentException ignored) {
            // fall back to the version alone
        }
        try {
            return Spec.resolve(name, string(lock.get("version")), where, npa);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static @Nullable String string(@Nullable Object val) {
        if (val instanceof String) {
            return (String) val;
        }
        if (!truthy(val)) {
            return null;
        }
        throw new IllegalArgumentException("not a string: " + val);
    }
}
