package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;

public class EmptyBlock extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove empty blocks";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new EmptyBlockFromCompilationUnitStyle();
    }

    private static class EmptyBlockFromCompilationUnitStyle extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
            EmptyBlockStyle style = cu.getStyle(EmptyBlockStyle.class);
            if (style == null) {
                style = IntelliJ.emptyBlock();
            }
            doAfterVisit(new EmptyBlockVisitor<>(style));
            return cu;
        }
    }
}
