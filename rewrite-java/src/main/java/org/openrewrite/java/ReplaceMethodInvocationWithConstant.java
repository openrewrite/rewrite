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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.trait.Traits;
import org.openrewrite.java.tree.Expression;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceMethodInvocationWithConstant extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A pattern to match method invocations to replace. " + MethodMatcher.METHOD_PATTERN_DESCRIPTION,
            example = "java.lang.StringBuilder append(java.lang.String)")
    String methodPattern;

    @Option(displayName = "Replacement",
            description = "The constant to replace the method invocation with.",
            example = "null")
    String replacement;

    @Override
    public String getDisplayName() {
        return "Replace method invocation with constant";
    }

    @Override
    public String getDescription() {
        return "Replace all method invocations matching the method pattern with the specified constant.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(methodPattern),
                Traits.methodAccess(methodPattern)
                        .asVisitor(ma ->
                                JavaTemplate.apply(replacement, ma.getCursor(),
                                        ((Expression) ma.getCursor().getValue()).getCoordinates().replace())));
    }
}
