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
package org.openrewrite.kotlin.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.marker.PrimaryConstructor;

import java.util.List;

/**
 * Ensures that whitespace is on the outermost AST element possible.
 */
public class NormalizeFormatVisitor<P> extends KotlinIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @JsonCreator
    public NormalizeFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @SuppressWarnings("unused")
    public NormalizeFormatVisitor() {
        this(null);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);

        if (!c.getLeadingAnnotations().isEmpty()) {
            c = concatenatePrefix(c, Space.firstPrefix(c.getLeadingAnnotations()));
            return c.withLeadingAnnotations(Space.formatFirstPrefix(c.getLeadingAnnotations(), Space.EMPTY));
        }

        int firstVisible = firstVisibleModifierIndex(c.getModifiers());
        if (firstVisible >= 0) {
            J.Modifier m = c.getModifiers().get(firstVisible);
            c = concatenatePrefix(c, m.getPrefix());
            List<J.Modifier> updated = ListUtils.map(c.getModifiers(), (i, mod) ->
                    i == firstVisible ? mod.withPrefix(Space.EMPTY) : mod);
            return c.withModifiers(updated);
        }

        if (!c.getPadding().getKind().getPrefix().isEmpty()) {
            c = concatenatePrefix(c, c.getPadding().getKind().getPrefix());
            return c.getPadding().withKind(c.getPadding().getKind().withPrefix(Space.EMPTY));
        }

        return c.withName(c.getName().withPrefix(c.getName().getPrefix().withWhitespace(" ")));
    }

    /**
     * Find the first modifier that prints something, skipping the synthetic
     * {@code final} the Kotlin parser inserts on every class/method/property
     * that doesn't write {@code open} (see
     * {@code KotlinTreeParserVisitor.buildFinalModifier()}: {@code type=Final,
     * keyword=null, empty prefix, no annotations}; the printer's
     * {@code visitModifier} emits nothing for it). Returning the synthetic
     * would make this visitor "move" an empty prefix onto the class and leave
     * the real leading whitespace stranded on {@code kind.prefix} — defeating
     * the whitespace-to-outermost convention.
     */
    private static int firstVisibleModifierIndex(List<J.Modifier> modifiers) {
        for (int i = 0; i < modifiers.size(); i++) {
            if (!isKotlinSyntheticFinal(modifiers.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isKotlinSyntheticFinal(J.Modifier m) {
        return m.getType() == J.Modifier.Type.Final &&
                m.getKeyword() == null &&
                m.getPrefix().getWhitespace().isEmpty() &&
                m.getAnnotations().isEmpty();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);

        if (m.getMarkers().findFirst(PrimaryConstructor.class).isPresent()) {
            return m;
        }

        if (!m.getLeadingAnnotations().isEmpty()) {
            m = concatenatePrefix(m, Space.firstPrefix(m.getLeadingAnnotations()));
            return m.withLeadingAnnotations(Space.formatFirstPrefix(m.getLeadingAnnotations(), Space.EMPTY));
        }

        int firstVisible = firstVisibleModifierIndex(m.getModifiers());
        if (firstVisible >= 0) {
            J.Modifier mod = m.getModifiers().get(firstVisible);
            m = concatenatePrefix(m, mod.getPrefix());
            List<J.Modifier> updated = ListUtils.map(m.getModifiers(), (i, x) ->
                    i == firstVisible ? x.withPrefix(Space.EMPTY) : x);
            return m.withModifiers(updated);
        }

        if (m.getAnnotations().getTypeParameters() != null) {
            if (!m.getAnnotations().getTypeParameters().getTypeParameters().isEmpty()) {
                m = concatenatePrefix(m, m.getAnnotations().getTypeParameters().getPrefix());
                m = m.getAnnotations().withTypeParameters(m.getAnnotations().getTypeParameters().withPrefix(Space.EMPTY));
            }
            return m;
        }

        if (m.getReturnTypeExpression() != null) {
            if (!m.getReturnTypeExpression().getPrefix().getWhitespace().isEmpty()) {
                m = concatenatePrefix(m, m.getReturnTypeExpression().getPrefix());
                m = m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(Space.EMPTY));
            }
            return m;
        }

        m = concatenatePrefix(m, m.getName().getPrefix());
        return m.withName(m.getName().withPrefix(Space.EMPTY));
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);

        if (!v.getLeadingAnnotations().isEmpty()) {
            v = concatenatePrefix(v, Space.firstPrefix(v.getLeadingAnnotations()));
            return v.withLeadingAnnotations(Space.formatFirstPrefix(v.getLeadingAnnotations(), Space.EMPTY));
        }

        int firstVisible = firstVisibleModifierIndex(v.getModifiers());
        if (firstVisible >= 0) {
            J.Modifier mod = v.getModifiers().get(firstVisible);
            v = concatenatePrefix(v, mod.getPrefix());
            List<J.Modifier> updated = ListUtils.map(v.getModifiers(), (i, x) ->
                    i == firstVisible ? x.withPrefix(Space.EMPTY) : x);
            return v.withModifiers(updated);
        }

        return v;
    }

    private <J2 extends J> J2 concatenatePrefix(J2 j, Space prefix) {
        String shift = StringUtils.commonMargin(null, j.getPrefix().getWhitespace());

        List<Comment> comments = ListUtils.concatAll(
                j.getComments(),
                ListUtils.map(prefix.getComments(), comment -> {
                    Comment c = comment;
                    if (shift.isEmpty()) {
                        return c;
                    }

                    if (comment instanceof TextComment) {
                        TextComment textComment = (TextComment) c;
                        c = textComment.withText(textComment.getText().replace("\n", "\n" + shift));
                    } else if (c instanceof Javadoc) {
                        c = (Comment) new JavadocVisitor<Integer>(new JavaVisitor<>()) {
                            @Override
                            public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, Integer integer) {
                                return lineBreak.withMargin(shift + lineBreak.getMargin());
                            }
                        }.visitNonNull((Javadoc) c, 0);
                    }

                    if(c.getSuffix().contains("\n")) {
                        c = c.withSuffix(c.getSuffix().replace("\n", "\n" + shift));
                    }

                    return c;
                })
        );

        return j.withPrefix(j.getPrefix()
                .withWhitespace(j.getPrefix().getWhitespace() + prefix.getWhitespace())
                .withComments(comments));
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
}
