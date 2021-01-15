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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public abstract class Recipe {
    public static final TreeProcessor<?, ExecutionContext> NOOP = new TreeProcessor<Tree, ExecutionContext>() {
        @Override
        public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            return tree;
        }
    };

    @Nullable
    private Recipe next;

    protected Supplier<TreeProcessor<?, ExecutionContext>> processor = () -> NOOP;

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

    private List<SourceFile> visit(List<SourceFile> before, ExecutionContext execution) {
        List<SourceFile> after = before;
        // if this recipe isn't valid we just skip it and proceed to next
        if (validate().isValid()) {
            after = ListUtils.map(after, execution.getForkJoinPool(), s -> {
                try {
                    SourceFile afterFile = (SourceFile) processor.get().visit(s, execution);
                    if (afterFile != null && afterFile != s) {
                        afterFile = afterFile.withMarkers(afterFile.getMarkers().compute(
                                new RecipeThatMadeChanges(getName()),
                                (r1, r2) -> {
                                    r1.names.addAll(r2.names);
                                    return r1;
                                }));
                    }
                    return afterFile;
                } catch (Throwable t) {
                    if (execution.getOnError() != null) {
                        execution.getOnError().accept(t);
                    }
                    return s;
                }
            });
        }
        if (next != null) {
            after = next.visit(after, execution);
        }
        return after;
    }

    public final List<Result> run(List<SourceFile> before) {
        return run(before, ExecutionContext.builder().build());
    }

    public final List<Result> run(List<SourceFile> before, ExecutionContext context) {
        List<SourceFile> acc = before;
        List<SourceFile> after = acc;
        for (int i = 0; i < context.getMaxCycles(); i++) {
            after = visit(before, context);
            if (after == acc && !context.isNeedAnotherCycle()) {
                break;
            }
            acc = after;
            context.nextCycle();
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
                results.add(new Result(original, s, s.getMarkers()
                        .findFirst(RecipeThatMadeChanges.class)
                        .orElseThrow(() -> new IllegalStateException("SourceFile changed but no recipe reported making a change?"))
                        .names));
            }
        }

        Set<UUID> afterIds = after.stream()
                .map(SourceFile::getId)
                .collect(toSet());

        // removed files
        for (SourceFile s : before) {
            if (!afterIds.contains(s.getId())) {
                // FIXME fix how we track which recipes are deleting files
                results.add(new Result(s, null, emptySet()));
            }
        }

        return results;
    }

    public Validated validate() {
        return Validated.none();
    }

    public String getName() {
        return getClass().getName();
    }

    private static class RecipeThatMadeChanges implements Marker {
        private final Set<String> names;

        private RecipeThatMadeChanges(String name) {
            this.names = new HashSet<>();
            this.names.add(name);
        }
    }
}
