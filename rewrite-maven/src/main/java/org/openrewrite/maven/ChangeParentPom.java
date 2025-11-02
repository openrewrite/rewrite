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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.maven.RemoveRedundantDependencyVersions.Comparator.GTE;
import static org.openrewrite.maven.tree.Parent.DEFAULT_RELATIVE_PATH;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeParentPom extends Recipe {
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

    @Override
    public String getDisplayName() {
        return "Change Maven parent";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", newGroupId, newArtifactId, newVersion);
    }

    @Override
    public String getDescription() {
        return "Change the parent pom of a Maven pom.xml by matching the existing parent via groupId and artifactId, " +
                "and updating it to a new groupId, artifactId, version, and optional relativePath. " +
                "Also updates the project to retain dependency management and properties previously inherited from the old parent that are no longer provided by the new parent. " +
                "Removes redundant dependency versions already managed by the new parent.";
    }

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

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;

        return Preconditions.check(new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Parent parent = getResolutionResult().getPom().getRequested().getParent();
                if (parent != null &&
                    matchesGlob(parent.getArtifactId(), oldArtifactId) &&
                    matchesGlob(parent.getGroupId(), oldGroupId)) {
                    return SearchResult.found(document);
                }
                return document;
            }
        }, new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            private Collection<String> availableVersions;

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
                            Optional<String> targetVersion = findAcceptableVersion(targetGroupId, targetArtifactId, oldVersion, ctx);
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
                            MavenPomDownloader mpd = new MavenPomDownloader(mrr.getProjectPoms(), ctx, mrr.getMavenSettings(), mrr.getActiveProfiles());
                            ResolvedPom newParent = mpd.download(new GroupArtifactVersion(targetGroupId, targetArtifactId, targetVersion.get()), null, resolvedPom, resolvedPom.getRepositories())
                                    .resolve(emptyList(), mpd, ctx);
                            List<ResolvedManagedDependency> dependenciesWithoutExplicitVersions = getDependenciesUnmanagedByNewParent(mrr, newParent);
                            for (ResolvedManagedDependency dep : dependenciesWithoutExplicitVersions) {
                                changeParentTagVisitors.add(new AddManagedDependencyVisitor(
                                        dep.getGav().getGroupId(), dep.getGav().getArtifactId(), dep.getGav().getVersion(),
                                        dep.getScope() == null ? null : dep.getScope().toString().toLowerCase(), dep.getType(), dep.getClassifier(), null));
                            }

                            // Retain properties from the old parent that are not present in the new parent
                            Map<String, String> propertiesInUse = getPropertiesInUse(getCursor().firstEnclosingOrThrow(Xml.Document.class), ctx);
                            Map<String, String> newParentProps = newParent.getProperties();
                            for (Map.Entry<String, String> propInUse : propertiesInUse.entrySet()) {
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
                                maybeUpdateModel();
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

            private Optional<String> findAcceptableVersion(String groupId, String artifactId, String currentVersion,
                                                                ExecutionContext ctx) throws MavenDownloadingException {
                String finalCurrentVersion = !Semver.isVersion(currentVersion) ? "0.0.0" : currentVersion;

                if (availableVersions == null) {
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    //noinspection EqualsWithItself
                    availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                            .filter(v -> versionComparator.isValid(finalCurrentVersion, v))
                            .filter(v -> Boolean.TRUE.equals(allowVersionDowngrades) || versionComparator.compare(finalCurrentVersion, finalCurrentVersion, v) <= 0)
                            .collect(toList());
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

    private static Map<String, String> getPropertiesInUse(Xml.Document pomXml, ExecutionContext ctx) {
        return new MavenIsoVisitor<Map<String, String>>() {
            @Nullable
            ResolvedPom resolvedPom = null;
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, Map<String, String> properties) {
                Xml.Tag t = super.visitTag(tag, properties);
                if (t.getContent() != null && t.getContent().size() == 1 && t.getContent().get(0) instanceof Xml.CharData) {
                    String text = ((Xml.CharData) t.getContent().get(0)).getText().trim();
                    Matcher m = PROPERTY_PATTERN.matcher(text);
                    while (m.find()) {
                        if (resolvedPom == null) {
                            resolvedPom = getResolutionResult().getPom();
                        }
                        String propertyName = m.group(1).trim();
                        if (resolvedPom.getProperties().containsKey(propertyName) && !isGlobalProperty(propertyName)) {
                            properties.put(m.group(1).trim(), resolvedPom.getProperties().get(propertyName));
                        }
                    }
                }
                return t;
            }

            private boolean isGlobalProperty(String propertyName) {
                return propertyName.startsWith("project.") || propertyName.startsWith("env.") ||
                        propertyName.startsWith("settings.") || "basedir".equals(propertyName);
            }
        }.reduce(pomXml, new HashMap<>());
    }

    private List<ResolvedManagedDependency> getDependenciesUnmanagedByNewParent(MavenResolutionResult mrr, ResolvedPom newParent) {
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
                            return dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId);
                        }))
                .map(dep -> new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), null))
                .collect(toCollection(LinkedHashSet::new));

        if (requestedWithoutExplicitVersion.isEmpty()) {
            return emptyList();
        }

        List<ResolvedManagedDependency> depsWithoutExplicitVersion = resolvedPom.getDependencyManagement().stream()
                .filter(dep -> requestedWithoutExplicitVersion.contains(dep.getGav().withVersion(null)))
                // Exclude dependencies managed by a bom imported by the current pom
                .filter(dep -> dep.getBomGav() == null || locallyManaged.stream()
                        .noneMatch(it -> {
                            String groupId = resolvedPom.getValue(it.getGroupId());
                            String artifactId = resolvedPom.getValue(it.getArtifactId());
                            return dep.getBomGav().getGroupId().equals(groupId) && dep.getBomGav().getArtifactId().equals(artifactId);
                        }))
                .collect(toList());

        if (depsWithoutExplicitVersion.isEmpty()) {
            return emptyList();
        }

        // Remove from the list any that would still be managed under the new parent
        Set<GroupArtifact> newParentManagedGa = newParent.getDependencyManagement().stream()
                .map(dep -> new GroupArtifact(dep.getGav().getGroupId(), dep.getGav().getArtifactId()))
                .collect(toSet());

        return depsWithoutExplicitVersion.stream()
                .filter(it -> !newParentManagedGa.contains(new GroupArtifact(it.getGav().getGroupId(), it.getGav().getArtifactId())))
                .collect(toList());
    }
}
