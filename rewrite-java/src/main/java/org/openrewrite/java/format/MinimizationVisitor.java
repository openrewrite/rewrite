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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.Style;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class MinimizationVisitor<P> extends JavaIsoVisitor<P> {

    private final SpacesStyle spacesStyle;

    public static Cursor minimized(Cursor cursor) {
        JavaSourceFile sourceFile = cursor.firstEnclosing(JavaSourceFile.class);
        if (sourceFile == null) {
            return minimized(cursor, IntelliJ.spaces());
        }
        return minimized(cursor, Style.from(SpacesStyle.class, sourceFile, IntelliJ::spaces));
    }

    public static Cursor minimized(Cursor cursor, SpacesStyle spacesStyle) {
        if (cursor.getValue() instanceof J) {
            return new Cursor(cursor.getParent(), new MinimizationVisitor<Integer>(spacesStyle).visit((J) cursor.getValue(), -1));
        }
        throw new IllegalArgumentException("Can only minimize J elements.");
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

        ContainerPosition beforePosition = null;
        ContainerPosition afterPosition = null;
        JContainer.Location containerLocation = cursor.getMessage("location");
        int index = -1;
        int size = -1;

        switch (loc) {
            case MEMBER_REFERENCE_CONTAINING:
                after = evaluate(() -> spacesStyle.getAroundOperators().getMethodReferenceDoubleColon(), false) ? " " : "";
                break;
            case PARENTHESES:
                parent = parentTreeCursor.getValue();
                if (parent instanceof J.If) {
                    before = evaluate(() -> spacesStyle.getWithin().getIfParentheses(), false) ? " " : "";
                } else if (parent instanceof J.WhileLoop || parent instanceof J.DoWhileLoop) {
                    before = evaluate(() -> spacesStyle.getWithin().getWhileParentheses(), false) ? " " : "";
                } else if (parent instanceof J.Switch) {
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
                } else if (((J.Parentheses<?>) getCursor().getValue()).getTree() instanceof J.Binary) {
                    before = evaluate(() -> spacesStyle.getWithin().getGroupingParentheses(), false) ? " " : "";
                }
                after = before;
                break;
            case FOR_INIT:
                before = evaluate(() -> spacesStyle.getWithin().getForParentheses(), false) ? " " : "";
                after = evaluate(() -> spacesStyle.getOther().getBeforeForSemicolon(), true) ? " " : "";
                break;
            case FOR_CONDITION:
                before = evaluate(() -> spacesStyle.getOther().getAfterForSemicolon(), true) ? " " : "";
                after = evaluate(() -> spacesStyle.getOther().getBeforeForSemicolon(), true) ? " " : "";
                break;
            case FOR_UPDATE:
                before = evaluate(() -> spacesStyle.getOther().getAfterForSemicolon(), true) ? " " : "";
                after = evaluate(() -> spacesStyle.getWithin().getForParentheses(), false) ? " " : "";
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
                        before = evaluate(() -> spacesStyle.getOther().getInsideOneLineEnumBraces(), false) ? " " : "";
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
                break;
        }

        if (index >= 0 && containerLocation != null) {
            beforePosition = ContainerPosition.AFTER_SEPARATOR;
            afterPosition = ContainerPosition.BEFORE_SEPARATOR;
            if (index == 0) {
                beforePosition = ContainerPosition.OPEN;
            }
            if (index == size - 1) {
                afterPosition = ContainerPosition.CLOSE;
            }
            before = getMinimizedWhitespaceWithin(null, containerLocation, beforePosition);
            after = getMinimizedWhitespaceWithin(null, containerLocation, afterPosition);
        }

        if (after != null) {
            if (index != size - 1 && right.getElement() instanceof J.Try.Resource) {
                //noinspection unchecked, ConstantConditions
                right = right.withElement((T) new JavaIsoVisitor<String>() {
                    @Override
                    public @Nullable <B> JRightPadded<B> visitRightPadded(@Nullable JRightPadded<B> right, JRightPadded.Location loc, String p) {
                        return right == null ? null : right.withAfter(minimized(right.getAfter(), p));
                    }
                }.visit((Tree) right.getElement(), after));
            } else {
                right = right.withAfter(minimized(right.getAfter(), after));
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

        Space afterSpace = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        Markers markers = visitMarkers(right.getMarkers(), p);
        return (afterSpace == right.getAfter() && t == right.getElement() && markers == right.getMarkers()) ? right : new JRightPadded<>(t, afterSpace, markers);
    }

    @Override
    public Space visitSpace(@Nullable Space space, Space.Location loc, P ctx) {
        if (space == null) {
            return super.visitSpace(null, loc, ctx);
        }
        if (getCursor().getValue() instanceof JContainer) {
            Arrays.stream(JContainer.Location.values()).filter(l -> l.getBeforeLocation().equals(loc)).findFirst().ifPresent(l ->
                    getCursor().computeMessageIfAbsent("location", __ -> l));
        }
        String whitespace = null;
        String before = getCursor().pollNearestMessage("before");
        if (before != null) {
            whitespace = before;
        }
        if (whitespace == null) {
            Cursor parentTreeCursor;
            J parent;
            switch (loc) {
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
                case ANNOTATED_TYPE_PREFIX:
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
                    if (!space.getWhitespace().isEmpty()) {
                        if (getCursor().getParentTreeCursor().getValue() instanceof J.MethodInvocation) {
                            whitespace = "";
                        } else {
                            whitespace = " ";
                        }
                    }
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
                case ELSE_PREFIX:
                    whitespace = evaluate(() -> spacesStyle.getBeforeKeywords().getElseKeyword(), true) ? " " : "";
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
                        whitespace = ((J.ClassDeclaration) parentTreeCursor.getValue()).getModifiers().isEmpty() ? "" : " ";
                    } else {
                        whitespace = evaluate(() -> spacesStyle.getTypeArguments().getBeforeOpeningAngleBracket(), false) ? " " : "";
                    }
                    break;
                case METHOD_INVOCATION_ARGUMENTS:
                    whitespace = evaluate(() -> spacesStyle.getBeforeParentheses().getMethodCall(), false) ? " " : "";
                    break;
                case NEW_ARRAY_INITIALIZER:
                    whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getArrayInitializerLeftBrace(), false) ? " " : "";
                    break;
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
                case ENUM_VALUE_SET_PREFIX:
                    Cursor classCursor = getCursor().getParentTreeCursor().getParentTreeCursor();
                    if (classCursor.getMessage("singleLineEnum") == Boolean.TRUE && evaluate(() -> spacesStyle.getOther().getInsideOneLineEnumBraces(), false)) {
                        whitespace = evaluate(() -> spacesStyle.getOther().getInsideOneLineEnumBraces(), false) ? " " : "";
                    }
                    break;
                case TYPE_BOUNDS:
                    whitespace = " ";
                    break;
                case BLOCK_END:
                    J.Block block = getCursor().getValue();
                    if (block.getStatements().isEmpty() && block.getEnd().getComments().isEmpty()) {
                        whitespace = "";
                    } else if (!StringUtils.hasLineBreak(space.getWhitespace()) && !space.getWhitespace().isEmpty()) {
                        parentTreeCursor = getCursor().dropParentWhile(v -> !(v instanceof J.ClassDeclaration));
                        if (parentTreeCursor.getMessage("singleLineEnum") == Boolean.TRUE) {
                            whitespace = evaluate(() -> spacesStyle.getOther().getInsideOneLineEnumBraces(), false) ? " " : "";
                        } else {
                            whitespace = " ";
                        }
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
                default:
                    if (!StringUtils.hasLineBreak(space.getWhitespace()) && !space.getWhitespace().isEmpty()) {
                        whitespace = " ";
                    }
                    break;
            }
        }
        if (whitespace != null) {
            return super.visitSpace(minimized(space, whitespace), loc, ctx);
        }
        return super.visitSpace(space, loc, ctx);
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast, P p) {
        String afterTypeCast = evaluate(() -> spacesStyle.getOther().getAfterTypeCast(), true) ? " " : "";

        return super.visitTypeCast(typeCast.withExpression(minimized(typeCast.getExpression(), afterTypeCast)), p);
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
            boolean singleLineEnum = true;
            if (hasLineBreakInSpace(classDecl.getBody().getPrefix()) || hasLineBreakInSpace(classDecl.getBody().getEnd())) {
                singleLineEnum = false;
            }
            if (classDecl.getBody().getStatements().size() == 1 && classDecl.getBody().getStatements().get(0) instanceof J.EnumValueSet) {
                J.EnumValueSet enumValueSet = (J.EnumValueSet) classDecl.getBody().getStatements().get(0);
                if (hasLineBreakInSpace(enumValueSet.getPrefix()) || enumValueSet.getEnums().stream().map(J.EnumValue::getPrefix).anyMatch(this::hasLineBreakInSpace)) {
                    singleLineEnum = false;
                }
            } else {
                singleLineEnum = false;
            }
            getCursor().putMessage("singleLineEnum", singleLineEnum);
        }
        return super.visitClassDeclaration(classDecl, p);
    }

    private <T extends Expression> T minimized(T j, String whitespace) {
        return j.withPrefix(minimized(j.getPrefix(), whitespace));
    }

    //IntelliJ does not format when comments are present.
    private Space minimized(Space space, String whitespace) {
        if (space.getComments().isEmpty()) {
            if (StringUtils.hasLineBreak(whitespace)) {
                //Reduce to single new line
                return space.withWhitespace(whitespace.substring(whitespace.lastIndexOf('\n')));
            }
            return space.withWhitespace(whitespace);
        }
        return space;
    }

    private @Nullable String getMinimizedWhitespaceWithin(@Nullable String whitespace, JContainer.Location loc, ContainerPosition containerPosition) {
        if (loc != JContainer.Location.TYPE_BOUNDS && loc != JContainer.Location.TRY_RESOURCES && containerPosition == ContainerPosition.AFTER_SEPARATOR) {
            return evaluate(() -> spacesStyle.getOther().getAfterComma(), true) ? " " : "";
        }
        if (loc != JContainer.Location.TYPE_BOUNDS && loc != JContainer.Location.TRY_RESOURCES && containerPosition == ContainerPosition.BEFORE_SEPARATOR) {
            return evaluate(() -> spacesStyle.getOther().getBeforeComma(), true) ? " " : "";
        }
        switch (loc) {
            case RECORD_STATE_VECTOR:
                if (getCursor().getValue() instanceof J.Empty) {
                    return ""; // there is no intelliJ style existing for this
                }
                return evaluate(() -> spacesStyle.getWithin().getRecordHeader(), false) ? " " : "";
            case METHOD_DECLARATION_PARAMETERS:
                if (getCursor().getValue() instanceof J.Empty) {
                    return evaluate(() -> spacesStyle.getWithin().getEmptyMethodDeclarationParentheses(), false) ? " " : "";
                }
                return evaluate(() -> spacesStyle.getWithin().getMethodDeclarationParentheses(), false) ? " " : "";
            case METHOD_INVOCATION_ARGUMENTS:
            case NEW_CLASS_ARGUMENTS:
                if (getCursor().getValue() instanceof J.Empty) {
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
                    } else {
                        return "";
                    }
                }
                return evaluate(() -> spacesStyle.getWithin().getTryParentheses(), false) ? " " : "";
            case NEW_ARRAY_INITIALIZER:
                if (getCursor().getValue() instanceof J.Empty) {
                    return evaluate(() -> spacesStyle.getWithin().getEmptyArrayInitializerBraces(), false) ? " " : "";
                }
                return evaluate(() -> spacesStyle.getWithin().getArrayInitializerBraces(), false) ? " " : "";
            case TYPE_PARAMETERS:
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
        return whitespace;
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
        OPEN, CLOSE, BEFORE_SEPARATOR, AFTER_SEPARATOR
    }
}
