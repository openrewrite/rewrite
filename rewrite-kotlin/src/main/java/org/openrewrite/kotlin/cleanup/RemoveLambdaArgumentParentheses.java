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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.marker.Markers;

import java.time.Duration;

public class RemoveLambdaArgumentParentheses extends Recipe {

    @Getter
    final String displayName = "Remove method invocation parentheses around single lambda argument";

    @Getter
    final String description = "For example, convert `1.let({ it + 1 })` to `1.let { it + 1 }`.";

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (method.getArguments().size() == 1 && method.getArguments().get(0) instanceof J.Lambda) {
                    J.MethodInvocation.Padding padding = method.getPadding();
                    JContainer<Expression> arguments = padding.getArguments();
                    Markers argumentsMarkers = arguments.getMarkers();
                    if (!argumentsMarkers.findFirst(OmitParentheses.class).isPresent()) {
                        return padding.withArguments(arguments
                                .withMarkers(argumentsMarkers.add(new OmitParentheses(Tree.randomId())))
                                .withBefore(Space.SINGLE_SPACE));
                    }
                }
                return method;
            }
        };
    }

}
