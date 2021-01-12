package org.openrewrite.java.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.tree.J;

public class AutoFormat extends Recipe {
    public AutoFormat() {
        this.processor = AutoFormatFromCompilationUnit::new;
    }

    private static class AutoFormatFromCompilationUnit extends JavaIsoProcessor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext context) {
            doAfterVisit(new AutoFormatProcessor<>(cu.getStyles()));
            return cu;
        }
    }
}
