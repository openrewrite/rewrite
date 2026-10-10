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
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.regex.Pattern;

/**
 * A JavaScript regular expression source as V8 reads it without the {@code u} flag, as a {@link Pattern}.
 */
final class JsRegExp {

    private JsRegExp() {
    }

    /**
     * {@code new RegExp(source)}: null when V8 throws its SyntaxError.
     */
    static @Nullable Pattern compile(String source) {
        return V8RegExp.compile(source);
    }

    /**
     * A regular expression literal of the ported module's source.
     */
    static Pattern of(String source) {
        Pattern pattern = compile(source);
        if (pattern == null) {
            throw new IllegalArgumentException("V8 rejects /" + source + "/");
        }
        return pattern;
    }
}
