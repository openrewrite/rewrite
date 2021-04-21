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
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.EqualsAndHashCode;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.internal.lang.NullUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.*;
import static org.openrewrite.Tree.randomId;

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
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
public abstract class Recipe {

    private static final Logger logger = LoggerFactory.getLogger(Recipe.class);

    @SuppressWarnings("unused")
    @JsonProperty("@c")
    public String getJacksonPolymorphicTypeTag() {
        return getClass().getName();
    }

    /**
     * This tree printer is used when comparing before/after source files and reifies any markers as a list of
     * hash codes.
     */
    private static final TreePrinter<ExecutionContext> MARKER_ID_PRINTER = new TreePrinter<ExecutionContext>() {
        @Override
        public void doBefore(@Nullable Tree tree, StringBuilder printerAcc, ExecutionContext executionContext) {
            if (tree instanceof Markers) {
                String markerIds = ((Markers)tree).entries().stream()
                        .filter(marker -> !(marker instanceof RecipeThatMadeChanges))
                        .map(marker -> String.valueOf(marker.hashCode()))
                        .collect(joining(","));
                if (!markerIds.isEmpty()) {
                    printerAcc
                            .append("markers[")
                            .append(markerIds)
                            .append("]->");
                }
            }
        }
    };

    public static final TreeVisitor<?, ExecutionContext> NOOP = new TreeVisitor<Tree, ExecutionContext>() {
        @Override
        public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            return tree;
        }
    };

    /**
     * A human-readable display name for the recipe, initial capped with no period.
     * For example, "Find text". The display name can be assumed to be rendered in
     * documentation and other places where markdown is understood, so it is possible
     * to use stylistic markers like backticks to indicate types. For example,
     * "Find uses of `java.util.List`".
     *
     * @return The display name.
     */
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
    public String getDescription() {
        return "";
    }

    /**
     * A set of strings used for categorizing related recipes. For example
     * "testing", "junit", "spring". Tags should not include information about
     * the language(s) this recipe is applicable to. See {@link #getLanguages()} instead.
     * Any individual tag should consist of a single word, all lowercase.
     *
     * @return The tags.
     */
    public Set<String> getTags() {
        return Collections.emptySet();
    }

    public final RecipeDescriptor getDescriptor() {
        return RecipeIntrospectionUtils.recipeDescriptorFromRecipe(this);
    }

    /**
     * @return Describes the language type(s) that this recipe applies to, e.g. java, xml, properties.
     */
    public List<String> getLanguages() {
        return emptyList();
//        return Stream.concat(Stream.of(getVisitor().getLanguage()), getRecipeList().stream().flatMap(r -> r.getLanguages().stream()))
//                .filter(Objects::nonNull)
//                .collect(toList());
    }

    @JsonIgnore
    private final List<Recipe> recipeList = new ArrayList<>();

    /**
     * @param recipe {@link Recipe} to add to this recipe's pipeline.
     * @return This recipe.
     */
    public Recipe doNext(Recipe recipe) {
        recipeList.add(recipe);
        return this;
    }

    public List<Recipe> getRecipeList() {
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
     *
     * To identify a tree as applicable, the visitor should mark or otherwise alter the tree at any level. The mutation
     * that the applicability test visitor makes to the tree will not included in the results.
     *
     * @return A tree visitor that performs an applicability test.
     */
    @Incubating(since = "7.2.0")
    @Nullable
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return null;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private <S extends SourceFile> List<SourceFile> visitInternal(List<S> before,
                                                                  ExecutionContext ctx,
                                                                  ForkJoinPool forkJoinPool,
                                                                  Map<UUID, Recipe> recipeThatDeletedSourceFile) {
        if(getApplicableTest() != null) {
            boolean applicable = false;
            for (S s : before) {
                if (getApplicableTest().visit(s, ctx) != s) {
                    applicable = true;
                    break;
                }
            }

            if (!applicable) {
                //noinspection unchecked
                return (List<SourceFile>) before;
            }
        }

        List<S> after = before;
        // if this recipe isn't valid we just skip it and proceed to next
        if (validate(ctx).isValid()) {
            after = ListUtils.map(after, forkJoinPool, s -> {
                Timer.Builder timer = Timer.builder("rewrite.recipe.visit").tag("recipe", getDisplayName());
                Timer.Sample sample = Timer.start();
                try {
                    @SuppressWarnings("unchecked") S afterFile = (S) getVisitor().visit(s, ctx);
                    if (afterFile != null && afterFile != s) {
                        afterFile = afterFile.withMarkers(afterFile.getMarkers().compute(
                                new RecipeThatMadeChanges(this),
                                (r1, r2) -> {
                                    r1.recipes.addAll(r2.recipes);
                                    return r1;
                                }));
                        sample.stop(MetricsHelper.successTags(timer, s, "changed").register(Metrics.globalRegistry));
                    } else if (afterFile == null) {
                        recipeThatDeletedSourceFile.put(s.getId(), this);
                        sample.stop(MetricsHelper.successTags(timer, s, "deleted").register(Metrics.globalRegistry));
                    } else {
                        sample.stop(MetricsHelper.successTags(timer, s, "unchanged").register(Metrics.globalRegistry));
                    }
                    return afterFile;
                } catch (Throwable t) {
                    sample.stop(MetricsHelper.errorTags(timer, s, t).register(Metrics.globalRegistry));
                    ctx.getOnError().accept(t);
                    return s;
                }
            });
        }

        // The type of the list is widened at this point, since a source file type may be generated that isn't
        // of a type that is in the original set of source files (e.g. only XML files are given, and the
        // recipe generates Java code).

        //noinspection unchecked
        List<SourceFile> afterWidened = visit((List<SourceFile>) after, ctx);

        for (SourceFile maybeGenerated : afterWidened) {
            if (!after.contains(maybeGenerated)) {
                // a new source file generated
                recipeThatDeletedSourceFile.put(maybeGenerated.getId(), this);
            }
        }

        for (SourceFile maybeDeleted : after) {
            if (!afterWidened.contains(maybeDeleted)) {
                // a source file deleted
                recipeThatDeletedSourceFile.put(maybeDeleted.getId(), this);
            }
        }

        for (Recipe recipe : recipeList) {
            afterWidened = recipe.visitInternal(afterWidened, ctx, forkJoinPool, recipeThatDeletedSourceFile);
        }

        return afterWidened;
    }

    /**
     * Override this to generate new source files or delete source files.
     *
     * @param before The set of source files to operate on.
     * @param ctx    The current execution context.
     * @return A set of source files, with some files potentially added/deleted/modified.
     */
    @SuppressWarnings("unused")
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        return before;
    }

    public final List<Result> run(List<? extends SourceFile> before) {
        return run(before, new InMemoryExecutionContext());
    }

    public final List<Result> run(List<? extends SourceFile> before, ExecutionContext ctx) {
        return run(before, ctx, 3);
    }

    public final List<Result> run(List<? extends SourceFile> before, ExecutionContext ctx, int maxCycles) {
        return run(before, ctx, ForkJoinPool.commonPool(), maxCycles);
    }

    public final List<Result> run(List<? extends SourceFile> before,
                                  ExecutionContext ctx,
                                  ForkJoinPool forkJoinPool,
                                  int maxCycles) {
        DistributionSummary.builder("rewrite.recipe.run")
                .tag("recipe", getDisplayName())
                .description("The distribution of recipe runs and the size of source file batches given to them to process.")
                .baseUnit("source files")
                .register(Metrics.globalRegistry)
                .record(before.size());

        Map<UUID, Recipe> recipeThatDeletedSourceFile = new HashMap<>();
        List<? extends SourceFile> acc = before;
        List<? extends SourceFile> after = acc;

        WatchForNewMessageExecutionContext ctxWithWatch = new WatchForNewMessageExecutionContext(ctx);
        for (int i = 0; i < maxCycles; i++) {
            after = visitInternal(acc, ctxWithWatch, forkJoinPool, recipeThatDeletedSourceFile);
            if (after == acc && !ctxWithWatch.needAnotherCycle) {
                break;
            }
            acc = after;
            ctxWithWatch.needAnotherCycle = false;
        }

        if (after == before) {
            return emptyList();
        }

        Map<UUID, SourceFile> sourceFileIdentities = before.stream()
                .collect(toMap(SourceFile::getId, Function.identity()));

        List<Result> results = new ArrayList<>();

        // added or changed files
        for (SourceFile s : after) {
            SourceFile original = sourceFileIdentities.get(s.getId());
            if (original != s) {
                if (original == null) {
                    results.add(new Result(null, s, singleton(recipeThatDeletedSourceFile.get(s.getId()))));
                } else {
                    //printing both the before and after (and including markers in the output) and then comparing the
                    //output to determine if a change has been made.
                    if (!original.print(MARKER_ID_PRINTER, ctx).equals(s.print(MARKER_ID_PRINTER, ctx))) {
                        results.add(new Result(original, s, s.getMarkers()
                                .findFirst(RecipeThatMadeChanges.class)
                                .orElseThrow(() -> new IllegalStateException("SourceFile changed but no recipe reported making a change?"))
                                .recipes));
                    }
                }
            }
        }

        Set<UUID> afterIds = after.stream()
                .map(SourceFile::getId)
                .collect(toSet());

        // removed files
        for (SourceFile s : before) {
            if (!afterIds.contains(s.getId())) {
                results.add(new Result(s, null, singleton(recipeThatDeletedSourceFile.get(s.getId()))));
            }
        }

        return results;
    }

    @SuppressWarnings("unused")
    @Incubating(since = "7.0.0")
    public Validated validate(ExecutionContext ctx) {
        return validate();
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
        List<Field> requiredFields = NullUtils.findNonNullFields(this.getClass());
        for (Field field : requiredFields) {
            try {
                validated = validated.and(Validated.required(field.getName(), field.get(this)));
            } catch (IllegalAccessException e) {
                logger.warn("Unable to validate the field [{}] on the class [{}]", field.getName(), this.getClass().getName());
            }
        }
        return validated;
    }

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

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class RecipeThatMadeChanges implements Marker {
        private final Set<Recipe> recipes;

        @EqualsAndHashCode.Include
        private final UUID id;

        private RecipeThatMadeChanges(Recipe recipe) {
            this.recipes = new HashSet<>();
            this.recipes.add(recipe);
            id = randomId();
        }

        @Override
        public UUID getId() {
            return id;
        }
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

    private static class WatchForNewMessageExecutionContext implements ExecutionContext {
        private boolean needAnotherCycle = true;
        private final ExecutionContext delegate;

        private WatchForNewMessageExecutionContext(ExecutionContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public void putMessage(String key, Object value) {
            needAnotherCycle = true;
            delegate.putMessage(key, value);
        }

        @Override
        public <T> @Nullable T getMessage(String key) {
            return delegate.getMessage(key);
        }

        @Override
        public <T> @Nullable T pollMessage(String key) {
            return delegate.pollMessage(key);
        }

        @Override
        public Consumer<Throwable> getOnError() {
            return delegate.getOnError();
        }
    }
}
