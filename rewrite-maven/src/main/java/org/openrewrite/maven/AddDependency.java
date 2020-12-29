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
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.NoopCache;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.maven.internal.MavenDownloader;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.Version;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.HyphenRange;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTag;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Pattern;

import static java.util.Collections.*;
import static org.openrewrite.Validated.required;

/**
 * Adds a dependency if there is no dependency matching <code>groupId</code> and <code>artifactId</code>.
 * A matching dependency with a different version or scope does NOT have its version or scope updated.
 * Use {@link ChangeDependencyVersion} or {@link UpgradeDependencyVersion} in the case of a different version.
 * Use {@link ChangeDependencyScope} in the case of a different scope.
 * <p>
 * Places a new dependency as physically "near" to a group of similar dependencies as possible.
 */
@EqualsAndHashCode(callSuper = false)
public class AddDependency extends MavenRefactorVisitor {
    private static final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");

    private String groupId;
    private String artifactId;

    /**
     * When other modules exist from the same dependency family, defined as those dependencies whose
     * groupId matches {@link #familyPattern}, we ignore version and attempt to align the new dependency
     * with the highest version already in use.
     * <p>
     * To pull the whole family up to a later version, use {@link UpgradeDependencyVersion}.
     */
    private String version;

    @Nullable
    private String metadataPattern;

    private boolean releasesOnly = true;

    /**
     * Allows us to extend version selection beyond the original Node Semver semantics. So for example,
     * We can pair a {@link HyphenRange} of "25-29" with a metadata pattern of "-jre" to select
     * Guava 29.0-jre
     *
     * @param metadataPattern The metadata pattern extending semver selection.
     */
    public void setMetadataPattern(@Nullable String metadataPattern) {
        this.metadataPattern = metadataPattern;
    }

    @Nullable
    private VersionComparator versionComparator;

    @Nullable
    private String classifier;

    @Nullable
    private String scope;

    private boolean skipIfPresent = true;

    @Nullable
    private Pattern familyPattern;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(@Nullable String version) {
        this.version = version;
    }

    public void setClassifier(@Nullable String classifier) {
        this.classifier = classifier;
    }

    public void setScope(@Nullable String scope) {
        this.scope = scope;
    }

    public void setSkipIfPresent(boolean skipIfPresent) {
        this.skipIfPresent = skipIfPresent;
    }

    public void setReleasesOnly(boolean releasesOnly) {
        this.releasesOnly = releasesOnly;
    }

    /**
     * @param familyPattern A glob expression used to identify other dependencies in the same
     *                      family as the dependency to be added.
     */
    public void setFamilyPattern(@Nullable String familyPattern) {
        this.familyPattern = familyPattern == null ?
                null :
                Pattern.compile(familyPattern.replace("*", ".*"));
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("version", version)
                        .or(Semver.validate(version, metadataPattern)));
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public Maven visitMaven(Maven maven) {
        model = maven.getModel();
        downloader = maven.getDownloader();

        Validated versionValidation = Semver.validate(version, metadataPattern);
        if (versionValidation.isValid()) {
            versionComparator = versionValidation.getValue();
        }

        if (skipIfPresent && findDependencies(groupId, artifactId).stream()
                .anyMatch(d -> (version == null || version.equals(d.getVersion())) &&
                        (classifier == null || classifier.equals(d.getClassifier())) &&
                        d.getScope().isInClasspathOf(Scope.fromName(scope))
                )) {
            return maven;
        }

        Xml.Tag root = maven.getRoot();
        if (!root.getChild("dependencies").isPresent()) {
            andThen(new AddToTag.Scoped(root, Xml.Tag.build("<dependencies/>"),
                    new MavenTagInsertionComparator(root.getChildren())));
        }

        andThen(new InsertDependencyInOrder());

        Collection<Pom.Dependency> dependencies = new ArrayList<>(model.getDependencies());
        dependencies.add(
                new Pom.Dependency(
                        Scope.fromName(scope),
                        classifier,
                        false,
                        new Pom(null, groupId, artifactId, version, null, "jar", classifier, null,
                                emptyList(), new Pom.DependencyManagement(emptyList()), emptyList(), emptyList(), emptyMap()),
                        version,
                        emptySet()
                )
        );

        return maven.withModel(maven.getModel().withDependencies(dependencies));
    }

    private class InsertDependencyInOrder extends MavenRefactorVisitor {
        public InsertDependencyInOrder() {
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag) {
            if (DEPENDENCIES_MATCHER.matches(getCursor())) {
                String versionToUse = null;

                if (model.getManagedVersion(groupId, artifactId) == null) {
                    if (familyPattern != null) {
                        versionToUse = findDependencies(d -> familyPattern.matcher(d.getGroupId()).matches()).stream()
                                .max(Comparator.comparing(d -> new Version(d.getVersion())))
                                .map(Pom.Dependency::getRequestedVersion)
                                .orElse(versionToUse);
                    }

                    if (versionToUse == null) {
                        versionToUse = findVersionToUse(groupId, artifactId);
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

                andThen(new AddToTag.Scoped(tag, dependencyTag,
                        new InsertDependencyComparator(tag.getChildren(), dependencyTag)));

                return tag;
            }

            return super.visitTag(tag);
        }
    }

    private String findVersionToUse(String groupId, String artifactId) {
        if (versionComparator == null) {
            return version;
        }

        MavenMetadata mavenMetadata = downloader.downloadMetadata(groupId, artifactId, emptyList());

        LatestRelease latest = new LatestRelease(metadataPattern);
        return mavenMetadata.getVersioning().getVersions().stream()
                .filter(versionComparator::isValid)
                .filter(v -> !releasesOnly || latest.isValid(v))
                .max(versionComparator)
                .orElse(version);
    }
}
