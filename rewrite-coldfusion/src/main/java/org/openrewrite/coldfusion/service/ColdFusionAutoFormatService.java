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
package org.openrewrite.coldfusion.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.format.*;
import org.openrewrite.coldfusion.style.BlankLinesStyle;
import org.openrewrite.coldfusion.style.CFFormat;
import org.openrewrite.coldfusion.style.TabsAndIndentsStyle;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.style.Style;

/**
 * The formatter {@code autoFormat}, {@code autoIndent} and {@code maybeAutoFormat} reach for a CFML file.
 */
public class ColdFusionAutoFormatService extends AutoFormatService {

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
        return new BlankLinesVisitor<>(Style.from(BlankLinesStyle.class, sourceFile, CFFormat::blankLines), stopAfter);
    }

    /**
     * CFML's spaces, from its own style. The Java spaces style passed here is Java's.
     */
    @Override
    public <P> JavaVisitor<P> spacesVisitor(SourceFile sourceFile, org.openrewrite.java.style.SpacesStyle spacesStyle,
                                            @Nullable Tree stopAfter) {
        return new SpacesVisitor<>(Style.from(org.openrewrite.coldfusion.style.SpacesStyle.class, sourceFile, CFFormat::spaces), stopAfter);
    }

    @Override
    public <P> JavaVisitor<P> tabsAndIndentsVisitor(SourceFile sourceFile, @Nullable Tree stopAfter) {
        return new TabsAndIndentsVisitor<>(Style.from(TabsAndIndentsStyle.class, sourceFile, CFFormat::tabsAndIndents), stopAfter);
    }
}
