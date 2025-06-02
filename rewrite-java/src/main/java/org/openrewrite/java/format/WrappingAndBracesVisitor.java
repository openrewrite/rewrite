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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
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
            Cursor possiblyClassDecl = getCursor().getParent(2);
            WrappingAndBracesStyle.Annotations annotationsStyle;
            if (possiblyClassDecl != null && possiblyClassDecl.getValue() instanceof J.ClassDeclaration) {
                annotationsStyle = style.getFieldAnnotations();
            } else {
                annotationsStyle = style.getLocalVariableAnnotations();
            }
            variableDeclarations = variableDeclarations.withLeadingAnnotations(wrapAnnotations(variableDeclarations.getLeadingAnnotations(), variableDeclarations.getPrefix(), annotationsStyle));
            if (!variableDeclarations.getLeadingAnnotations().isEmpty()) {
                if (!variableDeclarations.getModifiers().isEmpty()) {
                    variableDeclarations = variableDeclarations.withModifiers(withNewline(variableDeclarations.getModifiers(), variableDeclarations.getPrefix()));
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
        m = m.withLeadingAnnotations(wrapAnnotations(m.getLeadingAnnotations(), m.getPrefix(), style.getMethodAnnotations()));
        if (!m.getLeadingAnnotations().isEmpty()) {
            if (!m.getModifiers().isEmpty()) {
                m = m.withModifiers(withNewline(m.getModifiers(), m.getPrefix()));
            } else if (m.getAnnotations().getTypeParameters() != null) {
                m = m.getAnnotations().withTypeParameters(
                        m.getAnnotations().getTypeParameters().withPrefix(
                                withNewline(m.getPrefix())
                        )
                );
            } else if (m.getReturnTypeExpression() != null) {
                m = m.withReturnTypeExpression(
                        m.getReturnTypeExpression().withPrefix(
                                withNewline(m.getPrefix())
                        )
                );
            } else {
                m = m.withName(
                        m.getName().withPrefix(
                                withNewline(m.getPrefix())
                        )
                );
            }
        }
        m = m.withParameters(ListUtils.map(m.getParameters(), (param) -> {
            WrappingAndBracesStyle.Annotations annotationsStyle = style.getParameterAnnotations();
            if (param instanceof J.ClassDeclaration) {
                param = ((J.ClassDeclaration) param).withLeadingAnnotations(
                        wrapAnnotations(((J.ClassDeclaration) param).getLeadingAnnotations(), param.getPrefix(), annotationsStyle)
                );
            } else if (param instanceof J.MethodDeclaration) {
                param = ((J.MethodDeclaration) param).withLeadingAnnotations(
                        wrapAnnotations(((J.MethodDeclaration) param).getLeadingAnnotations(), param.getPrefix(), annotationsStyle)
                );
            } else if (param instanceof J.VariableDeclarations) {
                param = ((J.VariableDeclarations) param).withLeadingAnnotations(
                        wrapAnnotations(((J.VariableDeclarations) param).getLeadingAnnotations(), param.getPrefix(), annotationsStyle)
                );
            }
            return param;
        }));
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
        j = j.withLeadingAnnotations(wrapAnnotations(j.getLeadingAnnotations(), j.getPrefix(), style.getClassAnnotations()));
        if (!j.getLeadingAnnotations().isEmpty()) {
            if (!j.getModifiers().isEmpty()) {
                j = j.withModifiers(withNewline(j.getModifiers(), j.getPrefix()));
            } else {
                J.ClassDeclaration.Kind kind = j.getPadding().getKind();
                if (!kind.getPrefix().getWhitespace().contains("\n")) {
                    j = j.getPadding().withKind(kind.withPrefix(
                            kind.getPrefix().withWhitespace("\n" + kind.getPrefix().getWhitespace())
                    ));
                }
            }

        }
        return j;
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue _enum, P p) {
        J.EnumValue enumValue = super.visitEnumValue(_enum, p);
        enumValue = enumValue.withAnnotations(wrapAnnotations(enumValue.getAnnotations(), enumValue.getPrefix(), style.getEnumFieldAnnotations()));
        return enumValue;
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        if (!b.getEnd().getWhitespace().contains("\n")) {
            b = b.withEnd(withNewline(b.getEnd()));
        }
        return b;
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

    private List<J.Annotation> wrapAnnotations(List<J.Annotation> annotations, Space prefix, WrappingAndBracesStyle.Annotations annotationsStyle) {
        return ListUtils.map(annotations, (index, ann) -> {
            switch (annotationsStyle.getWrap()) {
                case DoNotWrap:
                    break;
                case WrapAlways:
                    if (index == 0) {
                        ann = ann.withPrefix(prefix.withWhitespace(""));
                        break;
                    }
                    ann = ann.withPrefix(prefix.withWhitespace((prefix.getWhitespace().contains("\n") ? "" : "\n") + prefix.getWhitespace()));
                    break;
            }
            return ann;
        });
    }

    private Space withNewline(Space prefix) {
        if (prefix.getComments().isEmpty()) {
            return prefix.withWhitespace((prefix.getWhitespace().contains("\n") ? "" : "\n") + prefix.getWhitespace());
        } else if (prefix.getComments().get(prefix.getComments().size()-1).isMultiline()) {
            return prefix.withComments(ListUtils.mapLast(prefix.getComments(), c -> c.withSuffix("\n")));
        }
        return prefix;
    }

    private List<J.Modifier> withNewline(List<J.Modifier> modifiers, Space prefix) {
        return ListUtils.mapFirst(modifiers,
                mod -> mod.withPrefix(prefix.withWhitespace(((prefix.getWhitespace().contains("\n") ? "" : "\n") + prefix.getWhitespace())))
        );
    }
}
