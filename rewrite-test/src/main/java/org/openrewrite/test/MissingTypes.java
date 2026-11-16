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
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.fail;

/**
 * Reruns a recipe with the test's sources parsed without the classpath they were parsed with, as when a repository's
 * dependencies don't resolve, and fails the test if the recipe throws there or changes a source file it left
 * unchanged with the types of that classpath.
 */
final class MissingTypes {
    private static final int MAX_DIFF_LINES = 30;

    private final Recipe recipe;
    private final Set<UUID> changed = new HashSet<>();
    private final Set<Path> generated = new HashSet<>();

    MissingTypes(Recipe recipe, Collection<Result> results) {
        this.recipe = recipe;
        for (Result result : results) {
            if (result.getBefore() != null) {
                changed.add(result.getBefore().getId());
            } else if (result.getAfter() != null) {
                generated.add(result.getAfter().getSourcePath());
            }
        }
    }

    void assertUnchanged(Map<SourceFile, SourceSpec<?>> specBySourceFile,
                         Function<SourceSpec<?>, Parser.@Nullable Builder> parsers, List<SourceFile> runnable,
                         @Nullable Path relativeTo, ExecutionContext parseCtx, ExecutionContext recipeCtx,
                         int cycles) {
        if (Recipe.noop().equals(recipe) || Probes.isAdHoc(recipe) || Probes.overBudget(recipe)) {
            return;
        }
        long start = System.nanoTime();
        try {
            Map<UUID, SourceFile> reparsed = reparse(specBySourceFile, parsers, relativeTo, parseCtx);
            if (reparsed == null || reparsed.isEmpty()) {
                return;
            }
            List<SourceFile> sources = runnable.stream()
                    .map(source -> reparsed.getOrDefault(source.getId(), source))
                    .collect(toList());
            List<Throwable> errors = new CopyOnWriteArrayList<>();
            ExecutionContext ctx = Probes.copy(recipeCtx, errors::add);
            ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, Probes.TIMEOUT);
            RecipeRun run = null;
            try {
                run = recipe.run(new InMemoryLargeSourceSet(sources), ctx, cycles, 1);
            } catch (Throwable t) {
                errors.add(t);
            }
            Throwable thrown = Probes.firstError(errors);
            if (thrown != null) {
                if (Probes.rejects(recipeCtx, thrown)) {
                    throw new AssertionError(threwMessage(thrown), thrown);
                }
                return;
            }
            if (run != null) {
                List<String> changes = changesOfUnchanged(run.getChangeset().getAllResults());
                if (!changes.isEmpty()) {
                    fail(changedMessage(changes));
                }
            }
        } finally {
            Probes.charge(recipe, System.nanoTime() - start);
        }
    }

    private List<String> changesOfUnchanged(List<Result> results) {
        List<String> changes = new ArrayList<>();
        for (Result result : results) {
            SourceFile before = result.getBefore();
            SourceFile after = result.getAfter();
            if (before == null) {
                if (after != null && !generated.contains(after.getSourcePath())) {
                    changes.add("generated " + after.getSourcePath() + ":\n" + diff(result));
                }
            } else if (!changed.contains(before.getId())) {
                if (after == null) {
                    changes.add("deleted " + before.getSourcePath());
                } else if (!after.printAll().equals(before.printAll())) {
                    changes.add("changed " + before.getSourcePath() + ":\n" + diff(result));
                }
            }
        }
        return changes;
    }

    /**
     * The test's sources parsed again from the text they print by a parser without the classpath of the one that
     * parsed them, by their ids, or null when one of them doesn't parse the same way.
     */
    private static @Nullable Map<UUID, SourceFile> reparse(Map<SourceFile, SourceSpec<?>> specBySourceFile,
                                                          Function<SourceSpec<?>, Parser.@Nullable Builder> parsers,
                                                          @Nullable Path relativeTo, ExecutionContext parseCtx) {
        Map<Parser.Builder, List<SourceFile>> byParser = new LinkedHashMap<>();
        for (Map.Entry<SourceFile, SourceSpec<?>> entry : specBySourceFile.entrySet()) {
            Parser.Builder builder = parsers.apply(entry.getValue());
            if (builder != null) {
                byParser.computeIfAbsent(builder, b -> new ArrayList<>()).add(entry.getKey());
            }
        }
        Map<UUID, SourceFile> reparsed = new HashMap<>();
        for (Map.Entry<Parser.Builder, List<SourceFile>> group : byParser.entrySet()) {
            List<SourceFile> sources = group.getValue();
            TreeShapeValidationService service = TreeShapes.service(sources.get(0));
            Parser.Builder withoutClasspath = service == null ? null :
                    service.parserWithoutClasspath(group.getKey());
            if (withoutClasspath == null) {
                continue;
            }
            List<Throwable> errors = new CopyOnWriteArrayList<>();
            ExecutionContext ctx = Probes.copy(parseCtx, errors::add);
            ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
            Map<Path, SourceFile> byPath = new HashMap<>();
            try {
                Parser parser = withoutClasspath.build();
                List<Parser.Input> inputs = new ArrayList<>(sources.size());
                for (SourceFile source : sources) {
                    Path path = relativeTo == null ? source.getSourcePath() : relativeTo.resolve(source.getSourcePath());
                    inputs.add(Parser.Input.fromString(path, source.printAll(), parser.getCharset(ctx)));
                }
                for (SourceFile parsed : parser.parseInputs(inputs, relativeTo, ctx).collect(toList())) {
                    byPath.put(parsed.getSourcePath(), parsed);
                }
            } catch (Exception | LinkageError e) {
                return null;
            }
            for (SourceFile source : sources) {
                SourceFile parsed = byPath.get(source.getSourcePath());
                if (parsed == null || parsed instanceof ParseError || parsed.getClass() != source.getClass() ||
                    !parsed.printAll().equals(source.printAll())) {
                    return null;
                }
                reparsed.put(source.getId(), parsed.withId(source.getId()).withMarkers(source.getMarkers()));
            }
        }
        return reparsed;
    }

    private static String diff(Result result) {
        String diff;
        try {
            diff = result.diff();
        } catch (Exception | LinkageError e) {
            diff = result.getAfter() == null ? "" : result.getAfter().printAll();
        }
        String[] lines = diff.split("\\R");
        StringBuilder shown = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, MAX_DIFF_LINES); i++) {
            shown.append("    ").append(lines[i]).append('\n');
        }
        if (lines.length > MAX_DIFF_LINES) {
            shown.append("    ...\n");
        }
        return shown.toString();
    }

    private String changedMessage(List<String> changes) {
        StringBuilder message = new StringBuilder("Check types before acting on code, so that the recipe leaves code " +
                                                  "whose types are missing unchanged instead of changing it.\n")
                .append(recipe.getName())
                .append(" changes code that it leaves unchanged with the types of the test's classpath, once those ")
                .append("types are missing:\n");
        for (String change : changes) {
            message.append("  ").append(change);
        }
        return message
                .append("Repositories whose dependencies don't all resolve have code like this, and a recipe that ")
                .append("relies on a type to tell it to leave code unchanged changes that code there.\n")
                .append("  - match with MethodMatcher, TypeMatcher, AnnotationMatcher or TypeUtils, which match ")
                .append("nothing whose type is missing, rather than by name\n")
                .append("  - where a type check decides to leave code unchanged, as `!TypeUtils.isOfClassType(..)` ")
                .append("does, leave the code unchanged when the type is missing too\n")
                .append("  - in a ScanningRecipe, treat a missing type as unknown rather than as absent before ")
                .append("removing or adding something because of it\n")
                .append("If none of these fits the recipe, leave this test failing and describe what the recipe needs.")
                .toString();
    }

    private String threwMessage(Throwable thrown) {
        return "Check for missing types before using them, so that the recipe leaves code whose types are missing " +
               "unchanged instead of throwing.\n" +
               recipe.getName() + " threw when the types of the test's classpath were missing:\n" +
               Probes.describe(thrown, "    ", recipe) + "\n" +
               "Repositories whose dependencies don't all resolve have code like this.\n" +
               "  - check that a type, such as that of `getMethodType()` or `getType()`, is neither null nor " +
               "`JavaType.Unknown` before using it, and return the tree unchanged when it is\n" +
               "  - use `TypeUtils` and matchers, which handle missing types, rather than casting a type\n" +
               "  - don't catch the exception to make this pass\n" +
               "If none of these fits the recipe, leave this test failing and describe what the recipe needs.";
    }
}
