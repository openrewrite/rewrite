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
package org.openrewrite.docker.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Which build stages of a Dockerfile reach which other stages, and which of them the image being
 * built actually needs. A stage is needed when it is the last stage of the file, or when a stage
 * that is itself needed names it in a {@code FROM}, a {@code COPY --from}, or a
 * {@code RUN --mount=...,from=} flag.
 * <p>
 * A reference that only a build-time variable resolves ({@code COPY --from=$BUILDER}) or that names
 * a stage by position ({@code COPY --from=0}, whose meaning moves when a stage is removed) leaves
 * the graph ambiguous: nothing about the file can be concluded, and every stage is reported as
 * reached. So does a file the parser only partly understood, since a reference it dropped is a
 * reference the graph cannot see.
 */
public class StageGraph {

    /// A `\\` or `` ` `` that ends a line is whitespace to a Dockerfile, so it stands where only formatting belongs.
    private static final Pattern LINE_CONTINUATION = Pattern.compile("[\\\\`][ \\t]*(?=\\r?\\n)");

    private final List<@Nullable String> names;
    private final List<Set<Integer>> references;
    private final List<Set<Integer>> referencedBy;
    private final boolean ambiguous;

    private StageGraph(List<@Nullable String> names, List<Set<Integer>> references, boolean ambiguous) {
        this.names = names;
        this.references = references;
        this.ambiguous = ambiguous;
        this.referencedBy = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            this.referencedBy.add(new LinkedHashSet<>());
        }
        for (int i = 0; i < references.size(); i++) {
            for (Integer target : references.get(i)) {
                this.referencedBy.get(target).add(i);
            }
        }
    }

    public static StageGraph of(Docker.File file) {
        List<Docker.Stage> stages = file.getStages();
        List<@Nullable String> names = new ArrayList<>(stages.size());
        for (Docker.Stage stage : stages) {
            Docker.From.As as = stage.getFrom().getAs();
            names.add(as == null ? null : as.getName().getText().toLowerCase(Locale.ROOT));
        }

        List<Set<Integer>> references = new ArrayList<>(stages.size());
        boolean ambiguous = !isFullyParsed(file);
        for (int i = 0; i < stages.size(); i++) {
            ReferenceCollector collector = new ReferenceCollector(names, i);
            collector.visit(stages.get(i), 0);
            references.add(collector.targets);
            ambiguous |= collector.ambiguous;
        }
        return new StageGraph(names, references, ambiguous);
    }

    /// Text the parser could not place does not go missing: the tree is built from source offsets, so a token the
    /// error recovery dropped reappears as the whitespace before whatever came next. The file still prints as it was
    /// read, and the instruction it held, which may be the `COPY --from` that makes a stage used, is simply absent.
    private static boolean isFullyParsed(Docker.File file) {
        return new DockerIsoVisitor<AtomicBoolean>() {
            @Override
            public Space visitSpace(Space space, AtomicBoolean parsed) {
                if (!LINE_CONTINUATION.matcher(space.getWhitespace()).replaceAll("").trim().isEmpty()) {
                    parsed.set(false);
                }
                return super.visitSpace(space, parsed);
            }
        }.reduce(file, new AtomicBoolean(true)).get();
    }

    public @Nullable String getName(int stage) {
        return names.get(stage);
    }

    /**
     * The stages naming the given stage, in the order they appear in the file, described the way the
     * file names them: by stage name where they have one, otherwise by position.
     */
    public List<String> getReferencedBy(int stage) {
        Set<Integer> referrers = referencedBy.get(stage);
        if (referrers.isEmpty()) {
            return emptyList();
        }
        List<String> described = new ArrayList<>(referrers.size());
        for (Integer referrer : referrers) {
            String name = names.get(referrer);
            described.add(name == null ? "#" + referrer : name);
        }
        return described;
    }

    private static Set<Integer> allIndices(int size) {
        Set<Integer> all = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            all.add(i);
        }
        return all;
    }

    /**
     * The stages needed once the named stages count as built in their own right, alongside the last stage
     * of the file. Names are matched the way Dockerfile matches them, ignoring case.
     */
    public Set<Integer> reachableGiven(Collection<String> builtStageNames) {
        Set<Integer> roots = new LinkedHashSet<>();
        if (!names.isEmpty()) {
            roots.add(names.size() - 1);
        }
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (name != null && builtStageNames.contains(name)) {
                roots.add(i);
            }
        }
        return ambiguous ? allIndices(names.size()) : reachableFrom(roots);
    }

    private Set<Integer> reachableFrom(Collection<Integer> roots) {
        Set<Integer> reached = new LinkedHashSet<>();
        Deque<Integer> worklist = new ArrayDeque<>(roots);
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

        ReferenceCollector(List<@Nullable String> names, int stage) {
            this.names = names;
            this.stage = stage;
        }

        @Override
        public Docker.From visitFrom(Docker.From from, Integer p) {
            if (from.getTag() == null && from.getDigest() == null) {
                String plain = ArgumentContents.text(from.getImageName());
                if (plain != null) {
                    reference(plain);
                } else if (!ArgumentContents.textWithVariables(from.getImageName()).contains("/")) {
                    ambiguous = true;
                }
            }
            return super.visitFrom(from, p);
        }

        @Override
        public Docker.Flag visitFlag(Docker.Flag flag, Integer p) {
            Docker.Argument value = flag.getValue();
            if (value != null) {
                if ("from".equals(flag.getName())) {
                    reference(ArgumentContents.textWithVariables(value));
                } else if ("mount".equals(flag.getName())) {
                    for (String field : ArgumentContents.textWithVariables(value).split(",")) {
                        if (field.regionMatches(true, 0, "from=", 0, "from=".length())) {
                            reference(field.substring("from=".length()));
                        }
                    }
                }
            }
            return super.visitFlag(flag, p);
        }

        private void reference(String value) {
            if (value.indexOf('$') >= 0 || isIndex(value)) {
                ambiguous = true;
                return;
            }
            String name = value.toLowerCase(Locale.ROOT);
            for (int i = stage - 1; i >= 0; i--) {
                if (name.equals(names.get(i))) {
                    targets.add(i);
                    return;
                }
            }
        }

        private static boolean isIndex(String value) {
            if (value.isEmpty()) {
                return false;
            }
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
