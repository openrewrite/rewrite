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

import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.List;

public class WrappingAndBracesVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final WrappingAndBracesStyle style;

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style) {
        this(style, null);
    }

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement j = super.visitStatement(statement, p);
        Tree parentTree = getCursor().getParentTreeCursor().getValue();
        if (parentTree instanceof J.Block && !(j instanceof J.EnumValueSet)) {
            // for `J.EnumValueSet` the prefix is on the enum constants
            if (!j.getPrefix().getWhitespace().contains("\n")) {
                j = j.withPrefix(withNewline(j.getPrefix()));
            }
        }

        return j;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {

        J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, p);
        if (getCursor().getParent() != null && getCursor().getParent().firstEnclosing(J.class) instanceof J.Block) {

            variableDeclarations = variableDeclarations.withLeadingAnnotations(withNewlines(variableDeclarations.getLeadingAnnotations()));

            if (!variableDeclarations.getLeadingAnnotations().isEmpty()) {
                if (!variableDeclarations.getModifiers().isEmpty()) {
                    variableDeclarations = variableDeclarations.withModifiers(withNewline(variableDeclarations.getModifiers()));
                } else if (variableDeclarations.getTypeExpression() != null &&
                        !variableDeclarations.getTypeExpression().getPrefix().getWhitespace().contains("\n")) {
                    variableDeclarations = variableDeclarations.withTypeExpression(
                            variableDeclarations.getTypeExpression().withPrefix(withNewline(variableDeclarations.getTypeExpression().getPrefix()))
                    );
                }
            }
        }
        return variableDeclarations;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        // TODO make annotation wrapping configurable
        m = m.withLeadingAnnotations(withNewlines(m.getLeadingAnnotations()));
        if (!m.getLeadingAnnotations().isEmpty()) {
            if (!m.getModifiers().isEmpty()) {
                m = m.withModifiers(withNewline(m.getModifiers()));
            } else if (m.getAnnotations().getTypeParameters() != null) {
                if (!m.getAnnotations().getTypeParameters().getPrefix().getWhitespace().contains("\n")) {
                    m = m.getAnnotations().withTypeParameters(
                            m.getAnnotations().getTypeParameters().withPrefix(
                                    withNewline(m.getAnnotations().getTypeParameters().getPrefix())
                            )
                    );
                }
            } else if (m.getReturnTypeExpression() != null) {
                if (!m.getReturnTypeExpression().getPrefix().getWhitespace().contains("\n")) {
                    m = m.withReturnTypeExpression(
                            m.getReturnTypeExpression().withPrefix(
                                    withNewline(m.getReturnTypeExpression().getPrefix())
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

    @Override
    public J.If.Else visitElse(J.If.Else else_, P p) {
        J.If.Else e = super.visitElse(else_, p);
        boolean hasBody = e.getBody() instanceof J.Block || e.getBody() instanceof J.If;
        if (hasBody) {
            if (style.getIfStatement().getElseOnNewLine() && !e.getPrefix().getWhitespace().contains("\n")) {
                e = e.withPrefix(e.getPrefix().withWhitespace("\n" + e.getPrefix().getWhitespace()));
            } else if (!style.getIfStatement().getElseOnNewLine() && e.getPrefix().getWhitespace().contains("\n")) {
                e = e.withPrefix(Space.EMPTY);
            }
        }

        return e;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration j = super.visitClassDeclaration(classDecl, p);
        // TODO make annotation wrapping configurable
        j = j.withLeadingAnnotations(withNewlines(j.getLeadingAnnotations()));
        if (!j.getLeadingAnnotations().isEmpty()) {
            if (!j.getModifiers().isEmpty()) {
                j = j.withModifiers(withNewline(j.getModifiers()));
            } else {
                J.ClassDeclaration.Kind kind = j.getAnnotations().getKind();
                if (!kind.getPrefix().getWhitespace().contains("\n")) {
                    j = j.getAnnotations().withKind(kind.withPrefix(
                            kind.getPrefix().withWhitespace("\n" + kind.getPrefix().getWhitespace())
                    ));
                }
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
        if (space.getComments().isEmpty()) {
            space = space.withWhitespace("\n" + space.getWhitespace());
        } else if (space.getComments().get(space.getComments().size()-1).isMultiline()) {
            space = space.withComments(ListUtils.mapLast(space.getComments(), c -> c.withSuffix("\n")));
        }

        return space;
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

    @Nullable
    @Override
    public J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
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
