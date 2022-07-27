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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.intellij.lang.annotations.Language;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.internal.lang.NullUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.scheduling.ForkJoinScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyList;

/**
 * Provides a formalized link list data structure of {@link Recipe recipes} and a {@link Recipe#run(List)} method which will
 * apply each recipes {@link TreeVisitor visitor} visit method to a list of {@link SourceFile sourceFiles}
 * <p>
 * Requires a name, {@link TreeVisitor visitor}.
 * Optionally a subsequent Recipe can be linked via {@link #doNext(Recipe)}}
 * <p>
 * An {@link ExecutionContext} controls parallel execution and lifecycle while providing a message bus
 * for sharing state between recipes and their visitors
 * <p>
 * returns a list of {@link Result results} for each modified {@link SourceFile}
 */
@PolyglotExport(typeScript = "Recipe", llvm = "Recipe")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
public abstract class Recipe {
    public static final String PANIC = "__AHHH_PANIC!!!__";

    private static final Logger logger = LoggerFactory.getLogger(Recipe.class);

    @SuppressWarnings("unused")
    @JsonProperty("@c")
    public String getJacksonPolymorphicTypeTag() {
        return getClass().getName();
    }

    public static final TreeVisitor<?, ExecutionContext> NOOP = new TreeVisitor<Tree, ExecutionContext>() {
        @Override
        public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            return tree;
        }
    };

    @Getter
    private final transient /*~~>*/List<TreeVisitor<?, ExecutionContext>> singleSourceApplicableTests = new ArrayList<>();

    @Getter
    private final transient /*~~>*/List<TreeVisitor<?, ExecutionContext>> applicableTests = new ArrayList<>();

    public static Recipe noop() {
        return new Noop();
    }

    static class Noop extends Recipe {
        @Override
        public String getDisplayName() {
            return "Do nothing";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return NOOP;
        }
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
    public String getDescription() {
        return "";
    }

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
        return null;
    }

    public final RecipeDescriptor getDescriptor() {
        return RecipeIntrospectionUtils.recipeDescriptorFromRecipe(this);
    }

    /**
     * @return Describes the language type(s) that this recipe applies to, e.g. java, xml, properties.
     * @deprecated
     */
    @Deprecated
    public /*~~>*/List<String> getLanguages() {
        return emptyList();
    }

    /**
     * @return Determines if another cycle is ran when this recipe makes a change. In some cases, like changing method declaration names,
     * a further cycle is needed to update method invocations of that declaration that were visited prior to the declaration change. But other
     * visitors never need to cause another cycle, such as those that format whitespace or add search markers. Note that even when this is false,
     * the recipe will still run on another cycle if any other recipe causes another cycle to run. But if every recipe reports no need to run
     * another cycle (or if there are no changes made in a cycle), then another will not run.
     */
    @Incubating(since = "7.3.0")
    public boolean causesAnotherCycle() {
        return recipeList.stream().anyMatch(Recipe::causesAnotherCycle);
    }

    @JsonIgnore
    private final /*~~>*/List<Recipe> recipeList = new CopyOnWriteArrayList<>();

    /**
     * @param recipe {@link Recipe} to add to this recipe's pipeline.
     * @return This recipe.
     */
    public Recipe doNext(Recipe recipe) {
        recipeList.add(recipe);
        return this;
    }

    public /*~~>*/List<Recipe> getRecipeList() {
        return recipeList;
    }

    /**
     * A recipe can optionally encasulate a visitor that performs operations on a set of source files. Subclasses
     * of the recipe may override this method to provide an instance of a visitor that will be used when the recipe
     * is executed.
     *
     * @return A tree visitor that will perform operations associated with the recipe.
     */
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return NOOP;
    }

    /**
     * A recipe can optionally include an applicability test that can be used to determine whether it should run on a
     * set of source files (or even be listed in a suggested list of recipes for a particular codebase).
     * <p>
     * To identify a tree as applicable, the visitor should mark or otherwise any tree at any level. Any mutation
     * that the applicability test visitor makes to a tree will not included in the results.
     *
     * @return A tree visitor that performs an applicability test.
     */
    @Nullable
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return null;
    }

    /**
     * A recipe can be configured with any number of applicable tests that can be used to determine whether it should run on a
     * particular source file.
     * <p>
     * To identify a {@link SourceFile} as applicable, the visitor should mark or change it at any level. Any mutation
     * that the applicability test visitor makes on the tree will not included in the results.
     * <p>
     * The applicability test only affects whether this recipes {@link #getVisitor()} will run. Downstream
     * {@link #doNext(Recipe)} will still run.
     *
     * @return A tree visitor that performs an applicability test.
     */
    @SuppressWarnings("unused")
    public Recipe addApplicableTest(TreeVisitor<?, ExecutionContext> test) {
        /*~~>*/this.applicableTests.add(test);
        return this;
    }

    /**
     * A recipe can optionally include an applicability test that can be used to determine whether it should run on a
     * particular source file.
     * <p>
     * To identify a {@link SourceFile} as applicable, the visitor should mark it at any level. Any mutation
     * that the applicability test visitor makes on the tree will not included in the results.
     * <p>
     * The applicability test only affects whether this recipes {@link #getVisitor()} will run. Downstream
     * {@link #doNext(Recipe)} will still run.
     *
     * @return A tree visitor that performs an applicability test.
     */
    @Nullable
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return null;
    }

    /**
     * A recipe can be configured with any number of applicable tests that can be used to determine whether it should run on a
     * particular source file.
     * <p>
     * To identify a {@link SourceFile} as applicable, the visitor should mark or change it at any level. Any mutation
     * that the applicability test visitor makes on the tree will not included in the results.
     * <p>
     * The applicability test only affects whether this recipes {@link #getVisitor()} will run. Downstream
     * {@link #doNext(Recipe)} will still run.
     *
     * @return A tree visitor that performs an applicability test.
     */
    public Recipe addSingleSourceApplicableTest(TreeVisitor<?, ExecutionContext> test) {
        /*~~>*/this.singleSourceApplicableTests.add(test);
        return this;
    }

    /**
     * Override this to generate new source files or delete source files.
     *
     * @param before The set of source files to operate on.
     * @param ctx    The current execution context.
     * @return A set of source files, with some files potentially added/deleted/modified.
     */
    @SuppressWarnings("unused")
    protected /*~~>*/List<SourceFile> visit(/*~~>*/List<SourceFile> before, ExecutionContext ctx) {
        return before;
    }

    public final /*~~>*/List<Result> run(/*~~>*/List<? extends SourceFile> before) {
        return run(before, new InMemoryExecutionContext());
    }

    public final /*~~>*/List<Result> run(/*~~>*/List<? extends SourceFile> before, ExecutionContext ctx) {
        return run(before, ctx, 3);
    }

    public final /*~~>*/List<Result> run(/*~~>*/List<? extends SourceFile> before, ExecutionContext ctx, int maxCycles) {
        return run(before, ctx, ForkJoinScheduler.common(), maxCycles, 1);
    }

    @Incubating(since = "7.3.0")
    public final /*~~>*/List<Result> run(/*~~>*/List<? extends SourceFile> before,
                                  ExecutionContext ctx,
                                  RecipeScheduler recipeScheduler,
                                  int maxCycles,
                                  int minCycles) {
        return recipeScheduler.scheduleRun(this, before, ctx, maxCycles, minCycles);
    }

    public Validated validate(ExecutionContext ctx) {
        Validated validated = validate();

        for(Recipe recipe : recipeList) {
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
    public Validated validate() {
        Validated validated = Validated.none();
        /*~~>*/List<Field> requiredFields = NullUtils.findNonNullFields(this.getClass());
        for (Field field : requiredFields) {
            try {
                validated = validated.and(Validated.required(field.getName(), field.get(this)));
            } catch (IllegalAccessException e) {
                logger.warn("Unable to validate the field [{}] on the class [{}]", field.getName(), this.getClass().getName());
            }
        }
        for(Recipe recipe : recipeList) {
            validated = validated.and(recipe.validate());
        }
        return validated;
    }

    @SuppressWarnings("unused")
    @Incubating(since = "7.0.0")
    public final Collection<Validated> validateAll(ExecutionContext ctx) {
        return validateAll(ctx, new ArrayList<>());
    }

    public final Collection<Validated> validateAll() {
        return validateAll(new InMemoryExecutionContext(), new ArrayList<>());
    }

    private Collection<Validated> validateAll(ExecutionContext ctx, Collection<Validated> acc) {
        acc.add(validate(ctx));
        for (Recipe recipe : recipeList) {
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

}
