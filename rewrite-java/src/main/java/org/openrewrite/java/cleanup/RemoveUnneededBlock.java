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
        public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
            J.Block bl = super.visitBlock(block, executionContext);
            bl = maybeAutoFormat(bl, block.withStatements(ListUtils.flatMap(bl.getStatements(), stmt -> {
                if (stmt instanceof J.Block) {
                    return ((J.Block) stmt).getStatements();
                } else {
                    return stmt;
                }
            })), executionContext, getCursor().getParent());
            return bl;
        }
    }
}
