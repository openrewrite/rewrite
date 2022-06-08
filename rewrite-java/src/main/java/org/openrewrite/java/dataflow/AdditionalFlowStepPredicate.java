package org.openrewrite.java.dataflow;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.Expression;

@Incubating(since = "7.25.0")
@FunctionalInterface
interface AdditionalFlowStepPredicate {
    boolean isAdditionalFlowStep(
            Expression startExpression,
            Cursor startCursor,
            Expression endExpression,
            Cursor endCursor
    );
}
