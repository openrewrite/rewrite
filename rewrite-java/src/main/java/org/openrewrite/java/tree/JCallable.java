package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

import java.util.List;

/**
 * A callable expression like {@link J.MethodInvocation} or {@link J.NewClass}.
 */
public interface JCallable extends Expression {
    @Nullable
    List<Expression> getArguments();
}
