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

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.scheduling.DirectScheduler;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

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

@Incubating(since = "7.20.1")
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

    default void defaults(RecipeSpec spec) {
    }

    default void rewriteRun(SourceSpecs... sourceSpecs) {
        rewriteRun(spec -> {
        }, sourceSpecs);
    }

    default void rewriteRun(Consumer<RecipeSpec> specChange, SourceSpecs... sourceSpecs) {
        rewriteRun(specChange, Arrays.stream(sourceSpecs)
                .flatMap(specGroup -> StreamSupport.stream(specGroup.spliterator(), false))
                .toArray(SourceSpec[]::new)
        );
    }

    default void rewriteRun(Consumer<RecipeSpec> specChange, SourceSpec<?>... sourceSpecs) {
        RecipeSpec testClassSpec = RecipeSpec.defaults();
        defaults(testClassSpec);

        RecipeSpec testMethodSpec = RecipeSpec.defaults();
        specChange.accept(testMethodSpec);

        Recipe recipe = testMethodSpec.recipe == null ? testClassSpec.recipe : testMethodSpec.recipe;
        assertThat(recipe)
                .as("A recipe must be specified")
                .isNotNull();

        if (!(recipe instanceof AdHocRecipe)) {
            RecipeSerializer recipeSerializer = new RecipeSerializer();
            assertThat(recipeSerializer.read(recipeSerializer.write(recipe)))
                    .as("Recipe must be serializable/deserializable")
                    .isEqualTo(recipe);
        }

        int cycles = testMethodSpec.cycles == null ? testClassSpec.getCycles() : testMethodSpec.getCycles();
        int expectedCyclesThatMakeChanges = testMethodSpec.expectedCyclesThatMakeChanges == null ?
                testClassSpec.getExpectedCyclesThatMakeChanges(cycles) : testMethodSpec.getExpectedCyclesThatMakeChanges(cycles);

        RecipeSchedulerCheckingExpectedCycles recipeSchedulerCheckingExpectedCycles =
                new RecipeSchedulerCheckingExpectedCycles(DirectScheduler.common(), expectedCyclesThatMakeChanges);

        ExecutionContext executionContext = testMethodSpec.executionContext == null ? testClassSpec.getExecutionContext() :
                testMethodSpec.getExecutionContext();

        Map<ParserSupplier, List<SourceSpec<?>>> sourceSpecsByParser = new HashMap<>();

        nextSource:
        for (SourceSpec<?> sourceSpec : sourceSpecs) {
            // ----- method specific parser -------------------------
            for (Parser<?> parser : testMethodSpec.parsers) {
                if (parserType(parser).equals(sourceSpec.sourceFileType)) {
                    sourceSpecsByParser.computeIfAbsent(
                            new ParserSupplier(sourceSpec.sourceFileType, sourceSpec.dsl, () -> parser),
                            p -> new ArrayList<>()).add(sourceSpec);
                    continue nextSource;
                }
            }

            // ----- test default parser -------------------------
            for (Parser<?> parser : testClassSpec.parsers) {
                if (parserType(parser).equals(sourceSpec.sourceFileType)) {
                    sourceSpecsByParser.computeIfAbsent(
                            new ParserSupplier(sourceSpec.sourceFileType, sourceSpec.dsl, () -> parser),
                            p -> new ArrayList<>()).add(sourceSpec);
                    continue nextSource;
                }
            }

            // ----- default parsers for each SourceFile type -------------------------
            if (J.CompilationUnit.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(J.CompilationUnit.class, sourceSpec.dsl, () -> JavaParser.fromJavaVersion()
                                .logCompilationWarningsAndErrors(true)
                                .build()),
                        p -> new ArrayList<>()).add(sourceSpec);
            } else if (Xml.Document.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(Xml.Document.class, sourceSpec.dsl, () -> {
                            if ("maven".equals(sourceSpec.dsl)) {
                                return MavenParser.builder().build();
                            }
                            return new XmlParser();
                        }),
                        p -> new ArrayList<>()).add(sourceSpec);
            }
        }

        Map<SourceFile, SourceSpec<?>> specBySourceFile = new HashMap<>(sourceSpecs.length);
        for (Map.Entry<ParserSupplier, List<SourceSpec<?>>> sourceSpecsForParser : sourceSpecsByParser.entrySet()) {
            Map<SourceSpec<?>, Parser.Input> inputs = new HashMap<>(sourceSpecsForParser.getValue().size());
            for (SourceSpec<?> sourceSpec : sourceSpecsForParser.getValue()) {
                String beforeTrimmed = trimIndentPreserveCRLF(sourceSpec.before);
                Path sourcePath = sourceSpecsForParser.getKey().get()
                        .sourcePathFromSourceText(sourceSpec.dir, beforeTrimmed);
                inputs.put(sourceSpec, new Parser.Input(sourcePath, () -> new ByteArrayInputStream(beforeTrimmed.getBytes(StandardCharsets.UTF_8))));
            }

            Path relativeTo = testMethodSpec.relativeTo == null ? testClassSpec.relativeTo : testMethodSpec.relativeTo;

            Iterator<SourceSpec<?>> sourceSpecIter = inputs.keySet().iterator();
            for (SourceFile sourceFile : sourceSpecsForParser.getKey().get()
                    .parseInputs(inputs.values(), relativeTo, executionContext)) {
                sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().withMarkers(ListUtils.concatAll(
                        sourceFile.getMarkers().getMarkers(), testClassSpec.allSources.markers)));
                sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().withMarkers(ListUtils.concatAll(
                        sourceFile.getMarkers().getMarkers(), testMethodSpec.allSources.markers)));

                SourceSpec<?> spec = sourceSpecIter.next();
                sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().withMarkers(ListUtils.concatAll(
                        sourceFile.getMarkers().getMarkers(), spec.markers)));

                specBySourceFile.put(sourceFile, spec);
            }
        }

        List<SourceFile> beforeSourceFiles = new ArrayList<>(specBySourceFile.keySet());
        List<Result> results = recipe.run(
                beforeSourceFiles,
                executionContext,
                recipeSchedulerCheckingExpectedCycles,
                cycles,
                expectedCyclesThatMakeChanges + 1
        );

        for (Result result : results) {
            SourceFile before = result.getBefore();
            SourceSpec<?> resultSpec = specBySourceFile.get(before);

            if (resultSpec.after == null) {
                if (result.diff().isEmpty()) {
                    fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.");
                }
                SourceFile after = result.getAfter();
                if (after == null) {
                    fail("The recipe deleted a source file that was not expected to change");
                } else {
                    assertThat(after.printAll())
                            .as("The recipe must not make changes")
                            .isEqualTo(before == null ? "" : before.printAll());
                }
            } else {
                assertThat(result.getAfter()).isNotNull();
                String actual = result.getAfter().printAll();
                String expected = trimIndentPreserveCRLF(resultSpec.after);
                assertThat(actual).isEqualTo(expected);

                //noinspection unchecked
                ((Consumer<SourceFile>) resultSpec.afterRecipe).accept(result.getAfter());
            }
        }

        nextSpec:
        for (Map.Entry<SourceFile, SourceSpec<?>> specForSourceFile : specBySourceFile.entrySet()) {
            if (specForSourceFile.getValue().after != null) {
                for (Result result : results) {
                    if (result.getBefore() == specForSourceFile.getKey()) {
                        continue nextSpec;
                    }
                }
                if (results.isEmpty()) {
                    fail("The recipe must make changes");
                }
            }
        }

        recipeSchedulerCheckingExpectedCycles.verify();
    }

    default void rewriteRun(SourceSpec<?>... sources) {
        rewriteRun(spec -> {
        }, sources);
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
