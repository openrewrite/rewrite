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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.semver.Semver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

@Incubating(since = "7.0.0")
@Getter
@RequiredArgsConstructor
@AllArgsConstructor(onConstructor_ = @JsonCreator)
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

    /**
     * When other modules exist from the same dependency family, defined as those dependencies whose groupId matches
     * {@link #familyPattern}, this recipe will ignore the version attribute and attempt to align the new dependency
     * with the highest version already in use.
     * <p>
     * To pull the whole family up to a later version, use {@link UpgradeDependencyVersion}.
     */
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
            description = "Whether to exclude snapshots from consideration.",
            required = false)
    @With
    private boolean releasesOnly = true;

    @Option(displayName = "Classifier",
            description = "A Maven classifier to add. Most commonly used to select shaded or test variants of a library",
            example = "test",
            required = false)
    @Nullable
    @With
    private String classifier;

    @Option(displayName = "Scope",
            valid = {"compile", "test", "runtime", "provided"},
            required = false)
    @Nullable
    @With
    private String scope;

    @Option(displayName = "Type",
            valid = {"jar", "pom"},
            required = false)
    @Nullable
    @With
    private String type;

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

    @Option(displayName = "Only if using",
            description = "Add the dependency only if using one of the supplied types. Types should be identified by fully qualified class name or a glob expression",
            example = "org.junit.jupiter.api.*",
            required = false)
    @Nullable
    private List<String> onlyIfUsing;

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
    protected void afterRecipe(ExecutionContext executionContext) {
        if (onlyIfUsing != null && !onlyIfUsing.isEmpty()
            && onlyIfUsing.stream().filter(fqn -> foundTypesContainsFullyQualified(executionContext, fqn)).findAny().isPresent()) {
                doNext(new AddDependency(groupId,
                        artifactId,
                        version,
                        versionPattern,
                        releasesOnly,
                        classifier,
                        scope,
                        type,
                        familyPattern,
                        null));
        }
    }

    protected boolean foundTypesContainsFullyQualified(ExecutionContext executionContext, String fullyQualifiedClassName) {
        return executionContext.getMessage(JavaType.FOUND_TYPE_CONTEXT_KEY, emptySet()).stream()
                .filter(JavaType.FullyQualified.class::isInstance).map(JavaType.FullyQualified.class::cast)
                .filter(f -> f.getFullyQualifiedName().equals(fullyQualifiedClassName)).findAny().isPresent();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> visitor;
        if (onlyIfUsing != null && !onlyIfUsing.isEmpty()) {
            // Find any specified types which have not been found
            visitor = new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                    onlyIfUsing.forEach(fqn -> {
                        if (!foundTypesContainsFullyQualified(executionContext, fqn)) {
                            executionContext.getMessage(JavaType.FOUND_TYPE_CONTEXT_KEY, new HashSet<>())
                                .addAll(FindTypes.find(cu, fqn).stream().map(TypeTree.class::cast).map(TypeTree::getType).collect(Collectors.toSet()));
                        }
                    });
                    return cu;
                }
            };
        } else {
            visitor = new AddDependencyVisitor(
                    groupId,
                    artifactId,
                    version,
                    versionPattern,
                    releasesOnly,
                    classifier,
                    scope,
                    type,
                    familyPattern == null ? null : Pattern.compile(familyPattern.replace("*", ".*"))
            );
        }
        return visitor;
    }
}
