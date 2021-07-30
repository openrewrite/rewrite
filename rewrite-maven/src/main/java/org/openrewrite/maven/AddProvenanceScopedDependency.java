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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaProvenance;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.InsertDependencyComparator;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.Version;
import org.openrewrite.maven.search.DependencyInsight;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.*;

/**
 * This recipe will detect the presence of Java types (in Java ASTs) to determine if a dependency should be added
 * to a maven build file. Java Provenance information is used to filter the type search to only those java ASTs that
 * have the same coordinates of that of the pom. Additionally, if a "scope" is specified in this recipe, the dependency
 * will only be added if there are types found in a given source set are transitively within that scope.
 *
 * NOTE: IF PROVENANCE INFORMATION IS NOT PRESENT, THIS RECIPE WILL DO NOTHING.
 */
@Incubating(since = "7.10.0")
@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@EqualsAndHashCode(callSuper = true)
public class AddProvenanceScopedDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.guava")
    private final String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "guava")
    private final String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number, or node-style semver selector used to select the version number.",
            example = "29.X")
    private final String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    @With
    private String versionPattern;

    @Option(displayName = "Releases only",
            description = "Whether to exclude snapshots from consideration when using a semver selector",
            example = "true",
            required = false)
    @With
    private boolean releasesOnly = true;

    @Option(displayName = "Scope",
            description = "The maven dependency scope to add the dependency to.",
            valid = {"compile", "test", "runtime", "provided"},
            example = "compile",
            required = false)
    @Nullable
    @With
    private String scope;

    @Option(displayName = "Type match expressions",
            description = "A list of fully qualified type names or glob expressions used to determine if the dependency will be added and which scope it should be placed.",
            example = "org.junit.jupiter.api.*",
            required = false)
    private List<String> typeMatchExpressions;

    @Option(displayName = "Type",
            description = "The type of dependency to add. If omitted Maven defaults to assuming the type is \"jar\".",
            valid = {"jar", "pom"},
            example = "jar",
            required = false)
    @Nullable
    @With
    private String type;

    @Option(displayName = "Classifier",
            description = "A Maven classifier to add. Most commonly used to select shaded or test variants of a library",
            example = "test",
            required = false)
    @Nullable
    @With
    private String classifier;

    @Option(displayName = "Optional",
            description = "Set the value of the `<optional>` tag. No `<optional>` tag will be added when this is `null`.",
            example = "true",
            required = false)
    @Nullable
    private Boolean optional;

    /**
     * A glob expression used to identify other dependencies in the same family as the dependency to be added.
     */
    @Option(displayName = "Family pattern",
            description = "A pattern, applied to groupIds, used to determine which other dependencies should have aligned version numbers. " +
                    "Accepts '*' as a wildcard character.",
            example = "com.fasterxml.jackson*",
            required = false)
    @Nullable
    @With
    private String familyPattern;

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        //noinspection ConstantConditions
        if (version != null) {
            validated = validated.or(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Add Maven dependency";
    }

    @Override
    public String getDescription() {
        return "Use type matching rules to conditionally add a maven dependency to a pom.xml file.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                for (String s : typeMatchExpressions) {
                    doAfterVisit(new UsesType<>(s));
                }
                return cu;
            }
        };
    }


    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {

        //Collect unique provenance information for any java sources that have matching types.
        Set<JavaProvenance> provenanceInfo = new HashSet<>();
        for (SourceFile source : before) {
            Optional<JavaProvenance> provenance = source.getMarkers().findFirst(JavaProvenance.class);
            if (provenance.isPresent() && !provenanceInfo.contains(provenance.get()) && provenance.get().getPublication() != null) {
                for (String s : typeMatchExpressions) {
                    if (source != new UsesType<>(s).visit(source, ctx)) {
                        provenanceInfo.add(provenance.get());
                    }
                }
            }
        }

        if (provenanceInfo.isEmpty()) {
            return before;
        }
        Pattern pattern = this.familyPattern == null ? null : Pattern.compile(this.familyPattern.replace("*", ".*"));

        return ListUtils.map(before, s -> {
            if (!(s instanceof Maven)) {
                return s;
            }
            return (Maven) new AddDependencyVisitor(provenanceInfo, pattern).visit(s, ctx);
        });
    }

    private static final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");

    public class AddDependencyVisitor extends MavenVisitor {

        Set<JavaProvenance> provenanceInfo;

        @Nullable
        private VersionComparator versionComparator;
        @Nullable
        private String resolvedVersion;
        @Nullable
        private Pattern familyRegex;

        private AddDependencyVisitor(Set<JavaProvenance> provenanceInfo, @Nullable Pattern familyRegex) {
            this.provenanceInfo = provenanceInfo;
            this.familyRegex = familyRegex;
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {

            Maven m = super.visitMaven(maven, ctx);

            Scope dependencyScope = Scope.fromName(scope);
            Set<JavaProvenance> filteredProvenance = provenanceInfo.stream().filter(p -> {
                        Scope provenanceScope = "test".equals(p.getSourceSetName()) ? Scope.Test : Scope.Compile;

                        return p.getPublication() != null &&
                                p.getPublication().getGroupId().equals(model.getGroupId()) &&
                                p.getPublication().getArtifactId().equals(model.getArtifactId()) &&
                                (dependencyScope == provenanceScope || provenanceScope.isInClasspathOf(dependencyScope));
                    }
            ).collect(Collectors.toSet());


            if (filteredProvenance.isEmpty() || DependencyInsight.isDependencyPresent(m, groupId, artifactId, scope)) {
                //Do not add the dependency if there are no types with matching coordinates/scope or if the dependency
                //already exists (as a first order or transitive dependency)
                return m;
            }

            Pom ancestor = model.getParent();
            while (ancestor != null) {
                if (ancestor.getDependencies().stream().anyMatch(d -> groupId.equals(d.getGroupId()) &&
                        artifactId.equals(d.getArtifactId()))) {
                    return m;
                }
                ancestor = ancestor.getParent();
            }

            Validated versionValidation = Semver.validate(version, versionPattern);
            if (versionValidation.isValid()) {
                versionComparator = versionValidation.getValue();
            }


            Xml.Tag root = m.getRoot();
            if (!root.getChild("dependencies").isPresent()) {
                doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<dependencies/>"),
                        new MavenTagInsertionComparator(root.getChildren())));
            }

            doAfterVisit(new InsertDependencyInOrder());

            Collection<Pom.Dependency> dependencies = new ArrayList<>(model.getDependencies());
            String packaging = (type == null) ? "jar" : type;

            String dependencyVersion = findVersionToUse(groupId, artifactId, ctx);
            dependencies.add(
                    new Pom.Dependency(
                            null,
                            Scope.fromName(scope),
                            classifier,
                            type,
                            optional != null && optional,
                            new Pom(groupId, artifactId, dependencyVersion, null, null, null, packaging, classifier, null,
                                    emptyList(), new Pom.DependencyManagement(emptyList()), emptyList(), emptyList(), emptyMap(), emptyMap()),
                            dependencyVersion,
                            emptySet()
                    )
            );

            return m.withModel(m.getModel().withDependencies(dependencies));
        }

        private String findVersionToUse(String groupId, String artifactId, ExecutionContext ctx) {
            if (resolvedVersion == null) {

                if (versionComparator == null) {
                    resolvedVersion = version;
                } else {

                    MavenMetadata mavenMetadata = new MavenPomDownloader(MavenPomCache.NOOP,
                            emptyMap(), ctx).downloadMetadata(groupId, artifactId, emptyList());

                    LatestRelease latest = new LatestRelease(versionPattern);
                    resolvedVersion = mavenMetadata.getVersioning().getVersions().stream()
                            .filter(versionComparator::isValid)
                            .filter(v -> !releasesOnly || latest.isValid(v))
                            .max(versionComparator)
                            .orElse(version);
                }
            }
            return resolvedVersion;
        }

        private class InsertDependencyInOrder extends MavenVisitor {

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (DEPENDENCIES_MATCHER.matches(getCursor())) {
                    String versionToUse = null;

                    if (model.getManagedVersion(groupId, artifactId) == null) {
                        if (familyRegex != null) {
                            versionToUse = findDependencies(d -> familyRegex.matcher(d.getGroupId()).matches()).stream()
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
                                    (optional == null ? "" :
                                            "<optional>" + optional + "</optional>\n") +
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
}
