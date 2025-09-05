/*
 * Copyright 2023 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinVisitor;

import java.time.Duration;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;


@Value
@EqualsAndHashCode(callSuper = false)
public class ImplicitParameterInLambda extends Recipe {
    @Override
    public String getDisplayName() {
        return "`it` shouldn't be used as a lambda parameter name";
    }

    @Override
    public String getDescription() {
        return "`it` is a special identifier that allows you to refer to the current parameter being passed to a " +
               "lambda expression without explicitly naming the parameter. " +
               "Lambda expressions are a concise way of writing anonymous functions. Many lambda expressions have " +
               "only one parameter, when this is true the compiler can determine the parameter type by context. Thus " +
               "when using it with single parameter lambda expressions, you do not need to declare the type.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-S6558");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                lambda = (J.Lambda) super.visitLambda(lambda, ctx);
                if (isParameterExplicitIt(lambda)) {
                    lambda = lambda.withParameters(lambda.getParameters().withParameters(emptyList()));
                    return autoFormat(lambda, ctx);
                }
                return lambda;
            }
        };
    }

    /**
     * Return ture when the lambda has only one parameter `it` and with no type.
     */
    private static boolean isParameterExplicitIt(J.Lambda lambda) {
        J.Lambda.Parameters parameters = lambda.getParameters();
        if (parameters.getParameters().size() != 1) {
            return false;
        }

        J parameter = parameters.getParameters().get(0);
        if (parameter instanceof J.VariableDeclarations) {
            J.VariableDeclarations vs = (J.VariableDeclarations) parameter;
            if (vs.getVariables().size() != 1 || vs.getTypeExpression() != null) {
                return false;
            }

            J.VariableDeclarations.NamedVariable v = vs.getVariables().get(0);
            return "it".equals(v.getSimpleName());
        }
        return false;
    }
}
