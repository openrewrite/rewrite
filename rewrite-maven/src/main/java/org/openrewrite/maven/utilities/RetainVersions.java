/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven.utilities;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.AddManagedDependencyVisitor;
import org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.search.FindDependency;
import org.openrewrite.maven.search.FindManagedDependency;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.StringUtils.matchesGlob;

public class RetainVersions {

    /**
     * Validates that every non-blank entry of a GAV list option consists of a group ID, an artifact ID,
     * and optionally a version. Blank entries are tolerated, as they are ignored when the recipe runs.
     */
    public static Validated<Object> validate(String optionName, @Nullable List<String> gavs) {
        Validated<Object> validated = Validated.none();
        if (gavs != null) {
            for (int i = 0; i < gavs.size(); i++) {
                String gav = gavs.get(i);
                if (StringUtils.isBlank(gav)) {
                    continue;
                }
                validated = validated.and(Validated.test(
                        String.format("%s[%d]", optionName, i),
                        "did not look like a two-or-three-part GAV",
                        gav,
                        maybeGav -> {
                            int gavParts = maybeGav.split(":").length;
                            return gavParts == 2 || gavParts == 3;
                        }));
            }
        }
        return validated;
    }

    /**
     * Returns a list of recipes which can be applied to add explicit versions
     * for dependencies matching the GAVs in param `retainVersions`
     */
    public static List<Recipe> plan(MavenVisitor<?> visitor, List<String> retainVersions) {
        return plan(visitor, retainVersions, emptyList(), null);
    }

