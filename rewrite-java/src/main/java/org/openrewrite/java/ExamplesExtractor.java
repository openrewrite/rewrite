package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;

public class ExamplesExtractor extends JavaIsoVisitor<ExecutionContext> {

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
        return super.visitCompilationUnit(cu, executionContext);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {


        return super.visitMethodInvocation(method, executionContext);
    }
}
