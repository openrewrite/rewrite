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
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.internal.BuildTargets;
import org.openrewrite.docker.internal.StageGraph;
import org.openrewrite.docker.tree.Comment;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveUnusedStages extends ScanningRecipe<Set<String>> {

    @Override
    public String getDisplayName() {
        return "Remove unused Docker build stages";
    }

    @Override
    public String getDescription() {
        return "Remove build stages that nothing builds. A stage is built when it is the last stage of the file, " +
               "when a stage that is itself built names it in a `FROM`, a `COPY --from`, or a `RUN --mount=...,from=` " +
               "flag, or when something in the repository asks for it by name: a `docker build --target`, a " +
               "`docker-bake.hcl`, a compose file, a CI workflow, or a comment in the Dockerfile showing how to " +
               "build it. The rest are dead weight that classic builds still build. Reachability is transitive, so " +
               "a chain of stages that only the removed stages reached goes with them. A file is left alone entirely " +
               "where a reference cannot be resolved without guessing, either because a build argument spells it " +
               "(`COPY --from=$BUILDER`) or because it names a stage by a position that removing a stage would move " +
               "(`COPY --from=0`), and where the parser left text it could not place. A build that names a target " +
               "somewhere this recipe cannot read, such as a developer's shell history, is still invisible to it.";
    }

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
                Set<Integer> keep = graph.reachableGiven(built);

                List<Docker.Stage> stages = file.getStages();
                List<Docker.Stage> kept = new ArrayList<>(stages.size());
                Space removedLeadingPrefix = null;
                for (int i = 0; i < stages.size(); i++) {
                    Docker.Stage stage = stages.get(i);
                    if (keep.contains(i)) {
                        if (kept.isEmpty() && removedLeadingPrefix != null) {
                            stage = stage.withPrefix(inheritPrefix(removedLeadingPrefix, stage.getPrefix()));
                        }
                        kept.add(stage);
                    } else if (kept.isEmpty() && removedLeadingPrefix == null) {
                        removedLeadingPrefix = stage.getPrefix();
                    }
                }
                return kept.size() == stages.size() ? file : file.withStages(kept);
            }
        };
    }

    /// The whitespace and comments above the first stage of a file are not the stage's own: they hold a parser
    /// directive, and removing that stage would take the directive with it. Where a leading stage goes, its prefix
    /// stays and stands in for the lead-in of whichever stage becomes the first.
    private static Space inheritPrefix(Space removed, Space kept) {
        List<Comment> keptComments = kept.getComments();
        if (keptComments.isEmpty()) {
            return removed;
        }
        List<Comment> merged = new ArrayList<>(removed.getComments());
        merged.addAll(ListUtils.mapFirst(keptComments, c -> c.withPrefix(removed.getWhitespace())));
        return Space.build(kept.getWhitespace(), merged);
    }
}
