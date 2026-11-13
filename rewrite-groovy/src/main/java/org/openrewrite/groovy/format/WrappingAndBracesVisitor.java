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
package org.openrewrite.groovy.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.groovy.marker.LambdaStyle;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.style.NamedStyles;

import java.util.List;

public class WrappingAndBracesVisitor<P> extends org.openrewrite.java.format.WrappingAndBracesVisitor<P> {
    public WrappingAndBracesVisitor(List<NamedStyles> styles, @Nullable Tree stopAfter) {
        super(styles, stopAfter);
    }

    public WrappingAndBracesVisitor(SpacesStyle spacesStyle, WrappingAndBracesStyle wrappingAndBracesStyle, @Nullable Tree stopAfter) {
        super(spacesStyle, wrappingAndBracesStyle, stopAfter);
    }

    @Override
    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (loc == JRightPadded.Location.BLOCK_STATEMENT && isSingleLineClosureBody(getCursor())) {
            loc = JRightPadded.Location.LANGUAGE_EXTENSION;
        }
        return super.visitRightPadded(right, loc, p);
    }

    @Override
    public Space visitSpace(@Nullable Space space, Space.Location loc, P p) {
        // The base visitor gives every statement in a block, and every block's closing brace, a line of its own
        if (loc == Space.Location.BLOCK_END ?
                isSingleLineClosureBody(getCursor()) :
                loc.name().endsWith("_PREFIX") && isSingleLineClosureBody(getCursor().getParentTreeCursor())) {
            loc = Space.Location.LANGUAGE_EXTENSION;
        }
        return super.visitSpace(space, loc, p);
    }

    /**
     * A closure written on one line, as `useJUnitPlatform { it.excludeTags 'slow' }` is throughout the Gradle DSL.
     * Java has no such convention, so the base visitor puts every block statement on its own line.
     */
    private static boolean isSingleLineClosureBody(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.Block)) {
            return false;
        }
        J.Block block = cursor.getValue();
        // An empty block is already governed by the style's keep-in-one-line settings
        if (block.getStatements().isEmpty() || block.getEnd().getWhitespace().contains("\n")) {
            return false;
        }
        Object parent = cursor.getParentTreeCursor().getValue();
        if (!(parent instanceof J.Lambda) ||
            ((J.Lambda) parent).getMarkers().findFirst(LambdaStyle.class).map(LambdaStyle::isJavaStyle).orElse(false)) {
            return false;
        }
        for (Statement statement : block.getStatements()) {
            if (statement.getPrefix().getWhitespace().contains("\n")) {
                return false;
            }
        }
        return true;
    }
}
