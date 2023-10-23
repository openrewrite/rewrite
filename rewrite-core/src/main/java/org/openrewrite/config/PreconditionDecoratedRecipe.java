package org.openrewrite.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

@EqualsAndHashCode(callSuper = true)
@Value
public class PreconditionDecoratedRecipe extends Recipe {

    TreeVisitor<?, ExecutionContext> precondition;
    Recipe delegate;

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(precondition, delegate.getVisitor());
    }
}
