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

import lombok.*;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
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

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.*;

/**
 * This recipe will detect the presence of Java types (in Java ASTs) to determine if a dependency should be added
 * to a maven build file. Java Provenance information is used to filter the type search to only those java ASTs that
 * have the same coordinates of that of the pom. Additionally, if a "scope" is specified in this recipe, the dependency
 * will only be added if there are types found in a given source set are transitively within that scope.
 * <p>
 * NOTE: IF PROVENANCE INFORMATION IS NOT PRESENT, THIS RECIPE WILL DO NOTHING.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddDependency extends Recipe {

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
    private String versionPattern;

    @Option(displayName = "Scope",
            description = "A scope to use when it is not what can be inferred from usage. Most of the time this will be left empty, but " +
                    "is used when adding a runtime, provided, or import dependency.",
            example = "runtime",
            valid = {"import", "runtime", "provided"},
            required = false)
    @Nullable
    private final String scope;

    @Option(displayName = "Releases only",
            description = "Whether to exclude snapshots from consideration when using a semver selector",
            example = "true",
            required = false)
    @Nullable
    private Boolean releasesOnly;

    @Option(displayName = "Only if using",
            description = "Used to determine if the dependency will be added and in which scope it should be placed.",
            example = "org.junit.jupiter.api.*")
    private String onlyIfUsing;

    @Option(displayName = "Type",
            description = "The type of dependency to add. If omitted Maven defaults to assuming the type is \"jar\".",
            valid = {"jar", "pom"},
            example = "jar",
            required = false)
    @Nullable
    private String type;

    @Option(displayName = "Classifier",
            description = "A Maven classifier to add. Most commonly used to select shaded or test variants of a library",
            example = "test",
            required = false)
    @Nullable
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
        return "Add a maven dependency to a `pom.xml` file in the correct scope based on where it is used.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesType<>(onlyIfUsing);
    }

    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Map<JavaProject, String> scopeByProject = new HashMap<>();
        for (SourceFile source : before) {
            source.getMarkers().findFirst(JavaProject.class).ifPresent(javaProject ->
                    source.getMarkers().findFirst(JavaSourceSet.class).ifPresent(sourceSet -> {
                        if (source != new UsesType<>(onlyIfUsing).visit(source, ctx)) {
                            scopeByProject.compute(javaProject, (jp, scope) -> "compile".equals(scope) ?
                                    scope /* a compile scope dependency will also be available in test source set */ :
                                    sourceSet.getName().equals("test") ? "test" : "compile"
                            );
                        }
                    }));
        }

        if (scopeByProject.isEmpty()) {
            return before;
        }

        Pattern pattern = this.familyPattern == null ? null : Pattern.compile(this.familyPattern.replace("*", ".*"));

        return ListUtils.map(before, s -> s.getMarkers().findFirst(JavaProject.class)
                .map(javaProject -> {
                    if (s instanceof Maven) {
                        Pom ancestor = ((Maven) s).getMavenModel().getPom();
                        while (ancestor != null) {
                            for (Pom.Dependency d : ancestor.getDependencies(Scope.Compile)) {
                                if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                                    return s;
                                }
                            }
                            for (Pom.Dependency d : ancestor.getDependencies(Scope.Test)) {
                                if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                                    return s;
                                }
                            }
                            ancestor = ancestor.getParent();
                        }

                        String scope = this.scope == null ? scopeByProject.get(javaProject) : this.scope;
                        return scope == null ? s : (SourceFile) new AddDependencyVisitor(scope, pattern).visit(s, ctx);
                    }
                    return s;
                })
                .orElse(s)
        );
    }

    private static final XPathMatcher DEPENDENCIES_MATCHER = new XPathMatcher("/project/dependencies");

    @RequiredArgsConstructor
    private class AddDependencyVisitor extends MavenVisitor {
        private final String scope;

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

            List<Pom.Dependency> dependencies = new ArrayList<>(model.getDependencies());
            String packaging = (type == null) ? "jar" : type;

            String dependencyVersion = findVersionToUse(groupId, artifactId, ctx);
            dependencies.add(
                    new Pom.Dependency(
                            null,
                            Scope.fromName(scope),
                            classifier,
                            type,
                            optional != null && optional,
                            Pom.build(groupId, artifactId, dependencyVersion, null, packaging, classifier),
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
                            .filter(v -> !Boolean.TRUE.equals(releasesOnly) || latest.isValid(v))
                            .max(versionComparator)
                            .orElse(version);
                }
            }
            return resolvedVersion;
        }

        @RequiredArgsConstructor
        private class InsertDependencyInOrder extends MavenVisitor {
            private final String scope;

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
                                    (scope.equals("compile") ? "" :
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
}
