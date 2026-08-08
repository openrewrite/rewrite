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
package org.openrewrite.kotlin.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.kotlin.format.AutoFormatVisitor;
import org.openrewrite.kotlin.format.BlankLinesVisitor;
import org.openrewrite.kotlin.format.NormalizeFormatVisitor;
import org.openrewrite.kotlin.format.SpacesVisitor;
import org.openrewrite.kotlin.style.BlankLinesStyle;
import org.openrewrite.style.Style;

import static org.openrewrite.kotlin.style.IntelliJ.blankLines;
import static org.openrewrite.kotlin.style.IntelliJ.spaces;

public class KotlinAutoFormatService extends AutoFormatService {

    @Override
    public <P> JavaVisitor<P> autoFormatVisitor(@Nullable Tree stopAfter) {
        return new AutoFormatVisitor<>(stopAfter);
    }

    @Override
    public <P> JavaVisitor<P> normalizeFormatVisitor(@Nullable Tree stopAfter) {
        return new NormalizeFormatVisitor<>(stopAfter);
    }

    @Override
    public <P> JavaVisitor<P> blankLinesVisitor(SourceFile sourceFile, @Nullable Tree stopAfter) {
        return new BlankLinesVisitor<>(Style.from(BlankLinesStyle.class, sourceFile, () -> blankLines()), stopAfter);
    }

    @Override
    public <P> JavaVisitor<P> spacesVisitor(SourceFile sourceFile, SpacesStyle spacesStyle, @Nullable Tree stopAfter) {
        return new SpacesVisitor<>(Style.from(org.openrewrite.kotlin.style.SpacesStyle.class, sourceFile, () -> spaces()), stopAfter);
    }
}
