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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.Scope;

import static org.openrewrite.Validated.notBlank;

@EqualsAndHashCode(callSuper = false)
@Value
public class DoesNotIncludeDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Only direct dependencies",
            description = "Default false. If enabled, transitive dependencies will not be considered.",
            required = false,
            example = "true")
    @Nullable
    Boolean onlyDirect;

    @Option(displayName = "Scope",
            description = "Default any. If specified, only the requested scope's classpaths will be checked.",
            required = false,
            valid = {"compile", "test", "runtime", "provided"},
            example = "compile")
    @Nullable
    String scope;

    @Override
    public String getDisplayName() {
        return "Does not include Maven dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "A precondition which returns false if visiting a Maven pom which includes the specified dependency in the classpath of some scope. " +
                "For compatibility with multimodule projects, this should most often be applied as a precondition.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(notBlank("groupId", groupId).and(notBlank("artifactId", artifactId)))
                .and(Validated.test("scope", "scope is a valid Maven scope", scope,
                        s -> Scope.fromName(s) != Scope.Invalid));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            final TreeVisitor<?, ExecutionContext> di = new DependencyInsight(groupId, artifactId, scope, null, onlyDirect).getVisitor();

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return di.isAcceptable(sourceFile, ctx);
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t2 = di.visit(tree, ctx);
                // if POM file is unchanged, we found something without the dependency
                if (t2 == tree) {
                    return SearchResult.found(tree);
                }
                return tree;
            }
        };
    }
}
