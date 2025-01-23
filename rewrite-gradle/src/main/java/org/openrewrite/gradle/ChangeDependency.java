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
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;

import java.util.*;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependency extends Recipe {
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

    /**
     * Keeping this constructor just for compatibility purposes
     *
     * @deprecated Use {@link ChangeDependency#ChangeDependency(String, String, String, String, String, String, Boolean)}
     */
    @Deprecated
    public ChangeDependency(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern) {
        this(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern, null);
    }

    @JsonCreator
    public ChangeDependency(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean overrideManagedVersion) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.overrideManagedVersion = overrideManagedVersion;
    }

    @Override
    public String getDisplayName() {
        return "Change Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", oldGroupId, oldArtifactId);
    }

    @Override
    public String getDescription() {
        return "Change a Gradle dependency coordinates. The `newGroupId` or `newArtifactId` **MUST** be different from before.";
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
        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(), new GroovyIsoVisitor<ExecutionContext>() {
            final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());

            GradleProject gradleProject;

            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
                if (!maybeGp.isPresent()) {
                    return cu;
                }

                gradleProject = maybeGp.get();

                G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);
                if (g != cu) {
                    g = g.withMarkers(g.getMarkers().setByType(updateGradleModel(gradleProject)));
                }
                return g;
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
                if (depArgs.get(0) instanceof J.Literal || depArgs.get(0) instanceof G.GString || depArgs.get(0) instanceof G.MapEntry || depArgs.get(0) instanceof G.MapLiteral) {
                    m = updateDependency(m, ctx);
                } else if (depArgs.get(0) instanceof J.MethodInvocation &&
                           (((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("platform") ||
                            ((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("enforcedPlatform"))) {
                    m = m.withArguments(ListUtils.mapFirst(depArgs, platform -> updateDependency((J.MethodInvocation) platform, ctx)));
                }

                return m;
            }

            private J.MethodInvocation updateDependency(J.MethodInvocation m, ExecutionContext ctx) {
                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof J.Literal) {
                    String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                    if (gav != null) {
                        Dependency original = DependencyStringNotationConverter.parse(gav);
                        if (original != null) {
                            Dependency updated = original;
                            if (!StringUtils.isBlank(newGroupId) && !updated.getGroupId().equals(newGroupId)) {
                                updated = updated.withGroupId(newGroupId);
                            }
                            if (!StringUtils.isBlank(newArtifactId) && !updated.getArtifactId().equals(newArtifactId)) {
                                updated = updated.withArtifactId(newArtifactId);
                            }
                            if (!StringUtils.isBlank(newVersion) && (!StringUtils.isBlank(original.getVersion()) || Boolean.TRUE.equals(overrideManagedVersion))) {
                                String resolvedVersion;
                                try {
                                    resolvedVersion = new DependencyVersionSelector(null, gradleProject, null)
                                            .select(new GroupArtifact(updated.getGroupId(), updated.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                                } catch (MavenDownloadingException e) {
                                    return e.warn(m);
                                }
                                if (resolvedVersion != null && !resolvedVersion.equals(updated.getVersion())) {
                                    updated = updated.withVersion(resolvedVersion);
                                }
                            }
                            if (original != updated) {
                                String replacement = updated.toStringNotation();
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
                        Dependency original = DependencyStringNotationConverter.parse((String) requireNonNull(literal.getValue()));
                        if (original != null) {
                            Dependency updated = original;
                            if (!StringUtils.isBlank(newGroupId) && !updated.getGroupId().equals(newGroupId)) {
                                updated = updated.withGroupId(newGroupId);
                            }
                            if (!StringUtils.isBlank(newArtifactId) && !updated.getArtifactId().equals(newArtifactId)) {
                                updated = updated.withArtifactId(newArtifactId);
                            }
                            if (!StringUtils.isBlank(newVersion)) {
                                String resolvedVersion;
                                try {
                                    resolvedVersion = new DependencyVersionSelector(null, gradleProject, null)
                                            .select(new GroupArtifact(updated.getGroupId(), updated.getArtifactId()), m.getSimpleName(), newVersion, versionPattern, ctx);
                                } catch (MavenDownloadingException e) {
                                    return e.warn(m);
                                }
                                if (resolvedVersion != null && !resolvedVersion.equals(updated.getVersion())) {
                                    updated = updated.withVersion(resolvedVersion);
                                }
                            }
                            if (original != updated) {
                                String replacement = updated.toStringNotation();
                                J.Literal newLiteral = literal.withValue(replacement)
                                        .withValueSource(gstring.getDelimiter() + replacement + gstring.getDelimiter());
                                m = m.withArguments(Collections.singletonList(newLiteral));
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
                            resolvedVersion = new DependencyVersionSelector(null, gradleProject, null)
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
                            resolvedVersion = new DependencyVersionSelector(null, gradleProject, null)
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
                }

                return m;
            }

            private GradleProject updateGradleModel(GradleProject gp) {
                Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
                Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
                boolean anyChanged = false;
                for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                    GradleDependencyConfiguration newGdc = gdc;
                    newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                        if (depMatcher.matches(requested.getGroupId(), requested.getArtifactId())) {
                            GroupArtifactVersion gav = requested.getGav();
                            if (newGroupId != null) {
                                gav = gav.withGroupId(newGroupId);
                            }
                            if (newArtifactId != null) {
                                gav = gav.withArtifactId(newArtifactId);
                            }
                            if (gav != requested.getGav()) {
                                return requested.withGav(gav);
                            }
                        }
                        return requested;
                    }));
                    newGdc = newGdc.withDirectResolved(ListUtils.map(gdc.getDirectResolved(), resolved -> {
                        if (depMatcher.matches(resolved.getGroupId(), resolved.getArtifactId())) {
                            ResolvedGroupArtifactVersion gav = resolved.getGav();
                            if (newGroupId != null) {
                                gav = gav.withGroupId(newGroupId);
                            }
                            if (newArtifactId != null) {
                                gav = gav.withArtifactId(newArtifactId);
                            }
                            if (gav != resolved.getGav()) {
                                return resolved.withGav(gav);
                            }
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
