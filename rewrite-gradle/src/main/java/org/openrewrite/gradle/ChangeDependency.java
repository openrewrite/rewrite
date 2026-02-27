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
import org.openrewrite.maven.tree.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependency extends Recipe {

    // Individual dependencies tend to appear in several places within a given dependency graph.
    // Minimize the number of allocations by caching the updated dependencies.
    @EqualsAndHashCode.Exclude
    transient Map<org.openrewrite.maven.tree.Dependency, org.openrewrite.maven.tree.Dependency> updatedRequested = new HashMap<>();

    @EqualsAndHashCode.Exclude
    transient Map<org.openrewrite.maven.tree.ResolvedDependency, org.openrewrite.maven.tree.ResolvedDependency> updatedResolved = new HashMap<>();

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @EqualsAndHashCode.Exclude
    transient Map<String, String> pendingPropertyUpdates = new ConcurrentHashMap<>();

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

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
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

            /**
             * Avoid duplicating dependencies when the target dependency already exists in the project.
             */
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

                if (!gradleDependencyMatcher.get(getCursor()).isPresent()) {
                    return m;
                }

                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal || depArgs.get(0) instanceof G.GString || depArgs.get(0) instanceof G.MapEntry || depArgs.get(0) instanceof G.MapLiteral || depArgs.get(0) instanceof J.Assignment || depArgs.get(0) instanceof K.StringTemplate) {
                    m = updateDependency(m, ctx);
                } else if (depArgs.get(0) instanceof J.MethodInvocation &&
                        ("platform".equals(((J.MethodInvocation) depArgs.get(0)).getSimpleName()) ||
                                "enforcedPlatform".equals(((J.MethodInvocation) depArgs.get(0)).getSimpleName()))) {
                    m = m.withArguments(ListUtils.mapFirst(depArgs, platform -> updateDependency((J.MethodInvocation) platform, ctx)));
                }

                return m;
            }

            private J.MethodInvocation updateDependency(J.MethodInvocation m, ExecutionContext ctx) {
                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal) {
                    String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                    if (gav != null) {
                        Dependency original = DependencyNotation.parse(gav);
                        if (original != null) {
                            Dependency updated = original;
                            if (!StringUtils.isBlank(newGroupId) && !Objects.equals(updated.getGroupId(), newGroupId)) {
                                updated = updated.withGav(updated.getGav().withGroupId(newGroupId));
                            }
                            if (!StringUtils.isBlank(newArtifactId) && !updated.getArtifactId().equals(newArtifactId)) {
                                updated = updated.withGav(updated.getGav().withArtifactId(newArtifactId));
                            }
                            if (!StringUtils.isBlank(newVersion) && (!StringUtils.isBlank(original.getVersion()) || Boolean.TRUE.equals(overrideManagedVersion))) {
                                String resolvedVersion;
                                try {
                                    resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                            .select(new GroupArtifact(updated.getGroupId(), updated.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                                } catch (MavenDownloadingException e) {
                                    return e.warn(m);
                                }
                                if (resolvedVersion != null && !resolvedVersion.equals(updated.getVersion())) {
                                    updated = updated.withGav(updated.getGav().withVersion(resolvedVersion));
                                }
                            }
                            if (original != updated) {
                                String replacement = DependencyNotation.toStringNotation(updated);
                                m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> ChangeStringLiteral.withStringValue((J.Literal) arg, replacement)));
                            }
                        }
                    }
                } else if (m.getArguments().get(0) instanceof G.GString) {
                    G.GString gstring = (G.GString) depArgs.get(0);
                    List<J> strings = gstring.getStrings();
                    if (strings.size() >= 2 && strings.get(0) instanceof J.Literal &&
                            ((J.Literal) strings.get(0)).getValue() != null) {

                        J.Literal literal = (J.Literal) strings.get(0);
                        Dependency original = DependencyNotation.parse((String) requireNonNull(literal.getValue()));
                        if (original != null) {
                            Dependency updated = original;
                            if (!StringUtils.isBlank(newGroupId) && !Objects.equals(updated.getGroupId(), newGroupId)) {
                                updated = updated.withGav(updated.getGav().withGroupId(newGroupId));
                            }
                            if (!StringUtils.isBlank(newArtifactId) && !updated.getArtifactId().equals(newArtifactId)) {
                                updated = updated.withGav(updated.getGav().withArtifactId(newArtifactId));
                            }
                            if (!StringUtils.isBlank(newVersion)) {
                                String resolvedVersion;
                                try {
                                    resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                            .select(new GroupArtifact(updated.getGroupId(), updated.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                                } catch (MavenDownloadingException e) {
                                    return e.warn(m);
                                }
                                if (resolvedVersion != null && !resolvedVersion.equals(updated.getVersion())) {
                                    updated = updated.withGav(updated.getGav().withVersion(resolvedVersion));
                                }
                            }
                            if (original != updated) {
                                // Always preserve GString structure, only update the literal prefix
                                String oldGav = original.getGroupId() + ":" + original.getArtifactId();
                                String newGav = updated.getGroupId() + ":" + updated.getArtifactId();
                                String oldValue = (String) literal.getValue();
                                String updatedValue = oldValue.replace(oldGav, newGav);
                                J.Literal updatedLiteral = literal.withValue(updatedValue).withValueSource(updatedValue);
                                m = m.withArguments(singletonList(
                                        gstring.withStrings(ListUtils.mapFirst(strings, s -> updatedLiteral))
                                ));
                                // If version was resolved, schedule property update
                                if (updated.getVersion() != null && !Objects.equals(original.getVersion(), updated.getVersion())) {
                                    String varName = extractVersionVariableName(strings);
                                    if (varName != null) {
                                        pendingPropertyUpdates.put(varName, updated.getVersion());
                                    }
                                }
                            }
                        }
                    }
                } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                    G.MapEntry groupEntry = null;
                    G.MapEntry artifactEntry = null;
                    G.MapEntry versionEntry = null;
                    String groupId = null;
                    String artifactId = null;
                    String version = null;

                    for (Expression e : depArgs) {
                        if (!(e instanceof G.MapEntry)) {
                            continue;
                        }
                        G.MapEntry arg = (G.MapEntry) e;
                        if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                            continue;
                        }
                        J.Literal key = (J.Literal) arg.getKey();
                        J.Literal value = (J.Literal) arg.getValue();
                        if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                            continue;
                        }
                        String keyValue = (String) key.getValue();
                        String valueValue = (String) value.getValue();
                        switch (keyValue) {
                            case "group":
                                groupEntry = arg;
                                groupId = valueValue;
                                break;
                            case "name":
                                artifactEntry = arg;
                                artifactId = valueValue;
                                break;
                            case "version":
                                versionEntry = arg;
                                version = valueValue;
                                break;
                        }
                    }
                    if (groupId == null || artifactId == null) {
                        return m;
                    }
                    if (!depMatcher.matches(groupId, artifactId)) {
                        return m;
                    }
                    String updatedGroupId = groupId;
                    if (!StringUtils.isBlank(newGroupId) && !updatedGroupId.equals(newGroupId)) {
                        updatedGroupId = newGroupId;
                    }
                    String updatedArtifactId = artifactId;
                    if (!StringUtils.isBlank(newArtifactId) && !updatedArtifactId.equals(newArtifactId)) {
                        updatedArtifactId = newArtifactId;
                    }
                    String updatedVersion = version;
                    if (!StringUtils.isBlank(newVersion) && (!StringUtils.isBlank(version) || Boolean.TRUE.equals(overrideManagedVersion))) {
                        String resolvedVersion;
                        try {
                            resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(updatedGroupId, updatedArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                        if (resolvedVersion != null && !resolvedVersion.equals(updatedVersion)) {
                            updatedVersion = resolvedVersion;
                        }
                    }

                    if (!updatedGroupId.equals(groupId) || !updatedArtifactId.equals(artifactId) || updatedVersion != null && !updatedVersion.equals(version)) {
                        G.MapEntry finalGroup = groupEntry;
                        String finalGroupIdValue = updatedGroupId;
                        G.MapEntry finalArtifact = artifactEntry;
                        String finalArtifactIdValue = updatedArtifactId;
                        G.MapEntry finalVersion = versionEntry;
                        String finalVersionValue = updatedVersion;
                        m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                            if (arg == finalGroup) {
                                return finalGroup.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalGroup.getValue(), finalGroupIdValue));
                            }
                            if (arg == finalArtifact) {
                                return finalArtifact.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalArtifact.getValue(), finalArtifactIdValue));
                            }
                            if (arg == finalVersion) {
                                return finalVersion.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalVersion.getValue(), finalVersionValue));
                            }
                            return arg;
                        }));
                    }
                } else if (m.getArguments().get(0) instanceof G.MapLiteral) {
                    G.MapLiteral map = (G.MapLiteral) depArgs.get(0);
                    G.MapEntry groupEntry = null;
                    G.MapEntry artifactEntry = null;
                    G.MapEntry versionEntry = null;
                    String groupId = null;
                    String artifactId = null;
                    String version = null;

                    for (G.MapEntry arg : map.getElements()) {
                        if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                            continue;
                        }
                        J.Literal key = (J.Literal) arg.getKey();
                        J.Literal value = (J.Literal) arg.getValue();
                        if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                            continue;
                        }
                        String keyValue = (String) key.getValue();
                        String valueValue = (String) value.getValue();
                        switch (keyValue) {
                            case "group":
                                groupEntry = arg;
                                groupId = valueValue;
                                break;
                            case "name":
                                artifactEntry = arg;
                                artifactId = valueValue;
                                break;
                            case "version":
                                versionEntry = arg;
                                version = valueValue;
                                break;
                        }
                    }
                    if (groupId == null || artifactId == null) {
                        return m;
                    }
                    if (!depMatcher.matches(groupId, artifactId)) {
                        return m;
                    }
                    String updatedGroupId = groupId;
                    if (!StringUtils.isBlank(newGroupId) && !updatedGroupId.equals(newGroupId)) {
                        updatedGroupId = newGroupId;
                    }
                    String updatedArtifactId = artifactId;
                    if (!StringUtils.isBlank(newArtifactId) && !updatedArtifactId.equals(newArtifactId)) {
                        updatedArtifactId = newArtifactId;
                    }
                    String updatedVersion = version;
                    if (!StringUtils.isBlank(newVersion) && (!StringUtils.isBlank(version) || Boolean.TRUE.equals(overrideManagedVersion))) {
                        String resolvedVersion;
                        try {
                            resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(updatedGroupId, updatedArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                        if (resolvedVersion != null && !resolvedVersion.equals(updatedVersion)) {
                            updatedVersion = resolvedVersion;
                        }
                    }

                    if (!updatedGroupId.equals(groupId) || !updatedArtifactId.equals(artifactId) || updatedVersion != null && !updatedVersion.equals(version)) {
                        G.MapEntry finalGroup = groupEntry;
                        String finalGroupIdValue = updatedGroupId;
                        G.MapEntry finalArtifact = artifactEntry;
                        String finalArtifactIdValue = updatedArtifactId;
                        G.MapEntry finalVersion = versionEntry;
                        String finalVersionValue = updatedVersion;
                        m = m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                            G.MapLiteral mapLiteral = (G.MapLiteral) arg;
                            return mapLiteral.withElements(ListUtils.map(mapLiteral.getElements(), e -> {
                                if (e == finalGroup) {
                                    return finalGroup.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalGroup.getValue(), finalGroupIdValue));
                                }
                                if (e == finalArtifact) {
                                    return finalArtifact.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalArtifact.getValue(), finalArtifactIdValue));
                                }
                                if (e == finalVersion) {
                                    return finalVersion.withValue(ChangeStringLiteral.withStringValue((J.Literal) finalVersion.getValue(), finalVersionValue));
                                }
                                return e;
                            }));
                        }));
                    }
                } else if (m.getArguments().get(0) instanceof J.Assignment) {
                    J.Assignment groupAssignment = null;
                    J.Assignment artifactAssignment = null;
                    J.Assignment versionAssignment = null;
                    String groupId = null;
                    String artifactId = null;
                    String version = null;

                    for (Expression e : depArgs) {
                        if (!(e instanceof J.Assignment)) {
                            continue;
                        }
                        J.Assignment arg = (J.Assignment) e;
                        if (!(arg.getVariable() instanceof J.Identifier) || !(arg.getAssignment() instanceof J.Literal)) {
                            continue;
                        }
                        J.Identifier identifier = (J.Identifier) arg.getVariable();
                        J.Literal assignment = (J.Literal) arg.getAssignment();
                        if (!(assignment.getValue() instanceof String)) {
                            continue;
                        }
                        String valueValue = (String) assignment.getValue();
                        switch (identifier.getSimpleName()) {
                            case "group":
                                groupAssignment = arg;
                                groupId = valueValue;
                                break;
                            case "name":
                                artifactAssignment = arg;
                                artifactId = valueValue;
                                break;
                            case "version":
                                versionAssignment = arg;
                                version = valueValue;
                                break;
                        }
                    }
                    if (groupId == null || artifactId == null) {
                        return m;
                    }
                    if (!depMatcher.matches(groupId, artifactId)) {
                        return m;
                    }
                    String updatedGroupId = groupId;
                    if (!StringUtils.isBlank(newGroupId) && !updatedGroupId.equals(newGroupId)) {
                        updatedGroupId = newGroupId;
                    }
                    String updatedArtifactId = artifactId;
                    if (!StringUtils.isBlank(newArtifactId) && !updatedArtifactId.equals(newArtifactId)) {
                        updatedArtifactId = newArtifactId;
                    }
                    String updatedVersion = version;
                    if (!StringUtils.isBlank(newVersion) && (!StringUtils.isBlank(version) || Boolean.TRUE.equals(overrideManagedVersion))) {
                        String resolvedVersion;
                        try {
                            resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(updatedGroupId, updatedArtifactId), m.getSimpleName(), newVersion, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                        if (resolvedVersion != null && !resolvedVersion.equals(updatedVersion)) {
                            updatedVersion = resolvedVersion;
                        }
                    }

                    if (!updatedGroupId.equals(groupId) || !updatedArtifactId.equals(artifactId) || updatedVersion != null && !updatedVersion.equals(version)) {
                        J.Assignment finalGroup = groupAssignment;
                        String finalGroupIdValue = updatedGroupId;
                        J.Assignment finalArtifact = artifactAssignment;
                        String finalArtifactIdValue = updatedArtifactId;
                        J.Assignment finalVersion = versionAssignment;
                        String finalVersionValue = updatedVersion;
                        m = m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                            if (arg == finalGroup) {
                                return finalGroup.withAssignment(ChangeStringLiteral.withStringValue((J.Literal) finalGroup.getAssignment(), finalGroupIdValue));
                            }
                            if (arg == finalArtifact) {
                                return finalArtifact.withAssignment(ChangeStringLiteral.withStringValue((J.Literal) finalArtifact.getAssignment(), finalArtifactIdValue));
                            }
                            if (arg == finalVersion) {
                                return finalVersion.withAssignment(ChangeStringLiteral.withStringValue((J.Literal) finalVersion.getAssignment(), finalVersionValue));
                            }
                            return arg;
                        }));
                    }
                } else if (depArgs.get(0) instanceof K.StringTemplate) {
                    K.StringTemplate template = (K.StringTemplate) depArgs.get(0);
                    List<J> strings = template.getStrings();
                    if (strings.size() >= 2 && strings.get(0) instanceof J.Literal &&
                            ((J.Literal) strings.get(0)).getValue() != null) {

                        J.Literal literal = (J.Literal) strings.get(0);
                        Dependency original = DependencyNotation.parse((String) requireNonNull(literal.getValue()));
                        if (original != null) {
                            Dependency updated = original;
                            if (!StringUtils.isBlank(newGroupId) && !Objects.equals(updated.getGroupId(), newGroupId)) {
                                updated = updated.withGav(updated.getGav().withGroupId(newGroupId));
                            }
                            if (!StringUtils.isBlank(newArtifactId) && !updated.getArtifactId().equals(newArtifactId)) {
                                updated = updated.withGav(updated.getGav().withArtifactId(newArtifactId));
                            }
                            if (!StringUtils.isBlank(newVersion)) {
                                String resolvedVersion;
                                try {
                                    resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                            .select(new GroupArtifact(updated.getGroupId(), updated.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                                } catch (MavenDownloadingException e) {
                                    return e.warn(m);
                                }
                                if (resolvedVersion != null && !resolvedVersion.equals(updated.getVersion())) {
                                    updated = updated.withGav(updated.getGav().withVersion(resolvedVersion));
                                }
                            }
                            if (original != updated) {
                                // Always preserve StringTemplate structure, only update the literal prefix
                                String oldGav = original.getGroupId() + ":" + original.getArtifactId();
                                String newGav = updated.getGroupId() + ":" + updated.getArtifactId();
                                String oldValue = (String) literal.getValue();
                                String updatedValue = oldValue.replace(oldGav, newGav);
                                J.Literal updatedLiteral = literal.withValue(updatedValue).withValueSource(updatedValue);
                                m = m.withArguments(singletonList(
                                        template.withStrings(ListUtils.mapFirst(strings, s -> updatedLiteral))
                                ));
                                // If version was resolved, schedule property update
                                if (updated.getVersion() != null && !Objects.equals(original.getVersion(), updated.getVersion())) {
                                    String varName = extractVersionVariableName(strings);
                                    if (varName != null) {
                                        pendingPropertyUpdates.put(varName, updated.getVersion());
                                    }
                                }
                            }
                        }
                    }
                }

                return m;
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

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof Properties.File && !pendingPropertyUpdates.isEmpty()) {
                    return new PropertiesVisitor<ExecutionContext>() {
                        @Override
                        public Properties.Entry visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                            String version = pendingPropertyUpdates.get(entry.getKey());
                            if (version != null && !version.equals(entry.getValue().getText())) {
                                return entry.withValue(entry.getValue().withText(version));
                            }
                            return entry;
                        }
                    }.visit(tree, ctx);
                }
                return gradleVisitor.visit(tree, ctx);
            }
        };
    }

    private static @Nullable String extractVersionVariableName(List<J> strings) {
        if (strings.size() >= 2) {
            J versionPart = strings.get(strings.size() - 1);
            // Groovy GString: strings[1] is G.GString.Value wrapping J.Identifier
            if (versionPart instanceof G.GString.Value) {
                J tree = ((G.GString.Value) versionPart).getTree();
                if (tree instanceof J.Identifier) {
                    return ((J.Identifier) tree).getSimpleName();
                }
            }
            // Kotlin StringTemplate: strings[1] is K.StringTemplate.Expression wrapping J.Identifier
            if (versionPart instanceof K.StringTemplate.Expression) {
                J tree = ((K.StringTemplate.Expression) versionPart).getTree();
                if (tree instanceof J.Identifier) {
                    return ((J.Identifier) tree).getSimpleName();
                }
            }
        }
        return null;
    }

}
