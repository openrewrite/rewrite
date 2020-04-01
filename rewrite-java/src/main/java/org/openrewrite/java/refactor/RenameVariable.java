package org.openrewrite.java.refactor;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaRetrieveCursorVisitor;
import org.openrewrite.java.tree.J;

public class RenameVariable extends ScopedJavaRefactorVisitor {
    private final String toName;

    private Cursor scopeCursor;
    private String scopeVariableName;

    public RenameVariable(J.VariableDecls.NamedVar scope, String toName) {
        super(scope.getId());
        this.toName = toName;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu) {
        scopeCursor = new JavaRetrieveCursorVisitor(getScope()).visit(cu);
        scopeVariableName = ((J.VariableDecls.NamedVar) scopeCursor.getTree()).getSimpleName();

        return super.visitCompilationUnit(cu);
    }

    @Override
    public J visitIdentifier(J.Ident ident) {
        if (ident.getSimpleName().equals(scopeVariableName) &&
                isInSameNameScope(scopeCursor, getCursor()) &&
                !(getCursor().getParentOrThrow().getTree() instanceof J.FieldAccess)) {
            return ident.withName(toName);
        }

        return super.visitIdentifier(ident);
    }
}
