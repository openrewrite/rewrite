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
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.maven.tree.Version;
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
        if (isDependencyTag() &&
                groupId.equals(tag.getChildValue("groupId").orElse(null)) &&
                artifactId.equals(tag.getChildValue("artifactId").orElse(null)) &&
                Scope.fromName(scope) == Scope.fromName(tag.getChildValue("scope").orElse(null))) {
            getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "alreadyHasDependency", true);
            return tag;
        }
        return super.visitTag(tag, executionContext);
    }


    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
        Xml.Document maven = super.visitDocument(document, executionContext);

        if(getCursor().getMessage("alreadyHasDependency", false)) {
            return document;
        }

        Scope resolvedScope = scope == null ? Scope.Compile : Scope.fromName(scope);
        Map<Scope, List<ResolvedDependency>> dependencies = getResolutionResult().getDependencies();
        if (dependencies.containsKey(resolvedScope)) {
            for (ResolvedDependency d : dependencies.get(resolvedScope)) {
                if (d.isDirect() && groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                    return maven;
                }
            }
        }

        Validated<VersionComparator> versionValidation = Semver.validate(version, versionPattern);
        if (versionValidation.isValid()) {
            versionComparator = versionValidation.getValue();
        }

        Xml.Tag root = maven.getRoot();
        if (!root.getChild("dependencies").isPresent()) {
            doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<dependencies/>"),
                    new MavenTagInsertionComparator(root.getContent() == null ? emptyList() : root.getContent())));
        }

        doAfterVisit(new InsertDependencyInOrder(scope));

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

                if (getResolutionResult().getPom().getManagedVersion(groupId, artifactId, type, classifier) == null) {
                    if (familyRegex != null) {
                        versionToUse = findDependencies(d -> familyRegex.matcher(d.getGroupId()).matches()).stream()
                                .max(Comparator.comparing(d -> new Version(d.getVersion())))
                                .map(d -> d.getRequested().getVersion())
                                .orElse(null);
                    }
                    if (versionToUse == null) {
                        try {
                            versionToUse = findVersionToUse(groupId, artifactId, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
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

                doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag,
                        new InsertDependencyComparator(tag.getContent() == null ? emptyList() : tag.getContent(), dependencyTag)));
                maybeUpdateModel();

                return tag;
            }

            return super.visitTag(tag, ctx);
        }

        private String findVersionToUse(String groupId, String artifactId, ExecutionContext ctx) throws MavenDownloadingException {
            if (resolvedVersion == null) {
                if (versionComparator == null || versionComparator instanceof ExactVersion) {
                    resolvedVersion = version;
                } else {
                    MavenMetadata mavenMetadata = metadataFailures == null ?
                            downloadMetadata(groupId, artifactId, ctx) :
                            metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
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
    }
}
