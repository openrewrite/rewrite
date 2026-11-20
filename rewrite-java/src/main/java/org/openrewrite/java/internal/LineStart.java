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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.StringUtils.repeat;

/**
 * The start of a line whose code was changed or replaced in place: a formatter gives such code the indentation of a
 * file formatted from scratch, but the line keeps the indentation, and the comments above it, that it had.
 */
public final class LineStart {

    private LineStart() {
    }

    /**
     * The prefix for code that took the place of code whose prefix was {@code original}, given {@code given} and then
     * formatted to {@code formatted}. When the code was given the original's indentation, so that it stays where the
     * original was, and the formatted code still starts its line, the line keeps that indentation. When the code was
     * given the original's comments, they keep their text and indentation too; comments the code was given instead
     * keep their formatting. Code given another indentation, as code moved elsewhere or given a fresh line break is,
     * keeps the formatter's. Blank lines above the line, when the code was given them as they were, keep their bytes:
     * how many there are and the whitespace on them. Above a line that had none, the formatter chooses the blank lines.
     */
    public static Space keep(Space formatted, Space original, Space given) {
        if (!startsLine(original) || !startsLine(given) || !startsLine(formatted) ||
            !indentation(given).equals(indentation(original))) {
            return formatted;
        }
        if (Objects.equals(given.getComments(), original.getComments())) {
            String whitespace = original.getWhitespace();
            String formattedWhitespace = formatted.getWhitespace();
            int lastBreak = whitespace.lastIndexOf('\n');
            // blank lines the code was given as they were are the file's, not the recipe's
            boolean blankLinesAsTheyWere = whitespace.indexOf('\n') != lastBreak &&
                                           whitespace.equals(given.getWhitespace());
            if (lastBreak >= 0 && formattedWhitespace.indexOf('\n') >= 0 && !blankLinesAsTheyWere) {
                String lineBreak = whitespace.contains("\r\n") ? "\r\n" : "\n";
                int breaks = (int) formattedWhitespace.chars().filter(c -> c == '\n').count();
                whitespace = repeat(lineBreak, breaks) + whitespace.substring(lastBreak + 1);
            }
            return original.withWhitespace(whitespace);
        }
        return withIndentation(formatted, indentation(original));
    }

    /**
     * A declaration formatted to {@code formatted} after its annotations, modifiers or other parts changed where it
     * stands, given the declaration it was, {@code original}, and the prefix it was given before formatting,
     * {@code given}. The line that holds its modifiers, type and name keeps the indentation it had, when it started a
     * line before and still starts one: an annotation added above that line, or a modifier removed from its start,
     * doesn't reindent it. A declaration given another indentation keeps the formatter's, as does one whose line can't
     * be told, such as a line that an annotation with arguments shares with the modifiers.
     */
    public static <J2 extends J> J2 keepDeclarationLine(J2 formatted, J original, Space given) {
        if (!startsLine(original.getPrefix()) || !startsLine(given) ||
            !indentation(given).equals(indentation(original.getPrefix()))) {
            return formatted;
        }
        String indentation = declarationLineIndentation(original);
        Space prefix = afterLeadingAnnotations(formatted);
        if (indentation == null || prefix == null || !startsLine(prefix) || indentation(prefix).equals(indentation)) {
            return formatted;
        }
        return withAfterLeadingAnnotations(formatted, withIndentation(prefix, indentation));
    }

    /**
     * The indentation of the line that holds a declaration's modifiers, type and name.
     */
    private static @Nullable String declarationLineIndentation(J declaration) {
        Space prefix = afterLeadingAnnotations(declaration);
        if (prefix == null) {
            return null;
        }
        if (startsLine(prefix)) {
            return indentation(prefix);
        }
        List<J.Annotation> annotations = leadingAnnotations(declaration);
        for (int i = annotations.size() - 1; i >= 0; i--) {
            J.Annotation annotation = annotations.get(i);
            if (annotation.getArguments() != null && !annotation.getArguments().isEmpty()) {
                // its arguments may span lines, so the line it ends on is not the one it starts
                return null;
            }
            if (startsLine(annotation.getPrefix())) {
                return indentation(annotation.getPrefix());
            }
        }
        return startsLine(declaration.getPrefix()) ? indentation(declaration.getPrefix()) : null;
    }

    private static List<J.Annotation> leadingAnnotations(J declaration) {
        if (declaration instanceof J.MethodDeclaration) {
            return ((J.MethodDeclaration) declaration).getLeadingAnnotations();
        } else if (declaration instanceof J.VariableDeclarations) {
            return ((J.VariableDeclarations) declaration).getLeadingAnnotations();
        } else if (declaration instanceof J.ClassDeclaration) {
            return ((J.ClassDeclaration) declaration).getLeadingAnnotations();
        }
        return emptyList();
    }

