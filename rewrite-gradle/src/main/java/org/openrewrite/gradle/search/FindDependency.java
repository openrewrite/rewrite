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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDependency extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate identifying its publisher.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate uniquely identifying it among artifacts from the same publisher.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in. If omitted then all configurations will be searched.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    transient FoundDependencyReport foundDependencyTable = new FoundDependencyReport(this);

    @Override
    public String getDisplayName() {
        return "Find Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Finds dependencies declared in gradle build files. See the [reference](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph) on Gradle configurations or the diagram below for a description of what configuration to use. " +
                "A project's compile and runtime classpath is based on these configurations.\n\n<img alt=\"Gradle compile classpath\" src=\"https://docs.gradle.org/current/userguide/img/java-library-ignore-deprecated-main.png\" width=\"200px\"/>\n" +
                "A project's test classpath is based on these configurations.\n\n<img alt=\"Gradle test classpath\" src=\"https://docs.gradle.org/current/userguide/img/java-library-ignore-deprecated-test.png\" width=\"200px\"/>.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        GradleDependency.Matcher matcher = new GradleDependency.Matcher();
        return matcher
                .groupId(groupId)
                .artifactId(artifactId)
                .configuration(configuration)
                .asVisitor((gd, ctx) -> {
                    foundDependencyTable.insertRow(ctx, new Row(getDeclaringFilePath(gd), gd.getResolvedDependency().getGroupId(), gd.getResolvedDependency().getArtifactId(), gd.getResolvedDependency().getVersion()));
                    return SearchResult.found(gd.getTree());
                });
    }

    private String getDeclaringFilePath(GradleDependency gd) {
        G.CompilationUnit gcu = gd.getCursor().firstEnclosing(G.CompilationUnit.class);
        if (gcu != null) {
            return gcu.getSourcePath().toString();
        }
        K.CompilationUnit kcu = gd.getCursor().firstEnclosing(K.CompilationUnit.class);
        if (kcu != null) {
            return kcu.getSourcePath().toString();
        }
        return "";
    }

    private static class FoundDependencyReport extends DataTable<Row> {
        public FoundDependencyReport(Recipe recipe) {
            super(recipe, "Dependencies found", "Dependencies found matching the groupId and artifactId");
        }
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "Full path to the file declaring the dependency.")
        String sourcePath;

        @Column(displayName = "GroupId",
                description = "The groupId of the dependency.")
        String groupId;

        @Column(displayName = "ArtifactId",
                description = "The artifactId of the dependency.")
        String artifactId;

        @Column(displayName = "Version",
                description = "The version of the dependency.")
        String version;
    }
}
