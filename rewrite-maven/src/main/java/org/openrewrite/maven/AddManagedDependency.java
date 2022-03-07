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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddManagedDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.apache.logging.log4j:ARTIFACT_ID:VERSION'.",
            example = "org.apache.logging.log4j")
    private final String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.apache.logging.log4j:log4j-bom:VERSION'.",
            example = "log4j-bom")
    private final String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number, or node-style semver selector used to select the version number.",
            example = "latest.release")
    private final String version;

    @Option(displayName = "Scope",
            description = "A scope to use when it is not what can be inferred from usage. Most of the time this will be left empty, but " +
                    "is used when adding a runtime, provided, or import dependency.",
            example = "import",
            valid = {"import", "runtime", "provided", "test"},
            required = false)
    @Nullable
    private final String scope;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    private String versionPattern;

    @Option(displayName = "Type",
            description = "The type of dependency to add. If omitted Maven defaults to assuming the type is \"jar\".",
            valid = {"jar", "pom", "war"},
            example = "pom",
            required = false)
    @Nullable
    private String type;

    @Option(displayName = "Releases only",
            description = "Whether to exclude snapshots from consideration when using a semver selector",
            example = "true",
            required = false)
    @Nullable
    private Boolean releasesOnly;

    @Option(displayName = "Only if using glob pattern",
            description = "Used to determine if the dependency will be added and in which scope it should be placed.",
            example = "org.apache.logging.log4j.*")
    private String onlyIfUsing;

    @Option(displayName = "Classifier",
            description = "A Maven classifier to add. Most commonly used to select shaded or test variants of a library",
            example = "test",
            required = false)
    @Nullable
    private String classifier;

    @Option(displayName = "Add to the root pom",
            description = "Add to the root pom where root is the eldest parent of the pom within the source set.",
            example = "true",
            required = false)
    private Boolean addToRootPom;

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
        return "Add Managed Maven Dependency";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesType<>(onlyIfUsing);
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Map<JavaProject, Set<String>> sourceSetsByProject = new HashMap<>();
        final AtomicReference<SourceFile> rootPom = new AtomicReference<>();
        for (SourceFile source : before) {
            source.getMarkers().findFirst(JavaProject.class).ifPresent(javaProject ->
                    source.getMarkers().findFirst(JavaSourceSet.class).ifPresent(sourceSet -> {
                        if (source != new UsesType<>(onlyIfUsing).visit(source, ctx)) {
                            sourceSetsByProject.computeIfAbsent(javaProject, v -> new HashSet<>()).add(sourceSet.getName());
                        }
                    }));
            source.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(mavenResolutionResult -> {
                if (rootPom.get() == null && mavenResolutionResult.getParent() == null) {
                    rootPom.set(source);
                }
            });
        }

        if (sourceSetsByProject.isEmpty()) {
            return before;
        }

        return ListUtils.map(before, s -> s.getMarkers().findFirst(JavaProject.class)
                .map(javaProject -> (Tree) new MavenVisitor<ExecutionContext>() {
                    @Override
                    public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
                        Xml maven = super.visitDocument(document, executionContext);

                        if ("test".equals(scope) && !sourceSetsByProject.get(javaProject).contains("test")) {
                            return maven;
                        }

                        if (!addToRootPom || (rootPom.get() != null && rootPom.get().equals(document))) {
                            doAfterVisit(new AddManagedDependencyVisitor(groupId,artifactId,version,versionPattern,scope,releasesOnly,type,classifier));
                        }

                        return maven;
                    }
                }.visit(s, ctx))
                .map(SourceFile.class::cast)
                .orElse(s)
        );
    }
}
