/*
 * Copyright 2022 the original author or authors.
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

import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.scheduling.DirectScheduler;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.openrewrite.internal.StringUtils.trimIndentPreserveCRLF;
import static org.openrewrite.test.ParserTypeUtils.parserType;

@SuppressWarnings("unused")
public interface RewriteTest extends SourceSpecs {
    static Recipe toRecipe(Supplier<TreeVisitor<?, ExecutionContext>> visitor) {
        return new AdHocRecipe(visitor);
    }

    static Recipe fromRuntimeClasspath(String recipe) {
        return Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes(recipe);
    }

    /**
     * @return always null, a method that better documents in code that a source file does not exist either
     * before or after a recipe run.
     */
    @Nullable
    default String doesNotExist() {
        return null;
    }

    default void defaults(RecipeSpec spec) {
        spec.recipe(Recipe.noop());
    }

    default void rewriteRun(SourceSpecs... sourceSpecs) {
        rewriteRun(spec -> {
        }, sourceSpecs);
    }

    default void rewriteRun(Consumer<RecipeSpec> spec, SourceSpecs... sourceSpecs) {
        rewriteRun(spec, Arrays.stream(sourceSpecs)
                .flatMap(specGroup -> StreamSupport.stream(specGroup.spliterator(), false))
                .toArray(SourceSpec[]::new)
        );
    }

    default void rewriteRun(Consumer<RecipeSpec> spec, SourceSpec<?>... sourceSpecs) {
        RecipeSpec testClassSpec = RecipeSpec.defaults();
        defaults(testClassSpec);

        RecipeSpec testMethodSpec = RecipeSpec.defaults();
        spec.accept(testMethodSpec);

        Recipe recipe = testMethodSpec.recipe == null ? testClassSpec.recipe : testMethodSpec.recipe;
        assertThat(recipe)
                .as("A recipe must be specified")
                .isNotNull();

        assertThat(recipe.validate().isValid())
                .as("Recipe validation must succeed")
                .isTrue();

        if (!(recipe instanceof AdHocRecipe) &&
                testClassSpec.serializationValidation &&
                testMethodSpec.serializationValidation) {
            RecipeSerializer recipeSerializer = new RecipeSerializer();
            assertThat(recipeSerializer.read(recipeSerializer.write(recipe)))
                    .as("Recipe must be serializable/deserializable")
                    .isEqualTo(recipe);
        }

        int cycles = testMethodSpec.cycles == null ? testClassSpec.getCycles() : testMethodSpec.getCycles();

        // There may not be any tests that have "after" assertions, but that change file attributes or file names.
        // If so, the test can declare an expected set of cycles that make changes.
        int expectedCyclesThatMakeChanges = testMethodSpec.expectedCyclesThatMakeChanges == null ?
                (testClassSpec.expectedCyclesThatMakeChanges == null ? 0 : testClassSpec.expectedCyclesThatMakeChanges) :
                testMethodSpec.expectedCyclesThatMakeChanges;

        // If there are any tests that have assertions (an "after"), then set the expected cycles.
        for (SourceSpec<?> s : sourceSpecs) {
            if (s.after != null) {
                expectedCyclesThatMakeChanges = testMethodSpec.expectedCyclesThatMakeChanges == null ?
                        testClassSpec.getExpectedCyclesThatMakeChanges(cycles) :
                        testMethodSpec.getExpectedCyclesThatMakeChanges(cycles);
                break;
            }
        }

        RecipeSchedulerCheckingExpectedCycles recipeSchedulerCheckingExpectedCycles =
                new RecipeSchedulerCheckingExpectedCycles(DirectScheduler.common(), expectedCyclesThatMakeChanges);

        ExecutionContext executionContext;
        if (testMethodSpec.getExecutionContext() != null) {
            executionContext = testMethodSpec.getExecutionContext();
        } else if (testClassSpec.getExecutionContext() != null) {
            executionContext = testClassSpec.getExecutionContext();
        } else {
            executionContext = defaultExecutionContext(sourceSpecs);
        }

        Map<ParserSupplier, List<SourceSpec<?>>> sourceSpecsByParser = new HashMap<>();

        for (SourceSpec<?> sourceSpec : sourceSpecs) {
            // ----- method specific parser -------------------------
            if (RewriteTestUtils.groupSourceSpecsByParser(testMethodSpec, sourceSpecsByParser, sourceSpec)) {
                continue;
            }

            // ----- test default parser -------------------------
            if (RewriteTestUtils.groupSourceSpecsByParser(testClassSpec, sourceSpecsByParser, sourceSpec)) {
                continue;
            }

            // ----- default parsers for each SourceFile type -------------------------
            sourceSpecsByParser.computeIfAbsent(sourceSpec.getParserSupplier(), p -> new ArrayList<>()).add(sourceSpec);
        }

        Map<SourceFile, SourceSpec<?>> specBySourceFile = new HashMap<>(sourceSpecs.length);
        for (Map.Entry<ParserSupplier, List<SourceSpec<?>>> sourceSpecsForParser : sourceSpecsByParser.entrySet()) {
            Map<SourceSpec<?>, Parser.Input> inputs = new LinkedHashMap<>(sourceSpecsForParser.getValue().size());
            for (SourceSpec<?> sourceSpec : sourceSpecsForParser.getValue()) {
                if (sourceSpec.before == null) {
                    continue;
                }
                String beforeTrimmed = trimIndentPreserveCRLF(sourceSpec.before);
                Path sourcePath;
                if (sourceSpec.sourcePath != null) {
                    sourcePath = sourceSpec.dir.resolve(sourceSpec.sourcePath);
                } else {
                    sourcePath = sourceSpecsForParser.getKey().get()
                            .sourcePathFromSourceText(sourceSpec.dir, beforeTrimmed);
                }
                inputs.put(sourceSpec, new Parser.Input(sourcePath, () -> new ByteArrayInputStream(beforeTrimmed.getBytes(StandardCharsets.UTF_8))));
            }

            Path relativeTo = testMethodSpec.relativeTo == null ? testClassSpec.relativeTo : testMethodSpec.relativeTo;

            Iterator<SourceSpec<?>> sourceSpecIter = inputs.keySet().iterator();

            //noinspection unchecked,rawtypes
            List<SourceFile> sourceFiles = (List) sourceSpecsForParser.getKey().get()
                    .parseInputs(inputs.values(), relativeTo, executionContext);
            assertThat(sourceFiles.size())
                    .as("Every input should be parsed into a SourceFile.")
                    .isEqualTo(inputs.size());

            for (int i = 0; i < sourceFiles.size(); i++) {
                SourceFile sourceFile = sourceFiles.get(i);
                sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().withMarkers(ListUtils.concatAll(
                        sourceFile.getMarkers().getMarkers(), testClassSpec.allSources.markers)));
                sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().withMarkers(ListUtils.concatAll(
                        sourceFile.getMarkers().getMarkers(), testMethodSpec.allSources.markers)));

                SourceSpec<?> nextSpec = sourceSpecIter.next();
                sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().withMarkers(ListUtils.concatAll(
                        sourceFile.getMarkers().getMarkers(), nextSpec.markers)));

                // Update the default 'main' JavaSourceSet Marker added by the JavaParser with the specs sourceSetName
