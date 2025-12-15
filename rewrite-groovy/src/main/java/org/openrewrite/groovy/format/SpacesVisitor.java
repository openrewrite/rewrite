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
package org.openrewrite.groovy.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.ToBeRemoved;
import org.openrewrite.java.style.EmptyForInitializerPadStyle;
import org.openrewrite.java.style.EmptyForIteratorPadStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

import java.util.List;
import java.util.function.Supplier;

public class SpacesVisitor<P> extends org.openrewrite.java.format.SpacesVisitor<P> {
    public SpacesVisitor(SourceFile sourceFile, @Nullable Tree stopAfter) {
        this(sourceFile.getMarkers().findAll(NamedStyles.class), stopAfter);
    }

    private SpacesVisitor(List<NamedStyles> styles, @Nullable Tree stopAfter) {
        this(getStyle(SpacesStyle.class, styles, IntelliJ::spaces), getStyle(EmptyForInitializerPadStyle.class, styles), getStyle(EmptyForIteratorPadStyle.class, styles), stopAfter);
    }

    public SpacesVisitor(SpacesStyle spacesStyle, @Nullable EmptyForInitializerPadStyle emptyForInitializerPadStyle, @Nullable EmptyForIteratorPadStyle emptyForIteratorPadStyle, @Nullable Tree stopAfter) {
        super(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle, stopAfter);
    }

    @Override
    public @Nullable <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        switch (loc) {
            case METHOD_SELECT:
                loc = JRightPadded.Location.LANGUAGE_EXTENSION;
                break;
        }
        return super.visitRightPadded(right, loc, p);
    }

    @Override
    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JLeftPadded.Location loc, P p) {
        return super.visitLeftPadded(left, JLeftPadded.Location.LANGUAGE_EXTENSION, p);
    }

    @Override
    public Space visitSpace(@Nullable Space space, Space.Location loc, P ctx) {
        switch (loc) {
            case TERNARY_PREFIX:
            case TERNARY_TRUE:
            case TERNARY_FALSE:
            case UNARY_PREFIX:
            case UNARY_OPERATOR:
            case BINARY_PREFIX:
            case BINARY_OPERATOR:
            case METHOD_SELECT_SUFFIX:
            case METHOD_INVOCATION_NAME:
                loc = Space.Location.LANGUAGE_EXTENSION;
                break;
        }
        return super.visitSpace(space, loc, ctx);
    }

    @ToBeRemoved(after = "30-01-2026", reason = "Replace me with org.openrewrite.style.StyleHelper.getStyle now available in parent runtime")
    private static <S extends Style> @Nullable S getStyle(Class<S> styleClass, List<NamedStyles> styles) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return (S) style.applyDefaults();
        }
        return null;
    }

    @ToBeRemoved(after = "30-01-2026", reason = "Replace me with org.openrewrite.style.StyleHelper.getStyle now available in parent runtime")
    private static <S extends Style> S getStyle(Class<S> styleClass, List<NamedStyles> styles, Supplier<S> defaultStyle) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return StyleHelper.merge(defaultStyle.get(), style);
        }
        return defaultStyle.get();
    }
}
