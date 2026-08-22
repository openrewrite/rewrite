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
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.SearchResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;

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

                String sourceFile = file.getSourcePath().toString();
                return file.withStages(ListUtils.map(file.getStages(), (i, stage) -> {
                    boolean reached = reachable.contains(i);
                    stageDependencies.insertRow(ctx, new StageDependencies.Row(
                            sourceFile,
                            graph.getName(i),
                            i,
                            baseImage(stage.getFrom()),
                            graph.extendsStage(i) ? null : registry(stage.getFrom()),
                            graph.getReferencedBy(i),
                            reached
                    ));
                    return reached ? stage :
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

    /**
     * Which build stages of a Dockerfile reach which other stages, and which of them the image being built
     * actually needs. A reference nothing can resolve without guessing leaves the graph ambiguous, and every
     * stage is then reported as reached rather than risk calling a used stage unused.
     */
    private static class StageGraph {

        /// A `\\` or `` ` `` that ends a line is whitespace to a Dockerfile, so it stands where only formatting belongs.
        private static final Pattern LINE_CONTINUATION = Pattern.compile("[\\\\`][ \\t]*(?=\\r?\\n)");

        private final List<@Nullable String> names;
        private final List<Set<Integer>> references;
        private final boolean[] extendsStage;
        private final boolean ambiguous;

        private StageGraph(List<@Nullable String> names, List<Set<Integer>> references, boolean[] extendsStage, boolean ambiguous) {
            this.names = names;
            this.references = references;
            this.extendsStage = extendsStage;
            this.ambiguous = ambiguous;
        }

        static StageGraph of(Docker.File file) {
            List<Docker.Stage> stages = file.getStages();
            List<@Nullable String> names = new ArrayList<>(stages.size());
            for (Docker.Stage stage : stages) {
                Docker.From.As as = stage.getFrom().getAs();
                names.add(as == null ? null : as.getName().getText().toLowerCase(Locale.ROOT));
            }

            List<Set<Integer>> references = new ArrayList<>(stages.size());
            boolean[] extendsStage = new boolean[stages.size()];
            boolean ambiguous = !isFullyParsed(file);
            for (int i = 0; i < stages.size(); i++) {
                ReferenceCollector collector = new ReferenceCollector(names, i);
                collector.visit(stages.get(i), 0);
                references.add(collector.targets);
                extendsStage[i] = collector.extendsStage;
                ambiguous |= collector.ambiguous;
            }
            return new StageGraph(names, references, extendsStage, ambiguous);
        }

        /// The tree is built from source offsets, so a token the error recovery dropped reappears as the whitespace
        /// before whatever came next, and the instruction it held is absent from the graph.
        private static boolean isFullyParsed(Docker.File file) {
            return new DockerIsoVisitor<AtomicBoolean>() {
                @Override
                public Space visitSpace(Space space, AtomicBoolean parsed) {
                    String whitespace = space.getWhitespace();
                    if (whitespace.indexOf('\\') >= 0 || whitespace.indexOf('`') >= 0) {
                        whitespace = LINE_CONTINUATION.matcher(whitespace).replaceAll("");
                    }
                    if (!whitespace.trim().isEmpty()) {
                        parsed.set(false);
                    }
                    return super.visitSpace(space, parsed);
                }
            }.reduce(file, new AtomicBoolean(true)).get();
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

        /// The stages the image the file ends with is built from, so the last stage and everything it names, directly
        /// or through another stage it names.
        Set<Integer> reachable() {
            if (ambiguous) {
                Set<Integer> all = new LinkedHashSet<>();
                for (int i = 0; i < names.size(); i++) {
                    all.add(i);
                }
                return all;
            }
            return names.isEmpty() ? emptySet() : reachableFrom(names.size() - 1);
        }

        private Set<Integer> reachableFrom(int root) {
            Set<Integer> reached = new LinkedHashSet<>();
            Deque<Integer> worklist = new ArrayDeque<>();
            worklist.add(root);
            while (!worklist.isEmpty()) {
                Integer stage = worklist.remove();
                if (reached.add(stage)) {
                    worklist.addAll(references.get(stage));
                }
            }
            return reached;
        }

        private static class ReferenceCollector extends DockerIsoVisitor<Integer> {
            private final List<@Nullable String> names;
            private final int stage;

            final Set<Integer> targets = new LinkedHashSet<>();
            boolean ambiguous;
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
                    } else if (!ArgumentContents.textWithVariables(from.getImageName()).contains("/")) {
                        ambiguous = true;
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
            /// declaration, as it is for Docker.
            private boolean reference(String value, int limit) {
                if (value.indexOf('$') >= 0 || StringUtils.isNumeric(value)) {
                    ambiguous = true;
                    return false;
                }
                String name = value.toLowerCase(Locale.ROOT);
                for (int i = limit - 1; i >= 0; i--) {
                    if (i != stage && name.equals(names.get(i))) {
                        targets.add(i);
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
