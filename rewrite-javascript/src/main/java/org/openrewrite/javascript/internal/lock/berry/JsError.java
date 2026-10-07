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

import lombok.Getter;

/**
 * An error JavaScript or Node raises inside Yarn, which Yarn prints as {@code <name>: <message>}.
 */
@Getter
final class JsError extends RuntimeException {

    static final String ERROR = "Error";
    static final String TYPE_ERROR = "TypeError";
    static final String RANGE_ERROR = "RangeError";
    static final String SYNTAX_ERROR = "SyntaxError";

    private final String name;

    JsError(String name, String message) {
        super(message);
        this.name = name;
    }

    @Override
    public String toString() {
        return name + ": " + getMessage();
    }
}
