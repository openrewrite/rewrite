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


import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.OmitBraces;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.marker.PrimaryConstructor;
import org.openrewrite.kotlin.marker.TrailingLambdaArgument;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.style.OtherStyle;
import org.openrewrite.kotlin.style.WrappingAndBracesStyle;
import org.openrewrite.marker.Marker;
import org.openrewrite.style.LineWrapSetting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.openrewrite.Tree.randomId;

public class WrappingAndBracesVisitor<P> extends KotlinIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final WrappingAndBracesStyle style;
    private final OtherStyle otherStyle;

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style) {
        this(style, null);
    }

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style, @Nullable Tree stopAfter) {
        this(style, IntelliJ.other(), stopAfter);
    }

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style, OtherStyle otherStyle, @Nullable Tree stopAfter) {
        this.style = style;
        this.otherStyle = otherStyle;
        this.stopAfter = stopAfter;
    }

    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, P p) {
        if (container != null && getCursor().getNearestMessage("stop") == null) {
            LineWrapSetting wrap = wrapSetting(loc);
            if (wrap == LineWrapSetting.WrapAlways || wrap == LineWrapSetting.ChopIfTooLong && exceedsHardWrap(container)) {
                container = wrap(container, loc);
            }
        }
        return super.visitContainer(container, loc, p);
    }

    private @Nullable LineWrapSetting wrapSetting(JContainer.Location loc) {
        switch (loc) {
            case METHOD_DECLARATION_PARAMETERS:
                return style.getFunctionDeclarationParameters().getWrap();
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                return style.getFunctionCallArguments().getWrap();
            default:
                return null;
        }
    }

    /**
     * Index of the last element that sits between the parentheses: a trailing lambda is printed after them and an
     * empty argument list holds a single {@link J.Empty}.
     */
    private static int lastWrappable(List<? extends JRightPadded<? extends J>> elements) {
        int last = elements.size() - 1;
        if (last >= 0 && elements.get(last).getElement().getMarkers().findFirst(TrailingLambdaArgument.class).isPresent()) {
            last--;
        }
        if (last >= 0 && elements.get(last).getElement() instanceof J.Empty) {
            last--;
        }
        return last;
    }

    /**
     * Measures the container by printing the nearest enclosing tree that starts a line, so a detached tree formatted
     * against a cursor is measured at the indentation of its prefix rather than of the source file it came from.
     */
    private boolean exceedsHardWrap(JContainer<? extends J> container) {
        List<? extends JRightPadded<? extends J>> elements = container.getPadding().getElements();
        int last = lastWrappable(elements);
        if (last < 0) {
            return false;
        }
        LineSentinel start = new LineSentinel(randomId());
        LineSentinel end = new LineSentinel(randomId());
        JContainer<J> marked = ((JContainer<J>) container).getPadding().withElements(ListUtils.map(((JContainer<J>) container).getPadding().getElements(), (i, e) -> {
            if (i == 0) {
                e = e.withElement(e.getElement().withMarkers(e.getElement().getMarkers().add(start)));
            }
            if (i == last) {
                e = e.withElement(e.getElement().withMarkers(e.getElement().getMarkers().add(end)));
            }
            return e;
        }));

        Cursor lineRoot = getCursor();
        for (Cursor c = getCursor(); c != null && !(c.getValue() instanceof SourceFile); c = c.getParent()) {
            if (c.getValue() instanceof J) {
                lineRoot = c;
                if (((J) c.getValue()).getPrefix().getWhitespace().contains("\n")) {
                    break;
                }
            }
        }
        J toPrint = new KotlinIsoVisitor<Integer>() {
            @Override
            public <J3 extends J> @Nullable JContainer<J3> visitContainer(@Nullable JContainer<J3> c, JContainer.Location loc, Integer p) {
                //noinspection unchecked
                return c == container ? (JContainer<J3>) marked : super.visitContainer(c, loc, p);
            }
        }.visitNonNull(lineRoot.getValue(), 0, lineRoot.getParentOrThrow());
        String text = toPrint.print(lineRoot.getParentOrThrow(), new PrintOutputCapture<>(0, new PrintOutputCapture.MarkerPrinter() {
            @Override
            public String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                return marker == start ? "\u0001" : "";
            }

            @Override
            public String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                return marker == end ? "\u0002" : "";
            }
        }));

        int s = text.indexOf('\u0001');
        int e = text.indexOf('\u0002');
        if (s < 0 || e < 0) {
            return false;
        }
        int maxColumn = 0;
        int column = 0;
        for (int i = text.lastIndexOf('\n', s) + 1; i < e; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                column = 0;
            } else if (c != '\u0001') {
                column++;
                maxColumn = Math.max(maxColumn, column);
            }
        }
        JRightPadded<? extends J> lastElement = elements.get(last);
        if (!lastElement.getAfter().getWhitespace().contains("\n")) {
            int closing = column + lastElement.getAfter().getWhitespace().length() +
                    (lastElement.getMarkers().findFirst(TrailingComma.class).isPresent() ? 1 : 0) + 1;
            maxColumn = Math.max(maxColumn, closing);
        }
        return maxColumn > style.getHardWrapAt();
    }

    private <J2 extends J> JContainer<J2> wrap(JContainer<J2> container, JContainer.Location loc) {
        List<JRightPadded<J2>> elements = container.getPadding().getElements();
        int last = lastWrappable(elements);
        if (last < 0) {
            return container;
        }
        boolean openNewLine;
        boolean closeNewLine;
        if (loc == JContainer.Location.METHOD_DECLARATION_PARAMETERS) {
            openNewLine = style.getFunctionDeclarationParameters().getNewLineAfterLeftParen();
            closeNewLine = style.getFunctionDeclarationParameters().getPlaceRightParenOnNewLine();
        } else {
            openNewLine = style.getFunctionCallArguments().getNewLineAfterLeftParen();
            closeNewLine = style.getFunctionCallArguments().getPlaceRightParenOnNewLine();
        }
        return container.getPadding().withElements(ListUtils.map(elements, (i, e) -> {
            if (i > last) {
                return e;
            }
            J2 element = e.getElement();
            if ((i > 0 || openNewLine) && !element.getPrefix().getWhitespace().contains("\n")) {
                e = e.withElement(element.withPrefix(withNewline(element.getPrefix())));
            }
            return i == last && closeNewLine ? closeOnNewLine(e) : e;
        }));
    }

    private <J2 extends J> JRightPadded<J2> closeOnNewLine(JRightPadded<J2> last) {
        TrailingComma trailingComma = last.getMarkers().findFirst(TrailingComma.class).orElse(null);
        if (trailingComma != null) {
            if (!trailingComma.getSuffix().getWhitespace().contains("\n")) {
                last = last.withMarkers(last.getMarkers().setByType(trailingComma.withSuffix(withNewline(trailingComma.getSuffix()))));
            }
        } else if (otherStyle.getUseTrailingComma()) {
            last = last.withMarkers(last.getMarkers().add(new TrailingComma(randomId(), withNewline(last.getAfter())))).withAfter(Space.EMPTY);
        } else if (!last.getAfter().getWhitespace().contains("\n")) {
            last = last.withAfter(withNewline(last.getAfter()));
        }
        return last;
    }

    @Value
    @With
    private static class LineSentinel implements Marker {
        UUID id;
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement j = super.visitStatement(statement, p);
        Tree parentTree = getCursor().getParentTreeCursor().getValue();

        if (parentTree instanceof J.Block && !(j instanceof J.EnumValueSet)) {
            J.Block parentBlock = (J.Block) parentTree;
            if (parentBlock.getMarkers().findFirst(OmitBraces.class).isPresent() ||
                parentBlock.getMarkers().findFirst(org.openrewrite.kotlin.marker.OmitBraces.class).isPresent()) {
                return j;
            }


            if (j instanceof J.MethodDeclaration) {
                J.MethodDeclaration m = (J.MethodDeclaration) j;
                // no new line for constructor
                if (Optional.ofNullable(m.getMethodType()).map(JavaType.Method::isConstructor).orElse(false)) {
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
        Cursor parentCursor = getCursor().getParentTreeCursor();
        if (parentCursor.getValue() instanceof J.Block) {
            variableDeclarations = variableDeclarations.withLeadingAnnotations(withNewlines(variableDeclarations.getLeadingAnnotations()));

            J grandparent;
            if (!variableDeclarations.getLeadingAnnotations().isEmpty() &&
                    ((grandparent = parentCursor.getParentTreeCursor().getValue()) instanceof J.ClassDeclaration || grandparent instanceof J.NewClass)) {
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
        if (m.getMarkers().findFirst(PrimaryConstructor.class).isPresent()) {
            return m;
        }

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

        J.ClassDeclaration.Kind k = c.getPadding().getKind();
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
            c = c.getPadding().withKind(k);
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
                J.ClassDeclaration.Kind kind = c.getPadding().getKind();
                Space kindPrefix = kind.getPrefix();
                if (!kindPrefix.getWhitespace().contains("\n") && kindPrefix.getComments().isEmpty()) {
                    kindPrefix = kindPrefix.withWhitespace("\n" + kindPrefix.getWhitespace());
                    c = c.getPadding().withKind(kind.withPrefix(kindPrefix));
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
        if (!b.getMarkers().findFirst(OmitBraces.class).isPresent() &&
                !b.getStatements().isEmpty() &&
                !b.getEnd().getWhitespace().contains("\n")) {
            b = b.withEnd(withNewline(b.getEnd()));
        }
        return b;
    }

    private Space withNewline(Space space) {
        if (space.getComments().isEmpty()) {
            space = space.withWhitespace("\n" + space.getWhitespace());
        } else if (space.getComments().get(space.getComments().size() - 1).isMultiline()) {
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
