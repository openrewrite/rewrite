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
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.style.LineWrapSetting;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.openrewrite.internal.StringUtils.hasLineBreak;
import static org.openrewrite.style.LineWrapSetting.*;

public class WrappingAndBracesVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;
    protected final boolean removeCustomLineBreaks;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final SpacesStyle spacesStyle;
    private final WrappingAndBracesStyle style;
    private final TabsAndIndentsStyle tabsAndIndentsStyle;

    public WrappingAndBracesVisitor(SourceFile sourceFile, @Nullable Tree stopAfter, boolean removeCustomLineBreaks) {
        this(sourceFile.getMarkers().findAll(NamedStyles.class), stopAfter, removeCustomLineBreaks);
    }

    public WrappingAndBracesVisitor(List<NamedStyles> styles, @Nullable Tree stopAfter, boolean removeCustomLineBreaks) {
        this(getStyle(SpacesStyle.class, styles, IntelliJ::spaces), getStyle(WrappingAndBracesStyle.class, styles, IntelliJ::wrappingAndBraces), getStyle(TabsAndIndentsStyle.class, styles, IntelliJ::tabsAndIndents), stopAfter, removeCustomLineBreaks);
    }

    public WrappingAndBracesVisitor(SpacesStyle spacesStyle, WrappingAndBracesStyle wrappingAndBracesStyle, TabsAndIndentsStyle tabsAndIndentsStyle, @Nullable Tree stopAfter, boolean removeCustomLineBreaks) {
        this.spacesStyle = spacesStyle;
        this.style = wrappingAndBracesStyle;
        this.tabsAndIndentsStyle = tabsAndIndentsStyle;
        this.stopAfter = stopAfter;
        this.removeCustomLineBreaks = removeCustomLineBreaks;
    }

    @Override
    public @Nullable <J2 extends J> JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, P p) {
        if (container == null) {
            //noinspection ConstantConditions
            return null;
        }
        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
        int size = container.getElements().stream().allMatch(it -> it instanceof J.Empty) ? 0 : container.getElements().size();
        List<JRightPadded<J2>> js = ListUtils.map(container.getPadding().getElements(), (index, right) -> {
            J jElement = right.getElement();
            if (shouldWrap(index, size, (JContainer<J>) container, loc)) {
                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (sourceFile != null) {
                    SourcePositionService positionService = sourceFile.service(SourcePositionService.class);
                    if (index != 0 && !opensOnNewLine(loc) && alignWhenMultiline(loc)) {
                        int startColumn = positionService.positionOf(getCursor(), container.getElements().get(0)).getStartColumn();
                        right = right.withElement(jElement.withPrefix(withWhitespace(jElement.getPrefix(), "\n" + StringUtils.repeat(" ", startColumn - 1)))); //since position is index-1-based
                    } else {
                        Cursor newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                        int column = ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length() + tabsAndIndentsStyle.getContinuationIndent();
                        right = right.withElement(jElement.withPrefix(withWhitespace(jElement.getPrefix(), "\n" + StringUtils.repeat(" ", column))));
                    }
                }
            } else if (right.getElement().getPrefix().getWhitespace().contains("\n")) {
                right = right.withElement(jElement.withPrefix(withWhitespaceSkipComments(jElement.getPrefix(), "")));
            }
            right = closeOnNewLine(index, size, right, loc);
            return visitRightPadded(right, loc.getElementLocation(), p);
        });

        setCursor(getCursor().getParent());

        return js == container.getPadding().getElements() && before == container.getBefore() ? container : JContainer.build(before, js, container.getMarkers());
    }

    @Override
    public @Nullable <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (right == null) {
            return super.visitRightPadded(right, loc, p);
        }
        JRightPadded<?> wrappedRight = right;
        int startColumn = -1;
        Cursor newLinedCursorElement;
        Cursor parentTreeCursor;
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
                if (m.getPadding().getSelect() == null || (!evaluate(() -> style.getChainedMethodCalls().getWrapFirstCall(), false) && chainStarter == m)) {
                    break;
                }

                boolean isBuilderMethod = evaluate(() -> style.getChainedMethodCalls().getBuilderMethods().stream(), Stream.empty())
                        .map(name -> String.format("*..* %s(..)", name))
                        .map(MethodMatcher::new)
                        .anyMatch(matcher -> matcher.matches(chainStarter));
                if (isBuilderMethod || (evaluate(() -> style.getChainedMethodCalls().getWrap(), DoNotWrap) == LineWrapSetting.WrapAlways || evaluate(() -> style.getChainedMethodCalls().getWrap(), DoNotWrap) == LineWrapSetting.ChopIfTooLong)) {
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
                        if (positionService.positionOf(cursor).getMaxColumn() <= evaluate(() -> style.getHardWrapAt(), 120)) {
                            break;
                        }
                    }

                    if (positionService == null) {
                        break;
                    }
                    boolean wrapFirstCall = evaluate(() -> style.getChainedMethodCalls().getWrapFirstCall(), false);
                    if (!((wrapFirstCall && chainStarter == m) || (!wrapFirstCall && chainStarter == m.getSelect())) && evaluate(() -> style.getChainedMethodCalls().getAlignWhenMultiline(), false)) {
                        startColumn = positionService.positionOf(getCursor(), chainStarter.getSelect()).getEndColumn() - 1; //since position is index-1-based
                    } else {
                        newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor());
                        if (newLinedCursorElement.getValue() instanceof J.MethodInvocation && findChainStarterInChain(newLinedCursorElement.getValue()) == chainStarter) {
                            newLinedCursorElement = newLinedCursorElement.dropParentUntil(it -> (!(it instanceof J.MethodInvocation || it instanceof JRightPadded || it instanceof JLeftPadded)));
                            if (!(newLinedCursorElement.getValue() instanceof Tree)) {
                                newLinedCursorElement = newLinedCursorElement.getParentTreeCursor();
                            }
                            newLinedCursorElement = positionService.computeNewLinedCursorElement(newLinedCursorElement);
                        }
                        if (newLinedCursorElement.getValue() instanceof J.MethodInvocation) {
                            startColumn = ((J.MethodInvocation) newLinedCursorElement.getValue()).getPadding().getSelect().getAfter().getIndent().length() + tabsAndIndentsStyle.getContinuationIndent();
                        }
                        if (startColumn < 0) {
                            startColumn = ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length() + tabsAndIndentsStyle.getContinuationIndent();
                        }
                    }
                }
                if (startColumn != -1) {
                    wrappedRight = wrappedRight.withAfter(withWhitespace(wrappedRight.getAfter(), "\n" + StringUtils.repeat(" ", startColumn)));
                    if (wrappedRight != right) {
                        m = m.getPadding().withSelect((JRightPadded<Expression>) wrappedRight);
                        setCursor(updateCursor(m));
                        parentTreeCursor = getCursor().getParentTreeCursor();
                        if (parentTreeCursor.getValue() instanceof J.MethodInvocation && ((J.MethodInvocation) parentTreeCursor.getValue()).getSelect() == m) {
                            setCursor(new Cursor(new Cursor(parentTreeCursor.getParentTreeCursor(), ((J.MethodInvocation) parentTreeCursor.getValue()).withSelect(m)), m));
                        }
                    }
                }
                break;
            case BLOCK_STATEMENT:
                newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor());
                if (newLinedCursorElement.getValue() instanceof J.MethodInvocation && ((J.MethodInvocation) newLinedCursorElement.getValue()).getSelect() != null) {
                    startColumn = ((J.MethodInvocation) newLinedCursorElement.getValue()).getPadding().getSelect().getAfter().getIndent().length() + tabsAndIndentsStyle.getIndentSize();
                } else if (getCursor().getValue() instanceof J.Block && !(wrappedRight.getElement() instanceof J.EnumValueSet)) {
                    // for `J.EnumValueSet` the prefix is on the enum constants
                    startColumn = ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length() + tabsAndIndentsStyle.getIndentSize();
                }
                if (startColumn >= 0 && wrappedRight.getElement() instanceof J) {
                    wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n" + StringUtils.repeat(" ", startColumn))));
                }
                break;
            case CASE:
                newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n" + StringUtils.repeat(" ", ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length() + tabsAndIndentsStyle.getIndentSize()))));
                break;
            case IF_ELSE:
                if (wrappedRight.getElement() instanceof J.If) {
                    break;
                }
                //Falling through on purpose here
            case IF_THEN:
                if (evaluate(() -> style.getIfStatement().getForceBraces(), WrappingAndBracesStyle.ForceBraces.DoNotForce) == WrappingAndBracesStyle.ForceBraces.DoNotForce) {
                    break;
                }
                if (!(wrappedRight.getElement() instanceof J.Block)) {
                    wrappedRight = ((JRightPadded<Statement>) wrappedRight).withElement(J.Block.createEmptyBlock()
                            .withPrefix(Space.format(evaluate(() -> spacesStyle.getBeforeLeftBrace().getIfLeftBrace(), true) ? " " : ""))
                            .withStatements(singletonList((Statement) wrappedRight.getElement())));
                    if (getCursor().getValue() instanceof J.If) {
                        setCursor(updateCursor(((J.If) getCursor().getValue()).getPadding().withThenPart((JRightPadded<Statement>) wrappedRight)));
                    } else {
                        setCursor(updateCursor(((J.If.Else) getCursor().getValue()).getPadding().withBody((JRightPadded<Statement>) wrappedRight)));
                    }
                }
                break;
            case FOR_INIT:
                if (shouldWrap(getCursor(), loc)) {
                    newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                    startColumn = ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length() + tabsAndIndentsStyle.getContinuationIndent();
                    wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n" + StringUtils.repeat(" ", startColumn))));
                }
                break;
            case FOR_CONDITION:
            case FOR_UPDATE:
                if (shouldWrap(getCursor(), loc)) {
                    newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                    if (!evaluate(() -> style.getForStatement().getOpenNewLine(), false) && evaluate(() -> style.getForStatement().getAlignWhenMultiline(), true)) {
                        J.ForLoop.Control control = getCursor().getValue();
                        startColumn = positionService.positionOf(getCursor(), control.getInit().get(0)).getStartColumn() - 1;
                    } else {
                        startColumn = ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length() + tabsAndIndentsStyle.getContinuationIndent();
                    }
                    wrappedRight = wrappedRight.withElement(((J) wrappedRight.getElement()).withPrefix(withWhitespace(((J) wrappedRight.getElement()).getPrefix(), "\n" + StringUtils.repeat(" ", startColumn))));
                    if (loc == JRightPadded.Location.FOR_UPDATE && evaluate(() -> style.getForStatement().getCloseNewLine(), false)) {
                        wrappedRight = wrappedRight.withAfter(withWhitespace(wrappedRight.getAfter(), "\n" + StringUtils.repeat(" ", ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length())));
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
        }

        return (JRightPadded<T>) super.visitRightPadded(wrappedRight, loc, p);
    }

    public Space visitSpace(@Nullable Space space, Space.Location loc, P p) {
        JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
        if (space != null && sourceFile != null) {
            SourcePositionService positionService = sourceFile.service(SourcePositionService.class);
            Cursor newLinedCursorElement;
            Cursor parentTreeCursor;
            J parent;
            LineWrapSetting wrap = null;
            int column;
            boolean isLong = false;
            switch (loc) {
                case BLOCK_END:
                    newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor());
                    if (newLinedCursorElement.getValue() instanceof J.MethodInvocation && ((J.MethodInvocation) newLinedCursorElement.getValue()).getSelect() != null) {
                        column = ((J.MethodInvocation) newLinedCursorElement.getValue()).getPadding().getSelect().getAfter().getIndent().length();
                    } else {
                        column = ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length();
                    }
                    space = withWhitespace(space, "\n" + StringUtils.repeat(" ", column));
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
                    newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                    column = ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length();
                    if (evaluate(() -> style.getSwitchStatement().getIndentCaseBranches(), true)) {
                        column += tabsAndIndentsStyle.getIndentSize();
                    }
                    space = withWhitespace(space, "\n" + StringUtils.repeat(" ", column));
                    break;
                case EXTENDS:
                case IMPLEMENTS:
                case PERMITS:
                    wrap = evaluate(() -> style.getExtendsImplementsPermitsKeyword().getWrap(), DoNotWrap);
                    if (evaluate(wrap, w -> w == WrapIfTooLong, false) || wrap == WrapAlways) {
                        newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                        if (evaluate(wrap, w -> w == WrapIfTooLong, false)) {
                            isLong = sourceFile.service(SourcePositionService.class).positionOf(newLinedCursorElement, ((J.ClassDeclaration) getCursor().getParentTreeCursor().getValue()).getBody()).getStartColumn() >= style.getHardWrapAt();
                        }
                        if (isLong || wrap == WrapAlways) {
                            column = ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length() + tabsAndIndentsStyle.getContinuationIndent();
                            space = withWhitespace(space, StringUtils.repeat(" ", column));
                        }
                    }
                    break;
                case ELSE_PREFIX:
                    J.If.Else e = getCursor().getValue();
                    boolean hasBody = e.getBody() instanceof J.Block || e.getBody() instanceof J.If;
                    if (hasBody) {
                        parentTreeCursor = getCursor().getParentTreeCursor();
                        parent = parentTreeCursor.getValue();
                        if (evaluate(() -> style.getIfStatement().getElseOnNewLine(), false)) {
                            space = withWhitespace(space, "\n" + StringUtils.repeat(" ", parent.getPrefix().getIndent().length()));
                        } else if (!evaluate(() -> style.getIfStatement().getElseOnNewLine(), false) && parent instanceof J.If) {
                            if (((J.If) parent).getThenPart() instanceof J.Block) {
                                space = withWhitespace(space, evaluate(() -> spacesStyle.getBeforeKeywords().getElseKeyword(), true) ? " " : "");
                            } else {
                                space = withWhitespace(space, "\n" + StringUtils.repeat(" ", parent.getPrefix().getIndent().length()));
                            }
                        }
                    }

                    break;
                case WHILE_CONDITION:
                    if (evaluate(() -> style.getDoWhileStatement().getWhileOnNewLine(), false)) {
                        newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                        space = withWhitespace(space, "\n" + StringUtils.repeat(" ", ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length()));
                    }
                    break;
                case CATCH_PREFIX:
                    if (evaluate(() -> style.getTryStatement().getCatchOnNewLine(), false)) {
                        newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                        space = withWhitespace(space, "\n" + StringUtils.repeat(" ", ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length()));
                    }
                    break;
                case TRY_FINALLY:
                    if (evaluate(() -> style.getTryStatement().getFinallyOnNewLine(), false)) {
                        newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                        space = withWhitespace(space, "\n" + StringUtils.repeat(" ", ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length()));
                    }
                    break;
                case CLASS_DECLARATION_PREFIX:
                case METHOD_DECLARATION_PREFIX:
                case VARIABLE_DECLARATIONS_PREFIX:
                    if (space.getLastWhitespace().contains("\n")) {
                        space = withWhitespace(space, "\n" + space.getIndent());
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
                            case WrapIfTooLong:
                                if (positionService.positionOf(getCursor()).getMaxColumn() < style.getHardWrapAt()) {
                                    break;
                                }
                            case WrapAlways:
                                parentTreeCursor.putMessage("annotations-wrapped", true);
                                //TODO update the cursor for length calculations of subsequent calculations
                                if (annotations.indexOf(getCursor().getValue()) > 0) {
                                    space = withWhitespace(space, "\n" + StringUtils.repeat(" ", positionService.positionOf(getCursor().getParentTreeCursor(), annotations.get(0)).getStartColumn() - 1));
                                }
                        }
                    }
                    break;
                case CLASS_KIND:
                    if (Boolean.TRUE.equals(getCursor().pollMessage("annotations-wrapped"))) {
                        space = withWhitespace(space, "\n" + StringUtils.repeat(" ", positionService.positionOf(getCursor().getParentTreeCursor()).getStartColumn() - 1));
                    }
                    break;
                case MODIFIER_PREFIX:
                    if (Boolean.TRUE.equals(getCursor().getParentTreeCursor().pollMessage("annotations-wrapped"))) {
                        if (space.getComments().isEmpty() || !space.getComments().get(space.getComments().size() - 1).isMultiline()) {
                            space = withWhitespace(space, "\n" + StringUtils.repeat(" ", positionService.positionOf(getCursor().getParentTreeCursor()).getStartColumn() - 1));
                        }
                    }
                    break;
                case TYPE_PARAMETERS_PREFIX:
                case IDENTIFIER_PREFIX:
                    if (Boolean.TRUE.equals(getCursor().getParentTreeCursor().pollMessage("annotations-wrapped"))) {
                        space = withWhitespace(space, "\n" + StringUtils.repeat(" ", positionService.positionOf(getCursor().getParentTreeCursor()).getStartColumn() - 1));
                    }
                    break;
                default:
                    if (getCursor().getValue() instanceof TypeTree && Boolean.TRUE.equals(getCursor().getParentTreeCursor().pollMessage("annotations-wrapped"))) {
                        space = withWhitespace(space, "\n" + StringUtils.repeat(" ", positionService.positionOf(getCursor().getParentTreeCursor()).getStartColumn() - 1));
                    }
                    break;
            }
        }
        return super.visitSpace(space, loc, p);
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue _enum, P p) {
        J.EnumValue enumValue = super.visitEnumValue(_enum, p);
        String whitespace = enumValue.getPrefix().getWhitespace().replaceFirst("^[\\n\\s]+\\n", "\n");
        enumValue = enumValue.withAnnotations(wrapAnnotations(enumValue.getAnnotations(), whitespace, style.getEnumFieldAnnotations()));
        if (!enumValue.getAnnotations().isEmpty() && style.getEnumFieldAnnotations() != null) {
            enumValue = enumValue.withName(enumValue.getName().withPrefix(wrapElement(enumValue.getName().getPrefix(), whitespace, style.getEnumFieldAnnotations())));
        }
        return enumValue;
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
            }
            if (annotationsStyle.getWrap() == WrapAlways) {
                return prefix.withWhitespace((whitespace.startsWith("\n") ? "" : "\n") + whitespace);
            }
        }
        return prefix;
    }

    private Space withWhitespaceSkipComments(Space space, String whitespace) {
        if (space.getComments().isEmpty()) {
            if (!removeCustomLineBreaks && StringUtils.hasLineBreak(space.getWhitespace())) {
                return space;
            }
            if (StringUtils.hasLineBreak(whitespace)) {
                //Reduce to single new line
                return space.withWhitespace(whitespace.substring(whitespace.lastIndexOf('\n')));
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
                    if (removeCustomLineBreaks) {
                        if (comment.isMultiline()) {
                            Object parent = getCursor().getParentTreeCursor().getValue();
                            if (!(parent instanceof J.Block || parent instanceof J.Case)) {
                                return comment.withSuffix(whitespace);
                            }
                        }
                        //Reduce to single new line
                        return comment.withSuffix(whitespace.substring(whitespace.lastIndexOf('\n')));
                    }
                    return comment;
                })
        );
    }

    private boolean shouldWrap(int index, int size, JContainer<J> container, JContainer.Location location) {
        if (size == 0) {
            return false;
        }

        LineWrapSetting wrap = null;
        boolean openNewLine = false;
        switch (location) {
            case METHOD_DECLARATION_PARAMETERS:
                if (index == 0) {
                    openNewLine = evaluate(() -> style.getMethodDeclarationParameters().getOpenNewLine(), false);
                }
                wrap = evaluate(() -> style.getMethodDeclarationParameters().getWrap(), DoNotWrap);
                break;
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                if (index == 0) {
                    openNewLine = evaluate(() -> style.getMethodCallArguments().getOpenNewLine(), false);
                }
                wrap = evaluate(() -> style.getMethodCallArguments().getWrap(), DoNotWrap);
                break;
            case RECORD_STATE_VECTOR:
                if (index == 0) {
                    openNewLine = evaluate(() -> style.getRecordComponents().getOpenNewLine(), false);
                }
                wrap = evaluate(() -> style.getRecordComponents().getWrap(), () -> WrapIfTooLong, DoNotWrap);
                break;
            case IMPLEMENTS:
            case PERMITS:
                wrap = evaluate(() -> style.getExtendsImplementsPermitsList().getWrap(), DoNotWrap);
                break;
            case TRY_RESOURCES:
                if (index == 0) {
                    openNewLine = evaluate(() -> style.getTryWithResources().getOpenNewLine(), false);
                }
                wrap = evaluate(() -> style.getTryWithResources().getWrap(), DoNotWrap);
                break;
        }
        JavaSourceFile sourceFile;
        if (wrap != null) {
            switch (wrap) {
                case WrapAlways:
                    if (size <= 1) {
                        return false;
                    }
                    if (index == 0) {
                        return openNewLine;
                    }
                    return true;
                case ChopIfTooLong:
                    sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (sourceFile != null) {
                        boolean isLong = sourceFile.service(SourcePositionService.class).positionOf(getCursor(), container).getMaxColumn() >= style.getHardWrapAt();
                        return isLong && (openNewLine || index != 0);
                    }
                case WrapIfTooLong:
                    sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (sourceFile != null) {
                        boolean isLong = sourceFile.service(SourcePositionService.class).positionOf(getCursor(), container).getMaxColumn() >= style.getHardWrapAt();
                        if (isLong) {
                            if (index == 0) {
                                return openNewLine;
                            }
                            return true;
                        }
                        //TODO we should check the current position for the length of the current item to see if we have to wrap.
                        return false;
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
        }
        JavaSourceFile sourceFile;
        if (wrap != null) {
            switch (wrap) {
                case WrapAlways:
                    return openNewLine == null || openNewLine;
                case ChopIfTooLong:
                    sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (sourceFile != null && countUntil != null) {
                        return sourceFile.service(SourcePositionService.class).positionOf(getCursor(), countUntil).getMaxColumn() >= style.getHardWrapAt();
                    }
                case WrapIfTooLong:
                    sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (sourceFile != null && countUntil != null) {
                        //TODO we should check the current position for the length of the current item to see if we have to wrap.
                        return sourceFile.service(SourcePositionService.class).positionOf(getCursor(), countUntil).getMaxColumn() >= style.getHardWrapAt();
                    }
            }
        }
        return false;
    }

    private boolean opensOnNewLine(JContainer.Location location) {
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
        }
        return false;
    }

    private <T> JRightPadded<T> closeOnNewLine(int index, int size, JRightPadded<T> right, JContainer.Location location) {
        if (index != size - 1 || (!(right.getElement() instanceof J))) {
            return right;
        }
        LineWrapSetting wrap = null;
        boolean closeOnNewLine = false;
        switch (location) {
            case METHOD_DECLARATION_PARAMETERS:
                closeOnNewLine = evaluate(() -> style.getMethodDeclarationParameters().getCloseNewLine(), false);
                wrap = evaluate(() -> style.getMethodDeclarationParameters().getWrap(), DoNotWrap);
                break;
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                closeOnNewLine = evaluate(() -> style.getMethodCallArguments().getCloseNewLine(), false);
                wrap = evaluate(() -> style.getMethodCallArguments().getWrap(), DoNotWrap);
                break;
            case RECORD_STATE_VECTOR:
                closeOnNewLine = evaluate(() -> style.getRecordComponents().getCloseNewLine(), false);
                wrap = evaluate(() -> style.getRecordComponents().getWrap(), () -> WrapIfTooLong, DoNotWrap);
                break;
            case TRY_RESOURCES:
                closeOnNewLine = evaluate(() -> style.getTryWithResources().getCloseNewLine(), false);
                wrap = evaluate(() -> style.getTryWithResources().getWrap(), DoNotWrap);
                break;
        }

        Space after = right.getAfter();
        if (closeOnNewLine) {
            JavaSourceFile sourceFile;
            sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
            Cursor newLinedCursorElement;
            SourcePositionService positionService = sourceFile.service(SourcePositionService.class);
            switch (wrap) {
                case WrapAlways:
                    newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                    after = withWhitespace(right.getAfter(), "\n" + StringUtils.repeat(" ", ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length()));
                    break;
                case ChopIfTooLong:
                case WrapIfTooLong:
                    boolean isLong = sourceFile.service(SourcePositionService.class).positionOf(getCursor(), (JContainer<J>) getCursor().getValue()).getMaxColumn() >= style.getHardWrapAt();
                    if (isLong) {
                        newLinedCursorElement = positionService.computeNewLinedCursorElement(getCursor().getParentTreeCursor());
                        after = withWhitespace(right.getAfter(), "\n" + StringUtils.repeat(" ", ((J) newLinedCursorElement.getValue()).getPrefix().getIndent().length()));
                    }
                    break;
            }
        }
        return right.withAfter(after);
    }

    private boolean alignWhenMultiline(JContainer.Location location) {
        switch (location) {
            case METHOD_DECLARATION_PARAMETERS:
                return evaluate(() -> style.getMethodDeclarationParameters().getAlignWhenMultiline(), true);
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                return evaluate(() -> style.getMethodCallArguments().getAlignWhenMultiline(), false);
            case RECORD_STATE_VECTOR:
                return evaluate(() -> style.getRecordComponents().getAlignWhenMultiline(), false);
            case IMPLEMENTS:
            case PERMITS:
                return evaluate(() -> style.getExtendsImplementsPermitsList().getAlignWhenMultiline(), false);
            case TRY_RESOURCES:
                return evaluate(() -> style.getTryWithResources().getAlignWhenMultiline(), true);
        }
        return false;
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

    private <T> T evaluate(Supplier<T> supplier, Supplier<T> defaultValueSupplier, T fallback) {
        try {
            return supplier.get();
        } catch (NoSuchMethodError | NoSuchFieldError ignored1) {
            // Handle newly introduced method calls on style that are not part of lst yet
            return evaluate(defaultValueSupplier, fallback);
        }
    }

    @ToBeRemoved(after = "30-01-2026", reason = "Replace me with org.openrewrite.style.StyleHelper.getStyle now available in parent runtime")
    private static <S extends Style> S getStyle(Class<S> styleClass, List<NamedStyles> styles, Supplier<S> defaultStyle) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return StyleHelper.merge(defaultStyle.get(), style);
        }
        return defaultStyle.get();
    }
}
