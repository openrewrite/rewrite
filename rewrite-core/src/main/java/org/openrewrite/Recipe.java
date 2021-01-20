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

import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * Provides a formalized link list data structure of {@link Recipe recipes} and a {@link #run(List<SourceFile>)} method will apply each recipes
 * {@link TreeProcessor processor} visit method to a list of {@link SourceFile sourceFiles}
 *
 * Requires a name, {@link TreeProcessor processor}. Optionally a subsequent Recipe can be linked via {@link #doNext(Recipe)}}
 *
 * An {@link ExecutionContext} controls parallel execution and lifecycle while providing a message bus
 * for sharing state between recipes and their processors
 *
 * returns a list of {@link Result results} for each modified element identifying each recipe which has modified the {@link SourceFile}
 *
 * characteristics
 *  - visit list of SourceFile each visit
 *  - a recipe which generates new source file overrides visit and adds something to the list
 *  - a linked list data-structure contains next(recipe) optionaly
 *  - can be a single unit of work or could be junit 4-5 migration
 *  - iterate over all SourceFiles visit each by the processor
 */
public class Recipe {
    public static final TreeProcessor<?, ExecutionContext> NOOP = new TreeProcessor<Tree, ExecutionContext>() {
        @Override
        public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            return tree;
        }
    };

    @Nullable
    private final String name;

    @Nullable
    private Recipe next;

    protected Supplier<TreeProcessor<?, ExecutionContext>> processor = () -> NOOP;

    public Recipe(@Nullable String name) {
        this.name = name;
    }

    public Recipe() {
        this(null);
    }

    /**
     * Append the provided {@link Recipe} to the last recipe in the doNext chain
     * @param recipe
     * @return
     */
    public Recipe doNext(Recipe recipe) {
        Recipe tail = this;
        //noinspection StatementWithEmptyBody
        for (; tail.next != null; tail = tail.next) ;
        tail.next = recipe;
        return this;
    }

    Supplier<TreeProcessor<?, ExecutionContext>> getProcessor() {
        return processor;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private <S extends SourceFile> List<SourceFile> visitInternal(List<S> before, ExecutionContext ctx) {
        List<S> after = before;
        // if this recipe isn't valid we just skip it and proceed to next
        if (validate(ctx).isValid()) {
            after = ListUtils.map(after, ctx.getForkJoinPool(), s -> {
                try {
                    @SuppressWarnings("unchecked") S afterFile = (S) processor.get().visit(s, ctx);
                    if (afterFile != null && afterFile != s) {
                        afterFile = afterFile.withMarkers(afterFile.getMarkers().compute(
                                new RecipeThatMadeChanges(getName()),
                                (r1, r2) -> {
                                    r1.names.addAll(r2.names);
                                    return r1;
                                }));
                    }
                    if (afterFile == null) {
                        ctx.recordSourceFileModification(s, this);
                    }
                    return afterFile;
                } catch (Throwable t) {
                    if (ctx.getOnError() != null) {
                        ctx.getOnError().accept(t);
                    }
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
                ctx.recordSourceFileModification(maybeGenerated, this);
            }
        }
        for (SourceFile maybeDeleted : after) {
            if (!afterWidened.contains(maybeDeleted)) {
                // a source file deleted
                ctx.recordSourceFileModification(maybeDeleted, this);
            }
        }

        if (next != null) {
            afterWidened = next.visitInternal(afterWidened, ctx);
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
        return run(before, ExecutionContext.builder().build());
    }

    public final List<Result> run(List<? extends SourceFile> before, ExecutionContext ctx) {
        List<? extends SourceFile> acc = before;
        List<? extends SourceFile> after = acc;
        for (int i = 0; i < ctx.getMaxCycles(); i++) {
            after = visitInternal(before, ctx);
            if (after == acc && !ctx.isNeedAnotherCycle()) {
                break;
            }
            acc = after;
            ctx.nextCycle();
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
                    results.add(new Result(null, s,
                            singleton(ctx.getRecipeThatModifiedSourceFile(s.getId()))));
                } else {
                    results.add(new Result(original, s, s.getMarkers()
                            .findFirst(RecipeThatMadeChanges.class)
                            .orElseThrow(() -> new IllegalStateException("SourceFile changed but no recipe reported making a change?"))
                            .names));
                }
            }
        }

        Set<UUID> afterIds = after.stream()
                .map(SourceFile::getId)
                .collect(toSet());

        // removed files
        for (SourceFile s : before) {
            if (!afterIds.contains(s.getId())) {
                results.add(new Result(s, null,
                        singleton(ctx.getRecipeThatModifiedSourceFile(s.getId()))));
            }
        }

        return results;
    }

    @SuppressWarnings("unused")
    @Incubating(since = "7.0.0")
    public Validated validate(ExecutionContext ctx) {
        return validate();
    }

    public Validated validate() {
        return Validated.none();
    }

    @Incubating(since = "7.0.0")
    public final Collection<Validated> validateAll(ExecutionContext ctx) {
        return validateAll(ctx, new ArrayList<>());
    }

    public final Collection<Validated> validateAll() {
        return validateAll(ExecutionContext.builder().build(), new ArrayList<>());
    }

    private Collection<Validated> validateAll(ExecutionContext ctx, Collection<Validated> acc) {
        acc.add(validate(ctx));
        if (next != null) {
            next.validateAll(ctx, acc);
        }
        return acc;
    }

    public String getName() {
        return name == null ? getClass().getName() : name;
    }

    private static class RecipeThatMadeChanges implements Marker {
        private final Set<String> names;

        private RecipeThatMadeChanges(String name) {
            this.names = new HashSet<>();
            this.names.add(name);
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
}
