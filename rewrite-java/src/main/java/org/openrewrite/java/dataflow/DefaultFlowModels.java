/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
