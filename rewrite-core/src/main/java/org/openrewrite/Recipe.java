/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Setter;
import org.intellij.lang.annotations.Language;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.RecipeExample;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NullUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.table.SourcesFileResults;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.RecipeIntrospectionUtils.dataTableDescriptorFromDataTable;

/**
 * Provides a formalized link list data structure of {@link Recipe recipes} and a {@link Recipe#run(LargeSourceSet, ExecutionContext)} method which will
 * apply each recipes {@link TreeVisitor visitor} visit method to a list of {@link SourceFile sourceFiles}
 * <p>
 * Requires a name, {@link TreeVisitor visitor}.
 * Optionally a subsequent Recipe can be linked via {@link #getRecipeList()}}
 * <p>
 * An {@link ExecutionContext} controls parallel execution and lifecycle while providing a message bus
 * for sharing state between recipes and their visitors
 * <p>
 * returns a list of {@link Result results} for each modified {@link SourceFile}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
@JsonPropertyOrder({"@c"}) // serialize type info first
public abstract class Recipe implements Cloneable {
    public static final String PANIC = "__AHHH_PANIC!!!__";

    @SuppressWarnings("unused")
    @JsonProperty("@c")
    public String getJacksonPolymorphicTypeTag() {
        return getClass().getName();
    }

    private transient RecipeDescriptor descriptor;

    @Nullable
    private transient List<DataTableDescriptor> dataTables;

    public static Recipe noop() {
        return new Noop();
    }

    static class Noop extends Recipe {
        @Override
        public String getDisplayName() {
            return "Do nothing";
        }

        @Override
        public String getDescription() {
            return "Default no-op test, does nothing.";
        }
    }

    /**
     * @return The maximum number of cycles this recipe is allowed to make changes in a recipe run.
     */
    @Incubating(since = "8.0.0")
    public int maxCycles() {
        return Integer.MAX_VALUE;
    }

    /**
     * A human-readable display name for the recipe, initial capped with no period.
     * For example, "Find text". The display name can be assumed to be rendered in
     * documentation and other places where markdown is understood, so it is possible
     * to use stylistic markers like backticks to indicate types. For example,
     * "Find uses of `java.util.List`".
     *
     * @return The display name.
     */
    @Language("markdown")
    public abstract String getDisplayName();

    /**
     * A human-readable display name for this recipe instance, including some descriptive
     * text about the recipe options that are supplied, if any. The name must be
     * initial capped with no period. For example, "Find text 'hello world'".
     * <br/>
     * For recipes with no options, by default this is equal to {@link #getDisplayName()}. For
     * recipes with options, the default implementation does its best to summarize those options
     * in a way that fits in a reasonable amount of characters.
     * <br/>
     * For consistency, when surrounding option descriptive text in quotes to visually differentiate
     * it from the text before it, use single ``.
     * <br/>
     * Override to provide meaningful recipe instance names for recipes with complex sets of options.
     *
     * @return A name that describes this recipe instance.
     */
    @Incubating(since = "8.12.0")
    @Language("markdown")
    public String getInstanceName() {
        @Language("markdown")
        String suffix = getInstanceNameSuffix();
        if (!StringUtils.isBlank(suffix)) {
            return getDisplayName() + " " + suffix;
        }

        List<OptionDescriptor> options = new ArrayList<>(getOptionDescriptors(getClass()));
        options.removeIf(opt -> !opt.isRequired());
        if (options.isEmpty()) {
            return getDisplayName();
        }
        if (options.size() == 1) {
            try {
                OptionDescriptor option = options.get(0);
                String name = option.getName();
                Field optionField = getClass().getDeclaredField(name);
                optionField.setAccessible(true);
                Object optionValue = optionField.get(this);
                if (optionValue != null &&
                    !Iterable.class.isAssignableFrom(optionValue.getClass()) &&
                    !optionValue.getClass().isArray()) {
                    return String.format("%s `%s`", getDisplayName(), optionValue);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignore) {
                // we tried...
            }
        }
        return getDisplayName();
    }

    /**
     * Since most instance names will be constructed with {@link #getDisplayName()} followed
     * by some further descriptive text about the recipe's options, this method provides a convenient
     * way to just specify the option descriptive text. When {@link #getInstanceName()} is overridden,
     * this method has no effect. Generally either override this method or {@link #getInstanceName()}
     * if you want to customize the instance name text.
     *
     * @return A suffix to append to the display name of a recipe.
     */
    public String getInstanceNameSuffix() {
        return "";
    }

    /**
     * A human-readable description for the recipe, consisting of one or more full
     * sentences ending with a period.
     * <p>
     * "Find methods by pattern." is an example. The description can be assumed to be rendered in
     * documentation and other places where markdown is understood, so it is possible
     * to use stylistic markers like backticks to indicate types. For example,
     * "Find uses of `java.util.List`.".
     *
     * @return The display name.
     */
    @Language("markdown")
    public abstract String getDescription();

    /**
     * A set of strings used for categorizing related recipes. For example
     * "testing", "junit", "spring". Any individual tag should consist of a
     * single word, all lowercase.
     *
     * @return The tags.
     */
    public Set<String> getTags() {
        return Collections.emptySet();
    }

    /**
     * @return An estimated effort were a developer to fix manually instead of using this recipe.
     */
    @Nullable
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    public final RecipeDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = createRecipeDescriptor();
        }
        return descriptor;
    }

    protected RecipeDescriptor createRecipeDescriptor() {
        List<OptionDescriptor> options = getOptionDescriptors(this.getClass());
        List<RecipeDescriptor> recipeList1 = new ArrayList<>();
        for (Recipe next : getRecipeList()) {
            recipeList1.add(next.getDescriptor());
        }
        URI recipeSource;
        try {
            recipeSource = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return new RecipeDescriptor(getName(), getDisplayName(), getDescription(), getTags(),
                getEstimatedEffortPerOccurrence(), options, recipeList1, getDataTableDescriptors(),
                getMaintainers(), getContributors(), getExamples(), recipeSource);
    }

    private List<OptionDescriptor> getOptionDescriptors(Class<?> recipeClass) {
        List<OptionDescriptor> options = new ArrayList<>();

        for (Field field : recipeClass.getDeclaredFields()) {
            Object value;
            try {
                field.setAccessible(true);
                value = field.get(this);
            } catch (IllegalAccessException e) {
                value = null;
            }
            Option option = field.getAnnotation(Option.class);
            if (option != null) {
                options.add(new OptionDescriptor(field.getName(),
                        field.getType().getSimpleName(),
                        option.displayName(),
                        option.description(),
                        option.example().isEmpty() ? null : option.example(),
                        option.valid().length == 1 && option.valid()[0].isEmpty() ? null : Arrays.asList(option.valid()),
                        option.required(),
                        value));
            }
        }
        return options;
    }

    private static final List<DataTableDescriptor> GLOBAL_DATA_TABLES = Arrays.asList(
            dataTableDescriptorFromDataTable(new SourcesFileResults(Recipe.noop())),
            dataTableDescriptorFromDataTable(new SourcesFileErrors(Recipe.noop())),
            dataTableDescriptorFromDataTable(new RecipeRunStats(Recipe.noop()))
    );

    public List<DataTableDescriptor> getDataTableDescriptors() {
        return ListUtils.concatAll(dataTables == null ? emptyList() : dataTables, GLOBAL_DATA_TABLES);
    }

    /**
     * @return a list of the organization(s) responsible for maintaining this recipe.
     */
    public List<Maintainer> getMaintainers() {
        return new ArrayList<>();
    }

    @Setter
    protected List<Contributor> contributors;

    public List<Contributor> getContributors() {
        if (contributors == null) {
            return new ArrayList<>();
        }
        return contributors;
    }

    @Setter
    protected transient List<RecipeExample> examples;

    public List<RecipeExample> getExamples() {
        if (examples == null) {
            return new ArrayList<>();
        }
        return examples;
    }

    /**
     * @return Determines if another cycle is run when this recipe makes a change. In some cases, like changing method declaration names,
     * a further cycle is needed to update method invocations of that declaration that were visited prior to the declaration change. But other
     * visitors never need to cause another cycle, such as those that format whitespace or add search markers. Note that even when this is false,
     * the recipe will still run on another cycle if any other recipe causes another cycle to run. But if every recipe reports no need to run
     * another cycle (or if there are no changes made in a cycle), then another will not run.
     */
    public boolean causesAnotherCycle() {
        return false;
    }

    /**
     * A list of recipes that run, source file by source file,
     * after this recipe. This method is guaranteed to be called only once
     * per cycle.
     *
     * @return The list of recipes to run.
     */
    public List<Recipe> getRecipeList() {
        return Collections.emptyList();
    }

    /**
     * A recipe can optionally encapsulate a visitor that performs operations on a set of source files. Subclasses
     * of the recipe may override this method to provide an instance of a visitor that will be used when the recipe
     * is executed.
     *
     * @return A tree visitor that will perform operations associated with the recipe.
     */
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return TreeVisitor.noop();
    }

    public void addDataTable(DataTable<?> dataTable) {
        if (dataTables == null) {
            dataTables = new ArrayList<>();
        }
        dataTables.add(dataTableDescriptorFromDataTable(dataTable));
    }

    public final RecipeRun run(LargeSourceSet before, ExecutionContext ctx) {
        return run(before, ctx, 3);
    }

    public final RecipeRun run(LargeSourceSet before, ExecutionContext ctx, int maxCycles) {
        return run(before, ctx, maxCycles, 1);
    }

    public final RecipeRun run(LargeSourceSet before, ExecutionContext ctx, int maxCycles, int minCycles) {
        return new RecipeScheduler().scheduleRun(this, before, ctx, maxCycles, minCycles);
    }

    @SuppressWarnings("unused")
    public Validated<Object> validate(ExecutionContext ctx) {
        Validated<Object> validated = validate();

        for (Recipe recipe : getRecipeList()) {
            validated = validated.and(recipe.validate(ctx));
        }
        return validated;
    }

    /**
     * The default implementation of validate on the recipe will look for package and field level annotations that
     * indicate a field is not-null. The annotations must have run-time retention and the simple name of the annotation
     * must match one of the common names defined in {@link NullUtils}
     *
     * @return A validated instance based using non-null/nullable annotations to determine which fields of the recipe are required.
     */
    public Validated<Object> validate() {
        Validated<Object> validated = Validated.none();
        Class<? extends Recipe> clazz = this.getClass();
        List<Field> requiredFields = NullUtils.findNonNullFields(clazz);
        for (Field field : requiredFields) {
            try {
                validated = validated.and(Validated.required(clazz.getSimpleName() + '.' + field.getName(), field.get(this)));
            } catch (IllegalAccessException e) {
                validated = Validated.invalid(field.getName(), null, "Unable to access " + clazz.getName() + "." + field.getName(), e);
            }
        }
        for (Recipe recipe : getRecipeList()) {
            validated = validated.and(recipe.validate());
        }
        return validated;
    }

    public final Collection<Validated<Object>> validateAll() {
        return validateAll(new InMemoryExecutionContext(), new ArrayList<>());
    }

    public Collection<Validated<Object>> validateAll(ExecutionContext ctx, Collection<Validated<Object>> acc) {
        acc.add(validate(ctx));
        for (Recipe recipe : getRecipeList()) {
            recipe.validateAll(ctx, acc);
        }
        return acc;
    }

    public String getName() {
        return getClass().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        return getName().equals(recipe.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
