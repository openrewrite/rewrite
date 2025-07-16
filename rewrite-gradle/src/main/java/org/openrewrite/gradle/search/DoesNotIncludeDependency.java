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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;

import static org.openrewrite.Validated.notBlank;

@Value
@EqualsAndHashCode(callSuper = false)
public class DoesNotIncludeDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope. If not specified, all configurations will be searched.",
            example = "compileClasspath",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Does not include Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "A precondition which returns false if visiting a Gradle file which includes the specified dependency in the classpath of some scope. " +
                "For compatibility with multimodule projects, this should most often be applied as a precondition.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(notBlank("groupId", groupId))
                .and(notBlank("artifactId", artifactId));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            final TreeVisitor<?, ExecutionContext> di = new DependencyInsight(groupId, artifactId, null, configuration).getVisitor();

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return di.isAcceptable(sourceFile, ctx);
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t2 = di.visit(tree, ctx);
                // if Gradle file is unchanged, we found something without the dependency
                if (t2 == tree) {
                    return SearchResult.found(tree);
                }
                return tree;
            }
        };
    }
}
