package org.openrewrite.java.refactor;

import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.search.FindReferencedTypes;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * Deletes standalone statements. Does not include deletion of control statements present in for loops.
 */
public class DeleteStatement extends ScopedJavaRefactorVisitor {
    public DeleteStatement(Statement scope) {
        super(scope.getId());
    }

    @Override
    public String getName() {
        return "core.DeleteStatement";
    }

    @Override
    public J visitIf(J.If iff) {
        J.If i = refactor(iff, super::visitIf);

        if (isScope(i.getThenPart())) {
            i = i.withThenPart(emptyBlock());
        } else if (i.getElsePart() != null && isScope(i.getElsePart())) {
            i = i.withElsePart(i.getElsePart().withStatement(emptyBlock()));
        }

        return i;
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop) {
        return isScope(forLoop.getBody()) ? forLoop.withBody(emptyBlock()) :
                super.visitForLoop(forLoop);
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forEachLoop) {
        return isScope(forEachLoop.getBody()) ? forEachLoop.withBody(emptyBlock()) :
                super.visitForEachLoop(forEachLoop);
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop) {
        return isScope(whileLoop.getBody()) ? whileLoop.withBody(emptyBlock()) :
                super.visitWhileLoop(whileLoop);
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
        return isScope(doWhileLoop.getBody()) ? doWhileLoop.withBody(emptyBlock()) :
                super.visitDoWhileLoop(doWhileLoop);
    }

    @Override
    public J visitBlock(J.Block<J> block) {
        J.Block<J> b = refactor(block, super::visitBlock);

        if (block.getStatements().stream().anyMatch(this::isScope)) {
            b = b.withStatements(b.getStatements().stream()
                    .filter(s -> !s.getId().equals(getScope()))
                    .collect(toList()));
        }

        return b;
    }

    @Override
    public J visitTree(Tree tree) {
        if (isScope(tree)) {
            new FindReferencedTypes().visit(tree).forEach(this::maybeRemoveImport);
        }
        return super.visitTree(tree);
    }

    private J.Block<J> emptyBlock() {
        return new J.Block<>(randomId(), null, emptyList(), Formatting.EMPTY, "");
    }
}
