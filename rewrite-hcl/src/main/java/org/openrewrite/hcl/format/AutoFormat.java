package org.openrewrite.hcl.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.hcl.HclVisitor;

public class AutoFormat extends Recipe {
    @Override
    public String getDisplayName() {
        return "Format HCL code";
    }

    @Override
    public String getDescription() {
        return "Format Java code using a standard comprehensive set of Java formatting recipes.";
    }

    @Override
    protected HclVisitor<ExecutionContext> getVisitor() {
        return new AutoFormatVisitor<>(null);
    }
}
