/*
 * Copyright 2026 the original author or authors.
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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RemoveInheritedDependencies extends Recipe {

    @Getter
    final String displayName = "Remove dependencies already inherited from an extended configuration";

    @Getter
    final String description = "Remove a dependency declaration whose configuration already receives the same " +
            "dependency from a configuration it extends. `testImplementation` extends `implementation`, and in " +
            "`java-library` `implementation` extends `api`, so declaring one coordinate on both reaches nothing the " +
            "wider declaration did not already reach — the second line only has to be kept in step with the first. " +
            "The declaration on the extending configuration is the one removed, leaving the widest. Declarations " +
            "whose versions differ are left alone, since removing one of those changes which version resolves, and " +
            "configurations unrelated by `extendsFrom`, such as `compileOnly` beside `implementation`, express two " +
            "intentions rather than a duplicate. See the " +
            "[Gradle best practice](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#avoid_duplicate_dependencies).";

    @Getter
    final Duration estimatedEffortPerOccurrence = Duration.ofMinutes(2);

    @Value
    private static class Declaration {
        UUID id;
        String configuration;
        String coordinate;

        @Nullable
        String version;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return (J) tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;
                GradleProject gradleProject = cu.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gradleProject == null) {
                    return cu;
                }

                List<Declaration> declarations = new ArrayList<>();
                new GradleDependency.Matcher().<Integer>asVisitor((dependency, p) -> {
                    J.MethodInvocation declaration = dependency.getTree();
                    String groupId = dependency.getDeclaredGroupId();
                    String artifactId = dependency.getDeclaredArtifactId();
                    if (groupId != null && artifactId != null) {
                        declarations.add(new Declaration(
                                declaration.getId(),
                                declaration.getSimpleName(),
                                groupId + ":" + artifactId,
                                dependency.getDeclaredVersion()));
                    }
                    return declaration;
                }).visit(cu, 0);

                Set<UUID> redundant = redundantDeclarations(declarations, gradleProject);
                if (redundant.isEmpty()) {
                    return cu;
                }

                return new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Block visitBlock(J.Block block, ExecutionContext blockCtx) {
                        J.Block b = super.visitBlock(block, blockCtx);
                        return b.withStatements(ListUtils.filter(b.getStatements(),
                                statement -> !redundant.contains(declarationId(statement))));
                    }
                }.visitNonNull(cu, ctx);
            }
        });
    }

    /**
     * A Groovy closure's trailing statement is a `J.Return` wrapping the expression, so the statement in the
     * `dependencies` block is not always the declaration itself.
     */
    private static UUID declarationId(Statement statement) {
        if (statement instanceof J.Return && ((J.Return) statement).getExpression() != null) {
            return ((J.Return) statement).getExpression().getId();
        }
        return statement.getId();
    }

    private static Set<UUID> redundantDeclarations(List<Declaration> declarations, GradleProject gradleProject) {
        Set<UUID> redundant = new HashSet<>();
        for (Declaration wider : declarations) {
            GradleDependencyConfiguration configuration =
                    gradleProject.getNameToConfiguration().get(wider.getConfiguration());
            if (configuration == null) {
                continue;
            }
            Set<String> extendingNames = new HashSet<>();
            for (GradleDependencyConfiguration extending :
                    gradleProject.configurationsExtendingFrom(configuration, true)) {
                extendingNames.add(extending.getName());
            }

            for (Declaration narrower : declarations) {
                if (narrower.getId().equals(wider.getId()) ||
                    !narrower.getCoordinate().equals(wider.getCoordinate()) ||
                    !extendingNames.contains(narrower.getConfiguration())) {
                    continue;
                }
                // A different version is a different resolution, not a duplicate declaration
                if (wider.getVersion() == null ? narrower.getVersion() == null :
                        wider.getVersion().equals(narrower.getVersion())) {
                    redundant.add(narrower.getId());
                }
            }
        }
        return redundant;
    }
}
