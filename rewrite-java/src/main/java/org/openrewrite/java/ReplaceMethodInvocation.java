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
import org.openrewrite.Incubating;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Incubating(since = "7.15.0")
@Value
@EqualsAndHashCode(callSuper = true)
public class ReplaceMethodInvocation extends Recipe {
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.joda.time.base.BaseDateTime getMillis()")
    String methodPattern;

    @Option(displayName = "Replace with",
            description = "A compilable template to replace the original call with.",
            example = "any")
    String replaceWith;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overloads of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Replace method invocation";
    }

    @Override
    public String getDescription() {
        return "Replace a method invocation with an arbitrary code block.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(methodPattern);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        MethodMatcher matcher = new MethodMatcher(methodPattern, matchOverrides);
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                if (matcher.matches(m)) {
                    JavaType.Method type = m.getType();
                    assert type != null;

                    String template = "#{any(" + type.getDeclaringType().getFullyQualifiedName() + ")}." + replaceWith + ";";
                    m = m.withTemplate(JavaTemplate.builder(this::getCursor, template)
                                    .javaParser(() -> JavaParser.fromJavaVersion()
                                            .logCompilationWarningsAndErrors(true)
                                            .build())
                                    .build(),
                            m.getCoordinates().replace(),
                            m.getSelect());
                }
                return m;
            }
        };
    }
}
