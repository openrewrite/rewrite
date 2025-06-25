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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.gradle.trait.SpringDependencyManagementPluginEntry;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;
import org.openrewrite.trait.Trait;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class ChangeManagedDependency extends Recipe {

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a dependency coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a dependency coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "rewrite-testing-frameworks")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use. Defaults to the existing group id.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use. Defaults to the existing artifact id.",
            example = "rewrite-testing-frameworks",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                    "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                    "the current version is a valid semantic version. For more details, you can look at the documentation " +
                    "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors).",
            example = "29.X",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    // Individual dependencies tend to appear in several places within a given dependency graph.
    // Minimize the number of allocations by caching the updated dependencies.
    transient Map<org.openrewrite.maven.tree.Dependency, org.openrewrite.maven.tree.Dependency> updatedRequested = new HashMap<>();
    transient Map<org.openrewrite.maven.tree.ResolvedDependency, org.openrewrite.maven.tree.ResolvedDependency> updatedResolved = new HashMap<>();

    @Override
    public String getDisplayName() {
        return "Change Gradle managed dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", oldGroupId, oldArtifactId);
    }

    @Override
    public String getDescription() {
        return "Change a Gradle managed dependency coordinates. The `newGroupId` or `newArtifactId` **MUST** be different from before.\n" +
                "For now, only Spring Dependency Management Plugin entries are supported and no other forms of managed dependencies (yet).";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        validated = validated.and(Validated.required("newGroupId", newGroupId).or(Validated.required("newArtifactId", newArtifactId)));
        validated = validated.and(Validated.test(
                "coordinates",
                "newGroupId OR newArtifactId must be different from before",
                this,
                r -> {
                    boolean sameGroupId = StringUtils.isBlank(r.newGroupId) || Objects.equals(r.oldGroupId, r.newGroupId);
                    boolean sameArtifactId = StringUtils.isBlank(r.newArtifactId) || Objects.equals(r.oldArtifactId, r.newArtifactId);
                    return !(sameGroupId && sameArtifactId);
                }
        ));
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(), new JavaIsoVisitor<ExecutionContext>() {

            private final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof G.CompilationUnit || sourceFile instanceof K.CompilationUnit;
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                J t = super.visit(tree, ctx);
                if (t instanceof JavaSourceFile && t != tree) {
                    Optional<GradleProject> maybeGp = tree.getMarkers().findFirst(GradleProject.class);
                    if (maybeGp.isPresent()) {
                        GradleProject gp = maybeGp.get();
                        t = t.withMarkers(t.getMarkers().setByType(updateGradleModel(gp)));
                    }
                }
                return t;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                return new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId)
                        .get(getCursor())
                        .map(dep -> dep.withGroupArtifactVersion(depMatcher, newGroupId, newArtifactId, newVersion, versionPattern, metadataFailures, ctx))
                        .map(Trait::getTree)
                        .orElse(m);
            }

            private GradleProject updateGradleModel(GradleProject gp) {
                Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
                Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
                boolean anyChanged = false;
                for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                    GradleDependencyConfiguration newGdc = gdc;
                    newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                        assert requested != null;
                        if (depMatcher.matches(requested.getGroupId(), requested.getArtifactId())) {
                            requested = updatedRequested.computeIfAbsent(requested, r -> {
                                GroupArtifactVersion gav = r.getGav();
                                if (newGroupId != null) {
                                    gav = gav.withGroupId(newGroupId);
                                }
                                if (newArtifactId != null) {
                                    gav = gav.withArtifactId(newArtifactId);
                                }
                                if (gav != r.getGav()) {
                                    r = r.withGav(gav);
                                }
                                return r;
                            });
                        }
                        return requested;
                    }));
                    newGdc = newGdc.withDirectResolved(ListUtils.map(gdc.getDirectResolved(), resolved -> {
                        assert resolved != null;
                        if (depMatcher.matches(resolved.getGroupId(), resolved.getArtifactId())) {
                            resolved = updatedResolved.computeIfAbsent(resolved, r -> {
                                ResolvedGroupArtifactVersion gav = r.getGav();
                                if (newGroupId != null) {
                                    gav = gav.withGroupId(newGroupId);
                                }
                                if (newArtifactId != null) {
                                    gav = gav.withArtifactId(newArtifactId);
                                }
                                if (gav != r.getGav()) {
                                    r = r.withGav(gav);
                                }
                                return r;
                            });
                        }
                        return resolved;
                    }));
                    anyChanged |= newGdc != gdc;
                    newNameToConfiguration.put(newGdc.getName(), newGdc);
                }
                if (anyChanged) {
                    gp = gp.withNameToConfiguration(newNameToConfiguration);
                }
                return gp;
            }
        });
    }
}
