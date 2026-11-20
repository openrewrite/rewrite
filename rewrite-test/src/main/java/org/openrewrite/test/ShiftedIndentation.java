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

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.fail;

/**
 * Reruns a recipe on the Java and XML sources it changed with one more space at the start of each line that opens
 * nothing, and checks that the lines it edits in place keep that indentation. Test inputs are formatted to their
 * style, so a recipe that recomputes the indentation of a line it changes, from its parent's indentation and the
 * style, produces what the test expects. A statement or tag one space deeper than its parent's indentation plus the
 * style's unit shows the difference, as files indented by hand do.
 * <p>
 * A measuring tool for finding such recipes, off unless the system property {@value #PROPERTY} is {@code true}. Lines
 * of a Java text block can move by different amounts, which can change its value.
 */
final class ShiftedIndentation {
    static final String PROPERTY = "org.openrewrite.test.keepEditedLineIndentation.shiftedInput";

    private static final RecipeSerializer SERIALIZER = new RecipeSerializer();
    private static final Duration RUN_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SOURCE_FILE_TIMEOUT = Duration.ofSeconds(5);

    private final Recipe recipeUnderTest;
    private final byte[] recipe;
    private final List<SourceFile> sources;
    private final List<SourceFile> parsed;
    private final Map<String, @Nullable Object> messages;
    private final int cycles;
    private final int minCycles;
    private final Function<SourceFile, List<Parser.Builder>> parsersFor;
    private final @Nullable Path relativeTo;
    private final ExecutionContext parseCtx;

    private ShiftedIndentation(Recipe recipeUnderTest, byte[] recipe, RewriteRunner.Context context,
                               List<SourceFile> parsed, Function<SourceFile, List<Parser.Builder>> parsersFor,
                               @Nullable Path relativeTo, ExecutionContext parseCtx) {
        this.recipeUnderTest = recipeUnderTest;
        this.recipe = recipe;
        this.sources = context.getParsedSources();
        this.parsed = parsed;
        this.messages = new HashMap<>(context.getExecutionContext().getMessages());
        this.messages.keySet().removeAll(NameMatching.RUN_STATE);
        this.cycles = context.getCycles();
        this.minCycles = context.getExpectedCyclesThatMakeChanges() + 1;
        this.parsersFor = parsersFor;
        this.relativeTo = relativeTo;
        this.parseCtx = parseCtx;
    }

    /**
     * @return null unless the system property asks for this check, the test keeps
     * {@link RecipeQualityValidation#keepEditedLineIndentation()} on and its run can be repeated faithfully.
     */
    static @Nullable ShiftedIndentation of(Recipe recipe, boolean keepEditedLineIndentation, RewriteRunner runner,
                                           RewriteRunner.Context context, List<SourceFile> parsed,
                                           Function<SourceFile, List<Parser.Builder>> parsersFor,
                                           @Nullable Path relativeTo, ExecutionContext parseCtx) {
        RecipeSpec methodSpec = context.getTestMethodSpec();
        RecipeSpec classSpec = context.getTestClassSpec();
        if (!Boolean.getBoolean(PROPERTY) || !keepEditedLineIndentation ||
            recipe.equals(Recipe.noop()) || FormattingScope.isFormatter(recipe) ||
            runner != RewriteRunner.IN_PROCESS ||
            methodSpec.getSourceSet() != null || classSpec.getSourceSet() != null ||
            !methodSpec.getBeforeRecipes().isEmpty() || !classSpec.getBeforeRecipes().isEmpty() ||
            NameMatching.isTestScaffolding(recipe)) {
            return null;
        }
        try {
            // serialized before the run, so that the rerun starts from a recipe in its initial state
            return new ShiftedIndentation(recipe, SERIALIZER.write(recipe), context, parsed, parsersFor, relativeTo,
                    parseCtx);
        } catch (Exception e) {
            return null;
        }
    }

