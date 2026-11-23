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
package org.openrewrite.javascript.internal.lock;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * An error JavaScript or Node raises inside a package manager, by its name ({@code TypeError}), with Node's code when
 * it has one; printed {@code <name>: <message>}.
 */
@Getter
public class JsError extends RuntimeException {

    public static final String ERROR = "Error";
    public static final String TYPE_ERROR = "TypeError";
    public static final String RANGE_ERROR = "RangeError";
    public static final String SYNTAX_ERROR = "SyntaxError";
    public static final String URI_ERROR = "URIError";

    private final String name;
    private final @Nullable String code;

    public JsError(String name, String message) {
        this(name, message, null);
    }

    public JsError(String name, String message, @Nullable String code) {
        super(message);
        this.name = name;
        this.code = code;
    }

    public boolean isSyntaxError() {
        return SYNTAX_ERROR.equals(name);
    }

    @Override
    public String toString() {
        return name + ": " + getMessage();
    }
}
