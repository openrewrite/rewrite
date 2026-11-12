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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Getter;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Getter
@SuppressWarnings("UnusedReturnValue")
public class RecipeSpec {
    public static Supplier<RecipeSpec> DEFAULTS = RecipeSpec::new;

    public static RecipeSpec defaults() {
        return DEFAULTS.get();
    }

    @Nullable
    Recipe recipe;

    /**
     * Default parsers to use if no more specific parser is set
     * on the {@link SourceSpec}.
     */
    List<Parser.Builder> parsers = new ArrayList<>();

    /**
     * Parsers of the recipe's output, where it needs another classpath than its input.
     */
    List<Parser.Builder> afterParsers = new ArrayList<>();

    /**
     * Used for both parsing and recipe execution unless an alternative recipe execution context is set with
     * {@link #recipeExecutionContext(ExecutionContext)}.
     */
    @Nullable
    ExecutionContext executionContext;

    /**
     * If not specified, will share {@link #executionContext} instance with the
     * parsing phase.
     */
    @Nullable
    ExecutionContext recipeExecutionContext;

    @Nullable
    Path relativeTo;

    @Nullable
    Integer cycles;

    @Nullable
    Integer expectedCyclesThatMakeChanges;

    @Nullable
    TypeValidation typeValidation;

    /**
     * @deprecated Use {@link #recipeQualityValidation}'s {@link RecipeQualityValidation#types}.
     */
    @Deprecated
    @Nullable
    TypeValidation afterTypeValidation;

    @Nullable
    RecipeQualityValidation recipeQualityValidation;

    boolean serializationValidation = true;

    PrintOutputCapture.@Nullable MarkerPrinter markerPrinter;

    List<UncheckedConsumer<List<SourceFile>>> beforeRecipes = new ArrayList<>();

    List<UncheckedConsumer<RecipeRun>> afterRecipes = new ArrayList<>();

    List<UncheckedConsumer<SourceSpec<?>>> allSources = new ArrayList<>();

    @Nullable
    Function<List<SourceFile>, LargeSourceSet> sourceSet;

    @Nullable
    RecipePrinter recipePrinter;

    @Nullable
    RewriteRunner runner;

    /**
     * Selects how the recipe is executed for this run. The default is
     * {@link RewriteRunner#IN_PROCESS}; pass an alternative (e.g. a moderne-cli
     * runner) to exercise the same test through an out-of-process backend.
     * <p>
     * Resolution order: per-call spec runner &gt; per-class
     * {@link RewriteTest#runner()} &gt; {@link RewriteRunner#IN_PROCESS}.
     */
    public RecipeSpec runner(RewriteRunner runner) {
        this.runner = runner;
        return this;
    }

    /**
     * Configuration that applies to all source file inputs.
     */
    public RecipeSpec allSources(UncheckedConsumer<SourceSpec<?>> allSources) {
        this.allSources.add(allSources);
        return this;
    }

    public RecipeSpec recipe(Recipe recipe) {
        this.recipe = recipe;
        return this;
    }

    public RecipeSpec recipes(Recipe... recipes) {
        this.recipe = new CompositeRecipe(Arrays.asList(recipes));
        return this;
    }

    public RecipeSpec recipe(InputStream yaml, String... activeRecipes) {
        return recipe(recipeFromInputStream(yaml, activeRecipes));
    }

