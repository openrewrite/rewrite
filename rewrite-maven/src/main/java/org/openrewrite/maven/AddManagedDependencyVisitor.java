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
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class AddManagedDependencyVisitor extends MavenIsoVisitor<ExecutionContext> {
    private static final XPathMatcher MANAGED_DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies");

    private final String groupId;
    private final String artifactId;
    private final VersionComparator version;

    @Nullable
    private final String fallbackVersion;

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
    private String resolvedVersion;

    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {

        Xml.Document doc = super.visitDocument(document, ctx);


        if (documentHasManagedDependency(doc, ctx)) {
            return document;
        }

        String versionToUse = findVersionToUse(ctx);
        if (Objects.equals(versionToUse, existingManagedDependencyVersion())) {
            return document;
        }

        Xml.Tag root = document.getRoot();
        List<? extends Content> rootContent = root.getContent() != null ? root.getContent() : emptyList();

        Xml.Tag dependencyManagementTag = root.getChild("dependencyManagement").orElse(null);
        if (dependencyManagementTag == null) {
            doc = (Xml.Document) new AddToTagVisitor<>(root, Xml.Tag.build("<dependencyManagement>\n<dependencies/>\n</dependencyManagement>"),
                    new MavenTagInsertionComparator(rootContent)).visitNonNull(doc, ctx);
        } else if (!dependencyManagementTag.getChild("dependencies").isPresent()) {
            doc = (Xml.Document) new AddToTagVisitor<>(dependencyManagementTag, Xml.Tag.build("\n<dependencies/>\n"),
                    new MavenTagInsertionComparator(rootContent)).visitNonNull(doc, ctx);
        }

        doc = (Xml.Document) new InsertDependencyInOrder(groupId, artifactId, versionToUse,
                type, scope, classifier).visitNonNull(doc, ctx);
        return doc;
    }

    private boolean documentHasManagedDependency(Xml.Document doc, ExecutionContext ctx) {
        AtomicBoolean managedDepExists = new AtomicBoolean(false);
        new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                Xml.Tag tg = super.visitTag(tag, executionContext);
                if (isManagedDependencyTag(groupId, artifactId)) {
                    managedDepExists.set(true);
                }
                return tg;
            }
        }.visitNonNull(doc, ctx);
        return managedDepExists.get();
    }

    @RequiredArgsConstructor
    private static class InsertDependencyInOrder extends MavenIsoVisitor<ExecutionContext> {
        private final String groupId;
        private final String artifactId;
        private final String version;
        @Nullable
        private final String type;
        @Nullable
        private final String scope;
        @Nullable
        private final String classifier;

        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {

            return super.visitDocument(document, ctx);
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (MANAGED_DEPENDENCIES_MATCHER.matches(getCursor())) {
                Xml.Tag dependencyTag = Xml.Tag.build(
                        "\n<dependency>\n" +
                                "<groupId>" + groupId + "</groupId>\n" +
                                "<artifactId>" + artifactId + "</artifactId>\n" +
                                "<version>" + version + "</version>\n" +
                                (classifier == null ? "" :
                                        "<classifier>" + classifier + "</classifier>\n") +
                                (type == null || type.equals("jar") ? "" :
                                        "<type>" + type + "</type>\n") +
                                (scope == null || "compile".equals(scope) ? "" :
                                        "<scope>" + scope + "</scope>\n") +
                                "</dependency>"
                );
                doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag,
                        new InsertDependencyComparator(tag.getContent() == null ? emptyList() : tag.getContent(), dependencyTag)));
                maybeUpdateModel();
            }
            return super.visitTag(tag, ctx);
        }
    }

    @Nullable
    private String existingManagedDependencyVersion() {
        return getResolutionResult().getPom().getDependencyManagement().stream()
                .map(resolvedManagedDep -> {
                    if (resolvedManagedDep.matches(groupId, artifactId, type, classifier)) {
                        return resolvedManagedDep.getGav().getVersion();
                    } else if (resolvedManagedDep.getRequestedBom() != null
                            && resolvedManagedDep.getRequestedBom().getGroupId().equals(groupId)
                            && resolvedManagedDep.getRequestedBom().getArtifactId().equals(artifactId)) {
                        return resolvedManagedDep.getRequestedBom().getVersion();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Nullable
    private String findVersionToUse(ExecutionContext ctx) {
        if (resolvedVersion == null) {
            MavenMetadata mavenMetadata = downloadMetadata(groupId, artifactId, ctx);
            LatestRelease latest = new LatestRelease(versionPattern);
            resolvedVersion = mavenMetadata.getVersioning().getVersions().stream()
                    .filter(v -> version.isValid(null, v))
                    .filter(v -> !Boolean.TRUE.equals(releasesOnly) || latest.isValid(null, v))
                    .max((v1, v2) -> version.compare(null, v1, v2))
                    .orElse(fallbackVersion);
        }
        return resolvedVersion;
    }
}
