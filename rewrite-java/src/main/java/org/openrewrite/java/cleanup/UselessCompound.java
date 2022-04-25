package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class UselessCompound extends Recipe {
    @Override
    public String getDisplayName() {
        return "Useless compound statement";
    }

    @Override
    public String getDescription() {
        return "Fixes or removes useless compound statements. For example, removing `b &= true`, and replacing `b &= false` with `b = false`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UselessCompoundVisitor<>();
    }
}
