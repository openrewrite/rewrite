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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.*;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;

import java.util.*;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependency extends ScanningRecipe<ChangeDependency.Accumulator> {
    private static final String GRADLE_PROPERTIES_FILE_NAME = "gradle.properties";

    // Individual dependencies tend to appear in several places within a given dependency graph.
    // Minimize the number of allocations by caching the updated dependencies.
    @EqualsAndHashCode.Exclude
    transient Map<org.openrewrite.maven.tree.Dependency, org.openrewrite.maven.tree.Dependency> updatedRequested = new HashMap<>();

    @EqualsAndHashCode.Exclude
    transient Map<org.openrewrite.maven.tree.ResolvedDependency, org.openrewrite.maven.tree.ResolvedDependency> updatedResolved = new HashMap<>();

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

    @Option(displayName = "Override managed version",
            description = "If the old dependency has a managed version, this flag can be used to explicitly set the version on the new dependency. " +
                    "WARNING: No check is done on the NEW dependency to verify if it is managed, it relies on whether the OLD dependency had a managed version. " +
                    "The default for this flag is `false`.",
            required = false)
    @Nullable
    Boolean overrideManagedVersion;

    @Option(displayName = "Update dependency management",
            description = "Also update the dependency management section. The default for this flag is `true`.",
            required = false)
    @Nullable
    Boolean changeManagedDependency;

    @JsonCreator
    public ChangeDependency(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean overrideManagedVersion, @Nullable Boolean changeManagedDependency) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.overrideManagedVersion = overrideManagedVersion;
        this.changeManagedDependency = changeManagedDependency;
    }

    public ChangeDependency(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean overrideManagedVersion) {
        this(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern, overrideManagedVersion, true);
    }

    String displayName = "Change Gradle dependency";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", oldGroupId, oldArtifactId);
    }

    String description = "Change a Gradle dependency coordinates. The `newGroupId` or `newArtifactId` **MUST** be different from before.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        validated = validated.and(Validated.required("newGroupId", newGroupId).or(Validated.required("newArtifactId", newArtifactId)));
        return validated.and(Validated.test(
                "coordinates",
                "newGroupId OR newArtifactId must be different from before",
                this,
                r -> {
                    boolean sameGroupId = StringUtils.isBlank(r.newGroupId) || Objects.equals(r.oldGroupId, r.newGroupId);
                    boolean sameArtifactId = StringUtils.isBlank(r.newArtifactId) || Objects.equals(r.oldArtifactId, r.newArtifactId);
                    return !(sameGroupId && sameArtifactId);
                }
        ));
    }

    public static class Accumulator {
        Map<String, Object> versionVariableUpdates = new HashMap<>();
        Map<String, Set<GroupArtifact>> versionVariableUsages = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaVisitor<ExecutionContext>() {
            @Nullable
            GradleProject gradleProject;

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return (sourceFile instanceof G.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle")) ||
                        (sourceFile instanceof K.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle.kts"));
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    gradleProject = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (gradleProject == null) {
                    return m;
                }

                new GradleDependency.Matcher().get(getCursor()).ifPresent(dep -> {
                    String varName = dep.getVersionVariable();
                    if (varName != null) {
                        acc.versionVariableUsages
                                .computeIfAbsent(varName, k -> new HashSet<>())
                                .add(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()));
                    }
                });

                new GradleDependency.Matcher()
                        .groupId(oldGroupId)
                        .artifactId(oldArtifactId)
                        .get(getCursor())
                        .ifPresent(dep -> {
                            String varName = dep.getVersionVariable();
                            if (varName != null && !StringUtils.isBlank(newVersion)) {
                                resolveAndRecordVersion(varName, m, dep, ctx);
                            }
                        });

                return m;
            }

            private void resolveAndRecordVersion(String varName, J.MethodInvocation m, GradleDependency dep, ExecutionContext ctx) {
                String resolvedGroupId = !StringUtils.isBlank(newGroupId) ? newGroupId : dep.getGroupId();
                String resolvedArtifactId = !StringUtils.isBlank(newArtifactId) ? newArtifactId : dep.getArtifactId();
                try {
                    String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                            .select(new GroupArtifact(resolvedGroupId, resolvedArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                    if (resolvedVersion != null) {
                        acc.versionVariableUpdates.put(varName, resolvedVersion);
                    }
                } catch (MavenDownloadingException e) {
                    acc.versionVariableUpdates.put(varName, e);
                }
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        TreeVisitor<?, ExecutionContext> gradleVisitor = Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(), new JavaIsoVisitor<ExecutionContext>() {
            final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
            final DependencyMatcher existingMatcher = requireNonNull(DependencyMatcher.build(newGroupId + ":" + newArtifactId + (newVersion == null ? "" : ":" + newVersion)).getValue());

            @SuppressWarnings("NotNullFieldNotInitialized")
            GradleProject gradleProject;

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof G.CompilationUnit || sourceFile instanceof K.CompilationUnit;
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile sourceFile = (JavaSourceFile) tree;
                    sourceFile = maybeRemoveDuplicateTargetDependency(sourceFile, ctx);
                    Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
                    if (!maybeGp.isPresent()) {
                        return sourceFile;
                    }
                    gradleProject = maybeGp.get();

                    sourceFile = (JavaSourceFile) super.visit(sourceFile, ctx);
                    if (sourceFile != null && sourceFile != tree) {
                        sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().setByType(updateGradleModel(gradleProject, ctx)));
                        if (changeManagedDependency == null || changeManagedDependency) {
                            doAfterVisit(new ChangeManagedDependency(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern).getVisitor());
                        }
                    }

                    return super.visit(sourceFile, ctx);
                }
                return super.visit(tree, ctx);
            }

            private JavaSourceFile maybeRemoveDuplicateTargetDependency(JavaSourceFile sourceFile, ExecutionContext ctx) {
                Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
                if (!maybeGp.isPresent()){
                    return sourceFile;
                }
                for (GradleDependencyConfiguration c : maybeGp.get().getConfigurations()) {
                    boolean oldFound = false;
                    boolean newFound = false;
                    for (Dependency d : c.getRequested()) {
                        String version = d.getVersion();
                        if (version == null) {
                            ResolvedDependency rd = c.findResolvedDependency(d.getGroupId(), d.getArtifactId());
                            if (rd == null) {
                                continue;
                            } else {
                                version = rd.getVersion();
                            }
                        }
                        oldFound |= depMatcher.matches(d.getGroupId(), d.getArtifactId(), version);
                        newFound |= existingMatcher.matches(d.getGroupId(), d.getArtifactId(), version);
                    }
                    if (oldFound && newFound) {
                        sourceFile = (JavaSourceFile) new RemoveDependency(oldGroupId, oldArtifactId, c.getName())
                                .getVisitor()
                                .visitNonNull(sourceFile, ctx);
                    }
                }
                return sourceFile;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher()
                        .groupId(oldGroupId)
                        .artifactId(oldArtifactId);

                Optional<GradleDependency> maybeDep = gradleDependencyMatcher.get(getCursor());
                if (!maybeDep.isPresent()) {
                    return m;
                }

                return updateDependency(m, maybeDep.get(), ctx);
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
                if (!canUpdateVariable(v.getSimpleName())) {
                    return v;
                }
                Object scanResult = acc.versionVariableUpdates.get(v.getSimpleName());
                if (scanResult instanceof Exception) {
                    return Markup.warn(v, (Exception) scanResult);
                }
                if (scanResult instanceof String && v.getInitializer() instanceof J.Literal) {
                    String resolvedVersion = (String) scanResult;
                    J.Literal initializer = (J.Literal) v.getInitializer();
                    if (initializer.getValue() instanceof String && !resolvedVersion.equals(initializer.getValue())) {
                        v = v.withInitializer(ChangeStringLiteral.withStringValue(initializer, resolvedVersion));
                    }
                }
                return v;
            }

            private boolean canUpdateVariable(String varName) {
                return ChangeDependency.this.canUpdateVariable(varName, depMatcher, acc);
            }

            private J.MethodInvocation updateDependency(J.MethodInvocation m, GradleDependency dep, ExecutionContext ctx) {
                GradleDependency updated = dep;

                if (!StringUtils.isBlank(newGroupId)) {
                    updated = updated.withDeclaredGroupId(newGroupId);
                }
                if (!StringUtils.isBlank(newArtifactId)) {
                    updated = updated.withDeclaredArtifactId(newArtifactId);
                }

                String varName = dep.getVersionVariable();
                if (varName != null && !canUpdateVariable(varName)) {
                    Object scanResult = acc.versionVariableUpdates.get(varName);
                    if (scanResult instanceof Exception) {
                        return ((MavenDownloadingException) scanResult).warn(m);
                    }
                    String resolvedVersion = scanResult instanceof String ? (String) scanResult : null;
                    if (resolvedVersion == null && !StringUtils.isBlank(newVersion)) {
                        try {
                            resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(
                                                    !StringUtils.isBlank(newGroupId) ? newGroupId : dep.getGroupId(),
                                                    !StringUtils.isBlank(newArtifactId) ? newArtifactId : dep.getArtifactId()),
                                            dep.getConfigurationName(), newVersion, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                    }
                    if (resolvedVersion != null) {
                        updated = updated.withDeclaredVersion(resolvedVersion);
                    }
                } else if (varName == null) {
                    String declaredVersion = dep.getDeclaredVersion();
                    if (!StringUtils.isBlank(newVersion) && (!StringUtils.isBlank(declaredVersion) || Boolean.TRUE.equals(overrideManagedVersion))) {
                        String resolvedVersion;
                        try {
                            resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(
                                                    !StringUtils.isBlank(newGroupId) ? newGroupId : dep.getGroupId(),
                                                    !StringUtils.isBlank(newArtifactId) ? newArtifactId : dep.getArtifactId()),
                                            dep.getConfigurationName(), newVersion, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                        if (resolvedVersion != null && !resolvedVersion.equals(declaredVersion)) {
                            updated = updated.withDeclaredVersion(resolvedVersion);
                        }
                    }
                }

                return updated.getTree();
            }

            private GradleProject updateGradleModel(GradleProject gp, ExecutionContext ctx) {
                Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
                Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
                boolean anyChanged = false;
                for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                    GradleDependencyConfiguration newGdc = gdc;
                    newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                        if (depMatcher.matches(requested.getGroupId(), requested.getArtifactId())) {
                            requested = updatedRequested.computeIfAbsent(requested, r -> {
                                GroupArtifactVersion gav = r.getGav();
                                if (newGroupId != null) {
                                    gav = gav.withGroupId(newGroupId);
                                }
                                if (newArtifactId != null) {
                                    gav = gav.withArtifactId(newArtifactId);
                                }
                                if (!StringUtils.isBlank(newVersion) && (!StringUtils.isBlank(gav.getVersion()) || Boolean.TRUE.equals(overrideManagedVersion))) {
                                    try {
                                        String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                                .select(new GroupArtifact(gav.getGroupId(), gav.getArtifactId()), gdc.getName(), newVersion, versionPattern, ctx);
                                        if (resolvedVersion != null && !resolvedVersion.equals(gav.getVersion())) {
                                            gav = gav.withVersion(resolvedVersion);
                                        }
                                    } catch (MavenDownloadingException e) {
                                        // Failure already in `metadataFailures`
                                    }
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
                                if (!StringUtils.isBlank(newVersion) && (!StringUtils.isBlank(gav.getVersion()) || Boolean.TRUE.equals(overrideManagedVersion))) {
                                    try {
                                        String resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                                .select(new GroupArtifact(gav.getGroupId(), gav.getArtifactId()), gdc.getName(), newVersion, versionPattern, ctx);
                                        if (resolvedVersion != null && !resolvedVersion.equals(gav.getVersion())) {
                                            gav = gav.withVersion(resolvedVersion);
                                        }
                                    } catch (MavenDownloadingException e) {
                                        // Failure already in `metadataFailures`
                                    }
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

        DependencyMatcher propsMatcher = requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof Properties.File) {
                    Properties.File propsFile = (Properties.File) tree;
                    if (propsFile.getSourcePath().endsWith(GRADLE_PROPERTIES_FILE_NAME) && !acc.versionVariableUpdates.isEmpty()) {
                        return new PropertiesVisitor<ExecutionContext>() {
                            @Override
                            public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                                if (!canUpdateVariable(entry.getKey(), propsMatcher, acc)) {
                                    return entry;
                                }
                                Object scanResult = acc.versionVariableUpdates.get(entry.getKey());
                                if (scanResult instanceof Exception) {
                                    return Markup.warn(entry, (Exception) scanResult);
                                }
                                if (scanResult instanceof String) {
                                    String resolvedVersion = (String) scanResult;
                                    if (!resolvedVersion.equals(entry.getValue().getText())) {
                                        return entry.withValue(entry.getValue().withText(resolvedVersion));
                                    }
                                }
                                return entry;
                            }
                        }.visitNonNull(tree, ctx);
                    }
                    return tree;
                }
                return gradleVisitor.visit(tree, ctx);
            }
        };
    }

    private boolean canUpdateVariable(String varName, DependencyMatcher depMatcher, Accumulator acc) {
        Set<GroupArtifact> usages = acc.versionVariableUsages.get(varName);
        if (usages == null) {
            return true;
        }
        for (GroupArtifact ga : usages) {
            if (!depMatcher.matches(ga.getGroupId(), ga.getArtifactId())) {
                return false;
            }
        }
        return true;
    }
}