    void assertKept(Collection<Result> results) {
        Map<String, List<EditedLineIndentation.Reindented>> found = new LinkedHashMap<>();
        for (Result result : results) {
            SourceFile before = result.getBefore();
            if (before == null || result.getAfter() == null || !isShiftable(before)) {
                continue;
            }
            String shifted = shift(EditedLineIndentation.printed(before));
            List<SourceFile> others = new ArrayList<>();
            for (SourceFile source : parsed) {
                if (!source.getId().equals(before.getId()) && source.getClass() == before.getClass()) {
                    others.add(source);
                }
            }
            SourceFile reparsed = AddedMarkers.reparse(before, shifted, others, parsersFor, relativeTo, parseCtx);
            if (reparsed == null || !EditedLineIndentation.printed(reparsed).equals(shifted)) {
                continue;
            }
            int index = sources.indexOf(before);
            if (index < 0) {
                continue;
            }
            List<SourceFile> rerunSources = new ArrayList<>(sources);
            // the same file in the same project, only indented differently
            rerunSources.set(index, reparsed.withId(before.getId()).withMarkers(before.getMarkers()));
            SourceFile after = rerun(rerunSources, before.getId());
            if (after == null) {
                continue;
            }
            List<EditedLineIndentation.Reindented> reindented = EditedLineIndentation.reindented(shifted,
                    EditedLineIndentation.printed(after));
            if (!reindented.isEmpty()) {
                found.put(before.getSourcePath().toString(), reindented);
            }
        }
        if (!found.isEmpty()) {
            StringBuilder message = new StringBuilder("Keep the indentation of the lines the recipe edits in place, " +
                                                      "however the file is indented. Run again on its sources with one more space at the start of " +
                                                      "each line that opens nothing, " + recipeUnderTest.getName() + " changed these lines' code " +
                                                      "and leading whitespace (· is a space, → a tab):\n");
            for (Map.Entry<String, List<EditedLineIndentation.Reindented>> file : found.entrySet()) {
                for (EditedLineIndentation.Reindented line : file.getValue()) {
                    message.append("  ").append(file.getKey()).append(':').append(line.getLine()).append('\n')
                            .append("    before: ").append(EditedLineIndentation.visible(line.getBefore())).append('\n')
                            .append("    after:  ").append(EditedLineIndentation.visible(line.getAfter())).append('\n');
                }
            }
            fail(message.append("A recipe that formats what it changes, or builds its prefix from the style, gives a ")
                    .append("line the indentation of a file formatted from scratch. Keep the prefix of the code it ")
                    .append("replaced instead.\n").toString());
        }
    }

    private static boolean isShiftable(SourceFile sourceFile) {
        String type = sourceFile.getClass().getName();
        return "org.openrewrite.java.tree.J$CompilationUnit".equals(type) ||
               "org.openrewrite.xml.tree.Xml$Document".equals(type);
    }

    /**
     * The text with one more space at the start of each indented, non-blank line that the next non-blank line isn't
     * indented deeper than: statements, closing lines and tags without children, but not the lines that open what
     * they nest. Lines at the first column stay there, as a file's imports and top-level declarations always do.
     */
    static String shift(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
        }
        StringBuilder shifted = new StringBuilder(text.length() + lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.trim().isEmpty() && width(line) > 0) {
                int next = i + 1;
                while (next < lines.size() && lines.get(next).trim().isEmpty()) {
                    next++;
                }
                if (next == lines.size() || width(lines.get(next)) <= width(line)) {
                    shifted.append(' ');
                }
            }
            shifted.append(line);
        }
        return shifted.toString();
    }

    /**
     * The columns a line's indentation spans, a tab counting as four.
     */
    private static int width(String line) {
        int columns = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                columns++;
            } else if (c == '\t') {
                columns += 4;
            } else {
                break;
            }
        }
        return columns;
    }

    /**
     * What a fresh copy of the recipe makes of the source with the given id, or null when it leaves the source
     * unchanged, deletes it or fails.
     */
    private @Nullable SourceFile rerun(List<SourceFile> rerunSources, UUID id) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(t -> {
        });
        messages.forEach(ctx::putMessage);
        ctx.putMessage(ExecutionContext.RUN_TIMEOUT, RUN_TIMEOUT);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, SOURCE_FILE_TIMEOUT);
        DataTableExecutionContextView.view(ctx).setDataTableStore(DataTableStore.noop());
        try {
            RecipeRun run = SERIALIZER.read(recipe).run(new InMemoryLargeSourceSet(rerunSources), ctx, cycles, minCycles);
            for (Result result : run.getChangeset().getAllResults()) {
                if (result.getBefore() != null && result.getBefore().getId().equals(id)) {
                    return result.getAfter();
                }
            }
        } catch (Exception | StackOverflowError | AssertionError | LinkageError ignored) {
            // a rerun that can't complete shows nothing about indentation
        }
        return null;
    }
}
