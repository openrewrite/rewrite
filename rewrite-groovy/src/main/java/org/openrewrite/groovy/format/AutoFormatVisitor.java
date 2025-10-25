/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.groovy.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.format.*;
import org.openrewrite.java.style.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;

import java.util.Optional;

import static org.openrewrite.java.format.AutodetectGeneralFormatStyle.autodetectGeneralFormatStyle;

public class AutoFormatVisitor<P> extends GroovyIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @SuppressWarnings("unused")
    public AutoFormatVisitor() {
        this(null);
    }

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public J visit(@Nullable Tree tree, P p, Cursor cursor) {
        JavaSourceFile cu = (tree instanceof JavaSourceFile) ?
                (JavaSourceFile) tree :
                cursor.firstEnclosingOrThrow(JavaSourceFile.class);

        J t = new NormalizeFormatVisitor<>(stopAfter).visit(tree, p, cursor.fork());

        t = new BlankLinesVisitor<>(Style.from(BlankLinesStyle.class, cu, IntelliJ::blankLines), stopAfter)
                .visit(t, p, cursor.fork());

        t = new WrappingAndBracesVisitor<>(Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces), stopAfter)
                .visit(t, p, cursor.fork());

        WrappingAndBracesStyle wrappingAndBracesStyle = Style.from(WrappingAndBracesStyle.class, cu, IntelliJ::wrappingAndBraces);
        TabsAndIndentsStyle tabsAndIndentsStyle = Style.from(TabsAndIndentsStyle.class, cu, IntelliJ::tabsAndIndents);
        SpacesStyle spacesStyle = Style.from(SpacesStyle.class, cu, IntelliJ::spaces);

        t = new SpacesVisitor<>(
                spacesStyle,
                Style.from(EmptyForInitializerPadStyle.class, cu),
                Style.from(EmptyForIteratorPadStyle.class, cu),
                stopAfter
        ).visit(t, p, cursor.fork());

        t = new NormalizeTabsOrSpacesVisitor<>(tabsAndIndentsStyle, stopAfter)
                .visit(t, p, cursor.fork());

        t = new TabsAndIndentsVisitor<>(tabsAndIndentsStyle, spacesStyle, wrappingAndBracesStyle, stopAfter)
                .visit(t, p, cursor.fork());

        t = new NormalizeLineBreaksVisitor<>(Optional.ofNullable(Style.from(GeneralFormatStyle.class, cu))
                .orElse(autodetectGeneralFormatStyle(cu)), stopAfter)
                .visit(t, p, cursor.fork());

        t = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(t, p, cursor.fork());

        t = new OmitParenthesesForLastArgumentLambdaVisitor<>(stopAfter).visitNonNull(t, p, cursor.fork());

        return new MinimumViableSpacingVisitor<>(stopAfter).visitNonNull(t, p, cursor.fork());
    }
}
