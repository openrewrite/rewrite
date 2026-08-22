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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.internal.StageGraph;
import org.openrewrite.docker.table.StageDependencies;
import org.openrewrite.docker.trait.ImageName;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

import static java.lang.String.join;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindStageGraph extends Recipe {

    transient StageDependencies stageDependencies = new StageDependencies(this);

    final String displayName = "Find Docker build stage dependencies";

    final String description = "Record which build stages of a multi-stage Dockerfile depend on which others, and mark the stages " +
            "the file never reaches. A stage is reached when it is the last stage of the file, or when a stage that is " +
            "itself reached names it in a `FROM`, a `COPY --from`, or a `RUN --mount=...,from=` flag. Where a reference " +
            "cannot be resolved without guessing, either because a build argument spells it (`COPY --from=$BUILDER`) or " +
            "because it names a stage by a position that moves when stages are removed (`COPY --from=0`), every stage in " +
            "that file is reported as reached. Only the Dockerfile is read, so a stage that exists to be built on its " +
            "own with `docker build --target` is reported as unreached: this says what the file builds, not what a " +
            "repository asks for.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                StageGraph graph = StageGraph.of(file);
                Set<Integer> reachable = graph.reachable();

                String sourcePath = file.getSourcePath().toString();
                return file.withStages(ListUtils.map(file.getStages(), (i, stage) -> {
                    stageDependencies.insertRow(ctx, new StageDependencies.Row(
                            sourcePath,
                            graph.getName(i),
                            i,
                            baseImage(stage.getFrom()),
                            graph.extendsStage(i) ? "" : registry(stage.getFrom()),
                            join(",", graph.getReferencedBy(i)),
                            reachable.contains(i)
                    ));
                    return reachable.contains(i) ? stage :
                            stage.withFrom(SearchResult.found(stage.getFrom(), "unreached"));
                }));
            }
        };
    }

    private static String registry(Docker.From from) {
        return ImageName.parse(ArgumentContents.textWithVariables(from.getImageName())).getResolvedRegistry();
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
