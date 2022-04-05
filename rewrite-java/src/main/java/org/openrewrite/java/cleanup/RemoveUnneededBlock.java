package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

public class RemoveUnneededBlock extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unneeded block";
    }

    @Override
    public String getDescription() {
        return "Flatten blocks into inline statements when possible";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new RemoveUnneededBlockStatementVisitor();
    }

    static class RemoveUnneededBlockStatementVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
            // Determine the first enclosing NewClass or ClassDeclaration statement
            J.NewClass newClass = getCursor().firstEnclosing(J.NewClass.class);
            J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
            // Determine the direct parent
            J directParent = (J) getCursor().dropParentUntil(J.class::isInstance).getValue();

            J.Block bl = super.visitBlock(block, executionContext);

            if (classDeclaration == directParent || newClass == directParent) {
                // If the direct parent is an initializer block or a static block, skip it
                return bl;
            }

            // Else perform the flattening on this block.
            return maybeAutoFormat(bl, block.withStatements(ListUtils.flatMap(bl.getStatements(), stmt -> {
                if (!(stmt instanceof J.Block)) {
                    return stmt;
                }
                J.Block nested = (J.Block) stmt;
                return nested.getStatements();
            })), executionContext, getCursor().getParentOrThrow());
        }
    }
}
