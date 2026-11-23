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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.EcmaScript.JsonSyntaxError;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.List;
import java.util.Map;

/**
 * Yarn's plain objects as the port holds them: property reads and {@code JSON.parse}.
 */
final class Js extends EcmaScript {

    /**
     * {@code JSON.parse} rejecting its input.
     */
    static final class SyntaxError extends RuntimeException {
        SyntaxError(String message) {
            super(message);
        }
    }

    private Js() {
    }

    /**
     * {@code value[key]}: the property, or {@link Syml#UNDEFINED} when the value is not an object or lacks it.
     */
    static @Nullable Object get(@Nullable Object value, String key) {
        if (value instanceof Map && ((Map<?, ?>) value).containsKey(key)) {
            return ((Map<?, ?>) value).get(key);
        }
        return Syml.UNDEFINED;
    }

    /**
     * {@code value[key]} where reading a property of {@code undefined} or {@code null} is V8's TypeError.
     */
    static @Nullable Object read(@Nullable Object value, String key) {
        if (value == null || value == Syml.UNDEFINED) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of " +
                                                  (value == null ? "null" : "undefined") + " (reading '" + key + "')");
        }
        return get(value, key);
    }

    static boolean isObject(@Nullable Object value) {
        return value instanceof Map || value instanceof List;
    }

    /**
     * {@code JSON.parse}, objects with their keys in the order JavaScript gives them and numbers as doubles.
     */
    public static @Nullable Object jsonParse(String text) {
        try {
            return EcmaScript.jsonParse(text);
        } catch (JsonSyntaxError e) {
            throw new SyntaxError(e.getMessage());
        }
    }
}
