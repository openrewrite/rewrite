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
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.StringUtils.hasLineBreak;
import static org.openrewrite.style.LineWrapSetting.DoNotWrap;
import static org.openrewrite.style.LineWrapSetting.WrapAlways;

public class WrappingAndBracesVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final SpacesStyle spacesStyle;
    private final WrappingAndBracesStyle style;

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style) {
        this(IntelliJ.spaces(), style, null);
    }

    public WrappingAndBracesVisitor(SpacesStyle spacesStyle, WrappingAndBracesStyle style, @Nullable Tree stopAfter) {
        this.spacesStyle = spacesStyle;
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement j = super.visitStatement(statement, p);
        Tree parentTree = getCursor().getParentTreeCursor().getValue();
        if (parentTree instanceof J.Block && !(j instanceof J.EnumValueSet)) {
            // for `J.EnumValueSet` the prefix is on the enum constants
            j = j.withPrefix(withNewline(j.getPrefix()));
        }

        return j;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, p);
        String whitespace = variableDeclarations.getPrefix().getWhitespace().replaceFirst("^[\\n\\s]+\\n", "\n");
        WrappingAndBracesStyle.Annotations annotationsStyle = null;
        Cursor possiblyBlock = getCursor().dropParentUntil(J.class::isInstance);
        if (possiblyBlock.getValue() instanceof J.Block) {
            if (possiblyBlock.getParent() != null && possiblyBlock.getParent().getValue() instanceof J.ClassDeclaration) {
                annotationsStyle = style.getFieldAnnotations();
            } else {
                annotationsStyle = style.getLocalVariableAnnotations();
            }
            variableDeclarations = variableDeclarations.withLeadingAnnotations(wrapAnnotations(variableDeclarations.getLeadingAnnotations(), whitespace, annotationsStyle));
        } else if (getCursor().getParent(3) != null && (getCursor().getParent(3).getValue() instanceof J.ClassDeclaration || getCursor().getParent(3).getValue() instanceof J.MethodDeclaration)) {
            annotationsStyle = style.getParameterAnnotations();
            variableDeclarations = variableDeclarations.withLeadingAnnotations(wrapAnnotations(variableDeclarations.getLeadingAnnotations(), whitespace, annotationsStyle));
        }
        if (!variableDeclarations.getLeadingAnnotations().isEmpty() && annotationsStyle != null) {
            if (!variableDeclarations.getModifiers().isEmpty()) {
                variableDeclarations = variableDeclarations.withModifiers(withNewline(variableDeclarations.getModifiers(), whitespace, annotationsStyle));
            } else {
                variableDeclarations = variableDeclarations.withTypeExpression(
                        variableDeclarations.getTypeExpression()
                                .withPrefix(wrapElement(variableDeclarations.getTypeExpression().getPrefix(), whitespace, annotationsStyle))
                );
            }
        }
        return variableDeclarations;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        String whitespace = m.getPrefix().getWhitespace().replaceFirst("^[\\n\\s]+\\n", "\n");
        m = m.withLeadingAnnotations(wrapAnnotations(m.getLeadingAnnotations(), whitespace, style.getMethodAnnotations()));
        if (!m.getLeadingAnnotations().isEmpty() && style.getMethodAnnotations() != null) {
            if (!m.getModifiers().isEmpty()) {
                m = m.withModifiers(withNewline(m.getModifiers(), whitespace, style.getMethodAnnotations()));
            } else if (m.getAnnotations().getTypeParameters() != null) {
                m = m.getAnnotations().withTypeParameters(
                        m.getAnnotations().getTypeParameters()
                                .withPrefix(wrapElement(m.getAnnotations().getTypeParameters().getPrefix(), whitespace, style.getMethodAnnotations()))
                );
            } else if (m.getReturnTypeExpression() != null) {
                m = m.withReturnTypeExpression(
                        m.getReturnTypeExpression()
                                .withPrefix(wrapElement(m.getReturnTypeExpression().getPrefix(), whitespace, style.getMethodAnnotations()))
                );
            } else {
                m = m.withName(
                        m.getName()
                                .withPrefix(wrapElement(m.getName().getPrefix(), whitespace, style.getMethodAnnotations()))
                );
            }
        }
        return (J.MethodDeclaration) new WrapMethodDeclarationParameters<>(spacesStyle, style).visit(m, p, getCursor().getParentTreeCursor());
    }

    @Override
    public J.If.Else visitElse(J.If.Else else_, P p) {
        J.If.Else e = super.visitElse(else_, p);
        boolean hasBody = e.getBody() instanceof J.Block || e.getBody() instanceof J.If;
        if (hasBody) {
            if (style.getIfStatement().getElseOnNewLine() && !hasLineBreak(e.getPrefix().getWhitespace())) {
                e = e.withPrefix(e.getPrefix().withWhitespace("\n" + e.getPrefix().getWhitespace()));
            } else if (!style.getIfStatement().getElseOnNewLine() && hasLineBreak(e.getPrefix().getWhitespace())) {
                e = e.withPrefix(Space.EMPTY);
            }
        }

        return e;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        method = (J.MethodInvocation) new WrapMethodChains<>(style).visit(method, p, getCursor().getParentTreeCursor());
        return super.visitMethodInvocation(method, p);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration j = super.visitClassDeclaration(classDecl, p);
        String whitespace = j.getPrefix().getWhitespace().replaceFirst("^[\\n\\s]+\\n", "\n");
        j = j.withLeadingAnnotations(wrapAnnotations(j.getLeadingAnnotations(), whitespace, style.getClassAnnotations()));
        if (!j.getLeadingAnnotations().isEmpty() && style.getClassAnnotations() != null) {
            if (!j.getModifiers().isEmpty()) {
                j = j.withModifiers(withNewline(j.getModifiers(), whitespace, style.getClassAnnotations()));
            } else {
                J.ClassDeclaration.Kind kind = j.getPadding().getKind();
                if (!hasLineBreak(kind.getPrefix().getWhitespace())) {
                    j = j.getPadding().withKind(kind.withPrefix(wrapElement(kind.getPrefix(), whitespace, style.getClassAnnotations())));
                }
            }
        }
        return j;
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue _enum, P p) {
        J.EnumValue enumValue = super.visitEnumValue(_enum, p);
        String whitespace = enumValue.getPrefix().getWhitespace().replaceFirst("^[\\n\\s]+\\n", "\n");
        enumValue = enumValue.withAnnotations(wrapAnnotations(enumValue.getAnnotations(), whitespace, style.getEnumFieldAnnotations()));
        if (!enumValue.getAnnotations().isEmpty() && style.getEnumFieldAnnotations() != null) {
            enumValue = enumValue.withName(
                    enumValue.getName()
                            .withPrefix(wrapElement(enumValue.getName().getPrefix(), whitespace, style.getEnumFieldAnnotations()))
            );
        }
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

    private List<J.Annotation> wrapAnnotations(List<J.Annotation> annotations, String whitespace, WrappingAndBracesStyle.@Nullable Annotations annotationsStyle) {
        if (annotationsStyle == null) {
            return annotations;
        }
        return ListUtils.map(annotations, (index, ann) -> {
            if (annotationsStyle.getWrap() == DoNotWrap && hasLineBreak(ann.getPrefix().getWhitespace())) {
                ann = ann.withPrefix(ann.getPrefix().withWhitespace(Space.SINGLE_SPACE.getWhitespace()));
            } else if (annotationsStyle.getWrap() == WrapAlways && index > 0) {
                ann = ann.withPrefix(ann.getPrefix().withWhitespace((whitespace.startsWith("\n") ? "" : "\n") + whitespace));
            }
            return ann;
        });
    }

    private Space wrapElement(Space prefix, String whitespace, WrappingAndBracesStyle.@Nullable Annotations annotationsStyle) {
        if (prefix.getComments().isEmpty() && annotationsStyle != null) {
            if (annotationsStyle.getWrap() == DoNotWrap && (hasLineBreak(prefix.getWhitespace()) || prefix.isEmpty())) {
                return prefix.withWhitespace(Space.SINGLE_SPACE.getWhitespace());
            } else if (annotationsStyle.getWrap() == WrapAlways) {
                return prefix.withWhitespace((whitespace.startsWith("\n") ? "" : "\n") + whitespace);
            }
        }
        return prefix;
    }

    private Space withNewline(Space prefix) {
        if (prefix.getComments().isEmpty()) {
            return prefix.withWhitespace((hasLineBreak(prefix.getWhitespace()) ? prefix.getWhitespace() : "\n"));
        } else if (prefix.getComments().get(prefix.getComments().size() - 1).isMultiline()) {
            return prefix.withComments(ListUtils.mapLast(prefix.getComments(), (Function<Comment, Comment>) c ->
                    c.withSuffix((hasLineBreak(c.getSuffix()) ? c.getSuffix() : "\n"))));
        }
        return prefix;
    }

    private List<J.Modifier> withNewline(List<J.Modifier> modifiers, String whitespace, WrappingAndBracesStyle.@Nullable Annotations annotationsStyle) {
        return ListUtils.mapFirst(modifiers, mod -> requireNonNull(mod).withPrefix(wrapElement(mod.getPrefix(), whitespace, annotationsStyle)));
    }
}
