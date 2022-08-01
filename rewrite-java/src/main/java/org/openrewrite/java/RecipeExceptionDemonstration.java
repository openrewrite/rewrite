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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RecipeExceptionDemonstration extends Recipe {
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Override
    public String getDisplayName() {
        return "Demonstrate rendering of recipe exceptions";
    }

    @Override
    public String getDescription() {
        return "Show how recipe exceptions are rendered in various forms of OpenRewrite tooling.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            final MethodMatcher methodMatcher = new MethodMatcher(methodPattern);

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (methodMatcher.matches(method)) {
                    throw new DemonstrationException("Demonstrating an exception thrown on a matching method.");
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }

    public static class DemonstrationException extends RuntimeException {
        static boolean restrictStackTrace = false;
        public DemonstrationException(String message) {
            super(message);
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            if (restrictStackTrace) {
                List<StackTraceElement> restricted = new ArrayList<>();
                for (StackTraceElement ste : super.getStackTrace()) {
                    if (ste.getClassName().startsWith(RecipeExceptionDemonstration.class.getName() + "$")) {
                        restricted.add(ste);
                    }
                }
                return restricted.toArray(new StackTraceElement[0]);
            } else {
                return super.getStackTrace();
            }
        }
    }
}
