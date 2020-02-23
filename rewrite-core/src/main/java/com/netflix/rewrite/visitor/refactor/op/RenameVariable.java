package com.netflix.rewrite.visitor.refactor.op;

import com.netflix.rewrite.tree.Cursor;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.visitor.RetrieveCursorVisitor;
import com.netflix.rewrite.visitor.refactor.AstTransform;
import com.netflix.rewrite.visitor.refactor.ScopedRefactorVisitor;

import java.util.List;

public class RenameVariable extends ScopedRefactorVisitor {
    private final String toName;

    private Cursor scopeCursor;
    private String scopeVariableName;

    public RenameVariable(Tr.VariableDecls.NamedVar scope, String toName) {
        super(scope.getId());
        this.toName = toName;
    }

    @Override
    public List<AstTransform> visitCompilationUnit(Tr.CompilationUnit cu) {
        scopeCursor = new RetrieveCursorVisitor(scope).visit(cu);
        scopeVariableName = ((Tr.VariableDecls.NamedVar) scopeCursor.getTree()).getSimpleName();
        return super.visitCompilationUnit(cu);
    }

    @Override
    public List<AstTransform> visitIdentifier(Tr.Ident ident) {
        return maybeTransform(ident,
                ident.getSimpleName().equals(scopeVariableName) &&
                        scopeCursor.isInSameNameScope(getCursor()),
                super::visitIdentifier,
                i -> i.withName(toName));
    }
}
