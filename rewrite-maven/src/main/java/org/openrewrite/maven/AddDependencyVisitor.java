/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.Version;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Pattern;

import static java.util.Collections.*;

/**
 * Adds a dependency if there is no dependency matching <code>groupId</code> and <code>artifactId</code>.
 * A matching dependency with a different version or scope does NOT have its version or scope updated.
 * Use {@link ChangeDependencyVersion} or {@link UpgradeDependencyVersion} in the case of a different version.
 * Use {@link ChangeDependencyScope} in the case of a different scope.
 * <p>
 * Places a new dependency as physically "near" to a group of similar dependencies as possible.
 */
@EqualsAndHashCode(callSuper = false)
public class AddDependencyVisitor extends MavenVisitor {
    private static final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");

    private final String groupId;
    private final String artifactId;
    private final String version;

    @Nullable
    private final String metadataPattern;

    private final boolean releasesOnly;

    @Nullable
    private final String classifier;

    @Nullable
    private final String scope;

    @Nullable
    private final String type;

    private final boolean skipIfPresent;

    @Nullable
    private final Pattern familyPattern;

    @Nullable
    private VersionComparator versionComparator;

    public AddDependencyVisitor(String groupId,
                                String artifactId,
                                String version,
                                @Nullable String metadataPattern,
                                boolean releasesOnly,
                                @Nullable String classifier,
                                @Nullable String scope,
                                @Nullable String type,
                                boolean skipIfPresent,
                                @Nullable Pattern familyPattern) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.metadataPattern = metadataPattern;
        this.releasesOnly = releasesOnly;
        this.classifier = classifier;
        this.scope = scope;
        this.type = type;
        this.skipIfPresent = skipIfPresent;
        this.familyPattern = familyPattern;
    }

    @Override
    public Maven visitMaven(Maven maven, ExecutionContext ctx) {
        model = maven.getModel();

        Validated versionValidation = Semver.validate(version, metadataPattern);
        if (versionValidation.isValid()) {
            versionComparator = versionValidation.getValue();
        }

        if (skipIfPresent && findDependencies(groupId, artifactId).stream().anyMatch(d ->
                version.equals(d.getVersion()) &&
                        (classifier == null || classifier.equals(d.getClassifier())) &&
                        d.getScope().isInClasspathOf(Scope.fromName(scope)))) {
            return maven;
        }

        Xml.Tag root = maven.getRoot();
        if (!root.getChild("dependencies").isPresent()) {
            doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<dependencies/>"),
                    new MavenTagInsertionComparator(root.getChildren())));
        }

        doAfterVisit(new InsertDependencyInOrder());

        Collection<Pom.Dependency> dependencies = new ArrayList<>(model.getDependencies());
        String packaging = (type == null) ? "jar" : type;
        dependencies.add(
                new Pom.Dependency(
                        null,
                        Scope.fromName(scope),
                        classifier,
                        type,
                        false,
                        new Pom(groupId, artifactId, version, null, packaging, classifier, null,
                                emptyList(), new Pom.DependencyManagement(emptyList()), emptyList(), emptyList(), emptyMap()),
                        version,
                        null,
                        emptySet()
                )
        );

        return maven.withModel(maven.getModel().withDependencies(dependencies));
    }

    private class InsertDependencyInOrder extends MavenVisitor {
        public InsertDependencyInOrder() {
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (DEPENDENCIES_MATCHER.matches(getCursor())) {
                String versionToUse = null;

                if (model.getManagedVersion(groupId, artifactId) == null) {
                    if (familyPattern != null) {
                        versionToUse = findDependencies(d -> familyPattern.matcher(d.getGroupId()).matches()).stream()
                                .max(Comparator.comparing(d -> new Version(d.getVersion())))
                                .map(Pom.Dependency::getRequestedVersion)
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
                                (scope == null ? "" :
                                        "<scope>" + scope + "</scope>\n") +
                                "</dependency>"
                );

                doAfterVisit(new AddToTagVisitor<>(tag, dependencyTag,
                        new InsertDependencyComparator(tag.getChildren(), dependencyTag)));

                return tag;
            }

            return super.visitTag(tag, ctx);
        }
    }

    private String findVersionToUse(String groupId, String artifactId, ExecutionContext ctx) {
        if (versionComparator == null) {
            return version;
        }

        MavenMetadata mavenMetadata = new MavenPomDownloader(MavenPomCache.NOOP,
                emptyMap(), ctx).downloadMetadata(groupId, artifactId, emptyList());

        LatestRelease latest = new LatestRelease(metadataPattern);
        return mavenMetadata.getVersioning().getVersions().stream()
                .filter(versionComparator::isValid)
                .filter(v -> !releasesOnly || latest.isValid(v))
                .max(versionComparator)
                .orElse(version);
    }
}
