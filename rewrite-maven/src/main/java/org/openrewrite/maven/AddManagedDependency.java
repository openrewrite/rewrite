/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.InlineMe;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.trait.MavenDependency;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddManagedDependency extends ScanningRecipe<AddManagedDependency.Scanned> {
    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

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
            description = "The reason for adding the managed dependency. This will be added as an XML comment preceding the managed dependency.",
            required = false,
            example = "CVE-2021-1234")
    @Nullable
    String because;

    @Deprecated
    @InlineMe(replacement = "this(groupId, artifactId, version, scope, type, classifier, versionPattern, releasesOnly, onlyIfUsing, addToRootPom, null)")
    public AddManagedDependency(String groupId,
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
    public AddManagedDependency(String groupId,
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
        Validated<Object> validated = super.validate();
        //noinspection ConstantConditions
        if (version != null) {
            validated = validated.or(Semver.validate(version, versionPattern));
        }
        if (!StringUtils.isNullOrEmpty(onlyIfUsing)) {
            validated = validated.and(Validated.test("onlyIfUsing", "invalid group:artifact glob pattern", onlyIfUsing, s -> {
                try {
                    return onlyIfUsing.matches("[\\w.-]+\\*?:([\\w-]+\\*?|\\*)");
                } catch (Throwable t) {
                    return false;
                }
            }));
        }
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Add managed Maven dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Add a managed Maven dependency to a `pom.xml` file.";
    }

    public static class Scanned {
        boolean usingType;
        List<SourceFile> rootPoms = new ArrayList<>();
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        Scanned scanned = new Scanned();
        scanned.usingType = onlyIfUsing == null;
        return scanned;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return Preconditions.check(acc.usingType || (!StringUtils.isNullOrEmpty(onlyIfUsing) && onlyIfUsing.contains(":")), new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                document.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(mavenResolutionResult -> {
                    if (mavenResolutionResult.getParent() == null) {
                        acc.rootPoms.add(document);
                    }
                });
                if (acc.usingType) {
                    return document;
                }
                super.visitDocument(document, ctx);
                return document;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isDependencyTag()) {
                    ResolvedDependency dependency = findDependency(t, null);
                    if (dependency != null) {
                        String[] ga = requireNonNull(onlyIfUsing).split(":");
                        ResolvedDependency match = dependency.findDependency(ga[0], ga[1]);
                        if (match != null) {
                            acc.usingType = true;
                        }
                    }
                }

                return t;
            }
        });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(acc.usingType, new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml maven = super.visitDocument(document, ctx);

                if (!Boolean.TRUE.equals(addToRootPom) || acc.rootPoms.contains(document)) {
                    ResolvedPom pom = getResolutionResult().getPom();
                    String convertedVersion = pom.getValue(version);
                    if (convertedVersion == null) {
                        return maven;
                    }
                    String convertedGroup = requireNonNull(pom.getValue(groupId));
                    String convertedArtifact = requireNonNull(pom.getValue(artifactId));
                    Validated<VersionComparator> versionValidation = Semver.validate(convertedVersion, versionPattern);
                    if (versionValidation.isValid()) {
                        VersionComparator versionComparator = requireNonNull(versionValidation.getValue());
                        try {
                            // The version of the dependency currently in use (if any) might influence the version comparator
                            // For example, "latest.patch" gives very different results depending on the version in use
                            String currentVersion = getResolutionResult().findDependencies(convertedGroup, convertedArtifact, Scope.fromName(scope)).stream()
                                    .map(ResolvedDependency::getVersion)
                                    .findFirst()
                                    .orElse(existingManagedDependencyVersion());
                            String versionToUse = MavenDependency.findNewerVersion(convertedGroup, convertedArtifact, currentVersion, getResolutionResult(), metadataFailures, versionComparator, ctx);

                            // Prevent downgrades
                            if (currentVersion != null && versionToUse != null &&
                                    versionComparator.compare(null, currentVersion, versionToUse) >= 0) {
                                return maven;
                            }

                            if (versionToUse != null && !versionToUse.equals(pom.getValue(existingManagedDependencyVersion()))) {
                                if (ResolvedPom.placeholderHelper.hasPlaceholders(version) && Objects.equals(convertedVersion, versionToUse)) {
                                    // revert back to the original version if the version has a placeholder
                                    versionToUse = version;
                                }
                                doAfterVisit(new AddManagedDependencyVisitor(groupId, artifactId,
                                        versionToUse, scope, type, classifier, because));
                                maybeUpdateModel();
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(document);
                        }
                    }
                }

                return maven;
            }

            private @Nullable String existingManagedDependencyVersion() {
                return getResolutionResult().getPom().getDependencyManagement().stream()
                        .map(resolvedManagedDep -> {
                            if (resolvedManagedDep.matches(groupId, artifactId, type, classifier)) {
                                return resolvedManagedDep.getGav().getVersion();
                            } else if (resolvedManagedDep.getRequestedBom() != null &&
                                       resolvedManagedDep.getRequestedBom().getGroupId().equals(groupId) &&
                                       resolvedManagedDep.getRequestedBom().getArtifactId().equals(artifactId)) {
                                return resolvedManagedDep.getRequestedBom().getVersion();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
            }
        });
    }
}
