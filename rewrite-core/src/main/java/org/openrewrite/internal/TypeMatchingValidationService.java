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
package org.openrewrite.internal;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lets the test framework check that a recipe finds the code it changes through type attribution, not by name or
 * printed text.
 */
public interface TypeMatchingValidationService {

    /**
     * @param reparse Parses a printed copy of a source file, or returns null when it can't.
     * @return Copies of {@code before} that spell the code changed in {@code after} differently but mean the same,
     * such as a static import in place of a qualified call.
     */
    List<Respelling> respell(SourceFile before, SourceFile after, Function<SourceFile, @Nullable SourceFile> reparse);

    /**
     * @return Whether {@code after} changes more of {@code before} than its imports and whitespace.
     */
    boolean changesCode(SourceFile before, @Nullable SourceFile after);

    @Value
    class Respelling {
        SourceFile sourceFile;

        /**
         * What was respelled, e.g. "`doNothing()` written `Mockito.doNothing()`".
         */
        String description;

        /**
         * Each of several respelled places on its own, to tell which of them a recipe misses.
         */
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        Supplier<List<Respelling>> eachPlace;
    }
}
