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
import lombok.EqualsAndHashCode;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NullUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
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
public class Recipe {
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
            if (tree != null) {
                String markerIds = tree.getMarkers().entries().stream()
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

    @Nullable
    private Recipe next;

    /**
     * @param recipe {@link Recipe} to append to the doNext chain
     */
    public Recipe doNext(Recipe recipe) {
        Recipe tail = this;
        //noinspection StatementWithEmptyBody
        for (; tail.next != null; tail = tail.next) ;
        tail.next = recipe;
        return this;
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

    @SuppressWarnings("SuspiciousMethodCalls")
    private <S extends SourceFile> List<SourceFile> visitInternal(List<S> before,
                                                                  ExecutionContext ctx,
                                                                  ForkJoinPool forkJoinPool,
                                                                  Map<UUID, Recipe> recipeThatDeletedSourceFile) {
        List<S> after = before;
        // if this recipe isn't valid we just skip it and proceed to next
        if (validate(ctx).isValid()) {
            after = ListUtils.map(after, forkJoinPool, s -> {
                try {
                    @SuppressWarnings("unchecked") S afterFile = (S) getVisitor().visit(s, ctx);
                    if (afterFile != null && afterFile != s) {
                        afterFile = afterFile.withMarkers(afterFile.getMarkers().compute(
                                new RecipeThatMadeChanges(this),
                                (r1, r2) -> {
                                    r1.recipes.addAll(r2.recipes);
                                    return r1;
                                }));
                    }
                    if (afterFile == null) {
                        recipeThatDeletedSourceFile.put(s.getId(), this);
                    }
                    return afterFile;
                } catch (Throwable t) {
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

        if (next != null) {
            afterWidened = next.visitInternal(afterWidened, ctx, forkJoinPool, recipeThatDeletedSourceFile);
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
        Map<UUID, Recipe> recipeThatDeletedSourceFile = new HashMap<>();
        List<? extends SourceFile> acc = before;
        List<? extends SourceFile> after = acc;

        WatchForNewMessageExecutionContext ctxWithWatch = new WatchForNewMessageExecutionContext(ctx);
        for (int i = 0; i < maxCycles; i++) {
            after = visitInternal(before, ctx, forkJoinPool, recipeThatDeletedSourceFile);
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
                    //output to dermine if a change has been made.
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
        if (next != null) {
            next.validateAll(ctx, acc);
        }
        return acc;
    }

    public String getName() {
        return getClass().getName();
    }

    @EqualsAndHashCode
    private static class RecipeThatMadeChanges implements Marker {
        private final Set<Recipe> recipes;

        private RecipeThatMadeChanges(Recipe recipe) {
            this.recipes = new HashSet<>();
            this.recipes.add(recipe);
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
