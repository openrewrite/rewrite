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
package org.openrewrite.java.cleanup;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class RemoveRedundantTypeCast extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove redundant casts";
    }

    @Override
    public String getDescription() {
        return "Removes unnecessary type casts. Does not currently check casts in lambdas, class constructors, and method invocations.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1905");
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitTypeCast(J.TypeCast typeCast, ExecutionContext executionContext) {
                Cursor parent = getCursor().dropParentUntil(is -> is instanceof J.NewClass ||
                        is instanceof J.Lambda ||
                        is instanceof J.MethodInvocation ||
                        is instanceof J.ClassDeclaration);

                // Not currently supported, this will be more accurate with dataflow analysis.
                if (parent.getValue() instanceof J.NewClass ||
                        parent.getValue() instanceof J.Lambda ||
                        parent.getValue() instanceof J.MethodInvocation) {
                    return typeCast;
                }

                JavaType cast = typeCast.getType();
                // Not currently supported, this will be more accurate with dataflow analysis.
                if (cast instanceof JavaType.GenericTypeVariable) {
                    return typeCast;
                }

                JavaType expressionType = typeCast.getExpression().getType();

                // Reduce the expressionType from array to its type.
                if (typeCast.getClazz().getTree() instanceof J.ArrayType) {
                    while (expressionType instanceof JavaType.Array) {
                        expressionType = ((JavaType.Array) expressionType).getElemType();
                    }
                }

                if (cast != null && expressionType != null &&
                        (TypeUtils.isOfType(cast, expressionType) || TypeUtils.isAssignableTo(cast, expressionType))) {
                    // Return the expression without the type cast.
                    return typeCast.getExpression();
                }

                return super.visitTypeCast(typeCast, executionContext);
            }
        };
    }
}
