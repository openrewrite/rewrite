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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.DependencyMatcher;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.marker.Markup.info;

/**
 * A visitor that adds an explicit version to dependencies that don't have one declared.
 * This is useful when a dependency version was previously managed (e.g., by a BOM or platform)
 * and the management is being removed, requiring the version to be explicitly specified.
 * <p>
 * The recipe looks up the resolved version from the GradleProject marker.
 * To change the version after adding it, use {@link UpgradeDependencyVersion}.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddExplicitDependencyVersion extends JavaIsoVisitor<ExecutionContext> {

    DependencyMatcher dependencyMatcher;

    public AddExplicitDependencyVersion(String groupId, String artifactId) {
        this.dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
        this.gp = null;
    }

    @Nullable
    @NonFinal
    GradleProject gp;

    @Override
    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (tree instanceof SourceFile) {
            tree.getMarkers().findFirst(GradleProject.class).ifPresent(gradleProject -> gp = gradleProject);
        }
        return super.visit(tree, ctx);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

        // Can't do anything without GradleProject
        if (gp == null) {
            return m;
        }

        // Use the GradleDependency trait to match dependencies
        return new GradleDependency.Matcher()
                .matcher(dependencyMatcher)
                .get(getCursor())
                .map(gradleDep -> {
                    // Skip if version is already declared
                    String declaredVersion = gradleDep.getResolvedDependency().getVersion();
                    if (!StringUtils.isBlank(declaredVersion) && gradleDep.getDeclaredVersion() != null) {
                        return m;
                    }

                    // Look up the resolved version from the GradleProject marker
                    String configName = m.getSimpleName();
                    String groupId = gradleDep.getResolvedDependency().getGroupId();
                    String artifactId = gradleDep.getResolvedDependency().getArtifactId();
                    String versionToAdd = findResolvedVersion(configName, groupId, artifactId);

                    if (StringUtils.isBlank(versionToAdd)) {
                        return info(m, "The Gradle project marker did not contain a resolved version for this dependency. No version was added.");
                    }

                    // Use the trait's withDeclaredVersion to add the version
                    GradleDependency updated = gradleDep.withDeclaredVersion(versionToAdd);
                    return updated.getTree();
                })
                .orElse(m);
    }

    private @Nullable String findResolvedVersion(String configurationName, String groupId, String artifactId) {
        GradleDependencyConfiguration gdc = requireNonNull(gp).getConfiguration(configurationName);
        if (gdc == null) {
            return null;
        }

        // Check this configuration's resolved dependencies
        for (ResolvedDependency resolved : gdc.getDirectResolved()) {
            if (groupId.equals(resolved.getGroupId()) && artifactId.equals(resolved.getArtifactId())) {
                return resolved.getVersion();
            }
        }

        // Declarable configurations (like 'implementation') don't have resolved dependencies directly.
        // We need to check resolvable configurations that extend from this one
        // (e.g., compileClasspath extends implementation)
        for (GradleDependencyConfiguration extending : gp.configurationsExtendingFrom(gdc, true)) {
            for (ResolvedDependency resolved : extending.getDirectResolved()) {
                if (groupId.equals(resolved.getGroupId()) && artifactId.equals(resolved.getArtifactId())) {
                    return resolved.getVersion();
                }
            }
        }

        return null;
    }
}
