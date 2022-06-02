package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

import java.util.List;

/**
 * A call expression. Specifically, an instance of {@link J.MethodInvocation} or {@link J.NewClass}.
 */
public interface Call extends Expression {
    @Nullable
    List<Expression> getArguments();
}
