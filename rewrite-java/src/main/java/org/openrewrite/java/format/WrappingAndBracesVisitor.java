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

import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.List;

public class WrappingAndBracesVisitor<P> extends JavaIsoVisitor<P> {

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final WrappingAndBracesStyle style;

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style) {
        this.style = style;
        setCursoringOn();
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement j = super.visitStatement(statement, p);
        J parentTree = getCursor().dropParentUntil(J.class::isInstance).getValue();
        if (parentTree instanceof J.Block) {
            if (!j.getPrefix().getWhitespace().contains("\n")) {
                j = j.withPrefix(withNewline(j.getPrefix()));
            }
        }

        return j;
    }

    @Override public J.MethodDecl visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl m = super.visitMethod(method, p);
        // TODO make annotation wrapping configurable
        m = m.withAnnotations(withNewlines(m.getAnnotations()));
        if (!m.getAnnotations().isEmpty()) {
            if (!m.getModifiers().isEmpty()) {
                m = m.withModifiers(withNewline(m.getModifiers()));
            } else if (m.getPadding().getTypeParameters() != null) {
                if (!m.getPadding().getTypeParameters().getBefore().getWhitespace().contains("\n")) {
                    m = m.getPadding().withTypeParameters(
                            m.getPadding().getTypeParameters().withBefore(
                                    withNewline(m.getPadding().getTypeParameters().getBefore())
                            )
                    );
                }
            } else if (m.getReturnTypeExpr() != null) {
                if (!m.getReturnTypeExpr().getPrefix().getWhitespace().contains("\n")) {
                    m = m.withReturnTypeExpr(
                            m.getReturnTypeExpr().withPrefix(
                                    withNewline(m.getReturnTypeExpr().getPrefix())
                            )
                    );
                }
            } else {
                if (!m.getName().getPrefix().getWhitespace().contains("\n")) {
                    m = m.withName(
                            m.getName().withPrefix(
                                    withNewline(m.getName().getPrefix())
                            )
                    );
                }
            }
        }
        return m;
    }

    @Override public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl j = super.visitClassDecl(classDecl, p);
        // TODO make annotation wrapping configurable
        j = j.withAnnotations(withNewlines(j.getAnnotations()));
        if (!j.getAnnotations().isEmpty()) {
            if (!j.getModifiers().isEmpty()) {
                j = j.withModifiers(withNewline(j.getModifiers()));
            } else {
                J.ClassDecl.Padding padding = j.getPadding();
                JLeftPadded<J.ClassDecl.Kind> kind = padding.getKind();
                j = padding.withKind(
                        kind.withBefore(
                                kind.getBefore().withWhitespace(
                                        "\n" + kind.getBefore().getWhitespace()
                                )
                        )
                );
            }

        }
        return j;
    }

    private List<J.Annotation> withNewlines(List<J.Annotation> annotations) {
        if (annotations.isEmpty()) {
            return annotations;
        }
        return ListUtils.map(annotations, (index, a) -> {
            if (index != 0 && !a.getPrefix().getWhitespace().contains("\n")) {
                a = a.withPrefix(withNewline(a.getPrefix()));
            }
            return a;
        });
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        if(!b.getEnd().getWhitespace().contains("\n")) {
            b = b.withEnd(withNewline(b.getEnd()));
        }
        return b;
    }

    private Space withNewline(Space space) {
        return space.withWhitespace("\n" + space.getWhitespace());
    }

    private List<J.Modifier> withNewline(List<J.Modifier> modifiers) {
        J.Modifier firstModifier = modifiers.iterator().next();
        if (!firstModifier.getPrefix().getWhitespace().contains("\n")) {
            return ListUtils.mapFirst(modifiers,
                    mod -> mod.withPrefix(
                            withNewline(mod.getPrefix())
                    )
            );
        }
        return modifiers;
    }
}
