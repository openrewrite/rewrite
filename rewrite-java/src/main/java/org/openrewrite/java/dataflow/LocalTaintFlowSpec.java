package org.openrewrite.java.dataflow;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public abstract class LocalTaintFlowSpec<Source extends Expression, Sink extends J> extends LocalFlowSpec<Source, Sink> {

    @Override
    public final boolean isAdditionalFlowStep(
            Expression startExpression,
            Cursor startCursor,
            Expression endExpression,
            Cursor endCursor
    ) {
        return ExternalFlowModels.instance().isAdditionalTaintStep(
                startExpression,
                startCursor,
                endExpression,
                endCursor
        ) || isAdditionalTaintStep(
                startExpression,
                startCursor,
                endExpression,
                endCursor
        );
    }

    public final boolean isAdditionalTaintStep(
            Expression startExpression,
            Cursor startCursor,
            Expression endExpression,
            Cursor endCursor
    ) {
        return false;
    }
}
