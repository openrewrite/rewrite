/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.format;

import org.openrewrite.Cursor;
import org.openrewrite.Style;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.JavaProcessor;
import org.openrewrite.java.style.BlankLineStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AutoFormatProcessor<P> extends JavaIsoProcessor<P> {
    private final List<JavaProcessor<P>> formatters = new ArrayList<>();
    
    public AutoFormatProcessor(Collection<? extends Style> styles) {
        formatters.add(new BlankLinesProcessor<>(styles.stream()
                .filter(BlankLineStyle.class::isInstance)
                .map(BlankLineStyle.class::cast)
                .findFirst()
                .orElse(IntelliJ.defaultBlankLine())));

        formatters.add(new TabsAndIndentsProcessor<>(styles.stream()
                .filter(TabsAndIndentsStyle.class::isInstance)
                .map(TabsAndIndentsStyle.class::cast)
                .findFirst()
                .orElse(IntelliJ.defaultTabsAndIndents())));
    }

    @Override
    public J visit(@Nullable Tree tree, P p, Cursor cursor) {
        for (JavaProcessor<P> f : formatters) {
            tree = f.visit(tree, p, cursor);
        }
        return (J) tree;
    }
}
