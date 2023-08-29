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


import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.marker.OmitBraces;
import org.openrewrite.kotlin.style.WrappingAndBracesStyle;

import java.util.List;
import java.util.Optional;

public class WrappingAndBracesVisitor<P> extends KotlinIsoVisitor<P> {
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
            J.Block parentBlock = (J.Block) parentTree;
            if (parentBlock.getMarkers().findFirst(OmitBraces.class).isPresent()) {
                return j;
            }


            if (j instanceof J.MethodDeclaration) {
                J.MethodDeclaration m = (J.MethodDeclaration) j;
                // no new line for constructor
                if ("<constructor>".equals(Optional.ofNullable(m.getMethodType()).map(JavaType.Method::getName).orElse(""))) {
                     return j;
                }
            }

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
        m = m.withLeadingAnnotations(withNewlines(m.getLeadingAnnotations()));

        List<J.Modifier> modifiers = method.getModifiers();
        modifiers = ListUtils.map(modifiers, mod -> {
            if (mod.getType() == J.Modifier.Type.LanguageExtension &&
                    // mod.getKeyword().equals("fun") &&
                    !mod.getAnnotations().isEmpty()) {
                mod = mod.withAnnotations(ListUtils.map(mod.getAnnotations(), (index, anno) -> {
                    if (index > 0 && !anno.getPrefix().getWhitespace().contains("\n")) {
                        return anno.withPrefix(withNewline(anno.getPrefix()));
                    }
                    return anno;
                }));

                if (!mod.getPrefix().getWhitespace().contains("\n")) {
                    mod = mod.withPrefix(withNewline(mod.getPrefix()));
                }
            }
            return mod;
        });

        m = m.withModifiers(modifiers);

        if (!m.getLeadingAnnotations().isEmpty()) {
            modifiers = method.getModifiers();

            // loop up first modifier needs to be in a new line
            int firstModifierIndex = -1;
            for (int i = 0; i < method.getModifiers().size(); i++) {
                if (method.getModifiers().get(i).getType() != J.Modifier.Type.Final) {
                    if (!method.getModifiers().get(i).getPrefix().getWhitespace().contains("\n")) {
                        firstModifierIndex = i;
                    }
                    break;
                }
            }

            if (firstModifierIndex >= 0) {
                int finalIndex = firstModifierIndex;
                m = m.withModifiers(ListUtils.map(modifiers, (index, mod) -> {
                    if (finalIndex == index) {
                        return mod.withPrefix(withNewline(mod.getPrefix()));
                    }
                    return mod;
                }));
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
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        c = c.withLeadingAnnotations(withNewlines(c.getLeadingAnnotations()));

        J.ClassDeclaration.Kind k = c.getAnnotations().getKind();
        List<J.Annotation> leadingAnnotations = k.getAnnotations();
        if (!leadingAnnotations.isEmpty()) {
            leadingAnnotations = ListUtils.map(leadingAnnotations, (index, anno) -> {
                if (index > 0 && !anno.getPrefix().getWhitespace().contains("\n")) {
                    return anno.withPrefix(withNewline(anno.getPrefix()));
                }
                return anno;
            });
            k = k.withAnnotations(leadingAnnotations);
            if (!k.getPrefix().getWhitespace().contains("\n")) {
                k = k.withPrefix(withNewline(k.getPrefix()));
            }
            c = c.getAnnotations().withKind(k);
        }

        if (!c.getLeadingAnnotations().isEmpty()) {
            boolean hasModifier = false;
            for (J.Modifier mod : c.getModifiers()) {
                if (mod.getType() != J.Modifier.Type.Final) {
                    hasModifier = true;
                    break;
                }
            }

            if (hasModifier) {
                c = c.withModifiers(withNewline(c.getModifiers()));
            } else {
                J.ClassDeclaration.Kind kind = c.getAnnotations().getKind();
                if (!kind.getPrefix().getWhitespace().contains("\n")) {
                    c = c.getAnnotations().withKind(kind.withPrefix(
                            kind.getPrefix().withWhitespace("\n" + kind.getPrefix().getWhitespace())
                    ));
                }
            }
        }
        return c;
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
        if (!b.getMarkers().findFirst(OmitBraces.class).isPresent() && !b.getEnd().getWhitespace().contains("\n")) {
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
        if (firstModifier.getType() == J.Modifier.Type.Final) {
            return modifiers;
        }

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

