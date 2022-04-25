package org.openrewrite.java.cleanup;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;

public class UselessCompoundVisitor<P> extends JavaVisitor<P> {
    @Nullable
    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        Expression cleanedUpAssignment = cleanupBooleanExpression(assignOp.getAssignment(), p);
        if (assignOp.getOperator() == J.AssignmentOperation.Type.BitAnd) {
            if (isLiteralTrue(cleanedUpAssignment)) {
                return null;
            } else if (isLiteralFalse(cleanedUpAssignment)) {
                return maybeAutoFormat(
                        assignOp,
                        new J.Assignment(
                                Tree.randomId(),
                                assignOp.getPrefix(),
                                assignOp.getMarkers(),
                                assignOp.getVariable(),
                                JLeftPadded.build(cleanedUpAssignment),
                                assignOp.getType()
                        ),
                        p,
                        getCursor().getParentOrThrow()
                );
            }
        } else if (assignOp.getOperator() == J.AssignmentOperation.Type.BitOr) {
            if (isLiteralFalse(cleanedUpAssignment)) {
                return null;
            } else if (isLiteralTrue(cleanedUpAssignment)) {
                return maybeAutoFormat(
                        assignOp,
                        new J.Assignment(
                                Tree.randomId(),
                                assignOp.getPrefix(),
                                assignOp.getMarkers(),
                                assignOp.getVariable(),
                                JLeftPadded.build(cleanedUpAssignment),
                                assignOp.getType()
                        ),
                        p,
                        getCursor().getParentOrThrow()
                );
            }
        }
        return super.visitAssignmentOperation(assignOp, p);
    }

    @SuppressWarnings("unchecked")
    private <E extends Expression> E cleanupBooleanExpression(
            E expression, P context
    ) {
        final E ex1 =
                (E) new UnnecessaryParenthesesVisitor<>(Checkstyle.unnecessaryParentheses())
                        .visitNonNull(expression, context, getCursor().getParentOrThrow());
        final E ex2 =
                (E) new SimplifyBooleanExpressionVisitor<>()
                        .visitNonNull(ex1, context, getCursor().getParentOrThrow());
        return ex2;
    }

    private static boolean isLiteralTrue(@Nullable Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getValue() == Boolean.valueOf(true);
    }

    private static boolean isLiteralFalse(@Nullable Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getValue() == Boolean.valueOf(false);
    }
}
