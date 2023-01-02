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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Getter;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.quark.QuarkParser;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("UnusedReturnValue")
@Getter
public class RecipeSpec {
    public static RecipeSpec defaults() {
        return new RecipeSpec();
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

    boolean serializationValidation = true;

    @Nullable
    PrintOutputCapture.MarkerPrinter markerPrinter;

    List<UncheckedConsumer<List<SourceFile>>> beforeRecipes = new ArrayList<>();

    List<UncheckedConsumer<RecipeRun>> afterRecipes = new ArrayList<>();

    // The before and after here don't mean anything
    SourceSpec<SourceFile> allSources = new SourceSpec<>(SourceFile.class, null, QuarkParser.builder(), "", null);

    /**
     * Configuration that applies to all source file inputs.
     */
    public SourceSpec<SourceFile> allSources() {
        return allSources;
    }

    public RecipeSpec recipe(Recipe recipe) {
        this.recipe = recipe;
        return this;
    }

    public RecipeSpec recipe(InputStream yaml, String... recipes) {
        return recipe(Environment.builder()
                .load(new YamlResourceLoader(yaml, URI.create("rewrite.yml"), new Properties()))
                .build()
                .activateRecipes(recipes));
    }

    public RecipeSpec recipe(String yamlResource, String... recipes) {
        return recipe(Objects.requireNonNull(RecipeSpec.class.getResourceAsStream(yamlResource)), recipes);
    }

    /**
     * @param parser The parser supplier to use when a matching source file is found.
     * @return The current recipe spec.
     */
    public RecipeSpec parser(Parser.Builder parser) {
        this.parsers.add(parser);
        return this;
    }

    public RecipeSpec executionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        return this;
    }

    public RecipeSpec recipeExecutionContext(ExecutionContext executionContext) {
        this.recipeExecutionContext = executionContext;
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

    public <E> RecipeSpec dataTable(DataTable<E> dataTable, UncheckedConsumer<List<E>> extract) {
        return afterRecipe(run -> {
            List<E> rows = run.getDataTable(dataTable);
            assertFalse(rows.isEmpty());
            extract.accept(rows);
        });
    }

    @Incubating(since = "7.35.0")
    public <E, V> RecipeSpec dataTable(DataTable<E> dataTable, Function<E, V> map, UncheckedConsumer<List<V>> extract) {
        return dataTable(dataTable, ex -> extract.accept(ex.stream().map(map).collect(Collectors.toList())));
    }

    @Incubating(since = "7.35.0")
    public <E, V> RecipeSpec dataTableAsCsv(DataTable<E> dataTable, String expect) {
        return dataTable(dataTable, ex -> {
            if (ex.isEmpty()) {
                assertThat(expect).isEmpty();
            } else {
                StringWriter writer = new StringWriter();
                CsvMapper mapper = new CsvMapper();
                Class<?> rowType = ex.iterator().next().getClass();
                CsvSchema schema = mapper.schemaFor(rowType).withHeader();
                mapper.writerFor(rowType).with(schema).writeValues(writer).writeAll(ex);
                assertThat(writer.toString()).isEqualTo(expect);
            }
        });
    }

    @Incubating(since = "7.35.0")
    public RecipeSpec validateRecipeSerialization(boolean validate) {
        this.serializationValidation = validate;
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

    @Nullable
    ExecutionContext getExecutionContext() {
        return executionContext;
    }
}
