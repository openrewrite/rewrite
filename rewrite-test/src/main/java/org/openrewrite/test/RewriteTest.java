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
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SourceSet;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.scheduling.DirectScheduler;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.openrewrite.internal.StringUtils.trimIndentPreserveCRLF;

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

        PrintOutputCapture.MarkerPrinter markerPrinter;
        if (testMethodSpec.getMarkerPrinter() != null) {
            markerPrinter = testMethodSpec.getMarkerPrinter();
        } else if (testClassSpec.getMarkerPrinter() != null) {
            markerPrinter = testClassSpec.getMarkerPrinter();
        } else {
            markerPrinter = PrintOutputCapture.MarkerPrinter.DEFAULT;
        }

        PrintOutputCapture<Integer> out = new PrintOutputCapture<>(0, markerPrinter);

        Recipe recipe = testMethodSpec.recipe == null ? testClassSpec.recipe : testMethodSpec.recipe;
        assertThat(recipe)
                .as("A recipe must be specified")
                .isNotNull();

        assertThat(recipe.validate().failures())
                .as("Recipe validation must have no failures")
                .isEmpty();

        if (!(recipe instanceof AdHocRecipe) &&
            testClassSpec.serializationValidation &&
            testMethodSpec.serializationValidation) {
            RecipeSerializer recipeSerializer = new RecipeSerializer();
            assertThat(recipeSerializer.read(recipeSerializer.write(recipe)))
                    .as("Recipe must be serializable/deserializable")
                    .isEqualTo(recipe);
            validateRecipeNameAndDescription(recipe);
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
        for (SourceSpec<?> s : sourceSpecs) {
            s.customizeExecutionContext.accept(executionContext);
        }

        Map<Parser.Builder, List<SourceSpec<?>>> sourceSpecsByParser = new HashMap<>();
        List<Parser.Builder> methodSpecParsers = testMethodSpec.parsers;
        // Clone class-level parsers to ensure that no state leaks between tests
        List<Parser.Builder> testClassSpecParsers = testClassSpec.parsers.stream()
                .map(Parser.Builder::clone)
                .collect(Collectors.toList());
        for (SourceSpec<?> sourceSpec : sourceSpecs) {
            // ----- method specific parser -------------------------
            if (RewriteTestUtils.groupSourceSpecsByParser(methodSpecParsers, sourceSpecsByParser, sourceSpec)) {
                continue;
            }

            // ----- test default parser -------------------------
            if (RewriteTestUtils.groupSourceSpecsByParser(testClassSpecParsers, sourceSpecsByParser, sourceSpec)) {
                continue;
            }

            // ----- default parsers for each SourceFile type -------------------------
            sourceSpecsByParser.computeIfAbsent(sourceSpec.getParser().clone(), p -> new ArrayList<>()).add(sourceSpec);
        }

        Map<SourceFile, SourceSpec<?>> specBySourceFile = new HashMap<>(sourceSpecs.length);
        for (Map.Entry<Parser.Builder, List<SourceSpec<?>>> sourceSpecsForParser : sourceSpecsByParser.entrySet()) {
            Map<SourceSpec<?>, Parser.Input> inputs = new LinkedHashMap<>(sourceSpecsForParser.getValue().size());
            Parser<?> parser = sourceSpecsForParser.getKey().build();
            for (SourceSpec<?> sourceSpec : sourceSpecsForParser.getValue()) {
                if (sourceSpec.before == null) {
                    continue;
                }
                String beforeTrimmed = sourceSpec.noTrim ?
                        sourceSpec.before :
                        trimIndentPreserveCRLF(sourceSpec.before);
                Path sourcePath;
                if (sourceSpec.sourcePath != null) {
                    sourcePath = sourceSpec.dir.resolve(sourceSpec.sourcePath);
                } else {
                    sourcePath = parser.sourcePathFromSourceText(sourceSpec.dir, beforeTrimmed);
                }
                inputs.put(sourceSpec, new Parser.Input(sourcePath, () -> new ByteArrayInputStream(beforeTrimmed.getBytes(parser.getCharset(executionContext)))));
            }

            Path relativeTo = testMethodSpec.relativeTo == null ? testClassSpec.relativeTo : testMethodSpec.relativeTo;

            Iterator<SourceSpec<?>> sourceSpecIter = inputs.keySet().iterator();

            //noinspection unchecked
            List<SourceFile> sourceFiles = (List<SourceFile>) parser.parseInputs(inputs.values(), relativeTo, executionContext);
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

                // Update the default 'main' SourceSet Marker added by the JavaParser with the specs sourceSetName
                if (nextSpec.sourceSetName != null) {
                    sourceFile = sourceFile.withMarkers((sourceFile.getMarkers().withMarkers(ListUtils.map(sourceFile.getMarkers().getMarkers(), m -> {
                        if (m instanceof SourceSet) {
                            m = ((SourceSet) m).withName(nextSpec.sourceSetName);
                        }
                        return m;
                    }))));
                }

                // Validate that printing a parsed AST yields the same source text
                int j = 0;
                for (Parser.Input input : inputs.values()) {
                    if (j++ == i && !(sourceFile instanceof Quark)) {
                        assertThat(sourceFile.printAll(out.clone()))
                                .as("When parsing and printing the source code back to text without modifications, " +
                                    "the printed source didn't match the original source code. This means there is a bug in the " +
                                    "parser implementation itself. Please open an issue to report this, providing a sample of the " +
                                    "code that generated this error!")
                                .isEqualTo(StringUtils.readFully(input.getSource(executionContext), parser.getCharset(executionContext)));
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

        List<SourceFile> runnableSourceFiles = new ArrayList<>(beforeSourceFiles.size());
        for (Map.Entry<SourceFile, SourceSpec<?>> sourceFileSpec : specBySourceFile.entrySet()) {
            if (!sourceFileSpec.getValue().isSkip()) {
                runnableSourceFiles.add(sourceFileSpec.getKey());
            }
        }

        ExecutionContext recipeExecutionContext = executionContext;
        if (testMethodSpec.getRecipeExecutionContext() != null) {
            recipeExecutionContext = testMethodSpec.getRecipeExecutionContext();
        } else if (testClassSpec.getRecipeExecutionContext() != null) {
            recipeExecutionContext = testClassSpec.getRecipeExecutionContext();
        }

        RecipeRun recipeRun = recipe.run(
                runnableSourceFiles,
                recipeExecutionContext,
                recipeSchedulerCheckingExpectedCycles,
                cycles,
                expectedCyclesThatMakeChanges + 1
        );

        testMethodSpec.afterRecipe.accept(recipeRun);
        testClassSpec.afterRecipe.accept(recipeRun);

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
                for (Result result : recipeRun.getResults()) {

                    if (result.getAfter() != null && sourceSpec.getSourcePath().equals(result.getAfter().getSourcePath())) {
                        expectedNewSources.remove(sourceSpec);
                        assertThat(result.getBefore())
                                .as("Expected a new file for the source path but there was an existing file already present: " +
                                    sourceSpec.getSourcePath())
                                .isNull();
                        String actual = result.getAfter().printAll(out.clone()).trim();
                        String expected = sourceSpec.noTrim ?
                                sourceSpec.after.apply(actual) :
                                trimIndentPreserveCRLF(sourceSpec.after.apply(actual));
                        assertThat(actual).isEqualTo(expected);
                        continue nextSourceSpec;
                    }
                }
                fail("Expected a new source file with the source path " + sourceSpec.getSourcePath());
            }

            // If the source spec has not defined a source path, look for a result with the exact contents. This logic
            // first looks for non-remote results.
            for (Result result : recipeRun.getResults()) {
                if (result.getAfter() != null && !(result.getAfter() instanceof Remote)) {
                    assertThat(sourceSpec.after).as("Either before or after must be specified in a SourceSpec").isNotNull();
                    String actual = result.getAfter().printAll(out.clone()).trim();
                    String expected = trimIndentPreserveCRLF(sourceSpec.after.apply(actual));
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
            for (Result result : recipeRun.getResults()) {
                if (result.getAfter() instanceof Remote) {
                    assertThat(sourceSpec.after).as("Either before or after must be specified in a SourceSpec").isNotNull();
                    String actual = result.getAfter().printAll(out.clone());
                    String expected = trimIndentPreserveCRLF(sourceSpec.after.apply(actual));
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
            for (Result result : recipeRun.getResults()) {
                if (result.getBefore() == specForSourceFile.getKey()) {
                    if (result.getAfter() != null) {
                        String expectedAfter = specForSourceFile.getValue().after == null ? null :
                                specForSourceFile.getValue().after.apply(result.getAfter().printAll(out.clone()));
                        if (expectedAfter != null) {
                            String actual = result.getAfter().printAll(out.clone());
                            String expected = trimIndentPreserveCRLF(expectedAfter);
                            assertThat(actual).isEqualTo(expected);
                            specForSourceFile.getValue().eachResult.accept(result.getAfter(), testMethodSpec, testClassSpec);
                        } else {
                            if (result.diff().isEmpty() && !(result.getAfter() instanceof Remote)) {
                                fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.");
                            }

                            assert result.getBefore() != null;
                            assertThat(result.getAfter().printAll(out.clone()))
                                    .as("The recipe must not make changes")
                                    .isEqualTo(result.getBefore().printAll(out.clone()));
                        }
                    } else if (result.getAfter() == null) {
                        if (specForSourceFile.getValue().after == null) {
                            // If the source spec was not expecting a change (spec.after == null) but the file has been
                            // deleted, assert failure.
                            assert result.getBefore() != null;
                            fail("The recipe deleted a source file \"" + result.getBefore().getSourcePath() + "\" that was not expected to change");
                        } else {
                            String expected = specForSourceFile.getValue().after.apply(null);
                            if (expected != null) {
                                // The spec expected the file to be changed, not deleted.
                                assert result.getBefore() != null;
                                assertThat((String) null)
                                        .as("The recipe deleted a source file \"" + result.getBefore().getSourcePath() + "\" but should have changed it instead")
                                        .isEqualTo(expected);
                            }
                        }
                    }


                    //noinspection unchecked
                    ((Consumer<SourceFile>) specForSourceFile.getValue().afterRecipe).accept(result.getAfter());
                    continue nextSourceFile;
                }
            }

            // if we get here, there was no result.
            if (specForSourceFile.getValue().after != null) {
                String before = trimIndentPreserveCRLF(specForSourceFile.getKey().printAll(out.clone()));
                String expected = trimIndentPreserveCRLF(specForSourceFile.getValue().after.apply(null));
                assertThat(expected)
                        .as("To assert that a Recipe makes no change, supply only \"before\" source.")
                        .isNotEqualTo(before);
                assertThat(before)
                        .as("The recipe should have made the following change.")
                        .isEqualTo(expected);
            }

            //noinspection unchecked
            ((Consumer<SourceFile>) specForSourceFile.getValue().afterRecipe).accept(specForSourceFile.getKey());
        }
        SoftAssertions newFilesGenerated = new SoftAssertions();
        for (SourceSpec<?> expectedNewSource : expectedNewSources) {
            newFilesGenerated.assertThat(expectedNewSource.after == null ? null : expectedNewSource.after.apply(null))
                    .as("No new source file was generated that matched.")
                    .isEmpty();
        }
        newFilesGenerated.assertAll();

        recipeSchedulerCheckingExpectedCycles.verify();
    }

    default void rewriteRun(SourceSpec<?>... sources) {
        rewriteRun(spec -> {
        }, sources);
    }

    default ExecutionContext defaultExecutionContext(SourceSpec<?>[] sourceSpecs) {
        return new InMemoryExecutionContext(t -> fail("Failed to parse sources or run recipe", t));
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

    default void validateRecipeNameAndDescription(Recipe recipe) {
        if (recipe instanceof CompositeRecipe) {
            for (Recipe childRecipe : recipe.getRecipeList()) {
                validateRecipeNameAndDescription(childRecipe);
            }
        } else {
            assertThat(recipe.getDisplayName().endsWith(".")).as("%s Display Name should not end with a period.", recipe.getName()).isFalse();
            assertThat(recipe.getDescription()).as("%s Description should not be null or empty", recipe.getName()).isNotEmpty();
            assertThat(recipe.getDescription().endsWith(".")).as("%s Description should end with a period.", recipe.getName()).isTrue();
        }
    }
}

class RewriteTestUtils {
    static boolean groupSourceSpecsByParser(List<Parser.Builder> parserBuilders, Map<Parser.Builder, List<SourceSpec<?>>> sourceSpecsByParser, SourceSpec<?> sourceSpec) {
        for (Map.Entry<Parser.Builder, List<SourceSpec<?>>> entry : sourceSpecsByParser.entrySet()) {
            if (entry.getKey().getSourceFileType().equals(sourceSpec.sourceFileType)) {
                entry.getValue().add(sourceSpec);
                return true;
            }
        }
        for (Parser.Builder parser : parserBuilders) {
            if (parser.getSourceFileType().equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(parser, p -> new ArrayList<>()).add(sourceSpec);
                return true;
            }
        }
        return false;
    }
}
