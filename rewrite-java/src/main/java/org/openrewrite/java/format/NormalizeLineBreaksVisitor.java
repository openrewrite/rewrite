/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.style.GeneralFormatStyle;

public class NormalizeLineBreaksVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final GeneralFormatStyle style;

    private final JavadocVisitor<Integer> lineBreakJavadocVisitor = new JavadocVisitor<Integer>(new JavaVisitor<>()) {
        @Override
        public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, Integer integer) {
            return lineBreak.withMargin(normalizeNewLines(lineBreak.getMargin(), style.isUseCRLFNewLines()));
        }
    };

    public NormalizeLineBreaksVisitor(GeneralFormatStyle style) {
        this(style, null);
    }

    public NormalizeLineBreaksVisitor(GeneralFormatStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        Space s = space.withWhitespace(normalizeNewLines(space.getWhitespace(), style.isUseCRLFNewLines()));

        return s.withComments(ListUtils.map(s.getComments(), comment -> {
            Comment c = comment;
            if (c.isMultiline()) {
                if (c instanceof Javadoc) {
                    c = c.withSuffix(normalizeNewLines(c.getSuffix(), style.isUseCRLFNewLines()));
                    return (Comment) lineBreakJavadocVisitor.visitNonNull((Javadoc) c, 0);
                } else {
                    TextComment textComment = (TextComment) c;
                    c = textComment.withText(normalizeNewLines(textComment.getText(), style.isUseCRLFNewLines()));
                }
            }

            return c.withSuffix(normalizeNewLines(c.getSuffix(), style.isUseCRLFNewLines()));
        }));
    }

    private static String normalizeNewLines(String text, boolean useCrlf) {
        if (!text.contains("\n")) {
            return text;
        }
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (useCrlf && c == '\n' && (i == 0 || text.charAt(i - 1) != '\r')) {
                normalized.append('\r').append('\n');
            } else if (useCrlf || c != '\r') {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    @Override
    public J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return tree;
    }

    @Nullable
    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }
}
