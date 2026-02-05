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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Validated;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class AddDependencyVisitor extends MavenIsoVisitor<ExecutionContext> {
    private static final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");

    private final String groupId;
    private final String artifactId;
    private final String version;

    @Nullable
    private final String versionPattern;

    @Nullable
    private final String scope;

    @Nullable
    private final Boolean releasesOnly;

    @Nullable
    private final String type;

    @Nullable
    private final String classifier;

    @Nullable
    private final Boolean optional;

    @Nullable
    private final Pattern familyRegex;

    @Nullable
    private final MavenMetadataFailures metadataFailures;

    @Nullable
    private VersionComparator versionComparator;

    @Nullable
    private String resolvedVersion;

    public AddDependencyVisitor(String groupId, String artifactId, String version,
                                @Nullable String versionPattern, @Nullable String scope,
                                @Nullable Boolean releasesOnly, @Nullable String type,
                                @Nullable String classifier, @Nullable Boolean optional,
                                @Nullable Pattern familyRegex) {
        this(groupId, artifactId, version, versionPattern, scope, releasesOnly, type,
                classifier, optional, familyRegex, null);
    }

    @Override
    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
        if (isDependencyTag()) {
            ResolvedPom resolvedPom = getResolutionResult().getPom();
            String existingGroupId = resolvedPom.getValue(tag.getChildValue("groupId").orElse(null));
            String existingArtifactId = resolvedPom.getValue(tag.getChildValue("artifactId").orElse(null));
            if (groupId.equals(existingGroupId) && artifactId.equals(existingArtifactId)) {
                Scope requestedScope = Scope.fromName(scope);
                Scope existingScope = Scope.fromName(resolvedPom.getValue(tag.getChildValue("scope").orElse(null)));
                if (tag.getMarkers().getMarkers().stream()
                        .anyMatch(m -> m instanceof Markup.Warn &&
                                ((Markup.Warn) m).getDetail().startsWith("org.openrewrite.maven.MavenDownloadingException"))
                ) {
                    getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "existingDependencyFailure", true);
                } else if (requestedScope != existingScope &&
                        // Scope reduction
                        ((existingScope.isInClasspathOf(requestedScope) &&
                                Scope.maxPrecedence(existingScope, requestedScope) == existingScope) ||
                                // System / Import / Invalid which we don't support changing to
                                (!existingScope.isInClasspathOf(requestedScope) &&
                                        !requestedScope.isInClasspathOf(existingScope) &&
                                        Scope.Test != requestedScope &&
                                        Scope.maxPrecedence(Scope.Test, requestedScope) == Scope.Test))
                ) {
                    getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "doNotAlterDependency", true);
                } else {
                    String versionToUse = null;
                    // Use fresh managed version check to handle stale markers from earlier recipe modifications
                    String managedVersion = getFreshManagedVersion(groupId, artifactId, type, classifier, executionContext);
                    if (managedVersion == null || (versionComparator != null && !versionComparator.isValid(version, managedVersion))) {
                        versionToUse = tryGetFamilyVersion();
                        if (versionToUse == null) {
                            try {
                                versionToUse = findVersionToUse(executionContext);
                            } catch (MavenDownloadingException e) {
                                return e.warn(tag);
                            }
                        }
                    }
                    if (versionToUse != null) {
                        getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "requestedVersionChange", true);
                        getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "newResolvedVersion", versionToUse);
                    }
                    if (requestedScope != existingScope) {
                        getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "requestedScopeChange", true);
                        getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "oldScope", existingScope);
                    }
                }
                return tag;
            }
        }
        return super.visitTag(tag, executionContext);
    }


    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
        Validated<VersionComparator> versionValidation = Semver.validate(version, versionPattern);
        if (versionValidation.isValid()) {
            versionComparator = versionValidation.getValue();
        }

        Xml.Document maven = super.visitDocument(document, executionContext);

        if (getCursor().getMessage("doNotAlterDependency", false) ||
                getCursor().getMessage("existingDependencyFailure", false)
        ) {
            return document;
        }

        boolean requestedVersionChange = getCursor().getMessage("requestedVersionChange", false);
        Scope resolvedScope = Scope.fromName(scope);
        Map<Scope, List<ResolvedDependency>> dependencies = getResolutionResult().getDependencies();
        if (dependencies.containsKey(resolvedScope)) {
            for (ResolvedDependency d : dependencies.get(resolvedScope)) {
                if (d.isDirect() && groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                    if (requestedVersionChange) {
                        checkVersionUpdate(d.getVersion());
                    }
                    return maven;
                }
            }

        }

        Xml.Tag root = maven.getRoot();
        if (!root.getChild("dependencies").isPresent()) {
            doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<dependencies/>"),
                    new MavenTagInsertionComparator(root.getContent() == null ? emptyList() : root.getContent())));
        }

        boolean isUpdating = false;
        if (getCursor().getMessage("requestedScopeChange", false)) {
            isUpdating = true;
            Scope oldScope = getCursor().getMessage("oldScope");
            if (dependencies.containsKey(oldScope)) {
                for (ResolvedDependency d : dependencies.get(oldScope)) {
                    if (d.isDirect() && groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                        if (requestedVersionChange) {
                            checkVersionUpdate(d.getVersion());
                        }
                        doAfterVisit(new ChangeDependencyScope(groupId, artifactId, scope).getVisitor());
                        maybeUpdateModel();
                        break;
                    }
                }
            } else { // Going from System / Import / Invalid to something else
                ResolvedPom resolvedPom = getResolutionResult().getPom();
                for (Dependency d : resolvedPom.getRequestedDependencies()) {
                    if (groupId.equals(resolvedPom.getValue(d.getGroupId())) && artifactId.equals(resolvedPom.getValue(d.getArtifactId()))) {
                        // This is run in this order because `ChangeDependencyScope` can end up moving a dependency from just requested to an actual dependency
                        // whereas updating the version relies on something being in the dependencies
                        doAfterVisit(new ChangeDependencyScope(groupId, artifactId, scope).getVisitor());
                        maybeUpdateModel();
                        if (requestedVersionChange && d.getVersion() != null) {
                            checkVersionUpdate(requireNonNull(resolvedPom.getValue(d.getVersion())));
                            maybeUpdateModel();
                        }
                        break;
                    }
                }

            }
        }

        if (!isUpdating) {
            doAfterVisit(new InsertDependencyInOrder(scope));
        }

        return maven;
    }

    @RequiredArgsConstructor
    private class InsertDependencyInOrder extends MavenVisitor<ExecutionContext> {

        @Nullable
        private final String scope;

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (DEPENDENCIES_MATCHER.matches(getCursor())) {
                String versionToUse = null;
                // Use fresh managed version check to handle stale markers from earlier recipe modifications
                String managedVersion = getFreshManagedVersion(groupId, artifactId, type, classifier, ctx);
                boolean scheduleVersionUpgrade = false;
                if (managedVersion == null) {
                    versionToUse = tryGetFamilyVersion();
                    if (versionToUse == null) {
                        try {
                            versionToUse = findVersionToUse(ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                } else if (versionComparator != null && !versionComparator.isValid(version, managedVersion)) {
                    scheduleVersionUpgrade = true;
                }

                Xml.Tag dependencyTag = Xml.Tag.build(
                        "\n<dependency>\n" +
                        "<groupId>" + groupId + "</groupId>\n" +
                        "<artifactId>" + artifactId + "</artifactId>\n" +
                        (versionToUse == null ? "" :
                                "<version>" + versionToUse + "</version>\n") +
                        (classifier == null ? "" :
                                "<classifier>" + classifier + "</classifier>\n") +
                        (type == null || "jar".equals(type) ? "" :
                                "<type>" + type + "</type>\n") +
                        (scope == null || "compile".equals(scope) ? "" :
                                "<scope>" + scope + "</scope>\n") +
                        (Boolean.TRUE.equals(optional) ? "<optional>true</optional>\n" : "") +
                        "</dependency>"
                );

                doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag, new InsertDependencyComparator(tag.getContent() == null ? emptyList() : tag.getContent(), dependencyTag)));
                maybeUpdateModel();
                if (scheduleVersionUpgrade) {
                    doAfterVisit(new UpgradeDependencyVersion(groupId, artifactId, version, versionPattern, true, null).getVisitor());
                }

                return tag;
            }

            return super.visitTag(tag, ctx);
        }
    }

    private @Nullable String tryGetFamilyVersion() {
        if (familyRegex != null) {
            return findDependencies(d -> familyRegex.matcher(d.getGroupId()).matches()).stream()
                    .max(Comparator.comparing(d -> new Version(d.getVersion())))
                    .map(d -> d.getRequested().getVersion())
                    .orElse(null);
        }
        return null;
    }

    private void checkVersionUpdate( String existingVersion) {
        String newResolvedVersion = requireNonNull(getCursor().getMessage("newResolvedVersion"));
        if (!existingVersion.equals(getResolutionResult().getPom().getValue(newResolvedVersion))) {
            doAfterVisit(new UpgradeDependencyVersion(groupId, artifactId, newResolvedVersion, versionPattern, true, null).getVisitor());
        }
    }

    private String findVersionToUse(ExecutionContext ctx) throws MavenDownloadingException {
        if (resolvedVersion == null) {
            if (versionComparator == null || versionComparator instanceof ExactVersion) {
                resolvedVersion = version;
            } else {
                MavenMetadata mavenMetadata = metadataFailures == null ?
                        downloadMetadata(groupId, artifactId, ctx) :
                        metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                // TODO This is hacky, but the class structure of LatestRelease is suboptimal, see https://github.com/openrewrite/rewrite/pull/5029
                // Fix it when we have a chance to refactor the code.
                if ("LatestRelease".equals(versionComparator.getClass().getSimpleName()) && mavenMetadata.getVersioning().getRelease() != null) {
                    return mavenMetadata.getVersioning().getRelease();
                }
                LatestRelease latest = new LatestRelease(versionPattern);
                resolvedVersion = mavenMetadata.getVersioning().getVersions().stream()
                        .filter(v -> versionComparator.isValid(null, v))
                        .filter(v -> !Boolean.TRUE.equals(releasesOnly) || latest.isValid(null, v))
                        .max((v1, v2) -> versionComparator.compare(null, v1, v2))
                        .orElse(version);
            }
        }

        return resolvedVersion;
    }

    /**
     * Check if a dependency is managed by the parent chain, re-resolving the parent if needed
     * to account for modifications made by earlier recipes in the same run.
     * <p>
     * This addresses the stale marker issue where the MavenResolutionResult marker on a POM
     * contains dependency management data from parse time, which may be outdated if a parent
     * POM was modified by an earlier recipe (e.g., UpgradeDependencyVersion upgrading a BOM).
     *
     * @param groupId    The dependency groupId to check
     * @param artifactId The dependency artifactId to check
     * @param type       The dependency type (may be null)
     * @param classifier The dependency classifier (may be null)
     * @param ctx        The execution context
     * @return The managed version if found, or null if not managed
     */
    private @Nullable String getFreshManagedVersion(String groupId, String artifactId,
                                                    @Nullable String type, @Nullable String classifier,
                                                    ExecutionContext ctx) {
        MavenResolutionResult mrr = getResolutionResult();
        ResolvedPom pom = mrr.getPom();

        // First check the marker's cached data (fast path)
        String cachedVersion = pom.getManagedVersion(groupId, artifactId, type, classifier);
        if (cachedVersion != null) {
            return cachedVersion;
        }

        // If not found in cache, check if any parent in the chain has fresh dependency management
        // that manages this dependency. This handles the case where a parent POM was modified
        // by an earlier recipe (e.g., UpgradeDependencyVersion upgrading a BOM).
        return getFreshManagedVersionFromParentChain(mrr, groupId, artifactId, type, classifier, ctx);
    }

    /**
     * Walk up the parent chain via markers and check if any parent manages this dependency.
     * For project POMs that were modified during this recipe run, we check the fresh resolved
     * data stored in execution context by UpdateMavenModel.
     * For external POMs, we download fresh copies.
     */
    private @Nullable String getFreshManagedVersionFromParentChain(
            MavenResolutionResult current,
            String groupId, String artifactId,
            @Nullable String type, @Nullable String classifier,
            ExecutionContext ctx) {

        MavenResolutionResult parent = current.getParent();
        if (parent == null) {
            return null;
        }

        ResolvedPom parentPom = parent.getPom();
        ResolvedGroupArtifactVersion parentGav = parentPom.getGav();

        // Check if parent is a project POM (has a source path, meaning it could have been modified)
        if (parentPom.getRequested().getSourcePath() != null) {
            // First check if there's a fresh version in execution context
            // (set by UpdateMavenModel when the parent POM was modified)
            String gavKey = parentGav.getGroupId() + ":" + parentGav.getArtifactId() + ":" + parentGav.getVersion();
            Map<String, ResolvedPom> freshPoms = UpdateMavenModel.getFreshResolvedPoms(ctx);
            ResolvedPom freshParentPom = freshPoms.get(gavKey);
            if (freshParentPom != null) {
                String managedVersion = freshParentPom.getManagedVersion(groupId, artifactId, type, classifier);
                if (managedVersion != null) {
                    return managedVersion;
                }
            } else {
                // No fresh version, use the marker's cached data
                String managedVersion = parentPom.getManagedVersion(groupId, artifactId, type, classifier);
                if (managedVersion != null) {
                    return managedVersion;
                }
            }
        } else {
            // For external POMs, try to download fresh copy
            try {
                MavenPomDownloader downloader = new MavenPomDownloader(emptyMap(), ctx,
                        getResolutionResult().getMavenSettings(), getResolutionResult().getActiveProfiles());
                GroupArtifactVersion gav = new GroupArtifactVersion(
                        parentGav.getGroupId(), parentGav.getArtifactId(), parentGav.getVersion());
                Pom freshParentPom = downloader.download(gav, null,
                        current.getPom(), current.getPom().getRepositories());
                ResolvedPom resolvedParent = freshParentPom.resolve(
                        getResolutionResult().getActiveProfiles(), downloader, ctx);
                String managedVersion = resolvedParent.getManagedVersion(groupId, artifactId, type, classifier);
                if (managedVersion != null) {
                    return managedVersion;
                }
            } catch (MavenDownloadingException e) {
                // Fall through to continue up the chain
            }
        }

        // Continue up the parent chain
        return getFreshManagedVersionFromParentChain(parent, groupId, artifactId, type, classifier, ctx);
    }
}
