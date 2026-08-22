/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.internal.BuildTargets;
import org.openrewrite.docker.internal.StageGraph;
import org.openrewrite.docker.table.StageDependencies;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.join;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindStageGraph extends ScanningRecipe<Set<String>> {

    transient StageDependencies stageDependencies = new StageDependencies(this);

    final String displayName = "Find Docker build stage dependencies";

    final String description = "Record which build stages of a multi-stage Dockerfile depend on which others, and mark the stages " +
            "nothing builds. A stage is built when it is the last stage of the file, when a stage that is itself " +
            "built names it in a `FROM`, a `COPY --from`, or a `RUN --mount=...,from=` flag, or when something in " +
            "the repository asks for it by name: a `docker build --target`, a `docker-bake.hcl`, a compose file, " +
            "a CI workflow, or a comment in the Dockerfile showing how to build it. Where a reference cannot be " +
            "resolved without guessing, either because a build argument spells it (`COPY --from=$BUILDER`) or " +
            "because it names a stage by a position that moves when stages are removed (`COPY --from=0`), every " +
            "stage in that file is reported as built. A stage nothing builds is dead weight that classic builds " +
            "still build. This reaches the same verdict as `RemoveUnusedStages`, so reading it across a fleet " +
            "shows what that recipe would delete before it deletes anything.";

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> builtStageNames) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    BuildTargets.scan((SourceFile) tree, builtStageNames);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> builtStageNames) {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                StageGraph graph = StageGraph.of(file);
                Set<String> built = new HashSet<>(builtStageNames);
                built.addAll(BuildTargets.inComments(file));
                Set<Integer> reachable = graph.reachableGiven(built);

                String sourcePath = file.getSourcePath().toString();
                return file.withStages(ListUtils.map(file.getStages(), (i, stage) -> {
                    String name = graph.getName(i);
                    stageDependencies.insertRow(ctx, new StageDependencies.Row(
                            sourcePath,
                            name,
                            i,
                            baseImage(stage.getFrom()),
                            join(",", graph.getReferencedBy(i)),
                            name != null && built.contains(name),
                            reachable.contains(i)
                    ));
                    return reachable.contains(i) ? stage :
                            stage.withFrom(SearchResult.found(stage.getFrom(), "unused"));
                }));
            }
        };
    }

    private static String baseImage(Docker.From from) {
        StringBuilder image = new StringBuilder(ArgumentContents.textWithVariables(from.getImageName()));
        if (from.getTag() != null) {
            image.append(':').append(ArgumentContents.textWithVariables(from.getTag()));
        }
        if (from.getDigest() != null) {
            image.append('@').append(ArgumentContents.textWithVariables(from.getDigest()));
        }
        return image.toString();
    }
}
