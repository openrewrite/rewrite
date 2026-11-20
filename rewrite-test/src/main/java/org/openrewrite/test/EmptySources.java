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
package org.openrewrite.test;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * Runs the recipe on an empty file of each kind it changed, and fails the test if the recipe throws there.
 */
final class EmptySources {

    private static final Set<List<Object>> PROBED = ConcurrentHashMap.newKeySet();

    /**
     * An empty XML document has no root, although {@code Xml.Document} declares it non-null.
     */
    private static final String XML_DOCUMENT = "org.openrewrite.xml.tree.Xml$Document";

    private final Recipe recipe;
    private final Set<UUID> changed = new HashSet<>();

    EmptySources(Recipe recipe, Collection<Result> results) {
        this.recipe = recipe;
        for (Result result : results) {
            if (result.getBefore() != null) {
                changed.add(result.getBefore().getId());
            }
        }
    }

    void assertNoneThrow(Map<SourceFile, SourceSpec<?>> specBySourceFile,
                         Function<SourceSpec<?>, Parser.@Nullable Builder> parsers, @Nullable Path relativeTo,
                         ExecutionContext parseCtx, ExecutionContext recipeCtx, int cycles) {
        if (changed.isEmpty() || Recipe.noop().equals(recipe) || Probes.isAdHoc(recipe)) {
            return;
        }
        Set<List<Object>> kinds = new HashSet<>();
        for (Map.Entry<SourceFile, SourceSpec<?>> entry : specBySourceFile.entrySet()) {
            SourceFile before = entry.getKey();
            SourceSpec<?> spec = entry.getValue();
            List<Object> kind = Arrays.asList(before.getClass(), spec.getDsl());
            if (!changed.contains(before.getId()) || XML_DOCUMENT.equals(before.getClass().getName()) ||
                !kinds.add(kind)) {
                continue;
            }
            Parser.Builder builder = parsers.apply(spec);
            List<Object> probe = Arrays.asList(recipe.getDescriptor(), before.getClass(), spec.getDsl());
            if (builder == null || Probes.overBudget(recipe) || !PROBED.add(probe)) {
                continue;
            }
            long start = System.nanoTime();
            try {
                SourceFile empty = parseEmpty(before, spec, builder, relativeTo, parseCtx);
                Throwable thrown = empty == null ? null :
                        Probes.run(recipe, singletonList(empty), recipeCtx, cycles);
                if (empty != null && thrown != null) {
                    if (Probes.rejects(recipeCtx, thrown)) {
                        throw new AssertionError(message(empty, thrown), thrown);
                    }
                    // a later test whose context doesn't tolerate errors should still see this
                    PROBED.remove(probe);
                }
            } finally {
                Probes.charge(recipe, System.nanoTime() - start);
            }
        }
    }

    /**
     * An empty file at the source's path, with the spec's markers and the markers that describe the source's
     * project, or null if the parser makes no empty source of the same kind from an empty file.
     */
    private static @Nullable SourceFile parseEmpty(SourceFile before, SourceSpec<?> spec, Parser.Builder builder,
                                                   @Nullable Path relativeTo, ExecutionContext parseCtx) {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutionContext ctx = Probes.copy(parseCtx, errors::add);
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
        Path sourcePath = relativeTo == null ? before.getSourcePath() : relativeTo.resolve(before.getSourcePath());
        SourceFile empty;
        try {
            // a fresh parser, as javac can't run annotation processing twice in one compiler
            Parser parser = builder.clone().build();
            List<SourceFile> parsed = parser.parseInputs(
                    singletonList(Parser.Input.fromString(sourcePath, "", parser.getCharset(ctx))), relativeTo, ctx
            ).collect(toList());
            empty = parsed.size() == 1 ? parsed.get(0) : null;
            // some kinds of file can't be empty, such as a pom.xml
            if (empty == null || empty.getClass() != before.getClass() || !errors.isEmpty() ||
                !empty.printAll().isEmpty()) {
                return null;
            }
        } catch (Exception | LinkageError ignored) {
            return null;
        }

        Markers markers = empty.getMarkers();
        for (Marker marker : spec.getMarkers()) {
            markers = markers.add(marker);
        }
        Set<Class<?>> present = new HashSet<>();
        for (Marker marker : markers.getMarkers()) {
            present.add(marker.getClass());
        }
        for (Marker marker : before.getMarkers().getMarkers()) {
            // a resolution result describes what the file itself declares, which an empty file doesn't
            if (!present.contains(marker.getClass()) &&
                !marker.getClass().getSimpleName().endsWith("ResolutionResult")) {
                markers = markers.add(marker);
            }
        }
        return empty.withMarkers(markers);
    }

    private String message(SourceFile empty, Throwable thrown) {
        StringBuilder message = new StringBuilder("Check for the structure the recipe edits before using it, so " +
                                                  "that it handles an empty file or leaves it unchanged instead of " +
                                                  "throwing.\n")
                .append(recipe.getName()).append(" threw when run on an empty ")
                .append(Probes.typeName(empty.getClass())).append(" at ").append(empty.getSourcePath());
        Set<String> markers = new LinkedHashSet<>();
        for (Marker marker : empty.getMarkers().getMarkers()) {
            markers.add(Probes.typeName(marker.getClass()));
        }
        if (!markers.isEmpty()) {
            message.append(" with the markers of the test's source (").append(String.join(", ", markers)).append(')');
        }
        return message.append(":\n").append(Probes.describe(thrown, "    ", recipe))
                .append("\nThe test gives the recipe only files that contain what it edits, but repositories ")
                .append("also hold empty files and files without that block, such as a placeholder application.yml, ")
                .append("a package-info.java or a subproject's empty build.gradle.\n")
                .append("  - return the tree unchanged when what the visitor assumes is absent: check `instanceof` ")
                .append("before a cast (`document.getBlock() instanceof Yaml.Mapping`), emptiness before `get(0)` ")
                .append("(`!mapping.getEntries().isEmpty()`) and null before a dereference ")
                .append("(`cu.getPackageDeclaration() != null`)\n")
                .append("  - if the recipe adds what is missing, build that content correctly for an empty file too\n")
                .append("  - don't catch the exception to make this pass\n")
                .append("Then add a test with an empty source, such as yaml(\"\"), java(\"\") or buildGradle(\"\"), ")
                .append("that states the intended result.")
                .toString();
    }
}
