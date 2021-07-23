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
import org.openrewrite.java.marker.JavaProvenance;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.Semver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This recipe will detect the presence of Java types (in Java ASTs) to determine if a dependency should be added
 * to a maven build file. If provenance information is available, the recipe will scope the search of types and will
 * also determine the dependency's scope.
 */
@Incubating(since = "7.10.0")
@Getter
@RequiredArgsConstructor
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@EqualsAndHashCode(callSuper = true)
public class AddTypeBasedDependency extends Recipe {

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

    @Option(displayName = "Type match expressions",
            description = "A list of fully qualified type names or glob expressions used to determine if the dependency will be added and which scope it should be placed.",
            example = "org.junit.jupiter.api.*",
            required = false)
    private List<String> typeMatchExpressions;

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
        if (typeMatchExpressions != null) {
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
        return null;
    }

    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Set<JavaProvenance> provenanceInfo = new HashSet<>();
        for (SourceFile source : before) {
            if (source instanceof J.CompilationUnit) {
                J.CompilationUnit cu = (J.CompilationUnit) source;
            }
        }
        return before;
    }
}
