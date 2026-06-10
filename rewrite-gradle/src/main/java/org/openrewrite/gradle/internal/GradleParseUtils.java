/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.internal;

import org.openrewrite.SourceFile;
import org.openrewrite.tree.ParseError;

import java.util.function.Function;

public final class GradleParseUtils {
    private GradleParseUtils() {
    }

    /**
     * A mapping {@link Function} for use in a {@code stream.map(...)} / {@code optional.map(...)} chain over the
     * result of parsing a generated Gradle code snippet, in place of {@code SomeType.class::cast}.
     * <p>
     * Recipes parse constant, valid Gradle snippets, so a {@link ParseError} here means the underlying Groovy or
     * Kotlin parser threw while parsing the snippet. Rethrow that real cause — its
     * {@link org.openrewrite.ParseExceptionResult} carries the full stack trace — instead of letting the cast mask
     * it with an opaque {@link ClassCastException} ("Cannot cast ParseError to ...").
     *
     * @param expected the {@link SourceFile} subtype the snippet is expected to parse into
     * @return a function that casts to {@code expected}, or throws with the underlying parse failure as its cause
     */
    public static <S extends SourceFile> Function<SourceFile, S> requireParsed(Class<S> expected) {
        return sourceFile -> {
            if (sourceFile instanceof ParseError) {
                throw new IllegalStateException(
                        "Failed to parse generated Gradle code as " + expected.getSimpleName(),
                        ((ParseError) sourceFile).toException());
            }
            return expected.cast(sourceFile);
        };
    }
}
