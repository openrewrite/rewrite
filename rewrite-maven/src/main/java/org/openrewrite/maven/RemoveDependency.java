/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.maven.utilities.JavaSourceSetUpdater;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Scope",
            description = "Only remove dependencies if they are in this scope. If 'runtime', this will" +
                          "also remove dependencies in the 'compile' scope because 'compile' dependencies are part of the runtime dependency set",
            valid = {"compile", "test", "runtime", "provided"},
            example = "compile",
            required = false)
    @Nullable
    String scope;

    String displayName = "Remove Maven dependency";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    String description = "Removes a single dependency from the <dependencies> section of the pom.xml. " +
            "Does not remove usage of the dependency classes, nor guard against the resulting compilation errors.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MavenIsoVisitor<ExecutionContext> mavenVisitor = new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag(groupId, artifactId)) {
                    Scope checkScope = scope != null ? Scope.fromName(scope) : null;
                    ResolvedDependency dependency = findDependency(tag, checkScope);
                    if (dependency != null) {
                        doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                        maybeUpdateModel();
                    }
                }

                return super.visitTag(tag, ctx);
            }
        };

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return mavenVisitor.isAcceptable(sourceFile, ctx) || sourceFile instanceof JavaSourceFile;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sf = (SourceFile) tree;
                if (mavenVisitor.isAcceptable(sf, ctx)) {
                    return mavenVisitor.visit(tree, ctx);
                }
                if (sf instanceof JavaSourceFile) {
                    Optional<JavaSourceSet> maybeSourceSet = sf.getMarkers().findFirst(JavaSourceSet.class);
                    if (maybeSourceSet.isPresent()) {
                        JavaSourceSet updated = JavaSourceSetUpdater.removeTypesMatching(
                                maybeSourceSet.get(), groupId, artifactId);
                        if (updated != maybeSourceSet.get()) {
                            return sf.withMarkers(sf.getMarkers().setByType(updated));
                        }
                    }
                }
                return tree;
            }
        };
    }

}
