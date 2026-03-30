/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.search.EffectiveMavenRepositoriesTable;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.StringJoiner;

import static org.openrewrite.PathUtils.separatorsToUnix;

@Value
@EqualsAndHashCode(callSuper = false)
public class EffectiveGradleRepositories extends Recipe {

    String displayName = "List effective Gradle project repositories";

    String description = "Lists the Gradle project repositories that would be used for dependency resolution, in order of precedence. " +
               "This includes Maven repositories defined in the Gradle build files and settings as determined when the LST was produced.";

    @Option(displayName = "Use markers",
            description = "Whether to add markers for each effective Gradle repository to the build file. Default `false`.",
            required = false)
    @Nullable
    Boolean useMarkers;

    transient EffectiveMavenRepositoriesTable table = new EffectiveMavenRepositoriesTable(this);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new IsBuildGradle<>(),
                new TreeVisitor<Tree, ExecutionContext>() {
                    @Override
                    public Tree preVisit(Tree tree, ExecutionContext ctx) {
                        stopAfterPreVisit();
                        if (!(tree instanceof SourceFile)) {
                            return tree;
                        }

                        SourceFile sourceFile = (SourceFile) tree;
                        StringJoiner repositories = new StringJoiner("\n");
                        String path = separatorsToUnix(sourceFile.getSourcePath().toString());

                        // Check if this is a build.gradle file
                        GradleProject gradleProject = sourceFile.getMarkers().findFirst(GradleProject.class).orElse(null);
                        if (gradleProject != null) {
                            for (MavenRepository repository : gradleProject.getMavenRepositories()) {
                                repositories.add(repository.getUri());
                                table.insertRow(ctx, new EffectiveMavenRepositoriesTable.Row(
                                        path,
                                        repository.getUri()));
                            }
                        }

                        if (Boolean.TRUE.equals(useMarkers) && repositories.length() > 0) {
                            return SearchResult.found(sourceFile, repositories.toString());
                        }

                        return sourceFile;
                    }
                }
        );
    }
}
