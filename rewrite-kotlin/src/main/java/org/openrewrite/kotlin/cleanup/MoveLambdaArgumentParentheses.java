/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.KotlinVisitor;

import java.time.Duration;
import java.util.List;

public class MoveLambdaArgumentParentheses extends Recipe {

    @Override
    public String getDisplayName() {
        return "Move lambda argument outside of method invocation parentheses";
    }

    @Override
    public String getDescription() {
        return "Move lambda argument outside of method invocation parentheses when they are the only argument. " +
                "For example, converts `1.let({ it + 1 })` to `1.let { it + 1 }`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                // Check if this is a method call has a single lambda argument
                List<Expression> arguments = method.getArguments();
                if (method.getArguments().size() == 1 && arguments.get(0) instanceof J.Lambda) {
                    String methodAsString = method.print(getCursor()).trim().replace(" ", "");
                    // check if method have lambda argument outside parentheses
                    if (methodAsString.endsWith("}") || methodAsString.endsWith("};")) {
                        return method;
                    }

                    String lambda = arguments.get(0).print(getCursor());
                    Object[] parameters;
                    String code;
                    if (method.getSelect() == null) {
                        parameters = new Object[]{method.getSimpleName(), lambda};
                        code = "#{} #{}";
                    } else {
                        parameters = new Object[]{method.getSelect(), method.getSimpleName(), lambda};
                        code = "#{any()}.#{} #{}";
                    }
                    return KotlinTemplate.builder(code)
                            .build()
                            .apply(getCursor(), method.getCoordinates().replace(), parameters)
                            .withPrefix(method.getPrefix());
                }
                return method;

            }
        };
    }

}
