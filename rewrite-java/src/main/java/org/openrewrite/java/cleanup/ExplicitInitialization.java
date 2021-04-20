package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;

public class ExplicitInitialization extends Recipe {

    @Override
    public String getDisplayName() {
        return "Explicit initialization";
    }

    @Override
    public String getDescription() {
        return "Checks if any class or object member is explicitly initialized to default for its type value (`null` for object references, zero for numeric types and `char` and `false` for `boolean`.";
    }

    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ExplicitInitializationFromCompilationUnitStyle();
    }

    private static class ExplicitInitializationFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
            ExplicitInitializationStyle style = cu.getStyle(ExplicitInitializationStyle.class);
            if (style == null) {
                style = IntelliJ.explicitInitialization();
            }
            doAfterVisit(new ExplicitInitializationVisitor<>(style));
            return cu;
        }
    }
}
