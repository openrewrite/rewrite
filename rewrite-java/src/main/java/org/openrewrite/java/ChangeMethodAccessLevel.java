/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.java.search.UsesMethod;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodAccessLevel extends Recipe {

    /**
     * A method pattern, expressed as a pointcut expression, that is used to find matching method declarations/invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern, expressed as a pointcut expression, that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New access level",
            description = "New method access level to apply to the method.")
    String newAccessLevel;

    @Override
    public String getDisplayName() {
        return "Change method access level";
    }

    @Override
    public String getDescription() {
        return "Change the access level (public, protected, private, package-private) of a method.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(methodPattern);
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new ChangeMethodAccessLevelVisitor<>(
                new MethodMatcher(methodPattern),
                ChangeMethodAccessLevelVisitor.MethodAccessLevel.fromKeyword(newAccessLevel)
        );
    }

}
