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
import org.openrewrite.java.style.EmptyForInitializerPadStyle;
import org.openrewrite.java.style.EmptyForIteratorPadStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class SpacesVisitor<P> extends JavaIsoVisitor<P> {

    protected final SpacesStyle spacesStyle;
    @Nullable
    protected final EmptyForInitializerPadStyle emptyForInitializerPadStyle;
    @Nullable
    protected final EmptyForIteratorPadStyle emptyForIteratorPadStyle;
    @Nullable
    protected final Tree stopAfter;
    protected final boolean removeCustomLineBreaks;

    public SpacesVisitor(SourceFile sourceFile, boolean removeCustomLineBreaks, @Nullable Tree stopAfter) {
        this(sourceFile.getMarkers().findAll(NamedStyles.class), removeCustomLineBreaks, stopAfter);
    }

    public SpacesVisitor(List<NamedStyles> styles, boolean removeCustomLineBreaks, @Nullable Tree stopAfter) {
        this(getStyle(SpacesStyle.class, styles, IntelliJ::spaces), getStyle(EmptyForInitializerPadStyle.class, styles), getStyle(EmptyForIteratorPadStyle.class, styles), stopAfter, removeCustomLineBreaks);
    }

    public SpacesVisitor(SpacesStyle spacesStyle, @Nullable Tree stopAfter) {
        this(spacesStyle, null, null, stopAfter, false);
    }

    public SpacesVisitor(SpacesStyle spacesStyle, boolean removeCustomLineBreaks, @Nullable Tree stopAfter) {
        this(spacesStyle, null, null, stopAfter, removeCustomLineBreaks);
    }

    @Deprecated
    public SpacesVisitor(SpacesStyle spacesStyle, @Nullable EmptyForInitializerPadStyle emptyForInitializerPadStyle, @Nullable EmptyForIteratorPadStyle emptyForIteratorPadStyle, @Nullable Tree stopAfter) {
        this(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle, stopAfter, false);
    }

    public SpacesVisitor(SpacesStyle spacesStyle, @Nullable EmptyForInitializerPadStyle emptyForInitializerPadStyle, @Nullable EmptyForIteratorPadStyle emptyForIteratorPadStyle, @Nullable Tree stopAfter, boolean removeCustomLineBreaks) {
        this.spacesStyle = spacesStyle;
        this.emptyForInitializerPadStyle = emptyForInitializerPadStyle;
        this.emptyForIteratorPadStyle = emptyForIteratorPadStyle;
        this.stopAfter = stopAfter;
        this.removeCustomLineBreaks = removeCustomLineBreaks;
    }

    @Override
    public @Nullable <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (right == null || !(right.getElement() instanceof J)) {
            return super.visitRightPadded(right, loc, p);
        }

        Cursor cursor = getCursor();
        Cursor parentTreeCursor = cursor.getParentTreeCursor();
        J parent;
        String before = null;
        String after = null;

        ContainerPosition beforePosition;
        ContainerPosition afterPosition;
        JContainer.Location containerLocation = cursor.getMessage("location");
        int index = -1;
        int size = -1;
        boolean emptyContainer = false;

        switch (loc) {
            case LANGUAGE_EXTENSION:
            case CASE_LABEL:
                break;
            case MEMBER_REFERENCE_CONTAINING:
                after = evaluate(() -> spacesStyle.getAroundOperators().getMethodReferenceDoubleColon(), false) ? " " : "";
                break;
            case PARENTHESES:
                parent = parentTreeCursor.getValue();
                if (parent instanceof J.If) {
                    before = evaluate(() -> spacesStyle.getWithin().getIfParentheses(), false) ? " " : "";
                } else if (parent instanceof J.WhileLoop || parent instanceof J.DoWhileLoop) {
                    before = evaluate(() -> spacesStyle.getWithin().getWhileParentheses(), false) ? " " : "";
                } else if (parent instanceof J.Switch || parent instanceof J.SwitchExpression) {
                    before = evaluate(() -> spacesStyle.getWithin().getSwitchParentheses(), false) ? " " : "";
                } else if (parent instanceof J.Try.Catch) {
                    before = evaluate(() -> spacesStyle.getWithin().getCatchParentheses(), false) ? " " : "";
                } else if (parent instanceof J.Synchronized) {
                    before = evaluate(() -> spacesStyle.getWithin().getSynchronizedParentheses(), false) ? " " : "";
                } else if (parent instanceof J.TypeCast) {
                    before = evaluate(() -> spacesStyle.getWithin().getTypeCastParentheses(), false) ? " " : "";
                } else if (parent instanceof J.ArrayAccess) {
                    before = evaluate(() -> spacesStyle.getWithin().getBrackets(), false) ? " " : "";
                } else if (parent instanceof J.NewArray) {
                    before = evaluate(() -> spacesStyle.getWithin().getBrackets(), false) ? " " : "";
                } else if (getCursor().getValue() instanceof J.Parentheses && ((J.Parentheses<?>) getCursor().getValue()).getTree() instanceof J.Binary) {
                    before = evaluate(() -> spacesStyle.getWithin().getGroupingParentheses(), false) ? " " : "";
                } else {
                    break;
                }
                after = before;
                break;
            case FOR_INIT:
                J.ForLoop.Control controlInit = getCursor().getValue();
                if (controlInit.getInit().stream().allMatch(i -> i instanceof J.Empty)) {
                    if (emptyForInitializerPadStyle != null) {
                        before = evaluate(() -> emptyForInitializerPadStyle.getSpace(), false) ? " " : "";
                    } else {
                        before = evaluate(() -> spacesStyle.getWithin().getForParentheses(), evaluate(() -> spacesStyle.getOther().getBeforeForSemicolon(), false)) ? " " : "";
                    }
                    after = "";
                } else {
                    before = evaluate(() -> spacesStyle.getOther().getAfterComma(), true) ? " " : "";
                    after = evaluate(() -> spacesStyle.getOther().getBeforeComma(), false) ? " " : "";
                    index = controlInit.getInit().indexOf(right.getElement());
                    if (index == 0) {
                        before = evaluate(() -> spacesStyle.getWithin().getForParentheses(), true) ? " " : "";
                    }
                    if (index == controlInit.getInit().size() - 1) {
                        after = evaluate(() -> spacesStyle.getOther().getBeforeForSemicolon(), false) ? " " : "";
                    }
                }
                break;
            case FOR_CONDITION:
                before = evaluate(() -> spacesStyle.getOther().getAfterForSemicolon(), true) ? " " : "";
                after = evaluate(() -> spacesStyle.getOther().getBeforeForSemicolon(), true) ? " " : "";
                break;
            case FOR_UPDATE:
                J.ForLoop.Control controlUpdate = getCursor().getValue();
                if (controlUpdate.getUpdate().stream().allMatch(i -> i instanceof J.Empty)) {
                    if (emptyForIteratorPadStyle != null) {
                        before = evaluate(() -> emptyForIteratorPadStyle.getSpace(), false) ? " " : "";
                    } else {
                        before = evaluate(() -> spacesStyle.getOther().getAfterForSemicolon(), evaluate(() -> spacesStyle.getWithin().getForParentheses(), true)) ? " " : "";
                    }
                    after = "";
                } else {
                    before = evaluate(() -> spacesStyle.getOther().getAfterComma(), true) ? " " : "";
                    after = evaluate(() -> spacesStyle.getOther().getBeforeComma(), false) ? " " : "";
                    index = controlUpdate.getUpdate().indexOf(right.getElement());
                    if (index == 0) {
                        before = evaluate(() -> spacesStyle.getOther().getAfterForSemicolon(), true) ? " " : "";
                    }
                    if (index == controlUpdate.getUpdate().size() - 1) {
                        after = evaluate(() -> spacesStyle.getWithin().getForParentheses(), false) ? " " : "";
                    }
                }
                break;
            case FOREACH_ITERABLE:
                before = " ";
                after = evaluate(() -> spacesStyle.getWithin().getForParentheses(), false) ? " " : "";
                break;
            case FOREACH_VARIABLE:
                before = evaluate(() -> spacesStyle.getWithin().getForParentheses(), false) ? " " : "";
                after = evaluate(() -> spacesStyle.getOther().getBeforeColonInForEach(), true) ? " " : "";
                break;
            case ARRAY_INDEX:
                before = evaluate(() -> spacesStyle.getWithin().getBrackets(), true) ? " " : "";
                after = evaluate(() -> spacesStyle.getWithin().getBrackets(), true) ? " " : "";
                break;
            case CATCH_ALTERNATIVE:
                J.MultiCatch multiCatch = cursor.getValue();
                before = evaluate(() -> spacesStyle.getAroundOperators().getBitwise(), true) ? " " : "";
                after = before;
                index = multiCatch.getAlternatives().indexOf(right.getElement());
                if (index == 0) {
                    before = "";
                }
                if (index == multiCatch.getAlternatives().size() - 1) {
                    after = "";
                }
                break;
            case ENUM_VALUE:
                Cursor classCursor = parentTreeCursor.getParentTreeCursor();
                AtomicInteger atomicSize = new AtomicInteger(-1);
                AtomicInteger atomicIndex = new AtomicInteger(-1);
                new JavaIsoVisitor<J.EnumValue>() {
                    @Override
                    public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enums, J.EnumValue ctx) {
                        if (enums.getEnums().contains(ctx)) {
                            atomicSize.set(enums.getEnums().size());
                            atomicIndex.set(enums.getEnums().indexOf(ctx));
                        }
                        return enums;
                    }
                }.visit((J) classCursor.getValue(), (J.EnumValue) right.getElement());
                if (classCursor.getMessage("singleLineEnum") == Boolean.TRUE) {
                    if (atomicIndex.get() == 0) {
                        before = ""; // goes into the EnumValueSet parent
                    } else if (atomicIndex.get() > 0) {
                        before = evaluate(() -> spacesStyle.getOther().getAfterComma(), true) ? " " : "";
                    }
                    if (atomicIndex.get() == atomicSize.get() - 1) {
                        after = evaluate(() -> spacesStyle.getOther().getInsideOneLineEnumBraces(), false) ? " " : "";
                    } else if (atomicIndex.get() >= 0) {
                        after = evaluate(() -> spacesStyle.getOther().getBeforeComma(), false) ? " " : "";
                    }
                } else {
                    if (atomicIndex.get() > 0 && !hasLineBreakInSpace(((J) right.getElement()).getPrefix())) {
                        before = " ";
                    }
                    after = "";
                }
                break;
            case LAMBDA_PARAM:
                parent = cursor.getValue();
                index = ((J.Lambda.Parameters) parent).getParameters().indexOf(right.getElement());
                size = ((J.Lambda.Parameters) parent).getParameters().size();
                containerLocation = JContainer.Location.METHOD_DECLARATION_PARAMETERS;
                break;
            case TYPE_PARAMETER:
                if (cursor.getValue() instanceof J.TypeParameters) {
                    J.TypeParameters params = cursor.getValue();
                    index = params.getTypeParameters().indexOf(right.getElement());
                    size = params.getTypeParameters().size();
                    containerLocation = JContainer.Location.TYPE_PARAMETERS;
                    break;
                }
                //if not we can fall through to the container handling
            case METHOD_DECLARATION_PARAMETER:
            case RECORD_STATE_VECTOR:
            case METHOD_INVOCATION_ARGUMENT:
            case NEW_CLASS_ARGUMENTS:
            case ANNOTATION_ARGUMENT:
            case TYPE_BOUND:
            case NEW_ARRAY_INITIALIZER:
            case TRY_RESOURCE:
                JContainer<J> container = cursor.getValue();
                index = container.getElements().indexOf(right.getElement());
                size = container.getElements().size();
                emptyContainer = container.getElements().stream().allMatch(element -> element instanceof J.Empty);
                break;
            case METHOD_SELECT:
                after = "";
                break;
            case INSTANCEOF:
                after = " ";
                break;
            default:
                if (hasLineBreakInSpace(right.getAfter())) {
                    after = right.getAfter().getWhitespace();
                    break;
                }
                after = "";
                break;
        }

        if (index >= 0 && containerLocation != null) {
            if (emptyContainer) {
                before = getMinimizedWhitespaceWithin(containerLocation, ContainerPosition.EMPTY);
                after = "";
            } else {
                beforePosition = ContainerPosition.AFTER_SEPARATOR;
                afterPosition = ContainerPosition.BEFORE_SEPARATOR;
                if (index == 0) {
                    beforePosition = ContainerPosition.OPEN;
                }
                if (index == size - 1) {
                    afterPosition = ContainerPosition.CLOSE;
                }

                before = getMinimizedWhitespaceWithin(containerLocation, beforePosition);
                after = getMinimizedWhitespaceWithin(containerLocation, afterPosition);
            }
        }

        if (after != null) {
            if (index != size - 1 && right.getElement() instanceof J.Try.Resource) {
                //noinspection unchecked, ConstantConditions
                right = right.withElement((T) new JavaIsoVisitor<String>() {

                    @Override
                    public @Nullable <B> JRightPadded<B> visitRightPadded(@Nullable JRightPadded<B> right, JRightPadded.Location loc, String p) {
                        return right == null ? null : right.withAfter(minimizedLastComment(right.getAfter(), p));
                    }
                }.visit((Tree) right.getElement(), after));
            } else {
                right = right.withAfter(minimizedLastComment(right.getAfter(), after));
            }
        }

        setCursor(new Cursor(getCursor(), right));
        if (before != null) {
            getCursor().putMessage("before", before);
        }

        T t = right.getElement();
        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        Space afterSpace = right.getAfter();
        if (after == null) {
            afterSpace = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        }
        Markers markers = visitMarkers(right.getMarkers(), p);
        return (afterSpace == right.getAfter() && t == right.getElement() && markers == right.getMarkers()) ? right : new JRightPadded<>(t, afterSpace, markers);
    }

    @Override
    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JLeftPadded.Location loc, P p) {
        if (left == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), left));

        String before = null;
        String beforeElem = null;
        switch (loc) {
            case LANGUAGE_EXTENSION:
                break;
            case TERNARY_TRUE:
                before = evaluate(() -> spacesStyle.getTernaryOperator().getBeforeQuestionMark(), true) ? " " : "";
                beforeElem = evaluate(() -> spacesStyle.getTernaryOperator().getAfterQuestionMark(), true) ? " " : "";
                break;
            case TERNARY_FALSE:
                before = evaluate(() -> spacesStyle.getTernaryOperator().getBeforeColon(), true) ? " " : "";
                beforeElem = evaluate(() -> spacesStyle.getTernaryOperator().getAfterColon(), true) ? " " : "";
                break;
        }
        if (before != null) {
            getCursor().putMessage("before", before);
        }
        Space beforeSpace = visitSpace(left.getBefore(), loc.getBeforeLocation(), p);
        if (beforeElem != null) {
            getCursor().putMessage("before", beforeElem);
        }
        T t = left.getElement();

        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) left.getElement(), p);
        }

        setCursor(getCursor().getParent());
        // If nothing changed leave AST node the same
        if (left.getElement() == t && beforeSpace == left.getBefore()) {
            return left;
        }

        //noinspection ConstantConditions
        return t == null ? null : new JLeftPadded<>(beforeSpace, t, left.getMarkers());
    }

    @Override
    public Space visitSpace(@Nullable Space space, Space.Location loc, P ctx) {
        if (space == null) {
            return super.visitSpace(null, loc, ctx);
        }
        if (getCursor().getValue() instanceof JContainer) {
            Arrays.stream(JContainer.Location.values()).filter(l -> l.getBeforeLocation() == loc).findFirst().ifPresent(l -> getCursor().computeMessageIfAbsent("location", __ -> l));
        }
        String whitespace = null;
        String before = getCursor().pollNearestMessage("before");
        if (before != null) {
            return super.visitSpace(minimizedSkipComments(space, before), loc, ctx);
        }
        Cursor parentTreeCursor;
        J parent;
        BiFunction<Space, String, Space> minimized = this::minimizedLastComment;
        switch (loc) {
            case LANGUAGE_EXTENSION:
                break;
            case BLOCK_PREFIX:
                parentTreeCursor = getCursor().getParentTreeCursor();
                if (parentTreeCursor.getValue() instanceof J) {
                    parent = parentTreeCursor.getValue();
                    if (parent instanceof J.ClassDeclaration) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getClassLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.MethodDeclaration) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getMethodLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.If) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getIfLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.If.Else) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getElseLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.ForEachLoop || parent instanceof J.ForLoop) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getForLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.WhileLoop) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getWhileLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.DoWhileLoop) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getDoLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.Switch || parent instanceof J.SwitchExpression) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getSwitchLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.Try.Catch) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getCatchLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.Synchronized) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getSynchronizedLeftBrace(), true) ? " " : "";
                    } else if (parent instanceof J.Try) {
                        if (((J.Try) parent).getFinally() != null && ((J.Try) parent).getFinally().getId() == ((J.Block) getCursor().getValue()).getId()) {
                            whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getFinallyLeftBrace(), true) ? " " : "";
                        } else {
                            whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getTryLeftBrace(), true) ? " " : "";
                        }
                    } else if (parent instanceof J.Lambda) {
                        whitespace = evaluate(() -> spacesStyle.getAroundOperators().getLambdaArrow(), true) ? " " : "";
                    }
                }
                break;
            case LAMBDA_ARROW_PREFIX:
                whitespace = evaluate(() -> spacesStyle.getAroundOperators().getLambdaArrow(), true) ? " " : "";
                break;
            case METHOD_DECLARATION_PARAMETERS:
                whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getMethodDeclaration(), false) ? " " : "";
                break;
            case PRIMITIVE_PREFIX:
                parentTreeCursor = getCursor().getParentTreeCursor();
                if (parentTreeCursor.getValue() instanceof J) {
                    parent = parentTreeCursor.getValue();
                    TypeTree type;
                    if (parent instanceof J.MethodDeclaration) {
                        J.MethodDeclaration m = (J.MethodDeclaration) parent;
                        type = m.getReturnTypeExpression();
                        if (m.getModifiers().isEmpty() && type == getCursor().getValue()) {
                            whitespace = space.getWhitespace();
                        }
                    } else if (parent instanceof J.VariableDeclarations) {
                        J.VariableDeclarations v = (J.VariableDeclarations) parent;
                        type = v.getTypeExpression();
                        if (v.getModifiers().isEmpty() && type == getCursor().getValue()) {
                            whitespace = space.getWhitespace();
                        }
                    }
                }
                if (whitespace == null && !space.getWhitespace().isEmpty()) {
                    whitespace = " ";
                }
                break;
            case IDENTIFIER_PREFIX:
                if (getCursor().getParentTreeCursor().getValue() instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = getCursor().getParentTreeCursor().getValue();
                    if (mi.getName() == getCursor().getValue() && mi.getTypeParameters() != null) {
                        whitespace = evaluate(() -> spacesStyle.getTypeArguments().getAfterClosingAngleBracket(), false) ? " " : "";
                    } else {
                        whitespace = "";
                    }
                } else if (!space.getWhitespace().isEmpty()) {
                    whitespace = " ";
                }
                minimized = this::minimizedSkipComments;
                break;
            case MODIFIER_PREFIX:
                parentTreeCursor = getCursor().getParentTreeCursor();
                if (parentTreeCursor.getValue() instanceof J) {
                    parent = parentTreeCursor.getValue();
                    List<J.Modifier> modifiers = null;
                    if (parent instanceof J.MethodDeclaration) {
                        modifiers = ((J.MethodDeclaration) parent).getModifiers();
                    } else if (parent instanceof J.ClassDeclaration) {
                        modifiers = ((J.ClassDeclaration) parent).getModifiers();
                    } else if (parent instanceof J.VariableDeclarations) {
                        modifiers = ((J.VariableDeclarations) parent).getModifiers();
                    }
                    if (modifiers != null && modifiers.indexOf((J.Modifier) getCursor().getValue()) > 0) {
                        whitespace = " ";
                    }
                    if (modifiers != null && modifiers.indexOf((J.Modifier) getCursor().getValue()) == 0) {
                        if (!StringUtils.hasLineBreak(space.getWhitespace()) && !space.getWhitespace().isEmpty()) {
                            whitespace = " ";
                        }
                    }
                }
                break;
            case MEMBER_REFERENCE_NAME:
                whitespace = evaluate(() -> spacesStyle.getAroundOperators().getMethodReferenceDoubleColon(), false) ? " " : "";
                break;
            case CATCH_PREFIX:
                whitespace = evaluate(() -> spacesStyle.getBeforeKeywords().getCatchKeyword(), true) ? " " : "";
                break;
            case WHILE_CONDITION:
                whitespace = evaluate(() -> spacesStyle.getBeforeKeywords().getWhileKeyword(), true) ? " " : "";
                break;
            case IF_PREFIX:
                parentTreeCursor = getCursor().getParentTreeCursor();
                if (parentTreeCursor.getValue() instanceof J.If.Else && space.getComments().isEmpty()) {
                    space = space.withWhitespace(" ");
                }
                break;
            case ELSE_PREFIX:
                parentTreeCursor = getCursor().getParentTreeCursor();
                parent = parentTreeCursor.getValue();
                if (parent instanceof J.If && ((J.If) parent).getThenPart() instanceof J.Block && space.getComments().isEmpty()) {
                    space = space.withWhitespace(evaluate(() -> spacesStyle.getBeforeKeywords().getElseKeyword(), true) ? " " : "");
                }
                break;
            case TRY_FINALLY:
                whitespace = evaluate(() -> spacesStyle.getBeforeKeywords().getFinallyKeyword(), true) ? " " : "";
                break;
            case RECORD_STATE_VECTOR_SUFFIX:
            case METHOD_DECLARATION_PARAMETER_SUFFIX:
            case METHOD_INVOCATION_ARGUMENT_SUFFIX:
            case TYPE_PARAMETER_SUFFIX:
                whitespace = evaluate(() -> spacesStyle.getOther().getBeforeComma(), false) ? " " : "";
                break;
            case ANNOTATION_ARGUMENTS:
                whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getAnnotationParameters(), true) ? " " : "";
                break;
            case TRY_RESOURCES:
                whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getTryParentheses(), true) ? " " : "";
                break;
            case FOR_CONTROL_PREFIX:
            case FOR_EACH_CONTROL_PREFIX:
                whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getForParentheses(), true) ? " " : "";
                break;
            case CONTROL_PARENTHESES_PREFIX:
                parentTreeCursor = getCursor().getParentTreeCursor();
                if (parentTreeCursor.getValue() instanceof J) {
                    parent = parentTreeCursor.getValue();
                    if (parent instanceof J.If) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getIfParentheses(), true) ? " " : "";
                    } else if (parent instanceof J.WhileLoop) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getWhileParentheses(), true) ? " " : "";
                    } else if (parent instanceof J.Switch) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getSwitchParentheses(), true) ? " " : "";
                    } else if (parent instanceof J.Try.Catch) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getCatchParentheses(), true) ? " " : "";
                    } else if (parent instanceof J.Synchronized) {
                        whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getSynchronizedParentheses(), true) ? " " : "";
                    }
                }
                break;
            case TYPE_PARAMETERS:
            case TYPE_PARAMETERS_PREFIX:
                parentTreeCursor = getCursor().getParentTreeCursor();
                if (parentTreeCursor.getValue() instanceof J.MethodDeclaration) {
                    whitespace = ((J.MethodDeclaration) parentTreeCursor.getValue()).getModifiers().isEmpty() ? "" : " ";
                } else if (parentTreeCursor.getValue() instanceof J.ClassDeclaration) {
                    whitespace = evaluate(() -> spacesStyle.getTypeParameters().getBeforeOpeningAngleBracket(), false) ? " " : "";
                } else {
                    whitespace = evaluate(() -> spacesStyle.getTypeArguments().getBeforeOpeningAngleBracket(), false) ? " " : "";
                }
                break;
            case NEW_CLASS_ARGUMENTS:
            case METHOD_INVOCATION_ARGUMENTS:
                whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getMethodCall(), false) ? " " : "";
                break;
            case NEW_ARRAY_INITIALIZER:
                whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getArrayInitializerLeftBrace(), false) ? " " : "";
                break;
            case UNARY_OPERATOR: //intelliJ does not format the i++ to i ++ when spacesStyle.getAroundOperators().getUnary() is true;
            case METHOD_SELECT_SUFFIX:
            case VARARGS:
            case TYPE_BOUND_SUFFIX:
                whitespace = "";
                break;
            case VARIABLE_PREFIX:
            case LAMBDA_PARAMETERS_PREFIX:
            case LAMBDA_PREFIX:
                if (!space.getWhitespace().isEmpty()) {
                    whitespace = " ";
                }
                break;
            case TYPE_BOUNDS:
                whitespace = " ";
                break;
            case BLOCK_END:
                J.Block block = getCursor().getValue();
                if (getCursor().dropParentWhile(v -> !(v instanceof J.ClassDeclaration) && !Cursor.ROOT_VALUE.equals(v)).getMessage("singleLineEnum") == Boolean.TRUE) {
                    if (block.getStatements().isEmpty()) {
                        whitespace = evaluate(() -> spacesStyle.getOther().getInsideOneLineEnumBraces(), false) ? " " : "";
                    } else {
                        whitespace = "";
                    }
                    break;
                } else if (block.getStatements().isEmpty() && block.getEnd().getComments().isEmpty()) {
                    parentTreeCursor = getCursor().getParentTreeCursor();
                    if (parentTreeCursor.getValue() instanceof J.ClassDeclaration || parentTreeCursor.getValue() instanceof J.MethodDeclaration) {
                        whitespace = evaluate(() -> spacesStyle.getWithin().getCodeBraces(), false) ? " " : "";
                    }
                }
                if (block.getStatements().isEmpty()) {
                    if (StringUtils.countOccurrences(block.getEnd().getWhitespace(), "\n") > 3) {
                        space = space.withWhitespace("\n\n\n" + block.getEnd().getWhitespace().substring(block.getEnd().getWhitespace().lastIndexOf("\n") + 1));
                    }
                    space = space.withComments(ListUtils.map(space.getComments(), comment -> {
                        if (StringUtils.countOccurrences(comment.getSuffix(), "\n") > 3) {
                            comment = comment.withSuffix("\n\n\n" + comment.getSuffix().substring(comment.getSuffix().lastIndexOf("\n") + 1));
                        }
                        return comment;
                    }));
                }
                break;
            case BINARY_OPERATOR:
                J.Binary binary = getCursor().getParentTreeCursor().getValue();
                whitespace = getWhitespaceAroundOperator(binary.getOperator());
                break;
            case ASSIGNMENT_OPERATION_OPERATOR:
            case ASSIGNMENT:
            case VARIABLE_INITIALIZER:
                parentTreeCursor = getCursor().getParentTreeCursor().getParentTreeCursor();
                if (parentTreeCursor.getValue() instanceof J.Annotation) {
                    whitespace = evaluate(() -> spacesStyle.getOther().getAroundEqualInAnnotationValuePair(), true) ? " " : "";
                } else {
                    whitespace = evaluate(() -> spacesStyle.getAroundOperators().getAssignment(), true) ? " " : "";
                }
                break;
            case ANNOTATION_PREFIX:
                boolean firstAnnotation = true;
                parentTreeCursor = getCursor().getParentTreeCursor();
                if (parentTreeCursor.getValue() instanceof J.ClassDeclaration) {
                    firstAnnotation = ((J.ClassDeclaration) parentTreeCursor.getValue()).getLeadingAnnotations().indexOf(getCursor().getValue()) <= 0;
                } else if (parentTreeCursor.getValue() instanceof J.MethodDeclaration) {
                    firstAnnotation = ((J.MethodDeclaration) parentTreeCursor.getValue()).getLeadingAnnotations().indexOf(getCursor().getValue()) <= 0;
                } else if (parentTreeCursor.getValue() instanceof J.VariableDeclarations) {
                    firstAnnotation = ((J.VariableDeclarations) parentTreeCursor.getValue()).getLeadingAnnotations().indexOf(getCursor().getValue()) <= 0;
                }
                if (!firstAnnotation && space.getWhitespace().isEmpty() && (space.getComments().isEmpty() || space.getComments().get(space.getComments().size() - 1).getSuffix().isEmpty())) {
                    whitespace = " ";
                }
            case ENUM_VALUE_SET_PREFIX:
                if (getCursor().getNearestMessage("singleLineEnum") == Boolean.TRUE) {
                    whitespace = evaluate(() -> spacesStyle.getOther().getInsideOneLineEnumBraces(), false) ? " " : "";
                }
                break;
            default:
                if (!StringUtils.hasLineBreak(space.getWhitespace()) && !space.getWhitespace().isEmpty()) {
                    whitespace = " ";
                }
                break;
        }
        if (whitespace != null) {
            return super.visitSpace(minimized.apply(space, whitespace), loc, ctx);
        }
        return super.visitSpace(space, loc, ctx);
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast, P p) {
        String afterTypeCast = evaluate(() -> spacesStyle.getOther().getAfterTypeCast(), true) ? " " : "";

        return super.visitTypeCast(typeCast.withExpression(minimized(typeCast.getExpression(), afterTypeCast)), p);
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, P p) {
        String afterArrow = evaluate(() -> spacesStyle.getAroundOperators().getLambdaArrow(), true) ? " " : "";
        return super.visitLambda(lambda.withBody(minimized(lambda.getBody(), afterArrow)), p);
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        switch (unary.getOperator()) {
            case PreIncrement:
            case PreDecrement:
            case Positive:
            case Negative:
            case Complement:
            case Not:
                unary = unary.withExpression(minimized(unary.getExpression(), evaluate(() -> spacesStyle.getAroundOperators().getUnary(), false) ? " " : ""));
                break;
        }
        return super.visitUnary(unary, p);
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        return super.visitBinary(binary.withRight(minimized(binary.getRight(), getWhitespaceAroundOperator(binary.getOperator()))), p);
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        if (variable.getInitializer() != null) {
            String afterOperator = evaluate(() -> spacesStyle.getAroundOperators().getAssignment(), true) ? " " : "";
            variable = variable.withInitializer(minimized(variable.getInitializer(), afterOperator));
        }
        return super.visitVariable(variable, p);
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, P p) {
        String afterOperator;
        if (getCursor().getParentTreeCursor().getValue() instanceof J.Annotation) {
            afterOperator = evaluate(() -> spacesStyle.getOther().getAroundEqualInAnnotationValuePair(), true) ? " " : "";
        } else {
            afterOperator = evaluate(() -> spacesStyle.getAroundOperators().getAssignment(), true) ? " " : "";
        }
        return super.visitAssignment(assignment.withAssignment(minimized(assignment.getAssignment(), afterOperator)), p);
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        String afterOperator = evaluate(() -> spacesStyle.getAroundOperators().getAssignment(), true) ? " " : "";
        return super.visitAssignmentOperation(assignOp.withAssignment(minimized(assignOp.getAssignment(), afterOperator)), p);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Enum) {
            boolean singleLineEnum = !hasLineBreakInSpace(classDecl.getBody().getPrefix()) && !hasLineBreakInSpace(classDecl.getBody().getEnd());
            if (classDecl.getBody().getStatements().size() == 1 && classDecl.getBody().getStatements().get(0) instanceof J.EnumValueSet) {
                J.EnumValueSet enumValueSet = (J.EnumValueSet) classDecl.getBody().getStatements().get(0);
                if (hasLineBreakInSpace(enumValueSet.getPrefix()) || enumValueSet.getEnums().stream().map(J.EnumValue::getPrefix).anyMatch(this::hasLineBreakInSpace)) {
                    singleLineEnum = false;
                }
            } else if (!classDecl.getBody().getStatements().isEmpty()) {
                singleLineEnum = false;
            }
            getCursor().putMessage("singleLineEnum", singleLineEnum);
        }
        return super.visitClassDeclaration(classDecl, p);
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    private <T extends Expression> T minimized(T j, String whitespace) {
        return j.withPrefix(minimizedSkipComments(j.getPrefix(), whitespace));
    }

    private J minimized(J j, String whitespace) {
        return j.withPrefix(minimizedSkipComments(j.getPrefix(), whitespace));
    }

    private Space minimizedSkipComments(Space space, String whitespace) {
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

    private Space minimizedLastComment(Space space, String whitespace) {
        //IntelliJ only formats last comments suffix.
        return minimizedSkipComments(space, whitespace).withComments(
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
                        return comment.withSuffix(comment.getSuffix().substring(comment.getSuffix().lastIndexOf('\n')));
                    }
                    return comment;
                })
        );
    }

    private @Nullable String getMinimizedWhitespaceWithin(JContainer.Location loc, ContainerPosition containerPosition) {
        if (loc != JContainer.Location.TYPE_BOUNDS && loc != JContainer.Location.TRY_RESOURCES && loc != JContainer.Location.TYPE_PARAMETERS && containerPosition == ContainerPosition.AFTER_SEPARATOR) {
            return evaluate(() -> spacesStyle.getOther().getAfterComma(), true) ? " " : "";
        }
        if (loc != JContainer.Location.TYPE_BOUNDS && loc != JContainer.Location.TRY_RESOURCES && loc != JContainer.Location.TYPE_PARAMETERS && containerPosition == ContainerPosition.BEFORE_SEPARATOR) {
            return evaluate(() -> spacesStyle.getOther().getBeforeComma(), true) ? " " : "";
        }
        switch (loc) {
            case RECORD_STATE_VECTOR:
                if (containerPosition == ContainerPosition.EMPTY) {
                    return ""; // there is no intelliJ style existing for this
                }
                return evaluate(() -> spacesStyle.getWithin().getRecordHeader(), false) ? " " : "";
            case METHOD_DECLARATION_PARAMETERS:
                if (containerPosition == ContainerPosition.EMPTY) {
                    return evaluate(() -> spacesStyle.getWithin().getEmptyMethodDeclarationParentheses(), false) ? " " : "";
                }
                return evaluate(() -> spacesStyle.getWithin().getMethodDeclarationParentheses(), false) ? " " : "";
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                if (containerPosition == ContainerPosition.EMPTY) {
                    return evaluate(() -> spacesStyle.getWithin().getEmptyMethodCallParentheses(), false) ? " " : "";
                }
                return evaluate(() -> spacesStyle.getWithin().getMethodCallParentheses(), false) ? " " : "";
            case ANNOTATION_ARGUMENTS:
                return evaluate(() -> spacesStyle.getWithin().getAnnotationParentheses(), false) ? " " : "";
            case TRY_RESOURCES:
                if (containerPosition == ContainerPosition.AFTER_SEPARATOR) {
                    return evaluate(() -> spacesStyle.getOther().getAfterForSemicolon(), true) ? " " : "";
                }
                if (containerPosition == ContainerPosition.BEFORE_SEPARATOR) {
                    JContainer<J.Try.Resource> resources = getCursor().getValue();
                    if (!resources.getElements().isEmpty() && resources.getElements().get(0).isTerminatedWithSemicolon()) {
                        return evaluate(() -> spacesStyle.getOther().getBeforeForSemicolon(), false) ? " " : "";
                    }
                    return "";
                }
                return evaluate(() -> spacesStyle.getWithin().getTryParentheses(), false) ? " " : "";
            case NEW_ARRAY_INITIALIZER:
                if (containerPosition == ContainerPosition.EMPTY) {
                    return evaluate(() -> spacesStyle.getWithin().getEmptyArrayInitializerBraces(), false) ? " " : "";
                }
                return evaluate(() -> spacesStyle.getWithin().getArrayInitializerBraces(), false) ? " " : "";
            case TYPE_PARAMETERS:
                if (containerPosition == ContainerPosition.AFTER_SEPARATOR) {
                    Object value = getCursor().getParentTreeCursor().getValue();
                    if (value instanceof J.MethodDeclaration || value instanceof J.ClassDeclaration) {
                        return evaluate(() -> spacesStyle.getOther().getAfterComma(), true) ? " " : "";
                    }
                    return evaluate(() -> spacesStyle.getTypeArguments().getAfterComma(), true) ? " " : "";
                }
                if (containerPosition == ContainerPosition.BEFORE_SEPARATOR || containerPosition == ContainerPosition.EMPTY) {
                    return ""; // there is no intelliJ style existing for this
                }
                return evaluate(() -> spacesStyle.getWithin().getAngleBrackets(), false) ? " " : "";
            case TYPE_BOUNDS:
                if (containerPosition == ContainerPosition.AFTER_SEPARATOR || containerPosition == ContainerPosition.BEFORE_SEPARATOR) {
                    return evaluate(() -> spacesStyle.getTypeParameters().getAroundTypeBounds(), true) ? " " : "";
                }
                if (containerPosition == ContainerPosition.OPEN) {
                    return " ";
                }
                return "";
        }
        return null;
    }

    private String getWhitespaceAroundOperator(J.Binary.Type operator) {
        switch (operator) {
            case Addition:
            case Subtraction:
                return evaluate(() -> spacesStyle.getAroundOperators().getAdditive(), true) ? " " : "";
            case Multiplication:
            case Division:
            case Modulo:
                return evaluate(() -> spacesStyle.getAroundOperators().getMultiplicative(), true) ? " " : "";
            case LessThan:
            case GreaterThan:
            case LessThanOrEqual:
            case GreaterThanOrEqual:
                return evaluate(() -> spacesStyle.getAroundOperators().getRelational(), true) ? " " : "";
            case Equal:
            case NotEqual:
                return evaluate(() -> spacesStyle.getAroundOperators().getEquality(), true) ? " " : "";
            case BitAnd:
            case BitOr:
            case BitXor:
                return evaluate(() -> spacesStyle.getAroundOperators().getBitwise(), true) ? " " : "";
            case LeftShift:
            case RightShift:
            case UnsignedRightShift:
                return evaluate(() -> spacesStyle.getAroundOperators().getShift(), true) ? " " : "";
            case Or:
            case And:
            default:
                return evaluate(() -> spacesStyle.getAroundOperators().getLogical(), true) ? " " : "";
        }
    }

    private boolean evaluate(Supplier<Boolean> supplier, boolean defaultValue) {
        try {
            return supplier.get();
        } catch (NoSuchMethodError e) {
            // Handle newly introduced method calls on style that are not part of lst yet
            return defaultValue;
        }
    }

    private boolean hasLineBreakInSpace(Space space) {
        if (StringUtils.hasLineBreak(space.getWhitespace())) {
            return true;
        }
        for (Comment comment : space.getComments()) {
            if (StringUtils.hasLineBreak(comment.getSuffix())) {
                return true;
            }
        }
        return false;
    }

    private enum ContainerPosition {
        OPEN, CLOSE, BEFORE_SEPARATOR, AFTER_SEPARATOR, EMPTY
    }

    @ToBeRemoved(after = "2026-01-30", reason = "Replace me with org.openrewrite.style.StyleHelper.getStyle now available in parent runtime")
    private static <S extends Style> @Nullable S getStyle(Class<S> styleClass, List<NamedStyles> styles) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return (S) style.applyDefaults();
        }
        return null;
    }

    @ToBeRemoved(after = "2026-01-30", reason = "Replace me with org.openrewrite.style.StyleHelper.getStyle now available in parent runtime")
    private static <S extends Style> S getStyle(Class<S> styleClass, List<NamedStyles> styles, Supplier<S> defaultStyle) {
        S style = NamedStyles.merge(styleClass, styles);
        if (style != null) {
            return StyleHelper.merge(defaultStyle.get(), style);
        }
        return defaultStyle.get();
    }
}
