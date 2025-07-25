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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.*;

import java.util.List;

/**
 * Ensures that whitespace is on the outermost AST element possible.
 */
public class NormalizeFormatVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @JsonCreator
    public NormalizeFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

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

        if (!c.getModifiers().isEmpty()) {
            c = concatenatePrefix(c, Space.firstPrefix(c.getModifiers()));
            return c.withModifiers(Space.formatFirstPrefix(c.getModifiers(), Space.EMPTY));
        }

        if (!c.getPadding().getKind().getPrefix().isEmpty()) {
            c = concatenatePrefix(c, c.getPadding().getKind().getPrefix());
            return c.getPadding().withKind(c.getPadding().getKind().withPrefix(Space.EMPTY));
        }

        JContainer<J.TypeParameter> typeParameters = c.getPadding().getTypeParameters();
        if (typeParameters != null && !typeParameters.getElements().isEmpty()) {
            c = concatenatePrefix(c, typeParameters.getBefore());
            return c.getPadding().withTypeParameters(typeParameters.withBefore(Space.EMPTY));
        }

        return c.withName(c.getName().withPrefix(c.getName().getPrefix().withWhitespace(" ")));
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);

        if (!m.getLeadingAnnotations().isEmpty()) {
            m = concatenatePrefix(m, Space.firstPrefix(m.getLeadingAnnotations()));
            return m.withLeadingAnnotations(Space.formatFirstPrefix(m.getLeadingAnnotations(), Space.EMPTY));
        }

        if (!m.getModifiers().isEmpty()) {
            m = concatenatePrefix(m, Space.firstPrefix(m.getModifiers()));
            return m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), Space.EMPTY));
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

        if (!v.getModifiers().isEmpty()) {
            v = concatenatePrefix(v, Space.firstPrefix(v.getModifiers()));
            return v.withModifiers(Space.formatFirstPrefix(v.getModifiers(), Space.EMPTY));
        }

        if (v.getTypeExpression() != null) {
            v = concatenatePrefix(v, v.getTypeExpression().getPrefix());
            return v.withTypeExpression(v.getTypeExpression().withPrefix(Space.EMPTY));
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
