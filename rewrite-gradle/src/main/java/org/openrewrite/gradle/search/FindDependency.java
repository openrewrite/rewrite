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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindDependency extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find Gradle Dependency";
    }

    @Override
    public String getDescription() {
        return "Finds dependencies declared in build.gradle files. Does not yet support detection of transitive dependencies.";
    }

    @Override
    protected GroovyVisitor<ExecutionContext> getVisitor() {
        MethodMatcher DEPENDENCIES_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");
        return new GroovyVisitor<ExecutionContext>() {

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                if(DEPENDENCIES_MATCHER.matches(method)) {
                    return method.withMarkers(method.getMarkers().searchResult());
                }
                return super.visitMethodInvocation(method, context);
            }
        };
    }
}