//                sourceFile = sourceFile.withMarkers((sourceFile.getMarkers().withMarkers(ListUtils.map(sourceFile.getMarkers().getMarkers(), m -> {
//                    if (m instanceof JavaSourceSet) {
//                        m = ((JavaSourceSet) m).withName(nextSpec.sourceSetName);
//                    }
//                    return m;
//                }))));

                // Validate that printing a parsed AST yields the same source text
                int j = 0;
                for (Parser.Input input : inputs.values()) {
                    if (j++ == i && !(sourceFile instanceof Quark)) {
                        assertThat(sourceFile.printAll())
                                .as("When parsing and printing the source code back to text without modifications, " +
                                        "the printed source didn't match the original source code. This means there is a bug in the " +
                                        "parser implementation itself. Please open an issue to report this, providing a sample of the " +
                                        "code that generated this error!")
                                .isEqualTo(StringUtils.readFully(input.getSource()));
                    }
                }

                //noinspection unchecked
                ((Consumer<SourceFile>) nextSpec.beforeRecipe).accept(sourceFile);

                specBySourceFile.put(sourceFile, nextSpec);
            }
        }

        List<SourceFile> beforeSourceFiles = new ArrayList<>(specBySourceFile.keySet());

        testClassSpec.beforeRecipe.accept(beforeSourceFiles);
        testMethodSpec.beforeRecipe.accept(beforeSourceFiles);

        List<Result> results = recipe.run(
                beforeSourceFiles,
                executionContext,
                recipeSchedulerCheckingExpectedCycles,
                cycles,
                expectedCyclesThatMakeChanges + 1
        );

        testMethodSpec.afterRecipe.accept(results);
        testClassSpec.afterRecipe.accept(results);

        Collection<SourceSpec<?>> expectedNewSources = Collections.newSetFromMap(new IdentityHashMap<>());
        for (SourceSpec<?> sourceSpec : sourceSpecs) {
            if (sourceSpec.before == null) {
                expectedNewSources.add(sourceSpec);
            }
        }


        nextSourceSpec:
        for (SourceSpec<?> sourceSpec : expectedNewSources) {
            assertThat(sourceSpec.after).as("Either before or after must be specified in a SourceSpec").isNotNull();
            // If the source spec defines a source path, look for a result where there is a new file at that path.
            if (sourceSpec.getSourcePath() != null) {
                // If sourceSpec defines a source path, enforce there is a result that has the same source path and
                // the contents match the expected value.
                for (Result result : results) {
                    if (result.getAfter() != null && sourceSpec.getSourcePath().equals(result.getAfter().getSourcePath())) {
                        expectedNewSources.remove(sourceSpec);
                        assertThat(result.getBefore())
                                .as("Expected a new file for the source path but there was an existing file already present: " +
                                        sourceSpec.getSourcePath())
                                .isNull();
                        String actual = result.getAfter().printAll().trim();
                        String expected = trimIndentPreserveCRLF(sourceSpec.after);
                        assertThat(actual).isEqualTo(expected);
                        continue nextSourceSpec;
                    }
                }
                fail("Expected a new source file with the source path " + sourceSpec.getSourcePath());
            }

            // If the source spec has not defined a source path, look for a result with the exact contents. This logic
            // first looks for non-remote results.
            for (Result result : results) {
                if (result.getAfter() != null && !(result.getAfter() instanceof Remote)) {
                    assertThat(sourceSpec.after).as("Either before or after must be specified in a SourceSpec").isNotNull();
                    String actual = result.getAfter().printAll().trim();
                    String expected = trimIndentPreserveCRLF(sourceSpec.after);
                    if (actual.equals(expected)) {
                        expectedNewSources.remove(sourceSpec);
                        //noinspection unchecked
                        ((Consumer<SourceFile>) sourceSpec.afterRecipe).accept(result.getAfter());
                        if (sourceSpec.sourcePath != null) {
                            assertThat(result.getAfter().getSourcePath())
                                    .isEqualTo(sourceSpec.dir.resolve(sourceSpec.sourcePath));
                        }
                        break;
                    }
                }
            }

            // we tried to avoid it, and now we'll try to match against remotes...
            for (Result result : results) {
                if (result.getAfter() instanceof Remote) {
                    assertThat(sourceSpec.after).as("Either before or after must be specified in a SourceSpec").isNotNull();
                    String actual = result.getAfter().printAll();
                    String expected = trimIndentPreserveCRLF(sourceSpec.after);
                    if (actual.equals(expected)) {
                        expectedNewSources.remove(sourceSpec);
                        //noinspection unchecked
                        ((Consumer<SourceFile>) sourceSpec.afterRecipe).accept(result.getAfter());
                        if (sourceSpec.sourcePath != null) {
                            assertThat(result.getAfter().getSourcePath())
                                    .isEqualTo(sourceSpec.dir.resolve(sourceSpec.sourcePath));
                        }
                        break;
                    }
                }
            }
        }

        nextSourceFile:
        for (Map.Entry<SourceFile, SourceSpec<?>> specForSourceFile : specBySourceFile.entrySet()) {
            String expectedAfter = specForSourceFile.getValue().after;
            for (Result result : results) {

                if (result.getBefore() == specForSourceFile.getKey()) {
                    if (expectedAfter != null && result.getAfter() != null) {
                        String actual = result.getAfter().printAll();
                        String expected = trimIndentPreserveCRLF(expectedAfter);
                        assertThat(actual).isEqualTo(expected);
                        specForSourceFile.getValue().eachResult.after(result.getAfter(), testMethodSpec, testClassSpec);
                    } else if (expectedAfter == null && result.getAfter() != null) {
                        if (result.diff().isEmpty()) {
                            fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.");
                        }

                        assert result.getBefore() != null;
                        assertThat(result.getAfter().printAll())
                                .as("The recipe must not make changes")
                                .isEqualTo(result.getBefore().printAll());
                    } else if (expectedAfter != null && result.getAfter() == null) {
                        assert result.getBefore() != null;
                        fail("The recipe deleted a source file [" + result.getBefore().getSourcePath() + "] that was not expected to change");
                    }


                    //noinspection unchecked
                    ((Consumer<SourceFile>) specForSourceFile.getValue().afterRecipe).accept(result.getAfter());
                    continue nextSourceFile;
                }
            }

            // if we get here, there was no result.
            if (expectedAfter != null) {
                String before = trimIndentPreserveCRLF(specForSourceFile.getKey().printAll());
                String expected = trimIndentPreserveCRLF(expectedAfter);

                assertThat(before)
                        .as("The recipe should have made the following change.")
                        .isEqualTo(expected);
            }

            //noinspection unchecked
            ((Consumer<SourceFile>) specForSourceFile.getValue().afterRecipe).accept(specForSourceFile.getKey());
        }
        SoftAssertions newFilesGenerated = new SoftAssertions();
        for (SourceSpec<?> expectedNewSource : expectedNewSources) {
            newFilesGenerated.assertThat(expectedNewSource.after).as("No new source file was generated that matched.").isEmpty();
        }
        newFilesGenerated.assertAll();

        recipeSchedulerCheckingExpectedCycles.verify();
    }

    default void rewriteRun(SourceSpec<?>... sources) {
        rewriteRun(spec -> {
        }, sources);
    }

    default ExecutionContext defaultExecutionContext(SourceSpec<?>[] sourceSpecs) {
        ExecutionContext executionContext = new InMemoryExecutionContext(
                t -> fail("Failed to run parse sources or recipe", t));
//
//        for (SourceSpec<?> spec : sourceSpecs) {
//            if (J.CompilationUnit.class.equals(spec.sourceFileType)) {
//                executionContext.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
//                break;
//            }
//        }

//        if (MavenSettings.readFromDiskEnabled()) {
//            for (SourceSpec<?> sourceSpec : sourceSpecs) {
//                if ("maven".equals(sourceSpec.dsl)) {
//                    MavenExecutionContextView.view(executionContext)
//                            .setMavenSettings(MavenSettings.readMavenSettingsFromDisk(executionContext));
//                    break;
//                }
//            }
//        }

        return executionContext;
    }

    @NotNull
    @Override
    default Iterator<SourceSpec<?>> iterator() {
        return new Iterator<SourceSpec<?>>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public SourceSpec<?> next() {
                throw new UnsupportedOperationException("RewriteTest is not intended to be iterated.");
            }
        };
    }
}

class RewriteTestUtils {
    static boolean groupSourceSpecsByParser(RecipeSpec testMethodSpec, Map<ParserSupplier, List<SourceSpec<?>>> sourceSpecsByParser, SourceSpec<?> sourceSpec) {
        for (Parser<?> parser : testMethodSpec.parsers) {
            if (parserType(parser).equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(sourceSpec.sourceFileType, sourceSpec.dsl, () -> parser),
                        p -> new ArrayList<>()).add(sourceSpec);
                return true;
            }
        }
        return false;
    }
}
