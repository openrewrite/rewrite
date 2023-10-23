package org.openrewrite.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class PreconditionDecoratedScanningRecipe<T>  extends ScanningRecipe<T> {

    TreeVisitor<?, ExecutionContext> precondition;
    ScanningRecipe<T> delegate;

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
    public T getInitialValue(ExecutionContext ctx) {
        return delegate.getInitialValue(ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(T acc) {
        return delegate.getScanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(T acc) {
        return Preconditions.check(precondition, delegate.getVisitor(acc));
    }
}