    public RecipeSpec recipeFromYaml(@Language("yaml") String yaml, String... activeRecipes) {
        return recipe(RECIPE_CACHE.computeIfAbsent(key("recipeFromYaml", yaml, key(activeRecipes)),
                k -> recipeFromInputStream(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), activeRecipes)));
    }

    public RecipeSpec recipeFromResource(String yamlResource, String... activeRecipes) {
        return recipe(RECIPE_CACHE.computeIfAbsent(key("recipeFromResource", yamlResource, key(activeRecipes)),
                k -> recipeFromInputStream(
                        Objects.requireNonNull(RecipeSpec.class.getResourceAsStream(yamlResource)), activeRecipes)));
    }

    public RecipeSpec recipeFromResources(String... activeRecipes) {
        return recipe(RECIPE_CACHE.computeIfAbsent(key("recipeFromResources", key(activeRecipes)),
                k -> Environment.builder()
                        .scanYamlResources()
                        .build()
                        .activateRecipes(activeRecipes)));
    }

    private static Recipe recipeFromInputStream(InputStream yaml, String... activeRecipes) {
        return Environment.builder()
                .load(new YamlResourceLoader(yaml, URI.create("rewrite.yml"), new Properties(), null, emptyList(),
                        mapper -> mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)))
                .build()
                .activateRecipes(activeRecipes);
    }

    private static String key(String... s) {
        return String.join("-", s);
    }

    private static final Map<String, Recipe> RECIPE_CACHE = new ConcurrentHashMap<>();

    /**
     * @param parser The parser supplier to use when a matching source file is found.
     * @return The current recipe spec.
     */
    public RecipeSpec parser(Parser.Builder parser) {
        this.parsers.add(parser);
        return this;
    }

    /**
     * The parser of the recipe's output, for a recipe whose output needs another classpath than its input, such as
     * one that migrates from one version of a library to the next. The checks that parse the text the output prints,
     * such as {@link RecipeQualityValidation#parserTypedTrees()}, parse it with this parser in place of the input's
     * parser of the same language, so that the types they compare the output with are those of the classpath it is
     * written for. It parses the output together with the sources the recipe left unchanged, so a class that differs
     * between the two classpaths belongs on them and not among the test's sources. A type it doesn't resolve isn't
     * compared, so it needs the whole classpath of the output and not only the libraries that change.
     *
     * @return The current recipe spec.
     */
    public RecipeSpec afterParser(Parser.Builder parser) {
        this.afterParsers.add(parser);
        return this;
    }

    public RecipeSpec executionContext(ExecutionContext ctx) {
        this.executionContext = ctx;
        return this;
    }

    public RecipeSpec recipeExecutionContext(ExecutionContext ctx) {
        this.recipeExecutionContext = ctx;
        return this;
    }

    public RecipeSpec markerPrinter(PrintOutputCapture.MarkerPrinter markerPrinter) {
        this.markerPrinter = markerPrinter;
        return this;
    }

    public RecipeSpec relativeTo(@Nullable Path relativeTo) {
        this.relativeTo = relativeTo;
        return this;
    }

    /**
     * The cycles to run, by default one more than {@link #expectedCyclesThatMakeChanges(int)} and at least two.
     */
    public RecipeSpec cycles(int cycles) {
        this.cycles = cycles;
        return this;
    }

    public RecipeSpec beforeRecipe(UncheckedConsumer<List<SourceFile>> beforeRecipe) {
        this.beforeRecipes.add(beforeRecipe);
        return this;
    }

    public RecipeSpec afterRecipe(UncheckedConsumer<RecipeRun> afterRecipe) {
        this.afterRecipes.add(afterRecipe);
        return this;
    }

    @Incubating(since = "7.35.0")
    public <E> RecipeSpec dataTable(Class<E> rowType, UncheckedConsumer<List<E>> extract) {
        return afterRecipe(run -> {
            DataTableStore store = run.getDataTableStore();
            for (DataTable<?> dt : store.getDataTables()) {
                if (dt.getType().equals(rowType)) {
                    List<E> rows;
                    try (Stream<?> stream = store.getRows(dt.getName(), dt.getGroup())) {
                        @SuppressWarnings("unchecked")
                        List<E> typed = (List<E>) stream.collect(toList());
                        rows = typed;
                    }
                    assertThat(rows).isNotNull();
                    assertThat(rows).isNotEmpty();
                    extract.accept(rows);
                    return;
                }
            }
            String message = "No data table found with row type: " + rowType;
            Collection<DataTable<?>> tables = store.getDataTables();
            if (!tables.isEmpty()) {
                message += "\nFound data tables row type(s): " + tables.stream()
                        .map(it -> it.getType().getName().replace("$", "."))
                        .collect(joining(","));
            }
            //noinspection ResultOfMethodCallIgnored
            fail(message);
        });
    }

    @Incubating(since = "7.35.0")
    public <E, V> RecipeSpec dataTableAsCsv(Class<? extends DataTable<?>> dataTableClass, String expect) {
        return dataTableAsCsv(dataTableClass.getName(), expect);
    }

    @Incubating(since = "7.35.0")
    public <E, V> RecipeSpec dataTableAsCsv(String name, String expect) {
        afterRecipe(run -> {
            DataTableStore store = run.getDataTableStore();
            DataTable<?> dataTable = null;
            for (DataTable<?> dt : store.getDataTables()) {
                if (dt.getName().equals(name)) {
                    dataTable = dt;
                    break;
                }
            }
            assertThat(dataTable).isNotNull();
            List<?> rows;
            try (Stream<?> stream = store.getRows(dataTable.getName(), dataTable.getGroup())) {
                rows = stream.collect(toList());
            }
            StringWriter writer = new StringWriter();
            CsvMapper mapper = CsvMapper.builder()
                    .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .build();
            CsvSchema schema = mapper.schemaFor(dataTable.getType()).withHeader();
            mapper.writerFor(dataTable.getType()).with(schema).writeValues(writer).writeAll(rows);
            assertThat(writer.toString()).isEqualTo(expect);
        });
        return this;
    }

    /**
     * Whether the recipe is checked to serialize, load and describe itself as recipes must. See
     * {@link #typeValidationOptions(TypeValidation)} for finding opt-outs that a test no longer needs.
     */
    @Incubating(since = "7.35.0")
    public RecipeSpec validateRecipeSerialization(boolean validate) {
        this.serializationValidation = validate;
        return this;
    }

    public RecipeSpec sourceSet(Function<List<SourceFile>, LargeSourceSet> sourceSetBuilder) {
        this.sourceSet = sourceSetBuilder;
        return this;
    }

    public RecipeSpec expectedCyclesThatMakeChanges(int expectedCyclesThatMakeChanges) {
        this.expectedCyclesThatMakeChanges = expectedCyclesThatMakeChanges;
        return this;
    }

    int getExpectedCyclesThatMakeChanges(int cycles) {
        return expectedCyclesThatMakeChanges == null ? cycles - 1 :
                expectedCyclesThatMakeChanges;
    }

    /**
     * Checks of the LST the test parses, and of what the recipe produces unless {@link #afterTypeValidationOptions} or
     * the types of {@link #recipeQualityValidation} are set.
     * <p>
     * To find opt-outs that tests no longer need, set the system property
     * {@code org.openrewrite.test.failOnUnneededOptOuts} to {@code all}, or to the options to look at, such as
     * {@code methodInvocations,unknown}. Then a test that turns off checks that pass on its sources fails, and says
     * which opt-outs it needs instead. This covers this option, {@link #afterTypeValidationOptions},
     * {@link #recipeQualityValidation} and {@link #validateRecipeSerialization}, whether a test or its class's defaults
     * set them.
     */
    public RecipeSpec typeValidationOptions(TypeValidation typeValidation) {
        this.typeValidation = typeValidation;
        return this;
    }

    /**
     * @deprecated Use {@link #recipeQualityValidation(RecipeQualityValidation)} with
     * {@link RecipeQualityValidation#types}.
     */
    @Deprecated
    public RecipeSpec afterTypeValidationOptions(TypeValidation typeValidation) {
        this.afterTypeValidation = typeValidation;
        return this;
    }

    /**
     * Checks of how the recipe behaves and what it produces, including the type attribution of its output. See
     * {@link #typeValidationOptions(TypeValidation)} for finding opt-outs that a test no longer needs.
     */
    public RecipeSpec recipeQualityValidation(RecipeQualityValidation recipeQualityValidation) {
        this.recipeQualityValidation = recipeQualityValidation;
        return this;
    }

    @Nullable
    ExecutionContext getExecutionContext() {
        return executionContext;
    }

    @Incubating(since = "8.12.1")
    public RecipeSpec printRecipe(RecipePrinter recipePrinter) {
        this.recipePrinter = recipePrinter;
        return this;
    }
}
