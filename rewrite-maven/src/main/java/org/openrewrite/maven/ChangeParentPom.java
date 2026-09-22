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
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddOrUpdateChild;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.maven.RemoveRedundantDependencyVersions.Comparator.GTE;
import static org.openrewrite.maven.tree.Parent.DEFAULT_RELATIVE_PATH;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeParentPom extends ScanningRecipe<ChangeParentPom.Accumulator> {
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Old group ID",
            description = "The group ID of the Maven parent pom to be changed away from.",
            example = "org.springframework.boot")
    String oldGroupId;

    @Option(displayName = "New group ID",
            description = "The group ID of the new maven parent pom to be adopted. If this argument is omitted it defaults to the value of `oldGroupId`.",
            example = "org.springframework.boot",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "Old artifact ID",
            description = "The artifact ID of the maven parent pom to be changed away from.",
            example = "spring-boot-starter-parent")
    String oldArtifactId;

    @Option(displayName = "New artifact ID",
            description = "The artifact ID of the new maven parent pom to be adopted. If this argument is omitted it defaults to the value of `oldArtifactId`.",
            example = "spring-boot-starter-parent",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String newVersion;

    @Option(displayName = "Old relative path",
            description = "The relativePath of the maven parent pom to be changed away from. " +
                          "Use an empty String to match `<relativePath />`, use `../pom.xml` to match the default value.",
            example = "../../pom.xml",
            required = false)
    @Nullable
    String oldRelativePath;

    @Option(displayName = "New relative path",
            description = "New relative path attribute for parent lookup.",
            example = "../pom.xml",
            required = false)
    @Nullable
    String newRelativePath;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Allow version downgrades",
            description = "If the new parent has the same group/artifact, this flag can be used to only upgrade the " +
                          "version if the target version is newer than the current.",
            required = false)
    @Nullable
    Boolean allowVersionDowngrades;

    @Option(displayName = "Except",
            description = "Accepts a list of GAVs that should be retained when calling `RemoveRedundantDependencyVersions`.",
            example = "com.jcraft:jsch",
            required = false)
    @Nullable
    List<String> except;

    String displayName = "Change Maven parent";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", newGroupId, newArtifactId, newVersion);
    }

    String description = "Change the parent pom of a Maven pom.xml by matching the existing parent via groupId and artifactId, " +
                "and updating it to a new groupId, artifactId, version, and optional relativePath. " +
                "Also updates the project to retain dependency management and properties previously inherited from the old parent that are no longer provided by the new parent. " +
                "Removes redundant dependency versions already managed by the new parent.";

    @Deprecated
    public ChangeParentPom(String oldGroupId,
                           @Nullable String newGroupId,
                           String oldArtifactId,
                           @Nullable String newArtifactId,
                           String newVersion,
                           @Nullable String oldRelativePath,
                           @Nullable String newRelativePath,
                           @Nullable String versionPattern,
                           @Nullable Boolean allowVersionDowngrades) {
        this(oldGroupId, newGroupId, oldArtifactId, newArtifactId, newVersion, oldRelativePath, newRelativePath, versionPattern, allowVersionDowngrades, null);
    }

    @JsonCreator
    public ChangeParentPom(String oldGroupId,
                           @Nullable String newGroupId,
                           String oldArtifactId,
                           @Nullable String newArtifactId,
                           String newVersion,
                           @Nullable String oldRelativePath,
                           @Nullable String newRelativePath,
                           @Nullable String versionPattern,
                           @Nullable Boolean allowVersionDowngrades,
                           @Nullable List<String> except) {
        this.oldGroupId = oldGroupId;
        this.newGroupId = newGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.oldRelativePath = oldRelativePath;
        this.newRelativePath = newRelativePath;
        this.versionPattern = versionPattern;
        this.allowVersionDowngrades = allowVersionDowngrades;
        this.except = except;
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        //noinspection ConstantConditions
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    public static class Accumulator {
        /**
         * The re-resolved marker for each pom whose {@code <parent>} is being changed, keyed by that pom's
         * resolved GAV. Only the poms actually being changed are retained here (not every pom in the run, as
         * a prior implementation did); descendants are re-parented onto these in the edit phase without being
         * stored, and one re-resolved marker is shared by all of a changed pom's descendants.
         */
        final Map<ResolvedGroupArtifactVersion, MavenResolutionResult> updatedByPom = new ConcurrentHashMap<>();

        /**
         * The same markers as {@link #updatedByPom}, but also declaring the dependency management that the
         * edit phase restores in the changed pom. Descendants resolve against this view; filled on demand in
         * the edit phase, as it depends on the new parent's own model.
         */
        final Map<ResolvedGroupArtifactVersion, MavenResolutionResult> restoredByPom = new ConcurrentHashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    private Optional<String> findAcceptableVersion(
            List<String> availableVersions, VersionComparator versionComparator,
            String groupId, String artifactId, String currentVersion,
            MavenIsoVisitor<?> visitor, ExecutionContext ctx) throws MavenDownloadingException {
        String finalCurrentVersion = !Semver.isVersion(currentVersion) ? "0.0.0" : currentVersion;

        if (availableVersions.isEmpty()) {
            MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> visitor.downloadMetadata(groupId, artifactId, ctx));
            mavenMetadata.getVersioning().getVersions().stream()
                    .filter(v -> versionComparator.isValid(finalCurrentVersion, v))
                    .filter(v -> Boolean.TRUE.equals(allowVersionDowngrades) || versionComparator.compare(null, finalCurrentVersion, v) <= 0)
                    .forEach(availableVersions::add);
        }
        if (Boolean.TRUE.equals(allowVersionDowngrades)) {
            return availableVersions.stream()
                    .max((v1, v2) -> versionComparator.compare(finalCurrentVersion, v1, v2));
        }
        Optional<String> upgradedVersion = versionComparator.upgrade(finalCurrentVersion, availableVersions);
        if (upgradedVersion.isPresent()) {
            return upgradedVersion;
        }
        return availableVersions.stream().filter(finalCurrentVersion::equals).findFirst();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;

        return new MavenIsoVisitor<ExecutionContext>() {
            final List<String> availableVersions = new ArrayList<>();

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();

                Parent parent = mrr.getPom().getRequested().getParent();
                if (parent != null &&
                    matchesGlob(parent.getArtifactId(), oldArtifactId) &&
                    matchesGlob(parent.getGroupId(), oldGroupId) &&
                    (oldRelativePath == null || matchesGlob(parent.getRelativePath(), oldRelativePath))) {

                    String currentVersion = parent.getGav().getVersion();
                    if (currentVersion == null) {
                        return document;
                    }

                    String targetGroupId = newGroupId != null ? newGroupId : parent.getGav().getGroupId();
                    String targetArtifactId = newArtifactId != null ? newArtifactId : parent.getGav().getArtifactId();

                    try {
                        Optional<String> targetVersion = findAcceptableVersion(availableVersions, versionComparator,
                                targetGroupId == null ? "" : targetGroupId, targetArtifactId, currentVersion, this, ctx);
                        if (targetVersion.isPresent()) {
                            String currentRelativePath = parent.getRelativePath();
                            String targetRelativePath = newRelativePath != null ? newRelativePath :
                                    (currentRelativePath != null ? currentRelativePath : oldRelativePath);

                            // Skip if nothing is actually changing
                            // Note: treat null and empty string as equivalent for relativePath
                            if (Objects.equals(targetGroupId, parent.getGav().getGroupId()) &&
                                Objects.equals(targetArtifactId, parent.getGav().getArtifactId()) &&
                                Objects.equals(targetVersion.get(), currentVersion) &&
                                (Objects.equals(targetRelativePath, currentRelativePath) ||
                                 (StringUtils.isBlank(targetRelativePath) && StringUtils.isBlank(currentRelativePath)))) {
                                return document;
                            }

                            // Re-resolve this changed pom against its new parent and record the marker keyed
                            // by GAV. Only the poms actually being changed are retained (not every pom in the
                            // run); descendants are re-parented onto these in the edit phase. Keying by GAV
                            // lets multiple changed poms - e.g. one root per repository in a multi-repo run -
                            // each propagate independently, and one marker is shared by all its descendants.
                            Parent updatedParentRef = new Parent(
                                    new GroupArtifactVersion(targetGroupId, targetArtifactId, targetVersion.get()),
                                    targetRelativePath);
                            Pom updatedPom = mrr.getPom().getRequested().withParent(updatedParentRef);
                            ResolvedPom updatedResolvedPom = mrr.getPom()
                                    .withRequested(updatedPom)
                                    .resolve(ctx, new MavenPomDownloader(
                                            mrr.getProjectPoms(), ctx, MavenExecutionContextView.view(ctx).effectiveSettings(mrr),
                                            mrr.getActiveProfiles()));
                            acc.updatedByPom.put(mrr.getPom().getGav(), mrr.withPom(updatedResolvedPom));
                        }
                    } catch (MavenDownloadingException e) {
                        // The edit-phase visitor re-attempts the download for the changed pom and surfaces
                        // any failure there; nothing needs to be recorded for descendants in that case.
                    }
                }
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;

        return Preconditions.check(new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                while (mrr != null) {
                    Parent parent = mrr.getPom().getRequested().getParent();
                    if (parent != null &&
                        matchesGlob(parent.getArtifactId(), oldArtifactId) &&
                        matchesGlob(parent.getGroupId(), oldGroupId)) {
                        return SearchResult.found(document);
                    }
                    mrr = mrr.getParent();
                }
                return document;
            }
        }, new MavenIsoVisitor<ExecutionContext>() {
            final List<String> availableVersions = new ArrayList<>();

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);

                // Descendants of a changed pom (the changed poms themselves are edited in visitTag): re-parent
                // this pom onto the changed ancestor's re-resolved marker and let UpdateMavenModel re-resolve
                // this pom's own model against it.
                MavenResolutionResult mrr = d.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
                if (mrr != null && !acc.updatedByPom.containsKey(mrr.getPom().getGav())) {
                    MavenResolutionResult reparented = reparentOntoChangedAncestor(mrr, acc, ctx);
                    if (reparented != null) {
                        d = pinVersionsDroppedByChangedAncestor(d, mrr, reparented, ctx);
                        d = d.withMarkers(d.getMarkers().computeByType(mrr,
                                (original, ignored) -> reparented));
                        maybeUpdateModel();
                        doAfterVisit(new RemoveRedundantDependencyVersions(null, null, GTE, except).getVisitor());
                    }
                }
                return d;
            }

            @SuppressWarnings("OptionalGetWithoutIsPresent")
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isParentTag()) {
                    MavenResolutionResult mrr = getResolutionResult();
                    ResolvedPom resolvedPom = mrr.getPom();

                    if (matchesGlob(resolvedPom.getValue(tag.getChildValue("groupId").orElse(null)), oldGroupId) &&
                        matchesGlob(resolvedPom.getValue(tag.getChildValue("artifactId").orElse(null)), oldArtifactId) &&
                        (oldRelativePath == null || matchesGlob(determineRelativePath(tag, resolvedPom), oldRelativePath))) {
                        String oldVersion = resolvedPom.getValue(tag.getChildValue("version").orElse(null));
                        assert oldVersion != null;
                        String currentGroupId = tag.getChildValue("groupId").orElse(oldGroupId);
                        String targetGroupId = newGroupId == null ? currentGroupId : newGroupId;
                        String currentArtifactId = tag.getChildValue("artifactId").orElse(oldArtifactId);
                        String targetArtifactId = newArtifactId == null ? currentArtifactId : newArtifactId;
                        String targetRelativePath = newRelativePath == null ? tag.getChildValue("relativePath").orElse(oldRelativePath) : newRelativePath;
                        try {
                            Optional<String> targetVersion = findAcceptableVersion(availableVersions, versionComparator,
                                    targetGroupId, targetArtifactId, oldVersion, this, ctx);
                            if (!targetVersion.isPresent() ||
                                (Objects.equals(targetGroupId, currentGroupId) &&
                                 Objects.equals(targetArtifactId, currentArtifactId) &&
                                 Objects.equals(targetVersion.get(), oldVersion) &&
                                 Objects.equals(targetRelativePath, oldRelativePath))) {
                                return t;
                            }

                            List<TreeVisitor<?, ExecutionContext>> changeParentTagVisitors = new ArrayList<>();

                            if (!currentGroupId.equals(targetGroupId)) {
                                changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("groupId").get(), targetGroupId));
                            }

                            if (!currentArtifactId.equals(targetArtifactId)) {
                                changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("artifactId").get(), targetArtifactId));
                            }

                            if (!oldVersion.equals(targetVersion.get())) {
                                changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("version").get(), targetVersion.get()));
                            }

                            // Retain managed versions from the old parent that are not managed in the new parent
                            MavenPomDownloader mpd = new MavenPomDownloader(mrr.getProjectPoms(), ctx, MavenExecutionContextView.view(ctx).effectiveSettings(mrr), mrr.getActiveProfiles());
                            ResolvedPom oldParent = mpd.download(new GroupArtifactVersion(currentGroupId, currentArtifactId, oldVersion), null, resolvedPom, resolvedPom.getRepositories())
                                    .resolve(emptyList(), mpd, ctx);
                            ResolvedPom newParent = mpd.download(new GroupArtifactVersion(targetGroupId, targetArtifactId, targetVersion.get()), null, resolvedPom, resolvedPom.getRepositories())
                                    .resolve(emptyList(), mpd, ctx);
                            // Inspect this pom plus any descendant modules that inherit from it, so dependency
                            // management dropped by the new parent can be restored in this (local) parent on
                            // behalf of child modules that declare those dependencies without an explicit version.
                            List<ResolvedManagedDependency> dependenciesWithoutExplicitVersions = getDependenciesUnmanagedByNewParent(mrr, newParent);
                            for (ResolvedManagedDependency dep : dependenciesWithoutExplicitVersions) {
                                changeParentTagVisitors.add(new AddManagedDependencyVisitor(
                                        dep.getGroupId(), dep.getArtifactId(), dep.getVersion() == null ? "" : dep.getVersion(),
                                        dep.getScope() == null ? null : dep.getScope().toString().toLowerCase(), dep.getType(), dep.getClassifier(), null));
                            }

                            Map<String, String> oldParentProps = oldParent.getProperties();
                            Map<String, String> newParentProps = newParent.getProperties();
                            // Retain properties from the old parent that are not present in the new parent
                            Map<String, String> propertiesInUse = getPropertiesInUse(getCursor().firstEnclosingOrThrow(Xml.Document.class), oldParentProps, ctx);
                            for (Map.Entry<String, String> propInUse : propertiesInUse.entrySet()) {
                                //noinspection ConstantValue
                                if (!newParentProps.containsKey(propInUse.getKey()) && propInUse.getValue() != null) {
                                    changeParentTagVisitors.add(new AddPropertyVisitor(propInUse.getKey(), propInUse.getValue(), false));
                                }
                            }

                            // Update or add relativePath
                            Optional<Xml.Tag> existingRelativePath = t.getChild("relativePath");
                            if (oldRelativePath != null && !oldRelativePath.equals(targetRelativePath) && existingRelativePath.isPresent()) {
                                if (StringUtils.isBlank(targetRelativePath)) {
                                    // ChangeTagValueVisitor would keep the closing tag
                                    changeParentTagVisitors.add(new AddOrUpdateChild<>(t, Xml.Tag.build("<relativePath />")));
                                } else {
                                    changeParentTagVisitors.add(new ChangeTagValueVisitor<>(existingRelativePath.get(), targetRelativePath));
                                }
                            } else if (mismatches(existingRelativePath.orElse(null), targetRelativePath)) {
                                final Xml.Tag relativePathTag;
                                if (StringUtils.isBlank(targetRelativePath)) {
                                    relativePathTag = Xml.Tag.build("<relativePath />");
                                } else {
                                    relativePathTag = Xml.Tag.build("<relativePath>" + targetRelativePath + "</relativePath>");
                                }
                                doAfterVisit(new AddToTagVisitor<>(t, relativePathTag, new MavenTagInsertionComparator(t.getChildren())));
                            }

                            if (!changeParentTagVisitors.isEmpty()) {
                                for (TreeVisitor<?, ExecutionContext> visitor : changeParentTagVisitors) {
                                    doAfterVisit(visitor);
                                }
                                maybeUpdateModel();
                                doAfterVisit(new RemoveRedundantDependencyVersions(null, null, GTE, except).getVisitor());
                            }
                        } catch (MavenDownloadingException e) {
                            for (Map.Entry<MavenRepository, String> repositoryResponse : e.getRepositoryResponses().entrySet()) {
                                MavenRepository repository = repositoryResponse.getKey();
                                metadataFailures.insertRow(ctx, new MavenMetadataFailures.Row(targetGroupId, targetArtifactId, newVersion,
                                        repository.getUri(), repository.getSnapshots(), repository.getReleases(), repositoryResponse.getValue()));
                            }
                            return e.warn(tag);
                        }
                    }
                }
                return t;
            }

            private boolean mismatches(Xml.@Nullable Tag relativePath, @Nullable String targetRelativePath) {
                if (relativePath == null) {
                    return targetRelativePath != null;
                }
                String relativePathValue = relativePath.getValue().orElse(null);
                if (relativePathValue == null) {
                    return !StringUtils.isBlank(targetRelativePath);
                }
                return !relativePathValue.equals(targetRelativePath);
            }
        });
    }

    private static @Nullable String determineRelativePath(Xml.Tag tag, ResolvedPom resolvedPom) {
        Optional<Xml.Tag> relativePath = tag.getChild("relativePath");
        if (relativePath.isPresent()) {
            return resolvedPom.getValue(relativePath.get().getValue().orElse(""));
        }
        return DEFAULT_RELATIVE_PATH;
    }

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static Map<String, String> getPropertiesInUse(Xml.Document pomXml, Map<String, String> oldParentProps, ExecutionContext ctx) {
        Map<String, String> cascadedProps = new HashMap<>(oldParentProps);
        Set<String> referencedProps = new HashSet<>();
        new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext context) {
                Xml.Tag t = super.visitTag(tag, context);
                if (t.getContent() != null && t.getContent().size() == 1 && t.getContent().get(0) instanceof Xml.CharData) {
                    String text = ((Xml.CharData) t.getContent().get(0)).getText().trim();
                    if (isPropertyTag()) {
                        cascadedProps.put(t.getName(), text);
                    }
                    Matcher m = PROPERTY_PATTERN.matcher(text);
                    while (m.find()) {
                        String propertyName = m.group(1).trim();
                        if (!isGlobalProperty(propertyName)) {
                            referencedProps.add(propertyName);
                        }
                    }
                }
                return t;
            }

            private boolean isGlobalProperty(String propertyName) {
                return propertyName.startsWith("project.") || propertyName.startsWith("env.") ||
                        propertyName.startsWith("settings.") || "basedir".equals(propertyName);
            }
        }.visitNonNull(pomXml, ctx);
        return referencedProps.stream()
                .filter(cascadedProps::containsKey)
                .collect(toMap(p -> p, cascadedProps::get));
    }

    /**
     * If {@code mrr} descends from a pom whose {@code <parent>} is being changed (an entry in
     * {@code updatedByPom}), return {@code mrr} re-parented onto that changed ancestor's re-resolved marker,
     * so this pom's view of its parent - including the parent's re-resolved dependency management - reflects
     * the new parent. {@link UpdateMavenModel} then re-resolves this pom's own model against it.
     * {@code UpdateMavenModel} re-resolves a pom and its modules but never its parent, so the ancestor must be
     * supplied already re-resolved here. Returns {@code null} when no ancestor is being changed.
     */
    private static @Nullable MavenResolutionResult reparentOntoChangedAncestor(
            MavenResolutionResult mrr, Accumulator acc, ExecutionContext ctx) {
        MavenResolutionResult parent = mrr.getParent();
        if (parent == null) {
            return null;
        }
        MavenResolutionResult updatedParent = acc.updatedByPom.get(parent.getPom().getGav());
        if (updatedParent == null) {
            updatedParent = reparentOntoChangedAncestor(parent, acc, ctx);
            if (updatedParent == null) {
                return null;
            }
        } else {
            updatedParent = withRestoredManagement(parent, updatedParent, acc, ctx);
        }
        return mrr.withParent(updatedParent);
    }

    /**
     * The changed pom as its descendants need to see it: resolved against the new parent, and declaring the
     * dependency management that the edit phase restores in it. A descendant resolved against the unrestored
     * view would find no version at all for a dependency it declares without one.
     */
    private static MavenResolutionResult withRestoredManagement(MavenResolutionResult original, MavenResolutionResult withNewParent,
                                                                Accumulator acc, ExecutionContext ctx) {
        ResolvedGroupArtifactVersion gav = withNewParent.getPom().getGav();
        MavenResolutionResult restored = acc.restoredByPom.get(gav);
        if (restored == null) {
            restored = restoreManagement(original, withNewParent, ctx);
            acc.restoredByPom.put(gav, restored);
        }
        return restored;
    }

    private static MavenResolutionResult restoreManagement(MavenResolutionResult original, MavenResolutionResult withNewParent, ExecutionContext ctx) {
        Parent parentRef = withNewParent.getPom().getRequested().getParent();
        if (parentRef == null) {
            return withNewParent;
        }

        ResolvedPom pom = withNewParent.getPom();
        MavenPomDownloader downloader = new MavenPomDownloader(withNewParent.getProjectPoms(), ctx, withNewParent.getMavenSettings(), withNewParent.getActiveProfiles());
        try {
            ResolvedPom newParent = downloader.download(parentRef.getGav(), null, pom, pom.getRepositories())
                    .resolve(emptyList(), downloader, ctx);
            // Read off the original resolution; the one against the new parent no longer holds the dropped entries
            List<ResolvedManagedDependency> dropped = getDependenciesUnmanagedByNewParent(original, newParent);
            if (dropped.isEmpty()) {
                return withNewParent;
            }

            List<ManagedDependency> managed = new ArrayList<>(pom.getRequested().getDependencyManagement());
            for (ResolvedManagedDependency dep : dropped) {
                managed.add(new ManagedDependency.Defined(dep.getGav(),
                        dep.getScope() == null ? null : dep.getScope().toString().toLowerCase(),
                        dep.getType(), dep.getClassifier(), null));
            }
            return withNewParent.withPom(pom
                    .withRequested(pom.getRequested().withDependencyManagement(managed))
                    .resolve(ctx, downloader));
        } catch (MavenDownloadingException e) {
            // The changed pom's own edit reports the failure; descendants keep the unrestored view
            return withNewParent;
        }
    }

    /**
     * Write back an explicit version for any dependency this pom declares without one that the changed ancestor
     * no longer manages. The ancestor restores management only for the modules it can see, and its snapshot of
     * them predates any edit made earlier in the same run, so a module whose version was stripped as redundant
     * by an earlier recipe has to restore it here or resolve to no version at all.
     */
    private static Xml.Document pinVersionsDroppedByChangedAncestor(Xml.Document document, MavenResolutionResult before,
                                                                    MavenResolutionResult reparented, ExecutionContext ctx) {
        ResolvedPom beforePom = before.getPom();
        List<Dependency> withoutExplicitVersion = new ArrayList<>();
        for (Dependency dep : beforePom.getRequested().getDependencies()) {
            if (dep.getVersion() == null) {
                withoutExplicitVersion.add(dep);
            }
        }
        if (withoutExplicitVersion.isEmpty()) {
            return document;
        }

        Map<String, String> pinned = new LinkedHashMap<>();
        try {
            MavenPomDownloader mpd = new MavenPomDownloader(reparented.getProjectPoms(), ctx, reparented.getMavenSettings(), reparented.getActiveProfiles());
            ResolvedPom afterPom = reparented.getPom().resolve(ctx, mpd);
            for (Dependency dep : withoutExplicitVersion) {
                String groupId = beforePom.getValue(dep.getGroupId());
                String artifactId = beforePom.getValue(dep.getArtifactId());
                if (artifactId == null || afterPom.getManagedVersion(groupId, artifactId, dep.getType(), dep.getClassifier()) != null) {
                    continue;
                }
                String version = beforePom.getManagedVersion(groupId, artifactId, dep.getType(), dep.getClassifier());
                if (version != null) {
                    pinned.put(dependencyKey(dep.getGroupId(), dep.getArtifactId(), dep.getType(), dep.getClassifier()), version);
                }
            }
        } catch (MavenDownloadingException e) {
            return document;
        }
        if (pinned.isEmpty()) {
            return document;
        }

        return (Xml.Document) new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isDependencyTag() && !t.getChild("version").isPresent()) {
                    String version = pinned.get(dependencyKey(t.getChildValue("groupId").orElse(null),
                            t.getChildValue("artifactId").orElse(null),
                            t.getChildValue("type").orElse(null),
                            t.getChildValue("classifier").orElse(null)));
                    if (version != null) {
                        t = (Xml.Tag) new AddToTagVisitor<>(t, Xml.Tag.build("<version>" + version + "</version>"),
                                new MavenTagInsertionComparator(t.getChildren())).visitNonNull(t, ctx, getCursor().getParentOrThrow());
                    }
                }
                return t;
            }
        }.visitNonNull(document, ctx);
    }

    private static String dependencyKey(@Nullable String groupId, @Nullable String artifactId,
                                        @Nullable String type, @Nullable String classifier) {
        return groupId + ":" + artifactId + ":" + (type == null ? "jar" : type) + ":" + classifier;
    }

    private static void collectDescendantModules(MavenResolutionResult mrr, List<MavenResolutionResult> out) {
        for (MavenResolutionResult module : mrr.getModules()) {
            out.add(module);
            collectDescendantModules(module, out);
        }
    }

    private static List<ResolvedManagedDependency> getDependenciesUnmanagedByNewParent(MavenResolutionResult mrr, ResolvedPom newParent) {
        // Remove from the list any that would still be managed under the new parent
        Set<GroupArtifact> newParentManagedGa = newParent.getDependencyManagement().stream()
                .map(dep -> new GroupArtifact(dep.getGav().getGroupId(), dep.getGav().getArtifactId()))
                .collect(toSet());

        List<MavenResolutionResult> modules = new ArrayList<>();
        modules.add(mrr);
        collectDescendantModules(mrr, modules);

        Map<GroupArtifact, ResolvedManagedDependency> unmanaged = new LinkedHashMap<>();
        for (MavenResolutionResult module : modules) {
            for (ResolvedManagedDependency dep : getManagementInheritedFromParent(module)) {
                GroupArtifact ga = new GroupArtifact(dep.getGav().getGroupId(), dep.getGav().getArtifactId());
                if (!newParentManagedGa.contains(ga)) {
                    unmanaged.putIfAbsent(ga, dep);
                }
            }
        }
        return new ArrayList<>(unmanaged.values());
    }

    private static List<ResolvedManagedDependency> getManagementInheritedFromParent(MavenResolutionResult mrr) {
        ResolvedPom resolvedPom = mrr.getPom();

        // Dependencies managed by the current pom's own dependency management are irrelevant to parent upgrade
        List<ManagedDependency> locallyManaged = resolvedPom.getRequested().getDependencyManagement();

        Set<GroupArtifactVersion> requestedWithoutExplicitVersion = resolvedPom.getRequested().getDependencies().stream()
                .filter(dep -> dep.getVersion() == null)
                // Dependencies explicitly managed by the current pom require no changes
                .filter(dep -> locallyManaged.stream()
                        .noneMatch(it -> {
                            String groupId = resolvedPom.getValue(it.getGroupId());
                            String artifactId = resolvedPom.getValue(it.getArtifactId());
                            return Objects.equals(dep.getGroupId(),groupId) && Objects.equals(dep.getArtifactId(), artifactId);
                        }))
                .map(dep -> new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), null))
                .collect(toCollection(LinkedHashSet::new));

        if (requestedWithoutExplicitVersion.isEmpty()) {
            return emptyList();
        }

        return resolvedPom.getDependencyManagement().stream()
                .filter(dep -> requestedWithoutExplicitVersion.contains(dep.getGav().withVersion(null)))
                // Exclude dependencies managed by a bom imported by the current pom
                .filter(dep -> dep.getBomGav() == null || locallyManaged.stream()
                        .noneMatch(it -> {
                            String groupId = resolvedPom.getValue(it.getGroupId());
                            String artifactId = resolvedPom.getValue(it.getArtifactId());
                            return dep.getBomGav().getGroupId().equals(groupId) && dep.getBomGav().getArtifactId().equals(artifactId);
                        }))
                .collect(toList());
    }
}
