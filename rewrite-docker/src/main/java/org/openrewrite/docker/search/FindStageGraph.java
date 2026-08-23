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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.table.StageDependencies;
import org.openrewrite.docker.trait.ImageName;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.StringUtils;

import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindStageGraph extends Recipe {

    transient StageDependencies stageDependencies = new StageDependencies(this);

    final String displayName = "Find Docker build stage dependencies";

    final String description = "Record which build stages of a multi-stage Dockerfile depend on which others, one row " +
            "per stage: what its `FROM` names, the registry that image is pulled from, and the stages that name it in " +
            "a `FROM`, a `COPY --from`, or a `RUN --mount=...,from=` flag. Only the Dockerfile is read. A reference " +
            "that cannot be resolved without guessing, because a build argument spells it (`COPY --from=$BUILDER`) or " +
            "because it names a stage by a position that moves when stages are removed (`COPY --from=0`), is recorded " +
            "as no reference at all.";

    private static final String SCRATCH = "scratch";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                StageGraph graph = StageGraph.of(file);
                String sourceFile = file.getSourcePath().toString();
                List<Docker.Stage> stages = file.getStages();
                for (int i = 0; i < stages.size(); i++) {
                    Docker.From from = stages.get(i).getFrom();
                    stageDependencies.insertRow(ctx, new StageDependencies.Row(
                            sourceFile,
                            graph.getName(i),
                            i,
                            baseImage(from),
                            graph.extendsStage(i) ? null : registry(from),
                            graph.getReferencedBy(i)
                    ));
                }
                return file;
            }
        };
    }

    /// @return The registry the stage's base image is pulled from, or `null` where nothing is pulled
    /// (`FROM scratch`) or where the name is spelled by a build argument that leaves the registry unknown.
    private static @Nullable String registry(Docker.From from) {
        String plain = ArgumentContents.text(from.getImageName());
        if (SCRATCH.equals(plain)) {
            return null;
        }
        ImageName imageName = ImageName.parse(ArgumentContents.textWithVariables(from.getImageName()));
        if (plain == null && imageName.getRegistry() == null) {
            return null;
        }
        return imageName.getResolvedRegistry();
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

    /**
     * Which build stages of a Dockerfile name which other stages.
     */
    private static class StageGraph {

        private final List<@Nullable String> names;
        private final List<Set<Integer>> references;
        private final boolean[] extendsStage;

        private StageGraph(List<@Nullable String> names, List<Set<Integer>> references, boolean[] extendsStage) {
            this.names = names;
            this.references = references;
            this.extendsStage = extendsStage;
        }

        static StageGraph of(Docker.File file) {
            List<Docker.Stage> stages = file.getStages();
            List<@Nullable String> names = new ArrayList<>(stages.size());
            for (Docker.Stage stage : stages) {
                Docker.From.As as = stage.getFrom().getAs();
                names.add(as == null ? null : as.getName().getText());
            }

            List<Set<Integer>> references = new ArrayList<>(stages.size());
            boolean[] extendsStage = new boolean[stages.size()];
            for (int i = 0; i < stages.size(); i++) {
                ReferenceCollector collector = new ReferenceCollector(names, i);
                collector.visit(stages.get(i), 0);
                references.add(collector.targets);
                extendsStage[i] = collector.extendsStage;
            }
            return new StageGraph(names, references, extendsStage);
        }

        @Nullable String getName(int stage) {
            return names.get(stage);
        }

        /// Whether the stage's `FROM` names another stage of the same file rather than an image to pull.
        boolean extendsStage(int stage) {
            return extendsStage[stage];
        }

        String getReferencedBy(int stage) {
            StringJoiner referrers = new StringJoiner(",");
            for (int i = 0; i < references.size(); i++) {
                if (references.get(i).contains(stage)) {
                    String name = names.get(i);
                    referrers.add(name == null ? "#" + i : name);
                }
            }
            return referrers.toString();
        }

        private static class ReferenceCollector extends DockerIsoVisitor<Integer> {
            private final List<@Nullable String> names;
            private final int stage;

            final Set<Integer> targets = new LinkedHashSet<>();
            boolean extendsStage;

            ReferenceCollector(List<@Nullable String> names, int stage) {
                this.names = names;
                this.stage = stage;
            }

            @Override
            public Docker.From visitFrom(Docker.From from, Integer p) {
                if (from.getTag() == null && from.getDigest() == null) {
                    String plain = ArgumentContents.text(from.getImageName());
                    if (plain != null) {
                        extendsStage = reference(plain, stage);
                    }
                }
                return super.visitFrom(from, p);
            }

            @Override
            public Docker.Flag visitFlag(Docker.Flag flag, Integer p) {
                Docker.Argument value = flag.getValue();
                if (value == null) {
                    return super.visitFlag(flag, p);
                }
                int limit = names.size();
                if ("from".equals(flag.getName())) {
                    reference(ArgumentContents.textWithVariables(value), limit);
                } else if ("mount".equals(flag.getName())) {
                    for (String field : ArgumentContents.textWithVariables(value).split(",")) {
                        if (field.regionMatches(true, 0, "from=", 0, "from=".length())) {
                            reference(field.substring("from=".length()), limit);
                        }
                    }
                }
                return super.visitFlag(flag, p);
            }

            /// A `FROM` sees only the stages declared before it, but a `--from` is resolved once the whole file is read,
            /// so it reaches later stages too; hence `limit`. Searching backwards leaves a duplicated name at its last
            /// declaration, as it is for Docker. A name a build argument spells, or a position rather than a name,
            /// resolves to nothing rather than to a guess.
            private boolean reference(String value, int limit) {
                if (value.indexOf('$') >= 0 || StringUtils.isNumeric(value)) {
                    return false;
                }
                for (int i = limit - 1; i >= 0; i--) {
                    if (i != stage && value.equalsIgnoreCase(names.get(i))) {
                        targets.add(i);
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
