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

import org.openrewrite.*;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Incubating(since = "7.23.0")
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitTypeCast(J.TypeCast typeCast, ExecutionContext executionContext) {
                Cursor parent = getCursor().dropParentUntil(is -> is instanceof J.VariableDeclarations ||
                        is instanceof J.NewClass ||
                        is instanceof J.Lambda ||
                        is instanceof J.MethodInvocation ||
                        is instanceof J.MethodDeclaration ||
                        is instanceof J.ClassDeclaration);

                // Not currently supported, this will be more accurate with dataflow analysis.
                if (!(parent.getValue() instanceof J.VariableDeclarations)) {
                    return typeCast;
                }

                TypeTree typeTree = typeCast.getClazz().getTree();
                JavaType expressionType = typeCast.getExpression().getType();

                JavaType namedVariableType = ((J.VariableDeclarations) parent.getValue()).getVariables().get(0).getType();
                if (!(namedVariableType instanceof JavaType.Array) && TypeUtils.isOfClassType(namedVariableType, "java.lang.Object") ||
                        (!(typeTree instanceof J.ParameterizedType) && (TypeUtils.isOfType(namedVariableType, expressionType) || TypeUtils.isAssignableTo(namedVariableType, expressionType)))) {
                    return typeCast.getExpression();
                }

                return super.visitTypeCast(typeCast, executionContext);
            }
        };
    }
}
