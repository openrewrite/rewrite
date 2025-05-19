package org.openrewrite.java;


import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
@EqualsAndHashCode(callSuper = false)
@Value
public class RelocateSuperCall extends Recipe {

    @Override
    public String getDisplayName() {
        return "Move `super()` after conditionals (Java 25+)";
    }

    @Override
    public String getDescription() {
        return "Relocates `super()` calls to occur after conditionals inside constructors, enabled by JEP 513 in Java 25+.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(25),
                new RelocateSuperCallVisitor());
    }

    private static class RelocateSuperCallVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (!method.isConstructor() || method.getBody() == null) {
                return method;
            }

            List<Statement> statements = method.getBody().getStatements();
            if (statements.size() < 2) {
                return method;
            }

            Statement first = statements.get(0);
            if (!(first instanceof J.MethodInvocation)) {
                return method;
            }
            J.MethodInvocation methodInvocation = (J.MethodInvocation) first;
            if (!"super".equals(methodInvocation.getSimpleName())) {
                return method;
            }

            // Move super() to the end
            List<Statement> updated = new java.util.ArrayList<>(statements);
            updated.remove(0);
            updated.add(methodInvocation);

            return method.withBody(method.getBody().withStatements(updated));
        }
    }
}
