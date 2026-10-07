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

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * An error JavaScript or Node raises inside yarn, which yarn prints as {@code <name>: <message>}.
 */
@Getter
class JsError extends RuntimeException {

    static final String SYNTAX_ERROR = "SyntaxError";
    static final String TYPE_ERROR = "TypeError";
    static final String ERROR = "Error";
    static final String INVARIANT_VIOLATION = "Invariant Violation";
    static final String INVALID_ARG_TYPE = "TypeError [ERR_INVALID_ARG_TYPE]";

    private final String name;

    JsError(String name, String message) {
        super(message);
        this.name = name;
    }

    /**
     * Node's {@code ERR_INVALID_ARG_TYPE} for a value yarn read from JSON, whose objects have no prototype.
     */
    static JsError invalidArgType(String expected, @Nullable Object received) {
        return new JsError(INVALID_ARG_TYPE, expected + ". Received " + specificType(received));
    }

    private static String specificType(@Nullable Object value) {
        if (value == null) {
            return "undefined";
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty() ? "[Object: null prototype] {}" : "[Object: null prototype]";
        }
        if (value instanceof List) {
            return "an instance of Array";
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return "type number (" + (d == 0 && 1 / d < 0 ? "-0" : Js.numberToString(d)) + ")";
        }
        return "type " + Js.typeOf(value) + " (" + (value instanceof String ? "'" + value + "'" : value) + ")";
    }

    boolean isSyntaxError() {
        return SYNTAX_ERROR.equals(name);
    }

    @Override
    public String toString() {
        return name + ": " + getMessage();
    }
}
