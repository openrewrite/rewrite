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
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.maven.tree.Scope;

@EqualsAndHashCode(callSuper = true)
@Value
public class DoesNotIncludeDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'com.google.guava:guava:VERSION'. Supports glob.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'com.google.guava:guava:VERSION'. Supports glob.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Check transitive dependencies",
            description = "Default false. If enabled, matching transitive dependencies will also yield a false result.",
            required = false,
            example = "true")
    Boolean checkTransitive;

    @Override
    public String getDisplayName() {
        return "Does not include Maven dependency";
    }

    @Override
    public String getDescription() {
        return "An applicability test which returns false iff visiting a Maven pom which uses the specified dependency. For multimodule projects, this should most often be applied as a single-source applicability test";
    }

    @Override
    public Validated validate() {
        return Validated.notBlank("groupId", groupId)
                .and(Validated.notBlank("artifactId", artifactId));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Applicability.not(dependencySearchVisitor());
    }

    @NotNull
    private TreeVisitor<?, ExecutionContext> dependencySearchVisitor() {
        return Boolean.TRUE.equals(checkTransitive)
                ? new DependencyInsight(groupId, artifactId, Scope.Runtime.toString()).getVisitor()
                : new FindDependency(groupId, artifactId).getVisitor();
    }
}
