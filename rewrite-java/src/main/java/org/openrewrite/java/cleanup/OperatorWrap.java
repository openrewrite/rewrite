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

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.OperatorWrapStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.TypeTree;

import static java.util.Objects.requireNonNull;

public class OperatorWrap extends Recipe {
    @Override
    public String getDisplayName() {
        return "Operator wrapping";
    }

    @Override
    public String getDescription() {
        return "Fixes line wrapping policies on operators.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new OperatorWrapVisitor();
    }

    private static class OperatorWrapVisitor extends JavaIsoVisitor<ExecutionContext> {
        OperatorWrapStyle operatorWrapStyle;

        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                SourceFile cu = (SourceFile) requireNonNull(tree);
                operatorWrapStyle = cu.getStyle(OperatorWrapStyle.class) == null ? Checkstyle.operatorWrapStyle() : cu.getStyle(OperatorWrapStyle.class);
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
            J.Binary b = super.visitBinary(binary, ctx);
            J.Binary.Type op = b.getOperator();
            if ((Boolean.TRUE.equals(operatorWrapStyle.getDiv()) && op == J.Binary.Type.Division) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getStar()) && op == J.Binary.Type.Multiplication) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getPlus()) && op == J.Binary.Type.Addition) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getMinus()) && op == J.Binary.Type.Subtraction) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getMod()) && op == J.Binary.Type.Modulo) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getSr()) && op == J.Binary.Type.RightShift) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getSl()) && op == J.Binary.Type.LeftShift) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getBsr()) && op == J.Binary.Type.UnsignedRightShift) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getEqual()) && op == J.Binary.Type.Equal) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getNotEqual()) && op == J.Binary.Type.NotEqual) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getGt()) && op == J.Binary.Type.GreaterThan) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getGe()) && op == J.Binary.Type.GreaterThanOrEqual) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getLt()) && op == J.Binary.Type.LessThan) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getLe()) && op == J.Binary.Type.LessThanOrEqual) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getBand()) && op == J.Binary.Type.BitAnd) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getBxor()) && op == J.Binary.Type.BitXor) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getBor()) && op == J.Binary.Type.BitOr) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getLand()) && op == J.Binary.Type.And) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getLor()) && op == J.Binary.Type.Or)) {
                if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                    if (b.getRight().getPrefix().getWhitespace().contains("\n")) {
                        b = b.getPadding().withOperator(
                                b.getPadding().getOperator().withBefore(
                                        b.getRight().getPrefix()
                                )
                        );
                        b = b.withRight(
                                b.getRight().withPrefix(
                                        b.getRight().getPrefix().withWhitespace(" ")
                                )
                        );
                    }
                } else if (b.getPadding().getOperator().getBefore().getWhitespace().contains("\n")) {
                    b = b.withRight(
                            b.getRight().withPrefix(
                                    b.getPadding().getOperator().getBefore()
                            )
                    );
                    b = b.getPadding().withOperator(
                            b.getPadding().getOperator().withBefore(
                                    b.getRight().getPrefix().withWhitespace(" ")
                            )
                    );
                }
            }
            return b;
        }

        @Override
        public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, ExecutionContext ctx) {
            J.TypeParameter tp = super.visitTypeParameter(typeParam, ctx);
            if (Boolean.TRUE.equals(operatorWrapStyle.getTypeExtensionAnd()) && tp.getPadding().getBounds() != null) {
                int typeBoundsSize = tp.getPadding().getBounds().getPadding().getElements().size();
                tp = tp.getPadding().withBounds(
                        tp.getPadding().getBounds().getPadding().withElements(
                                ListUtils.map(tp.getPadding().getBounds().getPadding().getElements(),
                                        (index, elemContainer) -> {
                                            if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                                                if (index != typeBoundsSize - 1 && typeParam.getPadding().getBounds() != null) {
                                                    JRightPadded<TypeTree> next = typeParam.getPadding().getBounds().getPadding().getElements().get(index + 1);
                                                    if (next.getElement().getPrefix().getWhitespace().contains("\n")) {
                                                        elemContainer = elemContainer.withAfter(
                                                                next.getElement().getPrefix()
                                                        );
                                                    }
                                                } else {
                                                    if (elemContainer.getElement().getPrefix().getWhitespace().contains("\n")) {
                                                        elemContainer = elemContainer.withElement(
                                                                elemContainer.getElement().withPrefix(
                                                                        elemContainer.getElement().getPrefix().withWhitespace(" ")
                                                                )
                                                        );
                                                    }
                                                }
                                            } else {
                                                if (index != typeBoundsSize - 1) {
                                                    if (elemContainer.getAfter().getWhitespace().contains("\n")) {
                                                        elemContainer = elemContainer.withAfter(
                                                                elemContainer.getAfter().withWhitespace(" ")
                                                        );
                                                    }
                                                } else if (typeBoundsSize > 1 && typeParam.getPadding().getBounds() != null) {
                                                    JRightPadded<TypeTree> previous = typeParam.getPadding().getBounds().getPadding().getElements().get(index - 1);
                                                    if (previous.getAfter().getWhitespace().contains("\n")) {
                                                        elemContainer = elemContainer.withElement(
                                                                elemContainer.getElement().withPrefix(
                                                                        previous.getAfter()
                                                                )
                                                        );
                                                    }
                                                }
                                            }
                                            return elemContainer;
                                        }
                                )
                        )
                );
            }
            return tp;
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext ctx) {
            J.InstanceOf i = super.visitInstanceOf(instanceOf, ctx);
            if (Boolean.TRUE.equals(operatorWrapStyle.getLiteralInstanceof())) {
                if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                    if (i.getClazz().getPrefix().getWhitespace().contains("\n")) {
                        i = i.getPadding().withExpr(
                                i.getPadding().getExpr().withAfter(
                                        i.getClazz().getPrefix()
                                )
                        );
                        i = i.withClazz(
                                i.getClazz().withPrefix(
                                        i.getClazz().getPrefix().withWhitespace(" ")
                                )
                        );
                    }
                } else if (i.getPadding().getExpr().getAfter().getWhitespace().contains("\n")) {
                    i = i.withClazz(
                            i.getClazz().withPrefix(
                                    i.getPadding().getExpr().getAfter()
                            )
                    );
                    i = i.getPadding().withExpr(
                            i.getPadding().getExpr().withAfter(
                                    i.getPadding().getExpr().getAfter().withWhitespace(" ")
                            )
                    );
                }
            }
            return i;
        }

        @Override
        public J.Ternary visitTernary(J.Ternary ternary, ExecutionContext ctx) {
            J.Ternary t = super.visitTernary(ternary, ctx);
            if (Boolean.TRUE.equals(operatorWrapStyle.getQuestion())) {
                if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                    if (t.getTruePart().getPrefix().getWhitespace().contains("\n")) {
                        t = t.getPadding().withTruePart(
                                t.getPadding().getTruePart().withBefore(
                                        t.getPadding().getTruePart().getElement().getPrefix()
                                )
                        );
                        t = t.getPadding().withTruePart(
                                t.getPadding().getTruePart().withElement(
                                        t.getPadding().getTruePart().getElement().withPrefix(
                                                t.getPadding().getTruePart().getElement().getPrefix().withWhitespace(" ")
                                        )
                                )
                        );
                    }
                } else if (t.getPadding().getTruePart().getBefore().getWhitespace().contains("\n")) {
                    t = t.getPadding().withTruePart(
                            t.getPadding().getTruePart().withElement(
                                    t.getPadding().getTruePart().getElement().withPrefix(
                                            t.getPadding().getTruePart().getBefore()
                                    )
                            )
                    );
                    t = t.getPadding().withTruePart(
                            t.getPadding().getTruePart().withBefore(
                                    t.getPadding().getTruePart().getElement().getPrefix().withWhitespace(" ")
                            )
                    );
                }
            }
            if (Boolean.TRUE.equals(operatorWrapStyle.getColon())) {
                if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                    if (t.getPadding().getFalsePart().getElement().getPrefix().getWhitespace().contains("\n")) {
                        t = t.getPadding().withFalsePart(
                                t.getPadding().getFalsePart().withBefore(
                                        t.getPadding().getFalsePart().getElement().getPrefix()
                                )
                        );
                        t = t.getPadding().withFalsePart(
                                t.getPadding().getFalsePart().withElement(
                                        t.getPadding().getFalsePart().getElement().withPrefix(
                                                t.getPadding().getFalsePart().getElement().getPrefix().withWhitespace(" ")
                                        )
                                )
                        );
                    }
                } else if (t.getPadding().getFalsePart().getBefore().getWhitespace().contains("\n")) {
                    t = t.getPadding().withFalsePart(
                            t.getPadding().getFalsePart().withElement(
                                    t.getPadding().getFalsePart().getElement().withPrefix(
                                            t.getPadding().getFalsePart().getBefore()
                                    )
                            )
                    );
                    t = t.getPadding().withFalsePart(
                            t.getPadding().getFalsePart().withBefore(
                                    t.getPadding().getFalsePart().getElement().getPrefix().withWhitespace(" ")
                            )
                    );
                }
            }
            return t;
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference m = super.visitMemberReference(memberRef, ctx);
            if (Boolean.TRUE.equals(operatorWrapStyle.getMethodRef())) {
                if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                    if (m.getPadding().getReference().getBefore().getWhitespace().contains("\n")) {
                        m = m.getPadding().withContaining(
                                m.getPadding().getContaining().withAfter(
                                        m.getPadding().getReference().getBefore()
                                )
                        );
                        m = m.getPadding().withReference(
                                m.getPadding().getReference().withBefore(
                                        m.getPadding().getReference().getBefore().withWhitespace("")
                                )
                        );
                    }
                } else if (m.getPadding().getContaining().getAfter().getWhitespace().contains("\n")) {
                    m = m.getPadding().withReference(
                            m.getPadding().getReference().withBefore(
                                    m.getPadding().getContaining().getAfter()
                            )
                    );
                    m = m.getPadding().withContaining(
                            m.getPadding().getContaining().withAfter(
                                    m.getPadding().getReference().getBefore().withWhitespace("")
                            )
                    );
                }
            }
            return m;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            J.Assignment a = super.visitAssignment(assignment, ctx);
            if (Boolean.TRUE.equals(operatorWrapStyle.getAssign())) {
                if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                    if (a.getPadding().getAssignment().getElement().getPrefix().getWhitespace().contains("\n")) {
                        a = a.getPadding().withAssignment(
                                a.getPadding().getAssignment().withBefore(
                                        a.getPadding().getAssignment().getElement().getPrefix()
                                )
                        );
                        a = a.getPadding().withAssignment(
                                a.getPadding().getAssignment().withElement(
                                        a.getPadding().getAssignment().getElement().withPrefix(
                                                a.getPadding().getAssignment().getElement().getPrefix().withWhitespace(" ")
                                        )
                                )
                        );
                    }
                } else if (a.getPadding().getAssignment().getBefore().getWhitespace().contains("\n")) {
                    a = a.getPadding().withAssignment(
                            a.getPadding().getAssignment().withElement(
                                    a.getPadding().getAssignment().getElement().withPrefix(
                                            a.getPadding().getAssignment().getBefore()
                                    )
                            )
                    );
                    a = a.getPadding().withAssignment(
                            a.getPadding().getAssignment().withBefore(
                                    a.getPadding().getAssignment().getBefore().withWhitespace(" ")
                            )
                    );
                }
            }
            return a;
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, ExecutionContext ctx) {
            J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, ctx);
            J.AssignmentOperation.Type op = a.getOperator();
            if ((Boolean.TRUE.equals(operatorWrapStyle.getPlusAssign()) && op == J.AssignmentOperation.Type.Addition) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getMinusAssign()) && op == J.AssignmentOperation.Type.Subtraction) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getStarAssign()) && op == J.AssignmentOperation.Type.Multiplication) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getDivAssign()) && op == J.AssignmentOperation.Type.Division) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getModAssign()) && op == J.AssignmentOperation.Type.Modulo) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getSrAssign()) && op == J.AssignmentOperation.Type.RightShift) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getSlAssign()) && op == J.AssignmentOperation.Type.LeftShift) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getBsrAssign()) && op == J.AssignmentOperation.Type.UnsignedRightShift) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getBandAssign()) && op == J.AssignmentOperation.Type.BitAnd) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getBxorAssign()) && op == J.AssignmentOperation.Type.BitXor) ||
                (Boolean.TRUE.equals(operatorWrapStyle.getBorAssign()) && op == J.AssignmentOperation.Type.BitOr)) {
                if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                    if (a.getAssignment().getPrefix().getWhitespace().contains("\n")) {
                        a = a.getPadding().withOperator(
                                a.getPadding().getOperator().withBefore(
                                        a.getAssignment().getPrefix()
                                )
                        );
                        a = a.withAssignment(
                                a.getAssignment().withPrefix(
                                        a.getAssignment().getPrefix().withWhitespace(" ")
                                )
                        );
                    }
                } else if (a.getPadding().getOperator().getBefore().getWhitespace().contains("\n")) {
                    a = a.withAssignment(
                            a.getAssignment().withPrefix(
                                    a.getPadding().getOperator().getBefore()
                            )
                    );
                    a = a.getPadding().withOperator(
                            a.getPadding().getOperator().withBefore(
                                    a.getAssignment().getPrefix().withWhitespace(" ")
                            )
                    );
                }
            }
            return a;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
            if (Boolean.TRUE.equals(operatorWrapStyle.getAssign()) && v.getPadding().getInitializer() != null) {
                if (OperatorWrapStyle.WrapOption.NL.equals(operatorWrapStyle.getWrapOption())) {
                    if (v.getPadding().getInitializer().getElement().getPrefix().getWhitespace().contains("\n")) {
                        v = v.getPadding().withInitializer(
                                v.getPadding().getInitializer().withBefore(
                                        v.getPadding().getInitializer().getElement().getPrefix()
                                )
                        );
                        if (v.getPadding().getInitializer() != null && v.getPadding().getInitializer().getElement() != null) {
                            v = v.getPadding().withInitializer(
                                    v.getPadding().getInitializer().withElement(
                                            v.getPadding().getInitializer().getElement().withPrefix(
                                                    v.getPadding().getInitializer().getElement().getPrefix().withWhitespace(" ")
                                            )
                                    )
                            );
                        }
                    }
                } else if (v.getPadding().getInitializer().getBefore().getWhitespace().contains("\n")) {
                    v = v.getPadding().withInitializer(
                            v.getPadding().getInitializer().withElement(
                                    v.getPadding().getInitializer().getElement().withPrefix(
                                            v.getPadding().getInitializer().getBefore()
                                    )
                            )
                    );
                    if (v.getPadding().getInitializer() != null && v.getPadding().getInitializer().getBefore() != null) {
                        v = v.getPadding().withInitializer(
                                v.getPadding().getInitializer().withBefore(
                                        v.getPadding().getInitializer().getElement().getPrefix().withWhitespace(" ")
                                )
                        );
                    }
                }
            }
            return v;
        }

    }

}
