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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Space;

public class MinimumViableSpacingVisitor<P> extends JavaIsoVisitor<P> {

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);

        boolean first = true;
        if (!c.getLeadingAnnotations().isEmpty()) {
            first = false;
        }
        if (!c.getModifiers().isEmpty()) {
            if (!first && Space.firstPrefix(c.getModifiers()).getWhitespace().isEmpty()) {
                c = c.withModifiers(Space.formatFirstPrefix(c.getModifiers(),
                        c.getModifiers().iterator().next().getPrefix().withWhitespace(" ")));
            }
            first = false;
        }

        J.ClassDeclaration.Padding padding = c.getPadding();
        JContainer<J.TypeParameter> typeParameters = padding.getTypeParameters();
        if (typeParameters != null && !typeParameters.getElements().isEmpty()) {
            if (!first && !typeParameters.getBefore().getWhitespace().isEmpty()) {
                c = padding.withTypeParameters(typeParameters.withBefore(typeParameters.getBefore().withWhitespace(" ")));
            }
        }

        if(c.getName().getPrefix().getWhitespace().isEmpty()) {
            c = c.withName(c.getName().withPrefix(c.getName().getPrefix().withWhitespace(" ")));
        }

        if (c.getPadding().getExtends() != null) {
            Space before = c.getPadding().getExtends().getBefore();
            if (before.getWhitespace().isEmpty()) {
                c = c.getPadding().withExtends(c.getPadding().getExtends().withBefore(before.withWhitespace(" ")));
            }
        }

        if (c.getPadding().getImplements() != null) {
            Space before = c.getPadding().getImplements().getBefore();
            if (before.getWhitespace().isEmpty()) {
                c = c.getPadding().withImplements(c.getPadding().getImplements().withBefore(before.withWhitespace(" ")));
            }
        }

        return c;
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        @SuppressWarnings("ConstantConditions") Object parent = getCursor().getParent().getValue();
        if (!b.isStatic() && (parent instanceof J.MethodDeclaration || parent instanceof J.ClassDeclaration) &&
                b.getPrefix().getWhitespace().isEmpty()) {
            b = b.withPrefix(b.getPrefix().withWhitespace(" "));
        }
        return b;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);

        boolean first = true;
        if (!m.getLeadingAnnotations().isEmpty()) {
            first = false;
        }
        if (!m.getModifiers().isEmpty()) {
            if (!first && Space.firstPrefix(m.getModifiers()).getWhitespace().isEmpty()) {
                m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(),
                        m.getModifiers().iterator().next().getPrefix().withWhitespace(" ")));
            }
            first = false;
        }
        J.TypeParameters typeParameters = m.getAnnotations().getTypeParameters();
        if (typeParameters != null && !typeParameters.getTypeParameters().isEmpty()) {
            if (!first && typeParameters.getPrefix().getWhitespace().isEmpty()) {
                m = m.getAnnotations().withTypeParameters(
                        typeParameters.withPrefix(
                                typeParameters.getPrefix().withWhitespace(" ")
                        )
                );
                first = false;
            }
        }
        if (m.getReturnTypeExpression() != null && m.getReturnTypeExpression().getPrefix().getWhitespace().isEmpty()) {
            if (!first) {
                m = m.withReturnTypeExpression(m.getReturnTypeExpression()
                        .withPrefix(m.getReturnTypeExpression().getPrefix().withWhitespace(" ")));
            }
            first = false;
        }
        if (!first) {
            m = m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace(" ")));
        }

        if (m.getPadding().getThrows() != null) {
            Space before = m.getPadding().getThrows().getBefore();
            if (before.getWhitespace().isEmpty()) {
                m = m.getPadding().withThrows(m.getPadding().getThrows().withBefore(before.withWhitespace(" ")));
            }
        }

        return m;
    }

    @Override
    public J.Return visitReturn(J.Return retrn, P p) {
        J.Return r = super.visitReturn(retrn, p);
        if (r.getExpression() != null && r.getExpression().getPrefix().getWhitespace().isEmpty()) {
            r = r.withExpression(r.getExpression().withPrefix(r.getExpression().getPrefix().withWhitespace(" ")));
        }
        return r;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);

        J firstEnclosing = getCursor().getParentOrThrow().firstEnclosing(J.class);
        if (!(firstEnclosing instanceof J.Lambda)) {
            if (Space.firstPrefix(v.getVariables()).getWhitespace().isEmpty()) {
                v = v.withVariables(Space.formatFirstPrefix(v.getVariables(),
                        v.getVariables().iterator().next().getPrefix().withWhitespace(" ")));
            }
        }

        /*
         * We need at least one space between multiple modifiers, otherwise we could get a run-on like "publicstaticfinal".
         * Note, this is applicable anywhere that modifiers can exist, such as class declarations, etc.
         */
        if (!v.getModifiers().isEmpty()) {
            v = v.withModifiers(
                    ListUtils.map(v.getModifiers(), (index, modifier) -> {
                        if (index == 0) {
                            /*
                             * Skip the first modifier in the modifier list (if any). We only
                             * care about ensuring there is at least one space between multiple modifiers.
                             */
                        } else {
                            if (modifier.getPrefix().getWhitespace().isEmpty())
                                modifier = modifier.withPrefix(modifier.getPrefix().withWhitespace(" "));
                        }
                        return modifier;
                    })
            );
        }

        if (!v.getModifiers().isEmpty()) {
            if (v.getTypeExpression() != null && v.getTypeExpression().getPrefix().isEmpty()) {
                v = v.withTypeExpression(v.getTypeExpression().withPrefix(v.getTypeExpression().getPrefix().withWhitespace(" ")));
            }
        }

        return v;
    }
}
