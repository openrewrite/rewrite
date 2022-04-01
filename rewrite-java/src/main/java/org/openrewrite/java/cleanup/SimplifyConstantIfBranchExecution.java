package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SimplifyConstantIfBranchExecution extends Recipe {

    @Override
    public String getDisplayName() {
        return "Simplify constant if branch execution";
    }

    @Override
    public String getDescription() {
        return "Checks for if expressions that are always `true` or `false` and simplifies them.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new SimplifyConstantIfBranchExecutionVisitor();
    }

    private static class SimplifyConstantIfBranchExecutionVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitWhileLoop(J.WhileLoop whileLoop, ExecutionContext executionContext) {
            return whileLoop;
        }

        @Override
        public J visitBlock(J.Block block, ExecutionContext executionContext) {
            J.Block bl = (J.Block) super.visitBlock(block, executionContext);
            List<Statement> addStatements = getCursor().pollMessage("statements");
            J.If removeIf = getCursor().pollMessage("remove-if");
            if (removeIf != null) {
                bl = maybeAutoFormat(bl, bl.withStatements(ListUtils.flatMap(bl.getStatements(), stmt -> {
                    if (stmt == removeIf) {
                        return addStatements;
                    }
                    return stmt;
                })), executionContext, getCursor().getParent());
            }
            return bl;
        }

        @Override
        public J visitIf(J.If if_, ExecutionContext context) {
            J.If if__ = (J.If) super.visitIf(if_, context);
            AtomicReference<Boolean> b = new AtomicReference<>(null);
            new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, ExecutionContext executionContext) {
                    J.ControlParentheses cp = super.visitControlParentheses(controlParens, executionContext);
                    J.ControlParentheses cp2 = (J.ControlParentheses) new SimplifyBooleanExpressionVisitor<ExecutionContext>().visitNonNull(cp, executionContext);
                    if (isLiteralTrue((Expression) cp2.getTree())) {
                        b.getAndSet(true);
                    } else if (isLiteralFalse((Expression) cp2.getTree())) {
                        b.getAndSet(false);
                    }
                    // else leave it as null
                    return cp;
                }
            }.visit(if__, context);
            // The compile-time constant value of the if condition control parentheses.
            Boolean compileTimeConstantBoolean = b.get();
            // The simplification process did not result in resolving to a single 'true' or 'false' value
            if (compileTimeConstantBoolean == null) {
                return if__; // Return the original `if` (unmodified)
            }
            // True branch
            if (compileTimeConstantBoolean) {
                Statement s = if__.getThenPart();
                if (s instanceof J.Block) {
                    getCursor().dropParentUntil(J.Block.class::isInstance).putMessage("statements", ((J.Block) s).getStatements());
                    getCursor().dropParentUntil(J.Block.class::isInstance).putMessage("remove-if", if__);
                    return if__; // Return the original `if` (unmodified) since this will need to be replaced
                } else {
                    return maybeAutoFormat(if__, s, context, getCursor().getParent());
                }
            } else { // False branch
                if (if__.getElsePart() != null) {
                    // The `else` part needs to be kept
                    Statement s = if__.getElsePart().getBody();
                    if (s instanceof J.Block) {
                        getCursor().dropParentUntil(J.Block.class::isInstance).putMessage("statements", ((J.Block) s).getStatements());
                        getCursor().dropParentUntil(J.Block.class::isInstance).putMessage("remove-if", if__);
                        return if__; // Return the original `if` (unmodified) since this will need to be replaced
                    } else {
                        return maybeAutoFormat(if__, s, context, getCursor().getParent());
                    }
                }
                // The if statement can be completely removed
                return null;
            }
        }

        private static boolean isLiteralTrue(@Nullable Expression expression) {
            return expression instanceof J.Literal && ((J.Literal) expression).getValue() == Boolean.valueOf(true);
        }

        private static boolean isLiteralFalse(@Nullable Expression expression) {
            return expression instanceof J.Literal && ((J.Literal) expression).getValue() == Boolean.valueOf(false);
        }
    }
}
