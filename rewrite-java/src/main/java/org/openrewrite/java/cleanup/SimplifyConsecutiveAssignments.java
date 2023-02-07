/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class SimplifyConsecutiveAssignments extends Recipe {
    @Override
    public String getDisplayName() {
        return "Simplify consecutive assignments";
    }

    @Override
    public String getDescription() {
        return "Combine consecutive assignments into a single statement where possible.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            // TODO if we had a `replace()` coordinate on every `Expression`, we wouldn't need the left side of this
            final JavaTemplate combinedAssignment = JavaTemplate
                    .builder(this::getCursor, "o = (#{any()} #{} #{any()});")
                    // ok to ignore invalid type info on left-hand side of assignment.
                    .build();

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                J.Block combined = b;
                do {
                    b = combined;
                    J.Block b2 = b;
                    AtomicInteger skip = new AtomicInteger(-1);

                    combined = b.withStatements(ListUtils.map(b.getStatements(), (i, stat) -> {
                        if (skip.get() == i) {
                            // this is the subsequent assignment op or unary which has been folded into a
                            // previous statement, so drop it
                            return null;
                        }

                        // is this a numeric variable assignment?
                        String name = numericVariableName(stat);
                        if (name != null && i < b2.getStatements().size() - 1) {
                            Statement nextStatement = b2.getStatements().get(i + 1);
                            Expression acc = numericVariableAccumulation(nextStatement, name);
                            String op = numericVariableOperator(nextStatement, name);

                            if (acc != null && op != null) {
                                skip.set(i + 1);
                                // combine this statement with the following statement into one binary expression
                                return combine(stat, op, acc);
                            }
                        }

                        return stat;
                    }));
                } while (combined != b);

                if(b != block) {
                    b = (J.Block) new UnnecessaryParenthesesVisitor<>(Checkstyle.unnecessaryParentheses())
                            .visitNonNull(b, ctx, getCursor().getParentOrThrow());
                }

                return b;
            }

            /**
             * @param s A statement
             * @return The name of a numeric variable being assigned or null if not a numeric
             * variable assignment.
             */
            @Nullable
            private String numericVariableName(Statement s) {
                if (s instanceof J.Assignment) {
                    return singleVariableName(((J.Assignment) s).getVariable());
                } else if (s instanceof J.VariableDeclarations) {
                    J.VariableDeclarations.NamedVariable firstNamedVariable = ((J.VariableDeclarations) s).getVariables().get(0);
                    return firstNamedVariable.getInitializer() == null ?
                            null :
                            singleVariableName(firstNamedVariable.getName());
                }
                return null;
            }

            @Nullable
            private Expression numericVariableAccumulation(Statement s, String name) {
                if (s instanceof J.Unary) {
                    if (name.equals(singleVariableName(((J.Unary) s).getExpression()))) {
                        return new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, 1, "1", null,
                                JavaType.Primitive.Int);
                    }
                } else if (s instanceof J.AssignmentOperation) {
                    J.AssignmentOperation assignOp = (J.AssignmentOperation) s;
                    if (name.equals(singleVariableName(assignOp.getVariable()))) {
                        return assignOp.getAssignment();
                    }
                }
                return null;
            }

            @Nullable
            private String numericVariableOperator(Statement s, String name) {
                if (s instanceof J.Unary) {
                    if (name.equals(singleVariableName(((J.Unary) s).getExpression()))) {
                        switch (((J.Unary) s).getOperator()) {
                            case PreDecrement:
                            case PostDecrement:
                                return "-";
                            case PreIncrement:
                            case PostIncrement:
                                return "+";
                        }
                    }
                } else if (s instanceof J.AssignmentOperation) {
                    J.AssignmentOperation assignOp = (J.AssignmentOperation) s;
                    if (name.equals(singleVariableName(assignOp.getVariable()))) {
                        switch (assignOp.getOperator()) {
                            case Addition:
                                return "+";
                            case BitAnd:
                                return "&";
                            case BitOr:
                                return "|";
                            case BitXor:
                                return "^";
                            case Division:
                                return "/";
                            case LeftShift:
                                return "<<";
                            case Modulo:
                                return "%";
                            case Multiplication:
                                return "*";
                            case RightShift:
                                return ">>";
                            case Subtraction:
                                return "-";
                            case UnsignedRightShift:
                                return ">>>";
                        }
                    }
                }
                return null;
            }

            @Nullable
            private String singleVariableName(Expression e) {
                JavaType.Primitive type = TypeUtils.asPrimitive(e.getType());
                return type != null && type.isNumeric() && e instanceof J.Identifier ?
                        ((J.Identifier) e).getSimpleName() :
                        null;
            }

            private Statement combine(Statement s, String op, Expression right) {
                if (s instanceof J.Assignment) {
                    J.Assignment assign = (J.Assignment) s;
                    J.Assignment after = s.withTemplate(combinedAssignment, s.getCoordinates().replace(),
                            assign.getAssignment(), op, right);
                    return assign.withAssignment(after.getAssignment());
                } else if (s instanceof J.VariableDeclarations) {
                    J.VariableDeclarations variables = (J.VariableDeclarations) s;
                    J.Assignment after = s.withTemplate(combinedAssignment, s.getCoordinates().replace(),
                            variables.getVariables().get(0).getInitializer(), op, right);
                    return variables.withVariables(ListUtils.map(variables.getVariables(), (i, namedVar) -> i == 0 ?
                            namedVar.withInitializer(after.getAssignment()) : namedVar));
                }
                throw new UnsupportedOperationException("Attempted to combine assignments into a " +
                        "single statement with type " + s.getClass().getSimpleName());
            }
        };
    }
}
