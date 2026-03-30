/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python.format;

import org.openrewrite.Incubating;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.python.PythonIsoVisitor;

import java.util.List;

/**
 * Ideally, we'll reuse AutoFormatVisitor from Java, but Python requires specific handling of whitespace
 * to maintain column alignment.
 * <p>
 * This is an example of whitespace formatting on Python limited to safe changes until column alignment is supported.
 */
@Incubating(since = "0.3.1")
public class PythonSpacesVisitor<P> extends PythonIsoVisitor<P> {

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = (J.MethodDeclaration) super.visitMethodDeclaration(method, p);
        // style.getBeforeParentheses().getMethodDeclaration())
        boolean styleBeforeMethodDeclarationParentheses = false;
        m = m.getPadding().withParameters(
                spaceBefore(m.getPadding().getParameters(), styleBeforeMethodDeclarationParentheses));
        if (m.getParameters().isEmpty() || m.getParameters().iterator().next() instanceof J.Empty) {
            boolean useSpace = false;
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    param -> param.withElement(spaceBefore(param.getElement(), useSpace))
                            )
                    )
            );
        } else {
            final int paramsSize = m.getParameters().size();
            boolean useSpace = false;
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    (index, param) -> {
                                        if (index == 0) {
                                            param = param.withElement(spaceBefore(param.getElement(), useSpace));
                                        } else {
                                            // style.getOther().getAfterComma()
                                            boolean spaceAfterComma = true;
                                            param = param.withElement(
                                                    spaceBefore(param.getElement(), spaceAfterComma)
                                            );
                                        }
                                        if (index == paramsSize - 1) {
                                            param = spaceAfter(param, useSpace);
                                        } else {
                                            // style.getOther().getBeforeComma()
                                            boolean spaceBeforeComma = false;
                                            param = spaceAfter(param, spaceBeforeComma);
                                        }
                                        return param;
                                    }
                            )
                    )
            );
        }

        return m;
    }

    <T> JContainer<T> spaceBefore(JContainer<T> container, boolean spaceBefore) {
        if (!container.getBefore().getComments().isEmpty()) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            List<Comment> comments = spaceLastCommentSuffix(container.getBefore().getComments(), spaceBefore);
            return container.withBefore(container.getBefore().withComments(comments));
        }

        if (spaceBefore && notSingleSpace(container.getBefore().getWhitespace()) && doesNotContainNewLine(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(" "));
        }
        if (!spaceBefore && onlySpacesAndNotEmpty(container.getBefore().getWhitespace()) && doesNotContainNewLine(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(""));
        }
        return container;
    }

    private static List<Comment> spaceLastCommentSuffix(List<Comment> comments, boolean spaceSuffix) {
        return ListUtils.mapLast(comments,
                comment -> spaceSuffix(comment, spaceSuffix));
    }

    private static Comment spaceSuffix(Comment comment, boolean spaceSuffix) {
        if (spaceSuffix && notSingleSpace(comment.getSuffix()) && doesNotContainNewLine(comment.getSuffix())) {
            return comment.withSuffix(" ");
        }
        if (!spaceSuffix && onlySpacesAndNotEmpty(comment.getSuffix()) && doesNotContainNewLine(comment.getSuffix())) {
            return comment.withSuffix("");
        }
        return comment;
    }

    <T extends J> T spaceBefore(T j, boolean spaceBefore) {
        if (!j.getComments().isEmpty()) {
            return j;
        }

        if (spaceBefore && notSingleSpace(j.getPrefix().getWhitespace()) && doesNotContainNewLine(j.getPrefix().getWhitespace())) {
            return j.withPrefix(j.getPrefix().withWhitespace(" "));
        }
        if (!spaceBefore && onlySpacesAndNotEmpty(j.getPrefix().getWhitespace()) && doesNotContainNewLine(j.getPrefix().getWhitespace())) {
            return j.withPrefix(j.getPrefix().withWhitespace(""));
        }
        return j;
    }

    <T extends J> JRightPadded<T> spaceAfter(JRightPadded<T> container, boolean spaceAfter) {
        if (!container.getAfter().getComments().isEmpty()) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            List<Comment> comments = spaceLastCommentSuffix(container.getAfter().getComments(), spaceAfter);
            return container.withAfter(container.getAfter().withComments(comments));
        }

        if (spaceAfter && notSingleSpace(container.getAfter().getWhitespace()) && doesNotContainNewLine(container.getAfter().getWhitespace())) {
            return container.withAfter(container.getAfter().withWhitespace(" "));
        }
        if (!spaceAfter && onlySpacesAndNotEmpty(container.getAfter().getWhitespace()) && doesNotContainNewLine(container.getAfter().getWhitespace())) {
            return container.withAfter(container.getAfter().withWhitespace(""));
        }
        return container;
    }

    /**
     * Checks if a string only contains spaces or tabs (excluding newline characters).
     * @return true if contains spaces or tabs only, or true for empty string.
     */
    private static boolean onlySpaces(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    private static boolean onlySpacesAndNotEmpty(String s) {
        return !StringUtils.isNullOrEmpty(s) && onlySpaces(s);
    }

    private static boolean notSingleSpace(String str) {
        return onlySpaces(str) && !" ".equals(str);
    }

    private static boolean doesNotContainNewLine(String str) {
        return !str.contains("\n");
    }
}
