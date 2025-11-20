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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.ToBeRemoved;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

import java.util.List;
import java.util.function.Supplier;

public class NormalizeTabsOrSpacesVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final TabsAndIndentsStyle style;

    public NormalizeTabsOrSpacesVisitor(SourceFile sourceFile, @Nullable Tree stopAfter) {
        this(sourceFile.getMarkers().findAll(NamedStyles.class), stopAfter);
    }

    public NormalizeTabsOrSpacesVisitor(List<NamedStyles> styles, @Nullable Tree stopAfter) {
        this(getStyle(TabsAndIndentsStyle.class, styles, IntelliJ::tabsAndIndents), stopAfter);
    }

    public NormalizeTabsOrSpacesVisitor(TabsAndIndentsStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        Space s = space.withWhitespace(normalizeAfterFirstNewline(space.getWhitespace()));

        return s.withComments(ListUtils.map(s.getComments(), comment -> {
            Comment c = comment;
            if (c.isMultiline()) {
                if (c instanceof Javadoc) {
                    c = c.withSuffix(normalize(c.getSuffix(), false));
                    return (Comment) new JavadocVisitor<Integer>(new JavaVisitor<>()) {
                        @Override
                        public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, Integer integer) {
                            return lineBreak.withMargin(normalize(lineBreak.getMargin(), true));
                        }
                    }.visitNonNull((Javadoc) c, 0);
                }
                TextComment textComment = (TextComment) c;
                if (textComment.getText().contains("\t")) {
                    c = textComment.withText(normalize(textComment.getText(), true));
                }
            }

            return c.withSuffix(normalizeAfterFirstNewline(c.getSuffix()));
        }));
    }

    @NonNull
    private String normalizeAfterFirstNewline(String text) {
        int firstNewline = text.indexOf('\n');
        if (firstNewline >= 0 && firstNewline != text.length() - 1) {
            return text.substring(0, firstNewline + 1) + normalize(text.substring(firstNewline + 1), false);
        }
        return text;
    }

    private String normalize(String text, boolean isComment) {
        if (!StringUtils.isNullOrEmpty(text)) {
            if (style.getUseTabCharacter() ? text.contains(" ") : text.contains("\t")) {
                StringBuilder textBuilder = new StringBuilder();
                int consecutiveSpaces = 0;
                boolean inMargin = true;
                outer:
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == '\n' || c == '\r') {
                        inMargin = true;
                        consecutiveSpaces = 0;
                        textBuilder.append(c);
                    } else if (!inMargin) {
                        textBuilder.append(c);
                    } else if (style.getUseTabCharacter() && c == ' ' && (!isComment ||
                            (i + 1 < text.length() && text.charAt(i + 1) != '*'))) {
                        int j = i + 1;
                        for (; j < text.length() && j < style.getTabSize(); j++) {
                            if (text.charAt(j) != ' ') {
                                continue outer;
                            }
                        }
                        i = j + 1;
                        textBuilder.append('\t');
                    } else if (!style.getUseTabCharacter() && c == '\t') {
                        for (int j = 0; j < style.getTabSize() - (consecutiveSpaces % style.getTabSize()); j++) {
                            textBuilder.append(' ');
                        }
                        consecutiveSpaces = 0;
                    } else if (Character.isWhitespace(c)) {
                        consecutiveSpaces++;
                        textBuilder.append(c);
                    } else {
                        inMargin = false;
                        textBuilder.append(c);
                    }
                }
                return textBuilder.toString();
            }
        }

        return text;
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
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