    /**
     * The prefix of what a declaration prints first after its leading annotations, or null when that is not a
     * declaration's or it prints annotations of its own ahead of that prefix.
     */
    private static @Nullable Space afterLeadingAnnotations(J declaration) {
        if (declaration instanceof J.MethodDeclaration) {
            J.MethodDeclaration m = (J.MethodDeclaration) declaration;
            if (!m.getModifiers().isEmpty()) {
                return modifierPrefix(m.getModifiers().get(0));
            }
            J.TypeParameters typeParameters = m.getAnnotations().getTypeParameters();
            if (typeParameters != null) {
                return typeParameters.getAnnotations().isEmpty() ? typeParameters.getPrefix() : null;
            }
            if (m.getReturnTypeExpression() != null) {
                return m.getReturnTypeExpression().getPrefix();
            }
            return m.getAnnotations().getName().getAnnotations().isEmpty() ? m.getName().getPrefix() : null;
        } else if (declaration instanceof J.VariableDeclarations) {
            J.VariableDeclarations v = (J.VariableDeclarations) declaration;
            if (!v.getModifiers().isEmpty()) {
                return modifierPrefix(v.getModifiers().get(0));
            }
            if (v.getTypeExpression() != null) {
                return v.getTypeExpression().getPrefix();
            }
            return v.getVariables().isEmpty() ? null : v.getVariables().get(0).getPrefix();
        } else if (declaration instanceof J.ClassDeclaration) {
            J.ClassDeclaration c = (J.ClassDeclaration) declaration;
            if (!c.getModifiers().isEmpty()) {
                return modifierPrefix(c.getModifiers().get(0));
            }
            J.ClassDeclaration.Kind kind = c.getPadding().getKind();
            return kind.getAnnotations().isEmpty() ? kind.getPrefix() : null;
        }
        return null;
    }

    private static @Nullable Space modifierPrefix(J.Modifier modifier) {
        return modifier.getAnnotations().isEmpty() ? modifier.getPrefix() : null;
    }

    @SuppressWarnings("unchecked")
    private static <J2 extends J> J2 withAfterLeadingAnnotations(J2 declaration, Space prefix) {
        if (declaration instanceof J.MethodDeclaration) {
            J.MethodDeclaration m = (J.MethodDeclaration) declaration;
            if (!m.getModifiers().isEmpty()) {
                return (J2) m.withModifiers(ListUtils.mapFirst(m.getModifiers(), mod -> mod.withPrefix(prefix)));
            }
            J.TypeParameters typeParameters = m.getAnnotations().getTypeParameters();
            if (typeParameters != null) {
                return (J2) m.getAnnotations().withTypeParameters(typeParameters.withPrefix(prefix));
            }
            if (m.getReturnTypeExpression() != null) {
                return (J2) m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(prefix));
            }
            return (J2) m.withName(m.getName().withPrefix(prefix));
        } else if (declaration instanceof J.VariableDeclarations) {
            J.VariableDeclarations v = (J.VariableDeclarations) declaration;
            if (!v.getModifiers().isEmpty()) {
                return (J2) v.withModifiers(ListUtils.mapFirst(v.getModifiers(), mod -> mod.withPrefix(prefix)));
            }
            if (v.getTypeExpression() != null) {
                return (J2) v.withTypeExpression(v.getTypeExpression().withPrefix(prefix));
            }
            return (J2) v.withVariables(ListUtils.mapFirst(v.getVariables(), variable -> variable.withPrefix(prefix)));
        } else if (declaration instanceof J.ClassDeclaration) {
            J.ClassDeclaration c = (J.ClassDeclaration) declaration;
            if (!c.getModifiers().isEmpty()) {
                return (J2) c.withModifiers(ListUtils.mapFirst(c.getModifiers(), mod -> mod.withPrefix(prefix)));
            }
            return (J2) c.getPadding().withKind(c.getPadding().getKind().withPrefix(prefix));
        }
        return declaration;
    }

    /**
     * Whether code with this prefix starts a line, after any comments the prefix holds.
     */
    public static boolean startsLine(Space prefix) {
        if (prefix.getWhitespace().indexOf('\n') >= 0) {
            return true;
        }
        for (Comment comment : prefix.getComments()) {
            if (comment.getSuffix().indexOf('\n') >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * The whitespace after the prefix's last line break, which indents the line the code starts.
     */
    private static String indentation(Space prefix) {
        String indentation = afterLastBreak(prefix.getWhitespace());
        for (Comment comment : prefix.getComments()) {
            String afterBreak = afterLastBreak(comment.getSuffix());
            if (comment.getSuffix().indexOf('\n') >= 0) {
                indentation = afterBreak;
            }
        }
        return indentation;
    }

    private static Space withIndentation(Space prefix, String indentation) {
        List<Comment> comments = prefix.getComments();
        for (int i = comments.size() - 1; i >= 0; i--) {
            String suffix = comments.get(i).getSuffix();
            if (suffix.indexOf('\n') >= 0) {
                int index = i;
                return prefix.withComments(ListUtils.map(comments, (j, c) ->
                        j == index ? c.withSuffix(reindent(suffix, indentation)) : c));
            }
        }
        return prefix.withWhitespace(reindent(prefix.getWhitespace(), indentation));
    }

    private static String reindent(String whitespace, String indentation) {
        int lastBreak = whitespace.lastIndexOf('\n');
        return lastBreak < 0 ? whitespace : whitespace.substring(0, lastBreak + 1) + indentation;
    }

    private static String afterLastBreak(String whitespace) {
        return whitespace.substring(whitespace.lastIndexOf('\n') + 1);
    }
}
