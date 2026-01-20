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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.ToBeRemoved;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.style.LineWrapSetting;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JContainer.Location.THROWS;
import static org.openrewrite.style.LineWrapSetting.DoNotWrap;
import static org.openrewrite.style.LineWrapSetting.WrapAlways;

public class WrappingAndBracesVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final SpacesStyle spacesStyle;
    private final WrappingAndBracesStyle style;

    public WrappingAndBracesVisitor(SourceFile sourceFile, @Nullable Tree stopAfter) {
        this(sourceFile.getMarkers().findAll(NamedStyles.class), stopAfter);
    }

    public WrappingAndBracesVisitor(List<NamedStyles> styles, @Nullable Tree stopAfter) {
        this(getStyle(SpacesStyle.class, styles, IntelliJ::spaces), getStyle(WrappingAndBracesStyle.class, styles, IntelliJ::wrappingAndBraces), stopAfter);
    }

    public WrappingAndBracesVisitor(SpacesStyle spacesStyle, WrappingAndBracesStyle wrappingAndBracesStyle, @Nullable Tree stopAfter) {
        this.spacesStyle = spacesStyle;
        this.style = wrappingAndBracesStyle;
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable <J2 extends J> JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, P p) {
        if (container == null) {
            //noinspection ConstantConditions
            return null;
        }
        if (getCursor().getNearestMessage("stop") != null) {
            return container;
        }
        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
        int size = container.getElements().stream().allMatch(it -> it instanceof J.Empty) ? 0 : container.getElements().size();
        AtomicBoolean hasNewLinedElement = new AtomicBoolean(false);
        List<JRightPadded<J2>> js = ListUtils.map(container.getPadding().getElements(), (index, right) -> {
            J jElement = right.getElement();
            if (jElement instanceof J.Empty) {
                return right;
            }
            if (shouldWrap(index, size, (JContainer<J>) container, loc)) {
                hasNewLinedElement.set(true);
                right = right.withElement(jElement.withPrefix(withWhitespace(jElement.getPrefix(), "\n")));
            }
            if (index == size - 1 && hasNewLinedElement.get() && closesOnNewLine(loc)) {
                right = right.withAfter(withWhitespace(right.getAfter(), "\n"));
            }
            return visitRightPadded(right, loc.getElementLocation(), p);
        });

        setCursor(getCursor().getParent());

        return js == container.getPadding().getElements() && before == container.getBefore() ? container : JContainer.build(before, js, container.getMarkers());
    }

    @Override
    public @Nullable <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (right == null) {
            return super.visitRightPadded(null, loc, p);
        }
        if (getCursor().getNearestMessage("stop") != null) {
            return right;
        }
        JRightPadded<?> wrappedRight = right;
        JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
        SourcePositionService positionService = null;
        if (sourceFile != null) {
            positionService = sourceFile.service(SourcePositionService.class);
        }
        switch (loc) {
            case METHOD_SELECT:
                J.MethodInvocation m = getCursor().getValue();
                J.MethodInvocation chainStarter = findChainStarterInChain(m);
                // If there is no chain starter in the chain, or the current method is the actual chain starter call and we do not need to wrap the first call
                if (!wrappedRight.getAfter().getWhitespace().contains("\n") && (m.getPadding().getSelect() == null || (!evaluate(() -> style.getChainedMethodCalls().getWrapFirstCall(), false) && chainStarter == m))) {
                    break;
                }

                boolean isBuilderMethod = evaluate(() -> style.getChainedMethodCalls().getBuilderMethods().stream(), Stream.empty())
                        .map(name -> String.format("*..* %s(..)", name))
                        .map(MethodMatcher::new)
                        .anyMatch(matcher -> matcher.matches(chainStarter));
                if (wrappedRight.getAfter().getWhitespace().contains("\n") || isBuilderMethod || (evaluate(() -> style.getChainedMethodCalls().getWrap(), DoNotWrap) == LineWrapSetting.WrapAlways || evaluate(() -> style.getChainedMethodCalls().getWrap(), DoNotWrap) == LineWrapSetting.ChopIfTooLong)) {
                    // always wrap builder methods
                    if (!isBuilderMethod && evaluate(() -> style.getChainedMethodCalls().getWrap(), DoNotWrap) == LineWrapSetting.ChopIfTooLong) {
                        if (positionService == null) {
                            break;
                        }
                        Cursor cursor = getCursor();
                        while (cursor.getParentTreeCursor().getValue() instanceof J.MethodInvocation) {
                            cursor = cursor.getParentTreeCursor();
                        }
                        // Not long enough to wrap
                        if (positionService.columnsOf(cursor).getMaxColumn() <= evaluate(() -> style.getHardWrapAt(), 120)) {
                            break;
                        }
                    }

                    boolean wrapFirstCall = evaluate(() -> style.getChainedMethodCalls().getWrapFirstCall(), false);
                    if (wrapFirstCall || chainStarter != m) {
                        wrappedRight = wrappedRight.withAfter(withWhitespace(wrappedRight.getAfter(), "\n"));
                    }
                }
                break;
            case BLOCK_STATEMENT:
                if (wrappedRight.getElement() instanceof J && getCursor().getValue() instanceof J.Block && !(wrappedRight.getElement() instanceof J.EnumValueSet)) {
                    // for `J.EnumValueSet` the prefix is on the enum constants
                    wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n")));
                }
                break;
            case CASE:
                if (!(right.getElement() instanceof J.Block)) {
                    wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n")));
                }
                break;
            case IF_ELSE:
                if (wrappedRight.getElement() instanceof J.If) {
                    break;
                }
                //Falling through on purpose here
            case IF_THEN:
                if (!(wrappedRight.getElement() instanceof J.Block)) {
                    if (evaluate(() -> style.getIfStatement().getForceBraces(), WrappingAndBracesStyle.ForceBraces.DoNotForce) != WrappingAndBracesStyle.ForceBraces.DoNotForce) {
                        wrappedRight = ((JRightPadded<Statement>) wrappedRight).withElement(J.Block.createEmptyBlock()
                                .withPrefix(Space.format(evaluate(() -> spacesStyle.getBeforeLeftBrace().getIfLeftBrace(), true) ? " " : ""))
                                .withStatements(singletonList((Statement) wrappedRight.getElement())));
                    }
                    if (getCursor().getValue() instanceof J.If) {
                        setCursor(updateCursor(((J.If) getCursor().getValue()).getPadding().withThenPart((JRightPadded<Statement>) wrappedRight)));
                    } else {
                        setCursor(updateCursor(((J.If.Else) getCursor().getValue()).getPadding().withBody((JRightPadded<Statement>) wrappedRight)));
                    }
                }
                break;
            case FOR_INIT:
                if (shouldWrap(getCursor(), loc)) {
                    wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n")));
                }
                break;
            case FOR_CONDITION:
            case FOR_UPDATE:
                if (shouldWrap(getCursor(), loc)) {
                    wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n")));
                    if (loc == JRightPadded.Location.FOR_UPDATE && evaluate(() -> style.getForStatement().getCloseNewLine(), false)) {
                        wrappedRight = wrappedRight.withAfter(withWhitespace(wrappedRight.getAfter(), "\n"));
                    }
                }
                break;
            case FOR_BODY:
                if (evaluate(() -> style.getForStatement().getForceBraces(), WrappingAndBracesStyle.ForceBraces.DoNotForce) == WrappingAndBracesStyle.ForceBraces.DoNotForce) {
                    break;
                }
                if (!(wrappedRight.getElement() instanceof J.Block)) {
                    wrappedRight = ((JRightPadded<Statement>) wrappedRight).withElement(J.Block.createEmptyBlock()
                            .withPrefix(Space.format(evaluate(() -> spacesStyle.getBeforeLeftBrace().getForLeftBrace(), true) ? " " : ""))
                            .withStatements(singletonList((Statement) wrappedRight.getElement())));
                }
                break;
            case WHILE_BODY:
                if (getCursor().getValue() instanceof J.DoWhileLoop && evaluate(() -> style.getDoWhileStatement().getForceBraces(), WrappingAndBracesStyle.ForceBraces.DoNotForce) == WrappingAndBracesStyle.ForceBraces.DoNotForce) {
                    break;
                }
                if (getCursor().getValue() instanceof J.WhileLoop && evaluate(() -> style.getWhileStatement().getForceBraces(), WrappingAndBracesStyle.ForceBraces.DoNotForce) == WrappingAndBracesStyle.ForceBraces.DoNotForce) {
                    break;
                }
                if (!(wrappedRight.getElement() instanceof J.Block)) {
                    Statement element = (Statement) wrappedRight.getElement();
                    element = J.Block.createEmptyBlock().withStatements(singletonList(element));
                    if (getCursor().getValue() instanceof J.DoWhileLoop) {
                        element = element.withPrefix(Space.format(evaluate(() -> spacesStyle.getBeforeLeftBrace().getDoLeftBrace(), true) ? " " : ""));
                    } else if (getCursor().getValue() instanceof J.WhileLoop) {
                        element = element.withPrefix(Space.format(evaluate(() -> spacesStyle.getBeforeLeftBrace().getWhileLeftBrace(), true) ? " " : ""));
                    }
                    wrappedRight = ((JRightPadded<Statement>) wrappedRight).withElement(element);
                }
                break;
            case ENUM_VALUE:
                List<J.EnumValue> enums = ((J.EnumValueSet) getCursor().getValue()).getEnums();
                if (!enums.isEmpty()) {
                    if (shouldWrap(getCursor(), loc)) {
                        wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n")));
                        getCursor().pollNearestMessage("single-line-enum");
                    }
                }
        }

        return (JRightPadded<T>) super.visitRightPadded(wrappedRight, loc, p);
    }

    @Override
    public Space visitSpace(@Nullable Space space, Space.Location loc, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return space;
        }
        JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
        if (space != null && sourceFile != null) {
            SourcePositionService positionService = sourceFile.service(SourcePositionService.class);
            Cursor parentTreeCursor;
            J parent;
            LineWrapSetting wrap = null;
            switch (loc) {
                case BLOCK_END:
                    if (Boolean.TRUE.equals(getCursor().pollMessage("single-line-enum"))) {
                        break;
                    }
                    if (((J.Block) getCursor().getValue()).getStatements().isEmpty()) {
                        parent = getCursor().getParentTreeCursor().getValue();
                        boolean keepSingleLine;
                        if (parent instanceof J.ClassDeclaration) {
                            keepSingleLine = evaluate(() -> style.getKeepWhenFormatting().getSimpleClassesInOneLine(), false);
                        } else if (parent instanceof J.MethodDeclaration) {
                            keepSingleLine = evaluate(() -> style.getKeepWhenFormatting().getSimpleMethodsInOneLine(), false);
                        } else if (parent instanceof J.Lambda) {
                            keepSingleLine = evaluate(() -> style.getKeepWhenFormatting().getSimpleLambdasInOneLine(), false);
                        } else {
                            keepSingleLine = evaluate(() -> style.getKeepWhenFormatting().getSimpleBlocksInOneLine(), false);
                        }
                        if (keepSingleLine) {
                            break;
                        }
                    }
                    space = withWhitespace(space, "\n");
                    break;
                case CASE_PREFIX:
                    if (!evaluate(() -> style.getSwitchStatement().getEachCaseOnSeparateLine(), true)) {
                        J.Switch switch_ = getCursor().getParentTreeCursor().getValue();
                        List<Statement> cases = switch_.getCases().getStatements();
                        int index = cases.indexOf(getCursor().getParentTreeCursor().getValue());
                        if (index > 0) {
                            Statement maybeCase = cases.get(index - 1);
                            if (maybeCase instanceof J.Case && ((J.Case) maybeCase).getStatements().isEmpty()) {
                                space = withWhitespace(space, " ");
                                break;
                            }
                        }
                    }
                    space = withWhitespace(space, "\n");
                    break;
                case EXTENDS:
                case IMPLEMENTS:
                case PERMITS:
                    wrap = evaluate(() -> style.getExtendsImplementsPermitsKeyword().getWrap(), DoNotWrap);
                    if (wrap == WrapAlways) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
                case ELSE_PREFIX:
                    J.If.Else e = getCursor().getValue();
                    parent = getCursor().getParentTreeCursor().getValue();
                    if (evaluate(() -> style.getIfStatement().getElseOnNewLine(), false)) {
                        space = withWhitespace(space, "\n");
                    } else if (!evaluate(() -> style.getIfStatement().getElseOnNewLine(), false) && parent instanceof J.If) {
                        if (((J.If) parent).getThenPart() instanceof J.Block) {
                            space = withWhitespace(space, evaluate(() -> spacesStyle.getBeforeKeywords().getElseKeyword(), true) ? " " : "");
                        } else {
                            space = withWhitespace(space, "\n");
                        }
                    }
                    updateCursor(e.withPrefix(space));

                    break;
                case WHILE_CONDITION:
                    if (evaluate(() -> style.getDoWhileStatement().getWhileOnNewLine(), false)) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
                case CATCH_PREFIX:
                    if (evaluate(() -> style.getTryStatement().getCatchOnNewLine(), false)) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
                case TRY_FINALLY:
                    if (evaluate(() -> style.getTryStatement().getFinallyOnNewLine(), false)) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
                case CLASS_DECLARATION_PREFIX:
                case METHOD_DECLARATION_PREFIX:
                case VARIABLE_DECLARATIONS_PREFIX:
                    if (space.getLastWhitespace().contains("\n")) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
                case ANNOTATION_PREFIX:
                    parentTreeCursor = getCursor().getParentTreeCursor();
                    parent = parentTreeCursor.getValue();
                    List<J.Annotation> annotations = null;
                    boolean doNotWrapSingle = false;
                    if (parent instanceof J.ClassDeclaration) {
                        wrap = evaluate(() -> style.getClassAnnotations().getWrap(), WrapAlways);
                        annotations = ((J.ClassDeclaration) parent).getLeadingAnnotations();
                    } else if (parent instanceof J.MethodDeclaration) {
                        wrap = evaluate(() -> style.getMethodAnnotations().getWrap(), WrapAlways);
                        annotations = ((J.MethodDeclaration) parent).getLeadingAnnotations();
                    } else if (parent instanceof J.VariableDeclarations) {
                        if (parentTreeCursor.getParentTreeCursor().getValue() instanceof J.MethodDeclaration) {
                            wrap = evaluate(() -> style.getParameterAnnotations().getWrap(), DoNotWrap);
                            doNotWrapSingle = evaluate(() -> style.getParameterAnnotations().getDoNotWrapAfterSingleAnnotation(), false);
                        } else if (parentTreeCursor.getParentTreeCursor().getValue() instanceof J.Block) {
                            if (parentTreeCursor.getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.ClassDeclaration) {
                                wrap = evaluate(() -> style.getFieldAnnotations().getWrap(), WrapAlways);
                                doNotWrapSingle = evaluate(() -> style.getFieldAnnotations().getDoNotWrapAfterSingleAnnotation(), false);
                            } else {
                                wrap = evaluate(() -> style.getLocalVariableAnnotations().getWrap(), DoNotWrap);
                            }
                        } else if (parentTreeCursor.getParentTreeCursor().getValue() instanceof J.ClassDeclaration) {
                            wrap = evaluate(() -> style.getRecordComponents().getNewLineForAnnotations(), false) ? WrapAlways : DoNotWrap;
                        }
                        annotations = ((J.VariableDeclarations) parent).getLeadingAnnotations();
                    } else if (parent instanceof J.EnumValue) {
                        wrap = evaluate(() -> style.getEnumFieldAnnotations().getWrap(), DoNotWrap);
                        annotations = ((J.EnumValue) parent).getAnnotations();
                    }
                    if (wrap != null && annotations != null && (!doNotWrapSingle || annotations.size() > 1)) {
                        parentTreeCursor.computeMessageIfAbsent("annotations-wrapped", __ -> false);
                        switch (wrap) {
                            case ChopIfTooLong:
                                if (positionService.columnsOf(getCursor()).getMaxColumn() < style.getHardWrapAt()) {
                                    break;
                                }
                            case WrapAlways:
                                parentTreeCursor.putMessage("annotations-wrapped", true);
                                if (annotations.indexOf(getCursor().getValue()) > 0) {
                                    space = withWhitespace(space, "\n");
                                }
                        }
                    }
                    break;
                case CLASS_KIND:
                    if (Boolean.TRUE.equals(getCursor().pollMessage("modifiers-wrapped")) ||  Boolean.TRUE.equals(getCursor().pollMessage("annotations-wrapped"))) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
                case MODIFIER_PREFIX:
                    getCursor().getParentTreeCursor().computeMessageIfAbsent("modifiers-wrapped", __ -> evaluate(() -> style.getModifierList().getWrapAfterModifierList(), false));
                    if (Boolean.TRUE.equals(getCursor().getParentTreeCursor().pollMessage("annotations-wrapped"))) {
                        if (space.getComments().isEmpty() || !space.getComments().get(space.getComments().size() - 1).isMultiline()) {
                            space = withWhitespace(space, "\n");
                        }
                    }
                    break;
                case TYPE_PARAMETERS_PREFIX:
                case IDENTIFIER_PREFIX:
                case PRIMITIVE_PREFIX:
                    if (Boolean.TRUE.equals(getCursor().getParentTreeCursor().pollMessage("modifiers-wrapped")) || Boolean.TRUE.equals(getCursor().getParentTreeCursor().pollMessage("annotations-wrapped"))) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
                case THROWS:
                    wrap = evaluate(() -> style.getThrowsKeyword().getWrap(), DoNotWrap);
                    if (wrap == WrapAlways) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
                case IMPORT_PREFIX:
                    space = withWhitespace(space, space.getWhitespace().replaceAll(" ", ""));
                default:
                    if (getCursor().getValue() instanceof TypeTree && Boolean.TRUE.equals(getCursor().getParentTreeCursor().pollMessage("annotations-wrapped"))) {
                        space = withWhitespace(space, "\n");
                    }
                    break;
            }
            //JavaTemplate calls autoFormat on statements in the block per statement iso formatting the entire block so we add newlines in that case for each statement causing the template to be new line sensitive if we do not have this check
            try {
                if (getCursor().getValue() instanceof Statement &&
                        loc != Space.Location.NEW_PREFIX &&
                        loc.name().endsWith("_PREFIX") &&
                        !(getCursor().getValue() instanceof J.EnumValueSet) &&
                        getCursor().getParentTreeCursor().getValue() instanceof J.Block &&
                        !space.getWhitespace().contains("\n")
                ) {
                    space = withWhitespace(space, "\n");
                }
            } catch (IllegalStateException ignored) {}
        }
        return super.visitSpace(space, loc, p);
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        if (block.getStatements().size() == 1 && block.getStatements().get(0) instanceof J.EnumValueSet) {
            getCursor().putMessage("single-line-enum", true);
        }
        return super.visitBlock(block, p);
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

    private Space withWhitespaceSkipComments(Space space, String whitespace) {
        if (space.getComments().isEmpty()) {
            if (StringUtils.hasLineBreak(whitespace)) {
                if (StringUtils.hasLineBreak(space.getWhitespace())) {
                    //Keep existing amount of new lines
                    return space.withWhitespace(space.getWhitespace().substring(0, space.getWhitespace().lastIndexOf("\n") + 1));
                }
            }
            return space.withWhitespace(whitespace);
        }
        return space;
    }

    private Space withWhitespace(Space space, String whitespace) {
        //IntelliJ only formats last comments suffix.
        return withWhitespaceSkipComments(space, whitespace).withComments(
                ListUtils.mapLast(space.getComments(), comment -> {
                    if (!StringUtils.hasLineBreak(comment.getSuffix())) {
                        return comment.withSuffix(whitespace);
                    }
                    if (comment.isMultiline()) {
                        Object parent = getCursor().getParentTreeCursor().getValue();
                        if (!(parent instanceof J.Block || parent instanceof J.Case)) {
                            return comment.withSuffix(whitespace);
                        }
                    }
                    if (StringUtils.hasLineBreak(comment.getSuffix())) {
                        //Keep existing amount of new lines
                        return comment.withSuffix(comment.getSuffix().substring(0, comment.getSuffix().lastIndexOf("\n") + 1));
                    }
                    //Reduce to single new line
                    return comment.withSuffix(whitespace);
                })
        );
    }

    private boolean shouldWrap(int index, int size, JContainer<J> container, JContainer.Location location) {
        if (size == 0) {
            return false;
        }

        LineWrapSetting wrap = getWrap(location);
        boolean openNewLine = opensOnNewLine(index, location);
        int minSizeForWrap = 1;
        if (location == THROWS) {
            openNewLine = true;
            minSizeForWrap = 0;
        }
        JavaSourceFile sourceFile;
        if (wrap != null) {
            switch (wrap) {
                case WrapAlways:
                    if (size <= minSizeForWrap) {
                        return false;
                    }
                    if (index == 0) {
                        return openNewLine;
                    }
                    return true;
                case ChopIfTooLong:
                    sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (sourceFile != null) {
                        boolean isLong = sourceFile.service(SourcePositionService.class).columnsOf(getCursor(), container).getMaxColumn() >= style.getHardWrapAt();
                        return isLong && (openNewLine || index != 0);
                    }
            }
        }
        return false;
    }

    private boolean shouldWrap(Cursor element, JRightPadded.Location location) {
        LineWrapSetting wrap = null;
        Boolean openNewLine = null;
        J countUntil = null;
        switch (location) {
            case FOR_INIT:
                countUntil = element.getValue();
                openNewLine = evaluate(() -> style.getForStatement().getOpenNewLine(), false);
                wrap = evaluate(() -> style.getForStatement().getWrap(), DoNotWrap);
                break;
            case FOR_CONDITION:
            case FOR_UPDATE:
                countUntil = element.getValue();
                wrap = evaluate(() -> style.getForStatement().getWrap(), DoNotWrap);
                break;
            case ENUM_VALUE:
                List<J.EnumValue> enums = ((J.EnumValueSet) getCursor().getValue()).getEnums();
                countUntil = enums.get(enums.size() - 1);
                wrap = evaluate(() -> style.getEnumConstants().getWrap(), DoNotWrap);
        }
        JavaSourceFile sourceFile;
        if (wrap != null) {
            switch (wrap) {
                case WrapAlways:
                    return openNewLine == null || openNewLine;
                case ChopIfTooLong:
                    sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (sourceFile != null && countUntil != null) {
                        return sourceFile.service(SourcePositionService.class).columnsOf(getCursor(), countUntil).getMaxColumn() >= style.getHardWrapAt();
                    }
            }
        }
        return false;
    }

    private boolean opensOnNewLine(int index, JContainer.Location location) {
        if (index != 0) {
            return false;
        }
        switch (location) {
            case METHOD_DECLARATION_PARAMETERS:
                return evaluate(() -> style.getMethodDeclarationParameters().getOpenNewLine(), false);
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                return evaluate(() -> style.getMethodCallArguments().getOpenNewLine(), false);
            case RECORD_STATE_VECTOR:
                return evaluate(() -> style.getRecordComponents().getOpenNewLine(), false);
            case TRY_RESOURCES:
                return evaluate(() -> style.getTryWithResources().getOpenNewLine(), false);
            case NEW_ARRAY_INITIALIZER:
                return evaluate(() -> style.getArrayInitializer().getNewLineAfterOpeningCurly(), false);
            case ANNOTATION_ARGUMENTS:
                return evaluate(() -> style.getAnnotationParameters().getOpenNewLine(), false);
        }
        return false;
    }

    private boolean closesOnNewLine(JContainer.Location location) {
        switch (location) {
            case METHOD_DECLARATION_PARAMETERS:
                return evaluate(() -> style.getMethodDeclarationParameters().getCloseNewLine(), false);
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                return evaluate(() -> style.getMethodCallArguments().getCloseNewLine(), false);
            case RECORD_STATE_VECTOR:
                return evaluate(() -> style.getRecordComponents().getCloseNewLine(), false);
            case TRY_RESOURCES:
                return evaluate(() -> style.getTryWithResources().getCloseNewLine(), false);
            case NEW_ARRAY_INITIALIZER:
                return evaluate(() -> style.getArrayInitializer().getPlaceClosingCurlyOnNewLine(), false);
            case ANNOTATION_ARGUMENTS:
                return evaluate(() -> style.getAnnotationParameters().getCloseNewLine(), false);
        }
        return false;
    }

    private @Nullable LineWrapSetting getWrap(JContainer.Location location) {
        switch (location) {
            case METHOD_DECLARATION_PARAMETERS:
                return evaluate(() -> style.getMethodDeclarationParameters().getWrap(), DoNotWrap);
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                return evaluate(() -> style.getMethodCallArguments().getWrap(), DoNotWrap);
            case RECORD_STATE_VECTOR:
                return evaluate(() -> style.getRecordComponents().getWrap(), DoNotWrap);
            case IMPLEMENTS:
            case PERMITS:
                return evaluate(() -> style.getExtendsImplementsPermitsList().getWrap(), DoNotWrap);
            case TRY_RESOURCES:
                return evaluate(() -> style.getTryWithResources().getWrap(), DoNotWrap);
            case NEW_ARRAY_INITIALIZER:
                return evaluate(() -> style.getArrayInitializer().getWrap(), DoNotWrap);
            case ANNOTATION_ARGUMENTS:
                return evaluate(() -> style.getAnnotationParameters().getWrap(), DoNotWrap);
            case THROWS:
                return evaluate(() -> style.getThrowsList().getWrap(), DoNotWrap);
        }
        return null;
    }

    private J.MethodInvocation findChainStarterInChain(J.MethodInvocation method) {
        J.MethodInvocation chainStarter = method;
        Expression select = method.getSelect();
        while (select instanceof J.MethodInvocation) {
            chainStarter = (J.MethodInvocation) select;
            select = chainStarter.getSelect();
        }
        return chainStarter;
    }

    private <I, O> O evaluate(I in, Function<I, O> supplier, O defaultValue) {
        try {
            return supplier.apply(in);
        } catch (NoSuchMethodError | NoSuchFieldError e) {
            // Handle newly introduced method calls on style that are not part of lst yet
            return defaultValue;
        }
    }

    private <T> T evaluate(Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (NoSuchMethodError | NoSuchFieldError e) {
            // Handle newly introduced method calls on style that are not part of lst yet
            return defaultValue;
        }
    }

    @ToBeRemoved(after = "30-02-2026", reason = "Replace me with org.openrewrite.style.StyleHelper.getStyle now available in parent runtime")
    private static <S extends Style> S getStyle(Class<S> styleClass, List<NamedStyles> styles, Supplier<S> defaultStyle) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return StyleHelper.merge(defaultStyle.get(), style);
        }
        return defaultStyle.get();
    }
}
