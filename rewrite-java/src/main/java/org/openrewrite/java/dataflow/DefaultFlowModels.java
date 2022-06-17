package org.openrewrite.java.dataflow;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.dataflow.internal.InvocationMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DefaultFlowModels {

    /**
     * Holds if the additional step from `src` to `sink` should be included in all
     * taint flow configurations.
     */
    static boolean isDefaultAdditionalTaintStep(
            Expression srcExpression,
            Cursor srcCursor,
            Expression sinkExpression,
            Cursor sinkCursor
    ) {
        return isLocalAdditionalTaintStep(
                srcExpression,
                srcCursor,
                sinkExpression,
                sinkCursor
        );
    }

    private static boolean isLocalAdditionalTaintStep(
            Expression srcExpression,
            Cursor srcCursor,
            Expression sinkExpression,
            Cursor sinkCursor
    ) {
        return AdditionalLocalTaint.isStringAddTaintStep(
                srcExpression,
                srcCursor,
                sinkExpression,
                sinkCursor
        );
    }

    private static final class AdditionalLocalTaint {

        private static boolean isStringAddTaintStep(
                Expression srcExpression,
                Cursor srcCursor,
                Expression sinkExpression,
                Cursor sinkCursor
        ) {
            if (sinkExpression instanceof J.Binary) {
                J.Binary binary = (J.Binary) sinkExpression;
                return J.Binary.Type.Addition.equals(binary.getOperator()) &&
                        (binary.getLeft() == srcExpression || binary.getRight() == srcExpression) &&
                        TypeUtils.isOfClassType(binary.getType(), "java.lang.String");
            }
            return false;
        }
    }
}
