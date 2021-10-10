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
import org.openrewrite.Tree;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddRepository extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add repository";
    }

    @Override
    public String getDescription() {
        return "Adds a repository to Gradle build scripts. Named repositories include \"jcenter\", \"mavenCentral\", \"mavenLocal\", and \"google\"";
    }

    @Option(displayName = "Repository",
            description = "The name of the repository to add",
            example = "mavenCentral")
    String repository;

    @Override
    protected IsBuildGradle<ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    protected GroovyVisitor<ExecutionContext> getVisitor() {
        MethodMatcher repositories = new MethodMatcher("RewriteGradleProject repositories(groovy.lang.Closure)");
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, context);
                if(repositories.matches(m)) {
                    if(FindMethods.find(m, "org.gradle.api.artifacts.dsl.RepositoryHandler " + repository + "()").size() > 0) {
                        return m;
                    }
                }
                if(m.getArguments().size() < 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
                    return m;
                }
                J.Lambda arg = (J.Lambda)m.getArguments().get(0);
                if(!(arg.getBody() instanceof J.Block)) {
                    return m;
                }
                J.Block body = (J.Block) arg.getBody();

                JavaType.Method.Signature signature = new JavaType.Method.Signature(JavaType.Class.build("org.gradle.api.artifacts.repositories.MavenArtifactRepository"), emptyList());
                JavaType.Method methodType = JavaType.Method.build(
                        singleton(Flag.Public),
                        JavaType.Class.build("org.gradle.api.artifacts.dsl.RepositoryHandler"),
                        repository,
                        signature,
                        signature,
                        emptyList(),
                        emptyList(),
                        emptyList()
                );
                J.MethodInvocation repositoryInvocation = new J.MethodInvocation(
                        randomId(),
                        Space.build("\n    ", emptyList()),
                        Markers.EMPTY,
                        null,
                        null,
                        J.Identifier.build(randomId(), Space.EMPTY, Markers.EMPTY, repository,null, null),
                        JContainer.build(Space.EMPTY, singletonList(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))), Markers.EMPTY),
                        methodType
                );
                body = body.withStatements(ListUtils.concat(repositoryInvocation, body.getStatements()));
                arg = arg.withBody(body);
                m = m.withArguments(singletonList(arg));

                return m;
            }
        };
    }
}
