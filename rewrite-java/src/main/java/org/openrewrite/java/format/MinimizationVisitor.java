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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.style.Style;

import java.util.List;
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
    public @Nullable J visit(@Nullable Tree tree, P p) {
        J changeTree = super.visit(tree, p);
        if (changeTree != tree) {
            return changeTree;
        }
        return (J) tree;
    }

    @Override
    public @Nullable <J2 extends J> JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, P p) {
        if (container == null || container.getElements().isEmpty()) {
            return super.visitContainer(container, loc, p);
        }
        return container
                .getPadding().withElements(ListUtils.map(container.getPadding().getElements(), (i, right) -> {
                    Space after = right.getAfter();
                    if (i == 0) {
                        right = right.withElement(right.getElement().withPrefix(minimized(right.getElement().getPrefix(), getMinimizedWhitespaceWithin(right.getElement().getPrefix().getWhitespace(), loc))));
                    }
                    if (i == container.getElements().size() - 1 && !(right.getElement() instanceof J.Empty)) {
                        after = minimized(after, getMinimizedWhitespaceWithin(after.getWhitespace(), loc));
                    }
                    return super.visitRightPadded(right.withAfter(after), loc.getElementLocation(), p);
                }))
                .withBefore(minimized(container.getBefore(), getMinimizedWhitespaceBefore(container.getBefore().getWhitespace(), loc)));
    }

    @Override
    public Space visitSpace(@Nullable Space space, Space.Location loc, P ctx) {
        if (space == null) {
            return super.visitSpace(null, loc, ctx);
        }
        String whitespace = null;

        boolean inRightPadded = getCursor().getParent() != null && getCursor().getParent().getValue() instanceof JRightPadded;
        if (inRightPadded) {
            switch (loc) {
                case BINARY_PREFIX:
                case EXPRESSION_PREFIX:
                case FIELD_ACCESS_PREFIX:
                case IDENTIFIER_PREFIX:
                case LITERAL_PREFIX:
                case METHOD_INVOCATION_ARGUMENTS:
                case METHOD_INVOCATION_PREFIX:
                case NEW_ARRAY_PREFIX:
                case NEW_PREFIX:
                case PRIMITIVE_PREFIX:
                case RECORD_STATE_VECTOR:
                case VARIABLE_DECLARATIONS_PREFIX:
                    Cursor parent = getCursor().getParentTreeCursor();
                    List<?> list = null;
                    J parentValue = parent.getValue();
                    if (parentValue instanceof J.MethodDeclaration) {
                        list = ((J.MethodDeclaration) parentValue).getPadding().getParameters().getElements();
                    } else if (parentValue instanceof J.ClassDeclaration && ((J.ClassDeclaration) parentValue).getPadding().getPrimaryConstructor() != null) {
                        list = ((J.ClassDeclaration) parentValue).getPadding().getPrimaryConstructor().getElements();
                    } else if (parentValue instanceof J.MethodInvocation) {
                        list = ((J.MethodInvocation) parentValue).getPadding().getArguments().getElements();
                    } else if (parentValue instanceof J.Lambda.Parameters) {
                        list = ((J.Lambda.Parameters) parentValue).getParameters();
                    }
                    if (list != null && list.indexOf(getCursor().getValue()) > 0) {
                        whitespace = evaluate(() -> spacesStyle.getOther().getAfterComma(), true) ? " " : "";
                    }
            }
        }
        if (whitespace == null) {
            Cursor parentCursor;
            switch (loc) {
                case BLOCK_PREFIX:
                    parentCursor = getCursor().getParentTreeCursor();
                    if (parentCursor.getValue() instanceof J) {
                        J parent = parentCursor.getValue();
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
                case TYPE_PARAMETERS_PREFIX:
                case IDENTIFIER_PREFIX:
                case VARIABLE_PREFIX:
                case LAMBDA_PARAMETERS_PREFIX:
                case LAMBDA_PREFIX:
                    if (!space.getWhitespace().isEmpty()) {
                        whitespace = " ";
                    }
                    break;
                case MODIFIER_PREFIX:
                    parentCursor = getCursor().getParentTreeCursor();
                    if (parentCursor.getValue() instanceof J) {
                        J parent = parentCursor.getValue();
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
                case NEW_ARRAY_INITIALIZER_SUFFIX:
                    whitespace = evaluate(() -> spacesStyle.getBeforeLeftBrace().getArrayInitializerLeftBrace(), false) ? " " : "";
                    break;
                case METHOD_SELECT_SUFFIX:
                case VARARGS:
                    whitespace = "";
                    break;
                case RECORD_STATE_VECTOR_SUFFIX:
                case METHOD_DECLARATION_PARAMETER_SUFFIX:
                case METHOD_INVOCATION_ARGUMENT_SUFFIX:
                case LAMBDA_PARAMETER:
                    whitespace = evaluate(() -> spacesStyle.getOther().getBeforeComma(), false) ? " " : "";
                    break;
            }
        }
        if (whitespace != null) {
            return super.visitSpace(minimized(space, whitespace), loc, ctx);
        }
        return super.visitSpace(space, loc, ctx);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        return super.visitMethodInvocation(method.withName(minimized(method.getName(), "")), p);
    }

    @Override
    public J.Lambda.Parameters visitLambdaParameters(J.Lambda.Parameters parameters, P p) {
        List<J> params = parameters.getParameters();
        parameters = parameters.getPadding().withParameters(
                ListUtils.map(parameters.getPadding().getParameters(),
                        (i, right) -> {
                            Space after = right.getAfter();
                            if (i == 0) {
                                right = right.withElement(right.getElement().withPrefix(minimized(right.getElement().getPrefix(), getMinimizedWhitespaceWithin(right.getElement().getPrefix().getWhitespace(), JContainer.Location.METHOD_DECLARATION_PARAMETERS))));
                            }
                            if (i == params.size() - 1 && !(right.getElement() instanceof J.Empty)) {
                                after = minimized(after, getMinimizedWhitespaceWithin(after.getWhitespace(), JContainer.Location.METHOD_DECLARATION_PARAMETERS));
                            }
                            return right.withAfter(after);
                        }
                )
        );
        return super.visitLambdaParameters(parameters, p);
    }

    private <T extends J> T minimized(T j, String whitespace) {
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

    private String getMinimizedWhitespaceBefore(String whitespace, JContainer.Location loc) {
        switch (loc) {
            case METHOD_DECLARATION_PARAMETERS:
                return evaluate(() -> spacesStyle.getBeforeParentheses().getMethodDeclaration(), false) ? " " : "";
            case METHOD_INVOCATION_ARGUMENTS:
                return evaluate(() -> spacesStyle.getBeforeParentheses().getMethodCall(), false) ? " " : "";
        }
        return whitespace;
    }

    private String getMinimizedWhitespaceWithin(String whitespace, JContainer.Location loc) {
        switch (loc) {
            case RECORD_STATE_VECTOR:
                if (getCursor().getValue() instanceof J.Empty) {
                    return ""; //TODO there is no intelliJ style existing for this
                }
                return evaluate(() -> spacesStyle.getWithin().getRecordHeader(), false) ? " " : "";
            case METHOD_DECLARATION_PARAMETERS:
                if (getCursor().getValue() instanceof J.Empty) {
                    return evaluate(() -> spacesStyle.getWithin().getEmptyMethodDeclarationParentheses(), false) ? " " : "";
                }
                return evaluate(() -> spacesStyle.getWithin().getMethodDeclarationParentheses(), false) ? " " : "";
            case METHOD_INVOCATION_ARGUMENTS:
                if (getCursor().getValue() instanceof J.Empty) {
                    return evaluate(() -> spacesStyle.getWithin().getEmptyMethodCallParentheses(), false) ? " " : "";
                }
                return evaluate(() -> spacesStyle.getWithin().getMethodCallParentheses(), false) ? " " : "";
        }
        return whitespace;
    }

    private boolean evaluate(Supplier<Boolean> supplier, boolean defaultValue) {
        try {
            return supplier.get();
        } catch (NoSuchMethodError e) {
            // Handle newly introduced method calls on style that are not part of lst yet
            return defaultValue;
        }
    }
}
