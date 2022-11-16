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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveRepository extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove repository";
    }

    @Override
    public String getDescription() {
        return "Removes a repository from Gradle build scripts. Named repositories include \"jcenter\", \"mavenCentral\", \"mavenLocal\", and \"google\".";
    }

    @Option(displayName = "Repository",
            description = "The name of the repository to remove",
            example = "jcenter")
    String repository;

    @Override
    protected IsBuildGradle<ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    public GroovyVisitor<ExecutionContext> getVisitor() {
        MethodMatcher repositories = new MethodMatcher("org.gradle.api.artifacts.dsl.RepositoryHandler " + repository + "()");
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if(repositories.matches(method)) {
                    //noinspection ConstantConditions
                    return null;
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
