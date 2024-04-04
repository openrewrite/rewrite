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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.TagNameComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.openrewrite.internal.StringUtils.matchesGlob;

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
            description = "The relativePath of the maven parent pom to be changed away from.",
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
        return "Change the parent pom of a Maven pom.xml. Identifies the parent pom to be changed by its groupId and artifactId.";
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
                        (oldRelativePath == null || matchesGlob(resolvedPom.getValue(tag.getChildValue("relativePath").orElse(null)), oldRelativePath))) {
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
                            MavenPomDownloader mpd = new MavenPomDownloader(mrr.getProjectPoms(), ctx, null, null);
                            Pom newParentPom = mpd.download(new GroupArtifactVersion(targetGroupId, targetArtifactId, targetVersion.get()), null, null, resolvedPom.getRepositories());
                            List<ResolvedManagedDependency> dependenciesWithoutExplicitVersions = getDependenciesWithoutExplicitVersions(resolvedPom);
                            for (ResolvedManagedDependency dep : dependenciesWithoutExplicitVersions) {
                                changeParentTagVisitors.add(new AddManagedDependencyVisitor(
                                        dep.getGav().getGroupId(), dep.getGav().getArtifactId(), dep.getGav().getVersion(),
                                        dep.getScope() == null ? null : dep.getScope().toString().toLowerCase(), dep.getType(), dep.getClassifier()));
                            }

                            // Retain properties from the old parent that are not present in the new parent
                            Map<String, String> propertiesInUse = getPropertiesInUse(getCursor().firstEnclosingOrThrow(Xml.Document.class), ctx);
                            Map<String, String> newParentProps = newParentPom.getProperties();
                            for (Map.Entry<String, String> propInUse : propertiesInUse.entrySet()) {
                                if(!newParentProps.containsKey(propInUse.getKey())) {
                                    changeParentTagVisitors.add(new UnconditionalAddProperty(propInUse.getKey(), propInUse.getValue()));
                                }
                            }

                            // Update or add relativePath
                            if (oldRelativePath != null && !oldRelativePath.equals(targetRelativePath)) {
                                changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("relativePath").get(), targetRelativePath));
                            } else if (mismatches(tag.getChild("relativePath").orElse(null), targetRelativePath)) {
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
                                doAfterVisit(new RemoveRedundantDependencyVersions(null, null, false, null).getVisitor());
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                }
                return t;
            }

            private boolean mismatches(@Nullable Xml.Tag relativePath, @Nullable String targetRelativePath) {
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
                            .collect(Collectors.toList());
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

    private static Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static Map<String, String> getPropertiesInUse(Xml.Document pomXml, ExecutionContext ctx) {
        Map<String, String> properties = new HashMap<>();
        new MavenIsoVisitor<ExecutionContext>() {

            @Nullable
            ResolvedPom resolvedPom = null;
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                Xml.Tag t = super.visitTag(tag, executionContext);
                if(t.getContent() != null && t.getContent().size() == 1 && t.getContent().get(0) instanceof Xml.CharData) {
                    String text = ((Xml.CharData) t.getContent().get(0)).getText().trim();
                    Matcher m = PROPERTY_PATTERN.matcher(text);
                    while(m.find()) {
                        if(resolvedPom == null) {
                            resolvedPom = getResolutionResult().getPom();
                        }
                        String propertyName = m.group(1).trim();
                        properties.put(m.group(1).trim(), resolvedPom.getProperties().get(propertyName));
                    }
                }
                return t;
            }
        }.visit(pomXml, ctx);
        return properties;
    }

    private static List<ResolvedManagedDependency> getDependenciesWithoutExplicitVersions(ResolvedPom resolvedPom) {

        Set<GroupArtifactVersion> requestedWithoutExplicitVersion = resolvedPom.getRequested().getDependencies().stream()
                .filter(dep -> dep.getVersion() == null)
                .map(dep -> new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), null))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return resolvedPom.getDependencyManagement().stream()
                .filter(dep -> requestedWithoutExplicitVersion.contains(dep.getGav().withVersion(null)))
                .collect(Collectors.toList());
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class UnconditionalAddProperty extends MavenIsoVisitor<ExecutionContext> {
        String key;
        String value;
        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
            Xml.Document d = super.visitDocument(document, ctx);
            Xml.Tag root = d.getRoot();
            Optional<Xml.Tag> properties = root.getChild("properties");
            if (!properties.isPresent()) {
                Xml.Tag propertiesTag = Xml.Tag.build("<properties>\n<" + key + ">" + value + "</" + key + ">\n</properties>");
                d = (Xml.Document) new AddToTagVisitor<ExecutionContext>(root, propertiesTag, new MavenTagInsertionComparator(root.getChildren())).visitNonNull(d, ctx);
            } else if (!properties.get().getChildValue(key).isPresent()) {
                Xml.Tag propertyTag = Xml.Tag.build("<" + key + ">" + value + "</" + key + ">");
                d = (Xml.Document) new AddToTagVisitor<>(properties.get(), propertyTag, new TagNameComparator()).visitNonNull(d, ctx);
            }
            if (d != document) {
                maybeUpdateModel();
            }
            return d;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (isPropertyTag() && key.equals(tag.getName())
                && !value.equals(tag.getValue().orElse(null))) {
                t = (Xml.Tag) new ChangeTagValueVisitor<>(tag, value).visitNonNull(t, ctx);
            }
            return t;
        }
    }
}
