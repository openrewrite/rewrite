package org.openrewrite.java.refactor;

import org.openrewrite.java.tree.J;

public class ChangeMethodName extends ScopedJavaRefactorVisitor {
    private final String name;

    public ChangeMethodName(J.MethodInvocation scope, String name) {
        super(scope.getId());
        this.name = name;
    }

    @Override
    public String getName() {
        return "core.ChangeMethodName{to=" + name + "}";
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        return isScope() ? method.withName(method.getName().withName(name)) :
                super.visitMethodInvocation(method);
    }
}