    /**
     * The explicit versions of {@link #plan(MavenVisitor, List)}, followed by a managed entry for each of
     * {@code forModules} that the pom does not already manage. A dependency without a version that one of those
     * entries manages keeps no version of its own, unless a GAV that names a version matches only dependencies
     * without one, as that GAV sets its version on all of them.
     */
    public static List<TreeVisitor<?, ExecutionContext>> plan(MavenVisitor<?> visitor, List<String> retainVersions,
                                                             List<ResolvedManagedDependency> forModules) {
        Xml.Document document = visitor.getCursor().firstEnclosingOrThrow(Xml.Document.class);
        // AddManagedDependencyVisitor adds nothing for a groupId and artifactId the pom already manages.
        Set<GroupArtifact> addedGroupArtifacts = new HashSet<>();
        List<ResolvedManagedDependency> added = new ArrayList<>();
        for (ResolvedManagedDependency dependency : forModules) {
            if (FindManagedDependency.find(document, dependency.getGroupId(), dependency.getArtifactId()).isEmpty() &&
                addedGroupArtifacts.add(new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId()))) {
                added.add(dependency);
            }
        }
        List<TreeVisitor<?, ExecutionContext>> visitors = new ArrayList<>();
        for (Recipe recipe : plan(visitor, retainVersions, added,
                document.getMarkers().findFirst(MavenResolutionResult.class).map(MavenResolutionResult::getPom).orElse(null))) {
            visitors.add(recipe.getVisitor());
        }
        for (ResolvedManagedDependency dependency : added) {
            visitors.add(new AddManagedDependencyVisitor(dependency.getGroupId(), dependency.getArtifactId(),
                    requireNonNull(dependency.getVersion()), dependency.getScope() == null ? null :
                    dependency.getScope().toString().toLowerCase(), dependency.getType(), dependency.getClassifier(), null));
        }
        return visitors;
    }

    private static List<Recipe> plan(MavenVisitor<?> visitor, List<String> retainVersions,
                                     List<ResolvedManagedDependency> managedHere, @Nullable ResolvedPom pom) {
        List<Recipe> recipes = new ArrayList<>();
        for (String gav : retainVersions) {
            if (StringUtils.isBlank(gav)) {
                continue;
            }
            String[] split = gav.split(":");
            String requestedRetainedGroupId = split[0];
            String requestedRetainedArtifactId = split[1];
            String requestedRetainedVersion = split.length == 3 ? split[2] : null;
            Set<Xml.Tag> existingDependencies = FindDependency.find(
                    visitor.getCursor().firstEnclosingOrThrow(Xml.Document.class),
                    requestedRetainedGroupId, requestedRetainedArtifactId);

            // optimization for glob GAVs: more efficient to use one CDGIAAI recipe if they all will have the same version anyway
            if (requestedRetainedVersion != null && noneMatch(existingDependencies, it -> it.getChild("version").isPresent())) {
                recipes.add(new ChangeDependencyGroupIdAndArtifactId(requestedRetainedGroupId, requestedRetainedArtifactId, null, null,
                        requestedRetainedVersion, null, true, true));
                continue;
            }

            for (Xml.Tag existingDependency : existingDependencies) {
                String retainedGroupId = existingDependency.getChildValue("groupId")
                        .orElseThrow(() -> new IllegalStateException("Dependency tag must have groupId"));
                String retainedArtifactId = existingDependency.getChildValue("artifactId")
                        .orElseThrow(() -> new IllegalStateException("Dependency tag must have artifactId"));
                String retainedVersion = requestedRetainedVersion;

                if (pom != null && !existingDependency.getChildValue("version").isPresent() &&
                    isManagedBy(managedHere, pom, existingDependency)) {
                    continue;
                }
                if (retainedVersion == null) {
                    if (existingDependency.getChildValue("version").isPresent()) {
                        continue;
                    } else {
                        ResolvedManagedDependency managedDependency = visitor.findManagedDependency(
                                retainedGroupId, retainedArtifactId);
                        retainedVersion = Objects.requireNonNull(managedDependency, String.format(
                                "'%s' from 'retainVersions' did not have a version specified and was not in the project's dependency management",
                                gav)).getVersion();

                    }
                }
                recipes.add(new ChangeDependencyGroupIdAndArtifactId(retainedGroupId, retainedArtifactId, null, null,
                        retainedVersion, null, true, true));
            }
        }
        return recipes;
    }

    /**
     * Retain management dropped by an upgraded BOM on behalf of descendant modules. The importing
     * POM owns this management: restoring it there leaves explicit dependency versions and child
     * dependency-management overrides in place.
     */
    public static List<ResolvedManagedDependency> planForModules(MavenResolutionResult importer,
                                                                ResolvedGroupArtifactVersion oldBom,
                                                                ResolvedPom newBom,
                                                                List<String> retainVersions) {
        Map<GroupArtifactClassifierType, ResolvedManagedDependency> retained = new LinkedHashMap<>();
        for (ResolvedManagedDependency managed : importer.getPom().getDependencyManagement()) {
            if (!oldBom.equals(managed.getBomGav()) ||
                newBom.getManagedDependency(managed.getGroupId(), managed.getArtifactId(),
                        managed.getType(), managed.getClassifier()) != null) {
                continue;
            }
            for (String gav : retainVersions) {
                if (StringUtils.isBlank(gav)) {
                    continue;
                }
                String[] parts = gav.split(":");
                if (matchesGlob(managed.getGroupId(), parts[0]) && matchesGlob(managed.getArtifactId(), parts[1]) &&
                    usedByDescendant(importer, managed)) {
                    ResolvedManagedDependency dependency = parts.length == 3 ?
                            managed.withGav(managed.getGav().withVersion(parts[2])) : managed;
                    retained.put(new GroupArtifactClassifierType(managed.getGroupId(), managed.getArtifactId(),
                            managed.getClassifier(), managed.getType()), dependency);
                }
            }
        }
        return new ArrayList<>(retained.values());
    }

    private static boolean isManagedBy(List<ResolvedManagedDependency> managed, ResolvedPom pom, Xml.Tag dependency) {
        String groupId = pom.getValue(dependency.getChildValue("groupId").orElse(null));
        String artifactId = pom.getValue(dependency.getChildValue("artifactId").orElse(null));
        String type = pom.getValue(dependency.getChildValue("type").orElse(null));
        String classifier = pom.getValue(dependency.getChildValue("classifier").orElse(null));
        for (ResolvedManagedDependency entry : managed) {
            if (artifactId != null && entry.matches(groupId, artifactId, type, classifier)) {
                return true;
            }
        }
        return false;
    }

    private static boolean usedByDescendant(MavenResolutionResult parent, ResolvedManagedDependency managed) {
        for (MavenResolutionResult module : parent.getModules()) {
            ResolvedPom pom = module.getPom();
            ResolvedManagedDependency inherited = pom.getManagedDependency(managed.getGroupId(), managed.getArtifactId(),
                    managed.getType(), managed.getClassifier());
            if (inherited == null || !Objects.equals(managed.getBomGav(), inherited.getBomGav())) {
                continue;
            }
            // An equal BOM provenance may come from a local re-import. That import is also
            // upgraded by the recipe and will no longer supply this dependency.
            for (Dependency dependency : pom.getRequested().getDependencies()) {
                if (dependency.getVersion() == null &&
                    managed.matches(pom.getValue(dependency.getGroupId()), pom.getValue(dependency.getArtifactId()),
                            pom.getValue(dependency.getType()), pom.getValue(dependency.getClassifier()))) {
                    return true;
                }
            }
            if (usedByDescendant(module, managed)) {
                return true;
            }
        }
        return false;
    }

    private static <T> boolean noneMatch(Set<T> existingDependencies, Predicate<T> predicate) {
        for (T existingDependency : existingDependencies) {
            if (predicate.test(existingDependency)) {
                return false;
            }
        }
        return true;
    }
}
