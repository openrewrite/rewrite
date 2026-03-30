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

import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.table.MavenRepositoryOrder;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.List;
import java.util.StringJoiner;

public class FindRepositoryOrder extends Recipe {
    transient MavenRepositoryOrder repositoryOrder = new MavenRepositoryOrder(this);

    @Override
    public String getDisplayName() {
        return "Gradle repository order";
    }

    @Override
    public String getDescription() {
        return "Determine the order in which dependencies will be resolved for each `build.gradle` " +
               "based on its defined repositories as determined when the LST was produced.";
    }

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
                        GradleProject gradleProject = sourceFile.getMarkers()
                                .findFirst(GradleProject.class).orElse(null);
                        if (gradleProject == null) {
                            return sourceFile;
                        }

                        List<MavenRepository> repositories = gradleProject.getMavenRepositories();
                        if (repositories.isEmpty()) {
                            return sourceFile;
                        }

                        StringJoiner uris = new StringJoiner("\n");
                        int i = 0;
                        for (MavenRepository repository : repositories) {
                            repositoryOrder.insertRow(ctx, new MavenRepositoryOrder.Row(
                                    repository.getId(),
                                    repository.getUri(),
                                    repository.isKnownToExist(),
                                    i++
                            ));
                            uris.add(repository.getUri());
                        }

                        return SearchResult.found(sourceFile, uris.toString());
                    }
                }
        );
    }
}
