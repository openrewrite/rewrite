package org.openrewrite.java.internal;

import org.openrewrite.Cursor;
import org.openrewrite.TreePrinter;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

class JavaTemplatePrinter extends JavaPrinter<Cursor> {
    private final JavaCoordinates<?> coordinates;
    private final Set<String> imports;

    JavaTemplatePrinter(JavaCoordinates<?> coordinates, Set<String> imports) {
        super(TreePrinter.identity());
        this.coordinates = coordinates;
        this.imports = imports;
        setCursoringOn();
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, Cursor insertionScope) {
        for (String impoort : imports) {
            doAfterVisit(new AddImport<>(impoort, null, false));
        }
        return super.visitCompilationUnit(cu, insertionScope);
    }

    @Override
    public J visitBlock(J.Block block, Cursor insertionScope) {
        Cursor parent = getCursor().dropParentUntil(J.class::isInstance);
        if (coordinates.getTree().getId().equals(block.getId()) ||
                (!insertionScope.isScopeInPath(block) && !(parent.getValue() instanceof J.ClassDecl))) {
            return block.withStatements(emptyList());
        }

        J.Block b;
        if (!(parent.getValue() instanceof J.ClassDecl)) {
            b = visitAndCast(block, insertionScope, this::visitEach);
            b = visitAndCast(b, insertionScope, this::visitStatement);

            if (b.getStatements().stream().anyMatch(insertionScope::isScopeInPath)) {
                // If a statement in the block is in insertion scope, then this will render each statement
                // up to the statement that is in insertion scope.
                List<Statement> statementsInScope = new ArrayList<>();
                for (Statement statement : b.getStatements()) {
                    statementsInScope.add((Statement) visit(statement, insertionScope));
                    if (insertionScope.isScopeInPath(statement)) {
                        break;
                    }
                }
                b = b.withStatements(statementsInScope);
            }
        } else {
            b = (J.Block) super.visitBlock(block, insertionScope);
        }

        return b;
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(classDecl)) {
            return super.visitClassDecl(classDecl, insertionScope);
        }
        J c = super.visitClassDecl(classDecl, insertionScope);
        return c;
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(method)) {
            return method.withAnnotations(emptyList()).withBody(null);
        }
        J.MethodDecl m = (J.MethodDecl) super.visitMethod(method, insertionScope);
        return m;
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(variable)) {
            // Variables in the original AST only need to be declared, nulls out the initializers.
            return variable.withInitializer(null);
        }
        return (J.VariableDecls.NamedVar) super.visitVariable(variable, insertionScope);
    }
}
