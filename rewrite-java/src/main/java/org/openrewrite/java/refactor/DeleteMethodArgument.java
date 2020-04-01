package org.openrewrite.java.refactor;

import org.openrewrite.Formatting;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class DeleteMethodArgument extends ScopedJavaRefactorVisitor {
    private final int pos;

    public DeleteMethodArgument(J.MethodInvocation scope, int pos) {
        super(scope.getId());
        this.pos = pos;
    }

    @Override
    public String getName() {
        return "core.DeleteMethodArgument";
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        List<Expression> originalArgs = method.getArgs().getArgs();
        if (isScope() && originalArgs.stream()
                .filter(a -> !(a instanceof J.Empty))
                .count() >= pos + 1) {
            List<Expression> args = new ArrayList<>(method.getArgs().getArgs());

            args.remove(pos);
            if (args.isEmpty()) {
                args = singletonList(new J.Empty(randomId(), Formatting.EMPTY));
            }

            return method.withArgs(method.getArgs().withArgs(args));
        }

        return super.visitMethodInvocation(method);
    }
}
