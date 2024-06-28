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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddParentPom extends Recipe {
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group ID",
            description = "The group ID of the maven parent pom to be adopted.",
            example = "org.springframework.boot")
    String groupId;

    @Option(displayName = "Artifact ID",
            description = "The artifact ID of the maven parent pom to be adopted.",
            example = "spring-boot-starter-parent")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String version;

    @Option(displayName = "Relative path",
            description = "New relative path attribute for parent lookup.",
            example = "../pom.xml")
    @Nullable
    String relativePath;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                          "Multiple patterns may be specified, separated by a semicolon `;`. " +
                          "If multiple patterns are supplied any of the patterns matching will be interpreted as a match. " +
                          "When not set, all source files are searched. ",
            required = false,
            example = "**/*-parent/grpc-*/pom.xml")
    @Nullable
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Change Maven parent";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Change the parent pom of a Maven pom.xml. Identifies the parent pom to be changed by its groupId and artifactId.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        //noinspection ConstantConditions
        if (version != null) {
            validated = validated.and(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = Semver.validate(version, versionPattern).getValue();
        assert versionComparator != null;

        return Preconditions.check(new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag root = document.getRoot();
                if (!root.getChild("parent").isPresent() &&
                    (filePattern == null || PathUtils.matchesGlob(document.getSourcePath(), filePattern))) {
                    return SearchResult.found(document);
                }
                return document;

            }
        }, new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            private Collection<String> availableVersions;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag root = document.getRoot();
                assert !root.getChild("parent").isPresent();

                document = (Xml.Document) new AddToTagVisitor<>(root, Xml.Tag.build("<parent/>"), new MavenTagInsertionComparator(root.getChildren()))
                        .visitNonNull(document, ctx, getCursor().getParentOrThrow());

                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isParentTag()) {
                    List<TreeVisitor<?, ExecutionContext>> addToTagVisitors = new ArrayList<>();

                    addToTagVisitors.add(new AddToTagVisitor<>(t, Xml.Tag.build("<groupId>" + groupId + "</groupId>")));
                    addToTagVisitors.add(new AddToTagVisitor<>(t, Xml.Tag.build("<artifactId>" + artifactId + "</artifactId>")));

                    try {
                        Optional<String> targetVersion = findAcceptableVersion(groupId, artifactId, ctx);
                        // TODO What to do if not present?
                        targetVersion.ifPresent((v) ->
                                addToTagVisitors.add(new AddToTagVisitor<>(t, Xml.Tag.build("<version>" + v + "</version>")))
                        );
                    } catch (MavenDownloadingException e) {
                        for (Map.Entry<MavenRepository, String> repositoryResponse : e.getRepositoryResponses().entrySet()) {
                            MavenRepository repository = repositoryResponse.getKey();
                            metadataFailures.insertRow(ctx, new MavenMetadataFailures.Row(groupId, artifactId, version,
                                    repository.getUri(), repository.getSnapshots(), repository.getReleases(), repositoryResponse.getValue()));
                        }
                        return e.warn(tag);
                    }

                    if (relativePath != null) {
                        final Xml.Tag relativePathTag;
                        if (StringUtils.isBlank(relativePath)) {
                            relativePathTag = Xml.Tag.build("<relativePath/>");
                        } else {
                            relativePathTag = Xml.Tag.build("<relativePath>" + relativePath + "</relativePath>");
                        }
                        addToTagVisitors.add(new AddToTagVisitor<>(t, relativePathTag));
                    }

                    for (TreeVisitor<?, ExecutionContext> visitor : addToTagVisitors) {
                        doAfterVisit(visitor);
                    }
                    maybeUpdateModel();
                    doAfterVisit(new RemoveRedundantDependencyVersions(null, null,
                            RemoveRedundantDependencyVersions.Comparator.GTE, null).getVisitor());
                }
                return t;
            }

            private Optional<String> findAcceptableVersion(String groupId, String artifactId, ExecutionContext ctx)
                    throws MavenDownloadingException {

                if (availableVersions == null) {
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    //noinspection EqualsWithItself
                    availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                            .filter(v -> versionComparator.isValid(null, v))
                            .collect(Collectors.toList());
                }
                return availableVersions.stream().max(versionComparator);
            }
        });
    }
}
