/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.format.BlankLinesVisitor;
import org.openrewrite.java.format.NormalizeFormatVisitor;
import org.openrewrite.java.format.SpacesVisitor;
import org.openrewrite.java.style.SpacesStyle;

@Incubating(since = "8.2.0")
public class AutoFormatService {

    public <P> JavaVisitor<P> autoFormatVisitor(@Nullable Tree stopAfter) {
        return new AutoFormatVisitor<>(stopAfter);
    }

    /**
     * Returns the language-appropriate {@link NormalizeFormatVisitor}. Edit
     * recipes that mutate an LST node — e.g. by removing a leading annotation
     * — can leave whitespace stranded on inner elements (modifier/kind
     * prefixes), violating the "whitespace lives on the outermost element"
     * convention. Running this visitor on the result moves the whitespace
     * back to the outermost element so downstream passes ({@link BlankLinesVisitor}
     * in particular) can normalize it.
     */
    public <P> JavaVisitor<P> normalizeFormatVisitor(@Nullable Tree stopAfter) {
        return new NormalizeFormatVisitor<>(stopAfter);
    }

    /**
     * Returns the language-appropriate {@link BlankLinesVisitor}.
     */
    public <P> JavaVisitor<P> blankLinesVisitor(SourceFile sourceFile, @Nullable Tree stopAfter) {
        return new BlankLinesVisitor<>(sourceFile, stopAfter);
    }

    /**
     * Returns the language-appropriate {@link SpacesVisitor}. The {@code spacesStyle}
     * carries Java-specific tuning (e.g. typecast parenthesis padding) that only applies
     * to languages with C-style casts; languages that reuse {@code J.TypeCast} for other
     * syntax resolve their own style from {@code sourceFile} instead.
     */
    public <P> JavaVisitor<P> spacesVisitor(SourceFile sourceFile, SpacesStyle spacesStyle, @Nullable Tree stopAfter) {
        return new SpacesVisitor<>(spacesStyle, stopAfter);
    }
}
