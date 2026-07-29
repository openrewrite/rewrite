/*
 * Copyright 2024 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toCollection;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeTransitiveDependencyVersion extends ScanningRecipe<UpgradeTransitiveDependencyVersion.Accumulator> {

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    String displayName = "Upgrade transitive Maven dependencies";

    String description = "Upgrades the version of a transitive dependency in a Maven pom file. " +
               "Leaves direct dependencies unmodified. " +
               "When the transitive dependency's version is already governed by a plain `<dependencyManagement>` " +
               "entry in the project, that entry is upgraded in place rather than adding a duplicate; otherwise " +
               "(including a version supplied by an imported BOM) a new managed dependency is added. " +
               "Can be paired with the regular Upgrade Dependency Version recipe to upgrade a dependency everywhere, " +
               "regardless of whether it is direct or transitive.";

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.apache.logging.log4j:ARTIFACT_ID:VERSION'.",
            example = "org.apache.logging.log4j")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.apache.logging.log4j:log4j-bom:VERSION'.",
            example = "log4j-bom")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "latest.release")
    String version;

    @Option(displayName = "Scope",
            description = "An optional scope to use for the dependency management tag.",
            example = "import",
            valid = {"import", "runtime", "provided", "test"},
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Type",
            description = "An optional type to use for the dependency management tag.",
            valid = {"jar", "pom", "war"},
            example = "pom",
            required = false)
    @Nullable
    String type;

    @Option(displayName = "Classifier",
            description = "An optional classifier to use for the dependency management tag",
            example = "test",
            required = false)
    @Nullable
    String classifier;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Releases only",
            description = "Whether to exclude snapshots from consideration when using a semver selector",
            required = false)
    @Nullable
    Boolean releasesOnly;

    @Option(displayName = "Only if using glob expression for group:artifact",
            description = "Only add managed dependencies to projects having a dependency matching the expression.",
            example = "org.apache.logging.log4j:log4j*",
            required = false)
    @Nullable
    String onlyIfUsing;

    @Option(displayName = "Add to the root pom",
            description = "Add to the root pom where root is the eldest parent of the pom within the source set.",
            required = false)
    @Nullable
    Boolean addToRootPom;

    @Option(displayName = "Because",
            description = "The reason for upgrading the transitive dependency. This will be added as an XML comment preceding the managed dependency.",
            required = false,
            example = "CVE-2021-1234")
    @Nullable
    String because;

    @Deprecated
    public UpgradeTransitiveDependencyVersion(String groupId,
                                              String artifactId,
                                              String version,
                                              @Nullable String scope,
                                              @Nullable String type,
                                              @Nullable String classifier,
                                              @Nullable String versionPattern,
                                              @Nullable Boolean releasesOnly,
                                              @Nullable String onlyIfUsing,
                                              @Nullable Boolean addToRootPom) {
        this(groupId, artifactId, version, scope, type, classifier, versionPattern, releasesOnly, onlyIfUsing, addToRootPom, null);
    }

    @JsonCreator
    public UpgradeTransitiveDependencyVersion(String groupId,
                                              String artifactId,
                                              String version,
                                              @Nullable String scope,
                                              @Nullable String type,
                                              @Nullable String classifier,
                                              @Nullable String versionPattern,
                                              @Nullable Boolean releasesOnly,
                                              @Nullable String onlyIfUsing,
                                              @Nullable Boolean addToRootPom,
                                              @Nullable String because) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.type = type;
        this.classifier = classifier;
        this.versionPattern = versionPattern;
        this.releasesOnly = releasesOnly;
        this.onlyIfUsing = onlyIfUsing;
        this.addToRootPom = addToRootPom;
        this.because = because;
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Semver.validate(version, versionPattern));
    }

    /**
     * The sub-recipe accumulators plus scan-time governance: {@code governingPoms} (poms with a plain managed entry
     * to update in place) and {@code locallyManagedByPom} (per pom, the matched coordinates it governs, so the
     * visitor skips pinning them without recomputing governance). Keyed per-pom by group:artifact:version - a
     * coordinate managed in one module must still be pinned in a sibling where it is unmanaged.
     */
    public static class Accumulator {
        final AddManagedDependency.Scanned addScanned;
        final UpgradeDependencyVersion.Accumulator upgradeAccumulator;
        final Set<GroupArtifactVersion> governingPoms = new HashSet<>();
        final Map<GroupArtifactVersion, Set<GroupArtifact>> locallyManagedByPom = new HashMap<>();

        Accumulator(AddManagedDependency.Scanned addScanned, UpgradeDependencyVersion.Accumulator upgradeAccumulator) {
            this.addScanned = addScanned;
            this.upgradeAccumulator = upgradeAccumulator;
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(
                addManagedDependency().getInitialValue(ctx),
                upgradeDependencyVersion().getInitialValue(ctx));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        TreeVisitor<?, ExecutionContext> addScanner = addManagedDependency().getScanner(acc.addScanned);
        TreeVisitor<?, ExecutionContext> upgradeScanner = upgradeDependencyVersion().getScanner(acc.upgradeAccumulator);
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (addScanner.isAcceptable(document, ctx)) {
                    addScanner.visit(document, ctx);
                }
                if (upgradeScanner.isAcceptable(document, ctx)) {
                    upgradeScanner.visit(document, ctx);
                }

                MavenResolutionResult mrr = getResolutionResult();
                for (ResolvedDependency dep : mrr.findDependencies(groupId, artifactId, null)) {
                    if (!dep.isTransitive()) {
                        continue;
                    }
                    GroupArtifact ga = new GroupArtifact(dep.getGroupId(), dep.getArtifactId());
                    // Record the pom declaring a plain managed entry so the update path targets it. A BOM-imported
                    // version is left to the pin path instead: overriding the member with a managed entry always
                    // reaches the target, whereas raising a (often framework-coupled) BOM may not; callers wanting
                    // the BOM raised use UpgradeDependencyVersion directly.
                    VersionGovernance governance = VersionGovernance.of(mrr, ga);
                    if (isLocalManaged(governance)) {
                        acc.governingPoms.add(governance.getGoverningPom().asGroupArtifactVersion());
                        acc.locallyManagedByPom.computeIfAbsent(mrr.getPom().getGav().asGroupArtifactVersion(), k -> new HashSet<>()).add(ga);
                    }
                }
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                Xml.Document d = document;

                // Where this pom declares the governing entry, upgrade it in place. A dead entry in a different pom
                // is never in scope here, so it is left untouched.
                if (acc.governingPoms.contains(mrr.getPom().getGav().asGroupArtifactVersion())) {
                    d = (Xml.Document) upgradeDependencyVersion().getVisitor(acc.upgradeAccumulator).visitNonNull(d, ctx);
                }

                // Pin the remaining transitive dependencies, skipping any governed by a plain managed entry in THIS
                // pom's resolution (handled by the update path above). The skip is per-pom: the same coordinate may
                // be unmanaged in a sibling and must still be pinned there.
                Set<GroupArtifact> managedHere = acc.locallyManagedByPom.getOrDefault(mrr.getPom().getGav().asGroupArtifactVersion(), emptySet());
                Set<ResolvedDependency> matchingDependencies = mrr.findDependencies(groupId, artifactId, null)
                        .stream()
                        .filter(ResolvedDependency::isTransitive)
                        .filter(dep -> !managedHere.contains(new GroupArtifact(dep.getGroupId(), dep.getArtifactId())))
                        .collect(toCollection(LinkedHashSet::new));
                if (matchingDependencies.isEmpty()) {
                    return d;
                }

                // Skip transitive dependencies that a project parent also has,
                // since the parent will get the managed dependency and children will inherit it
                MavenResolutionResult current = mrr;
                while (current.parentPomIsProjectPom()) {
                    MavenResolutionResult parentResult = current.getParent();
                    List<ResolvedDependency> parentTransitiveDeps = parentResult.findDependencies(groupId, artifactId, null);
                    matchingDependencies.removeIf(dep ->
                            parentTransitiveDeps.stream().anyMatch(pd ->
                                    pd.isTransitive() &&
                                    pd.getGroupId().equals(dep.getGroupId()) &&
                                    pd.getArtifactId().equals(dep.getArtifactId())));
                    current = parentResult;
                }

                if (matchingDependencies.isEmpty()) {
                    return d;
                }

                for (ResolvedDependency matchingDependency : matchingDependencies) {
                    d = (Xml.Document) addManagedDependency(matchingDependency.getGroupId(), matchingDependency.getArtifactId())
                            .getVisitor(acc.addScanned)
                            .visitNonNull(d, ctx);
                }
                return d;
            }
        };
    }

    private AddManagedDependency addManagedDependency() {
        return addManagedDependency(groupId, artifactId);
    }

    private AddManagedDependency addManagedDependency(String groupId, String artifactId) {
        return new AddManagedDependency(groupId, artifactId, version, scope, type, classifier, versionPattern, releasesOnly, onlyIfUsing, addToRootPom, because);
    }

    private UpgradeDependencyVersion upgradeDependencyVersion() {
        return new UpgradeDependencyVersion(groupId, artifactId, version, versionPattern, true, null);
    }

    private static boolean isLocalManaged(@Nullable VersionGovernance governance) {
        return governance != null && governance.getKind() == VersionGovernance.Kind.LOCAL_MANAGED;
    }
}
