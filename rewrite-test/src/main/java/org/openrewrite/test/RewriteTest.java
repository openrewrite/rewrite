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
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.TypeValidation;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.protobuf.ProtoParser;
import org.openrewrite.protobuf.tree.Proto;
import org.openrewrite.quark.Quark;
import org.openrewrite.quark.QuarkParser;
import org.openrewrite.remote.Remote;
import org.openrewrite.scheduling.DirectScheduler;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

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
            if (Quark.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(Quark.class, sourceSpec.dsl, QuarkParser::new),
                        p -> new ArrayList<>()).add(sourceSpec);
            } else if (J.CompilationUnit.class.equals(sourceSpec.sourceFileType)) {
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
            } else if (G.CompilationUnit.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(G.CompilationUnit.class, sourceSpec.dsl, () -> {
                            if ("gradle".equals(sourceSpec.dsl)) {
                                return new GradleParser(GroovyParser.builder());
                            }
                            return GroovyParser.builder().build();
                        }),
                        p -> new ArrayList<>()).add(sourceSpec);
            } else if (Yaml.Documents.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(Yaml.Documents.class, sourceSpec.dsl, YamlParser::new),
                        p -> new ArrayList<>()).add(sourceSpec);
            } else if (Json.Document.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(Json.Document.class, sourceSpec.dsl, JsonParser::new),
                        p -> new ArrayList<>()).add(sourceSpec);
            } else if (Hcl.ConfigFile.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(Hcl.ConfigFile.class, sourceSpec.dsl, () -> HclParser.builder().build()),
                        p -> new ArrayList<>()).add(sourceSpec);
            } else if (Proto.Document.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(Proto.Document.class, sourceSpec.dsl, ProtoParser::new),
                        p -> new ArrayList<>()).add(sourceSpec);
            } else if (PlainText.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(PlainText.class, sourceSpec.dsl, PlainTextParser::new),
                        p -> new ArrayList<>()).add(sourceSpec);
            } else if (Properties.File.class.equals(sourceSpec.sourceFileType)) {
                sourceSpecsByParser.computeIfAbsent(
                        new ParserSupplier(Properties.File.class, sourceSpec.dsl, PropertiesParser::new),
                        p -> new ArrayList<>()).add(sourceSpec);
            }
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
                sourceFile = sourceFile.withMarkers((sourceFile.getMarkers().withMarkers(ListUtils.map(sourceFile.getMarkers().getMarkers(), m -> {
                    if (m instanceof JavaSourceSet) {
                        m = ((JavaSourceSet) m).withName(nextSpec.sourceSetName);
                    }
                    return m;
                }))));

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

        // take one pass at this attempting to satisfy expected new sources, checking Remote last to optimize
        // for not downloading huge files in unit tests if not necessary.
        for (SourceSpec<?> sourceSpec : expectedNewSources) {
            if (sourceSpec.before == null) {
                for (Result result : results) {
                    if (result.getAfter() != null && !(result.getAfter() instanceof Remote)) {
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
                        if (result.getAfter() instanceof JavaSourceFile) {
                            TypeValidation typeValidation = testMethodSpec.typeValidation != null ? testMethodSpec.typeValidation : testClassSpec.typeValidation;
                            if (typeValidation == null) {
                                typeValidation = new TypeValidation();
                            }
                            typeValidation.assertValidTypes((JavaSourceFile) result.getAfter());
                        }
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

            //If we get here, there was no result.
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

        for (SourceSpec<?> spec : sourceSpecs) {
            if (J.CompilationUnit.class.equals(spec.sourceFileType)) {
                executionContext.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
                break;
            }
        }

        if (MavenSettings.readFromDiskEnabled()) {
            for (SourceSpec<?> sourceSpec : sourceSpecs) {
                if ("maven".equals(sourceSpec.dsl)) {
                    MavenExecutionContextView.view(executionContext)
                            .setMavenSettings(MavenSettings.readMavenSettingsFromDisk(executionContext));
                    break;
                }
            }
        }

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
