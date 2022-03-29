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

import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

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
public class AddDependencyNoQuestionsAsked extends Recipe {

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
            example = "29.X",
            required = false)
    private final String version;

    @Option(displayName = "Scope",
            description = "A scope to use when it is not what can be inferred from usage. Most of the time this will be left empty, but " +
                    "is used when adding a runtime, provided, or import dependency.",
            example = "runtime",
            valid = {"import", "runtime", "provided"},
            required = false)
    @Nullable
    private final String scope;

    @Option(displayName = "Type",
            description = "The type of dependency to add. If omitted Maven defaults to assuming the type is \"jar\".",
            valid = {"jar", "pom", "war"},
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

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        //noinspection ConstantConditions
        if (version != null) {
            validated = validated.or(Semver.validate(version, null));
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
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        return ListUtils.map(before, s -> s.getMarkers().findFirst(MavenResolutionResult.class)
                .map(javaProject -> (Tree) new MavenVisitor<ExecutionContext>() {
                    @Override
                    public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
                        Xml maven = super.visitDocument(document, executionContext);

                        doAfterVisit(new AddDependencyNoQuestionsAskedVisitor(
                                groupId, artifactId, version, scope,
                                type, classifier, optional));

                        return maven;
                    }
                }.visit(s, ctx))
                .map(SourceFile.class::cast)
                .orElse(s)
        );
    }
}
