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
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * This recipe will detect the presence of Java types (in Java ASTs) to determine if a dependency should be added
 * to a maven build file. Java Provenance information is used to filter the type search to only those java ASTs that
 * have the same coordinates of that of the pom. Additionally, if a "scope" is specified in this recipe, the dependency
 * will only be added if there are types found in a given source set are transitively within that scope.
 * <p>
 * NOTE: IF PROVENANCE INFORMATION IS NOT PRESENT, THIS RECIPE WILL DO NOTHING.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class AddDependency extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Scope",
            description = "A scope to use when it is not what can be inferred from usage. Most of the time this will be left empty, but " +
                    "is used when adding a runtime, provided, or import dependency.",
            example = "runtime",
            valid = {"import", "runtime", "provided"},
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Releases only",
            description = "Whether to exclude snapshots from consideration when using a semver selector",
            example = "true",
            required = false)
    @Nullable
    Boolean releasesOnly;

    @Option(displayName = "Only if using",
            description = "Used to determine if the dependency will be added and in which scope it should be placed.",
            example = "org.junit.jupiter.api.*")
    String onlyIfUsing;

    @Option(displayName = "Type",
            description = "The type of dependency to add. If omitted Maven defaults to assuming the type is \"jar\".",
            valid = {"jar", "pom", "war"},
            example = "jar",
            required = false)
    @Nullable
    String type;

    @Option(displayName = "Classifier",
            description = "A Maven classifier to add. Most commonly used to select shaded or test variants of a library",
            example = "test",
            required = false)
    @Nullable
    String classifier;

    @Option(displayName = "Optional",
            description = "Set the value of the `<optional>` tag. No `<optional>` tag will be added when this is `null`.",
            example = "true",
            required = false)
    @Nullable
    Boolean optional;

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
    String familyPattern;

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
                                    "test".equals(sourceSet.getName()) ? "test" : "compile"
                            );
                        }
                    }));
        }

        if (scopeByProject.isEmpty()) {
            return before;
        }

        Pattern familyPatternCompiled = this.familyPattern == null ? null : Pattern.compile(this.familyPattern.replace("*", ".*"));

        return ListUtils.map(before, s -> s.getMarkers().findFirst(JavaProject.class)
                .map(javaProject -> (Tree) new MavenVisitor<ExecutionContext>() {
                    @Override
                    public Xml visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                        if(isDependencyTag() &&
                           groupId.equals(tag.getChildValue("groupId").orElse(null)) &&
                           artifactId.equals(tag.getChildValue("artifactId").orElse(null)) &&
                           (scope == null || scope.equals(tag.getChildValue("scope").orElse(null)))) {
                            getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "alreadyHasDependency", true);
                        }
                        return super.visitTag(tag, executionContext);
                    }

                    @Override
                    public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
                        Xml maven = super.visitDocument(document, executionContext);

                        String maybeScope = scopeByProject.get(javaProject);
                        if (maybeScope == null) {
                            return maven;
                        }

                        if(getCursor().getMessage("alreadyHasDependency", false)) {
                            return maven;
                        }

                        //If the dependency is already in scope, no need to continue.
                        for (ResolvedDependency d : getResolutionResult().getDependencies().get(Scope.Compile)) {
                            if (d.isDirect() && groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                                return maven;
                            }
                        }

                        String resolvedScope = scope == null ? maybeScope : scope;
                        Scope resolvedScopeEnum = Scope.fromName(resolvedScope);
                        if (resolvedScopeEnum == Scope.Provided || resolvedScopeEnum == Scope.Test) {
                            for (ResolvedDependency d : getResolutionResult().getDependencies().get(resolvedScopeEnum)) {
                                if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                                    return maven;
                                }
                            }
                        }

                        return new AddDependencyVisitor(
                                groupId, artifactId, version, versionPattern, resolvedScope, releasesOnly,
                                type, classifier, optional, familyPatternCompiled, metadataFailures).visitNonNull(s, ctx);
                    }
                }.visit(s, ctx))
                .map(SourceFile.class::cast)
                .orElse(s)
        );
    }
}
