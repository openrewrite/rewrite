/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.search.UsesMethod;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveMethodInvocations extends Recipe {
    @Option(displayName = "Method pattern",
            description = "A pattern to match method invocations for removal.",
            example = "java.lang.StringBuilder append(java.lang.String)")
    String methodPattern;

    @Override
    public String getDisplayName() {
        return "Remove method invocations";
    }

    @Override
    public String getDescription() {
        return "Remove method invocations if syntactically safe.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern),
                new RemoveMethodInvocationsVisitor(singletonList(methodPattern)));
    }
}
