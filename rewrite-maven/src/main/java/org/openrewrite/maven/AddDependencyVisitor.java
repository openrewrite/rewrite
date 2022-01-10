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
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.Version;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@RequiredArgsConstructor
public class AddDependencyVisitor extends MavenVisitor {
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
    private VersionComparator versionComparator;

    @Nullable
    private String resolvedVersion;

    @Override
    public Maven visitMaven(Maven maven, ExecutionContext ctx) {
        Maven m = super.visitMaven(maven, ctx);

        Validated versionValidation = Semver.validate(version, versionPattern);
        if (versionValidation.isValid()) {
            versionComparator = versionValidation.getValue();
        }

        Xml.Tag root = m.getRoot();
        if (!root.getChild("dependencies").isPresent()) {
            doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<dependencies/>"),
                    new MavenTagInsertionComparator(root.getChildren())));
        }

        doAfterVisit(new InsertDependencyInOrder(scope));

        List<ResolvedDependency> dependenciesForScope = new ArrayList<>(resolutionResult.getDependencies().get(Scope.fromName(scope)));
        String packaging = (type == null) ? "jar" : type;

        String dependencyVersion = findVersionToUse(groupId, artifactId, ctx);
//        dependenciesForScope.add(
//                new ResolvedDependency(
//                        null,
//                        new ResolvedGroupArtifactVersion(MavenRepository.MAVEN_CENTRAL.getUri(),
//                                groupId, artifactId, dependencyVersion, null),
//                        new Dependency(
//                                new GroupArtifactVersion(groupId, artifactId, dependencyVersion),
//                                classifier,
//                                type,
//                                scope,
//                                emptyList(),
//                                optional != null && optional
//                        ),
//                        Scope.fromName(scope),
//                        emptyList(),
//                        emptyList()
//                )
//        );

        Map<Scope, List<ResolvedDependency>> dependencies = new HashMap<>(resolutionResult.getDependencies());
        dependencies.put(Scope.fromName(scope), dependenciesForScope);

        return m.withMavenResolutionResult(m.getMavenResolutionResult().withDependencies(dependencies));
    }

    private String findVersionToUse(String groupId, String artifactId, ExecutionContext ctx) {
        if (resolvedVersion == null) {

            if (versionComparator == null) {
                resolvedVersion = version;
            } else {
                MavenMetadata mavenMetadata = new MavenPomDownloader(emptyMap(), ctx)
                        .downloadMetadata(new GroupArtifact(groupId, artifactId), emptyList());

                if(mavenMetadata != null) {
                    LatestRelease latest = new LatestRelease(versionPattern);
                    resolvedVersion = mavenMetadata.getVersioning().getVersions().stream()
                            .filter(v -> versionComparator.isValid(null, v))
                            .filter(v -> !Boolean.TRUE.equals(releasesOnly) || latest.isValid(null, v))
                            .max((v1, v2) -> versionComparator.compare(null, v1, v2))
                            .orElse(version);
                }
            }
        }

        return resolvedVersion;
    }

    @RequiredArgsConstructor
    private class InsertDependencyInOrder extends MavenVisitor {

        @Nullable
        private final String scope;

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (DEPENDENCIES_MATCHER.matches(getCursor())) {
                String versionToUse = null;

                if (resolutionResult.getPom().getManagedVersion(groupId, artifactId, type, classifier) == null) {
                    if (familyRegex != null) {
                        versionToUse = findDependencies(d -> familyRegex.matcher(d.getGroupId()).matches()).stream()
                                .max(Comparator.comparing(d -> new Version(d.getVersion())))
                                .map(d -> d.getRequested().getVersion())
                                .orElse(null);
                    }
                    if (versionToUse == null) {
                        versionToUse = findVersionToUse(groupId, artifactId, ctx);
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
                                (scope == null || scope.equals("compile") ? "" :
                                        "<scope>" + scope + "</scope>\n") +
                                (Boolean.TRUE.equals(optional) ? "<optional>true</optional>\n" : "") +
                                "</dependency>"
                );

                doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag,
                        new InsertDependencyComparator(tag.getChildren(), dependencyTag)));

                return tag;
            }

            return super.visitTag(tag, ctx);
        }
    }
}
