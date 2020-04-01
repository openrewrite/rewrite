package org.openrewrite.java.refactor;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;

public class UnwrapParentheses extends ScopedJavaRefactorVisitor {
    public UnwrapParentheses(J.Parentheses<?> scope) {
        super(scope.getId());
    }

    @Override
    public String getName() {
        return "core.UnwrapParentheses";
    }

    @Override
    public boolean isCursored() {
        return true;
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens) {
        return isScope(parens) && isUnwrappable(getCursor()) ?
                parens.getTree().withFormatting(parens.getFormatting()) :
                super.visitParentheses(parens);
    }

    public static boolean isUnwrappable(Cursor parensScope) {
        if(!(parensScope.getTree() instanceof J.Parentheses)) {
            return false;
        }
        J parent = parensScope.getParentOrThrow().getTree();
        return !(parent instanceof J.DoWhileLoop.While ||
                parent instanceof J.If ||
                parent instanceof J.Switch ||
                parent instanceof J.Synchronized ||
                parent instanceof J.Try.Catch ||
                parent instanceof J.TypeCast ||
                parent instanceof J.WhileLoop);
    }
}
