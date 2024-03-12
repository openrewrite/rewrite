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
import lombok.Value;
import lombok.With;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * This recipe will detect the presence of Java types (in Java ASTs) to determine if a dependency should be added
 * to a maven build file. Java Provenance information is used to filter the type search to only those java ASTs that
 * have the same coordinates of that of the pom. Additionally, if a "scope" is specified in this recipe, the dependency
 * will only be added if there are types found in a given source set are transitively within that scope.
 * <p>
 * NOTE: IF PROVENANCE INFORMATION IS NOT PRESENT, THIS RECIPE WILL DO NOTHING.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddDependency extends ScanningRecipe<AddDependency.Scanned> {
    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
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
            required = false)
    @Nullable
    Boolean releasesOnly;

    @Option(displayName = "Only if using",
            description = "Used to determine if the dependency will be added and in which scope it should be placed.",
            example = "org.junit.jupiter.api.*",
            required = false)
    @Nullable
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

    @Option(displayName = "Accept transitive",
            description = "Default false. If enabled, the dependency will not be added if it is already on the classpath as a transitive dependency.",
            example = "true",
            required = false)
    @Nullable
    Boolean acceptTransitive;

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
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
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Add a Maven dependency to a `pom.xml` file in the correct scope based on where it is used.";
    }

    public static class Scanned {
        boolean usingType;
        Map<JavaProject, String> scopeByProject = new HashMap<>();
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (tree instanceof JavaSourceFile) {
                    if (onlyIfUsing == null || sourceFile != new UsesType<>(onlyIfUsing, true).visit(sourceFile, ctx)) {
                        acc.usingType = true;
                        sourceFile.getMarkers().findFirst(JavaProject.class).ifPresent(javaProject ->
                                sourceFile.getMarkers().findFirst(JavaSourceSet.class).ifPresent(sourceSet ->
                                        acc.scopeByProject.compute(javaProject, (jp, scope) -> "compile".equals(scope) ?
                                                scope /* a `compile` scope dependency will also be available in test source set */ :
                                                "test".equals(sourceSet.getName()) ? "test" : "compile"
                                        )
                                )
                        );
                    }
                }
                return sourceFile;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(onlyIfUsing == null || acc.usingType && !acc.scopeByProject.isEmpty(), new MavenVisitor<ExecutionContext>() {
            @Nullable
            final Pattern familyPatternCompiled = familyPattern == null ? null : Pattern.compile(familyPattern.replace("*", ".*"));

            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml maven = super.visitDocument(document, ctx);

                JavaProject javaProject = document.getMarkers().findFirst(JavaProject.class).orElse(null);
                String maybeScope = javaProject == null ? null : acc.scopeByProject.get(javaProject);
                if (maybeScope == null && !acc.scopeByProject.isEmpty()) {
                    return maven;
                }

                // If the dependency is already in compile scope it will be available everywhere, no need to continue
                for (ResolvedDependency d : getResolutionResult().getDependencies().get(Scope.Compile)) {
                    if (hasAcceptableTransitivity(d, acc)
                        && groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                        return maven;
                    }
                }

                String resolvedScope = scope == null ? maybeScope : scope;
                Scope resolvedScopeEnum = Scope.fromName(resolvedScope);
                if (resolvedScopeEnum == Scope.Provided || resolvedScopeEnum == Scope.Test) {
                    for (ResolvedDependency d : getResolutionResult().getDependencies().get(resolvedScopeEnum)) {
                        if (hasAcceptableTransitivity(d, acc)
                            && groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                            return maven;
                        }
                    }
                }

                return new AddDependencyVisitor(
                        groupId, artifactId, version, versionPattern, resolvedScope, releasesOnly,
                        type, classifier, optional, familyPatternCompiled, metadataFailures).visitNonNull(document, ctx);
            }
        });
    }

    private boolean hasAcceptableTransitivity(ResolvedDependency d, Scanned acc) {
        return d.isDirect() || Boolean.TRUE.equals(acceptTransitive) && !acc.scopeByProject.isEmpty();
    }
}
