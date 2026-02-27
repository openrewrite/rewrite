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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SuppressWarnings("UnusedReturnValue")
@Getter
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

    @Nullable
    TypeValidation afterTypeValidation;

    boolean serializationValidation = true;

    boolean recipeValidation = true;

    PrintOutputCapture.@Nullable MarkerPrinter markerPrinter;

    List<UncheckedConsumer<List<SourceFile>>> beforeRecipes = new ArrayList<>();

    List<UncheckedConsumer<RecipeRun>> afterRecipes = new ArrayList<>();

    List<UncheckedConsumer<SourceSpec<?>>> allSources = new ArrayList<>();

    @Nullable
    Function<List<SourceFile>, LargeSourceSet> sourceSet;

    @Nullable
    RecipePrinter recipePrinter;

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
            for (Map.Entry<DataTable<?>, List<?>> dataTableListEntry : run.getDataTables().entrySet()) {
                if (dataTableListEntry.getKey().getType().equals(rowType)) {
                    List<E> rows = run.getDataTableRows(dataTableListEntry.getKey().getName());
                    assertThat(rows).isNotNull();
                    assertThat(rows).isNotEmpty();
                    extract.accept(rows);
                    return;
                }
            }
            String message = "No data table found with row type: " + rowType;
            Set<DataTable<?>> tables = run.getDataTables().keySet();
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
            DataTable<?> dataTable = run.getDataTable(name);
            assertThat(dataTable).isNotNull();
            List<E> rows = run.getDataTableRows(name);
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

    @Incubating(since = "7.35.0")
    public RecipeSpec validateRecipeSerialization(boolean validate) {
        this.serializationValidation = validate;
        return this;
    }

    public RecipeSpec validateRecipe(boolean validate) {
        this.recipeValidation = validate;
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

    int getCycles() {
        return cycles == null ? 2 : cycles;
    }

    int getExpectedCyclesThatMakeChanges(int cycles) {
        return expectedCyclesThatMakeChanges == null ? cycles - 1 :
                expectedCyclesThatMakeChanges;
    }

    public RecipeSpec typeValidationOptions(TypeValidation typeValidation) {
        this.typeValidation = typeValidation;
        return this;
    }

    public RecipeSpec afterTypeValidationOptions(TypeValidation typeValidation) {
        this.afterTypeValidation = typeValidation;
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
