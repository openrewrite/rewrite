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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

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

    @Override
    public String getDisplayName() {
        return "Add Maven parent";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Add a parent pom to a Maven pom.xml. Does nothing if a parent pom is already present.";
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
        return Preconditions.check(new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag root = document.getRoot();
                if (!root.getChild("parent").isPresent()) {
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

                try {
                    Optional<String> targetVersion = findAcceptableVersion(groupId, artifactId, ctx);
                    if (targetVersion.isPresent()) {
                        Xml.Tag parentTag = Xml.Tag.build(
                                "<parent>\n" +
                                "<groupId>" + groupId + "</groupId>\n" +
                                "<artifactId>" + artifactId + "</artifactId>\n" +
                                "<version>" + targetVersion.get() + "</version>\n" +
                                (relativePath == null ? "" : StringUtils.isBlank(relativePath) ?
                                        "<relativePath/>" : "<relativePath>" + relativePath + "</relativePath>") +
                                "</parent>");

                        document = (Xml.Document) new AddToTagVisitor<>(root, parentTag, new MavenTagInsertionComparator(root.getChildren()))
                                .visitNonNull(document, ctx, getCursor().getParentOrThrow());

                        maybeUpdateModel();
                        doAfterVisit(new RemoveRedundantDependencyVersions(null, null,
                                RemoveRedundantDependencyVersions.Comparator.GTE, null).getVisitor());
                    }
                } catch (MavenDownloadingException e) {
                    for (Map.Entry<MavenRepository, String> repositoryResponse : e.getRepositoryResponses().entrySet()) {
                        MavenRepository repository = repositoryResponse.getKey();
                        metadataFailures.insertRow(ctx, new MavenMetadataFailures.Row(groupId, artifactId, version,
                                repository.getUri(), repository.getSnapshots(), repository.getReleases(), repositoryResponse.getValue()));
                    }
                    return e.warn(document);
                }

                return super.visitDocument(document, ctx);
            }

            private final VersionComparator versionComparator = Objects.requireNonNull(Semver.validate(version, versionPattern).getValue());

            private Optional<String> findAcceptableVersion(String groupId, String artifactId, ExecutionContext ctx)
                    throws MavenDownloadingException {
                if (availableVersions == null) {
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                            .filter(v -> versionComparator.isValid(null, v))
                            .collect(toList());
                }
                return availableVersions.stream().max(versionComparator);
            }
        });
    }
}
