/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.openrewrite.internal.lang.NonNullApi;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.StreamSupport.stream;

/**
 * A refactoring operation on a single source file.
 *
 * @param <S>
 * @param <T>
 */
@NonNullApi
public class Refactor<S extends SourceFile, T extends Tree> {
    private final S original;

    private final List<SourceVisitor<T>> ops = new ArrayList<>();

    public Refactor(S original) {
        this.original = original;
    }

    @SafeVarargs
    public final Refactor<S, T> visit(SourceVisitor<T>... visitors) {
        Collections.addAll(ops, visitors);
        return this;
    }

    /**
     * Shortcut for building refactoring operations for a collection of like-typed {@link Tree} elements.
     *
     * @param ts              The list of tree elements to operate on.
     * @param refactorForEach Build a refactoring operation built for each tree item in the collection.
     *                        The function should return null when there is some condition under which an
     *                        item in the list should not be transformed.
     * @param <T2>             The type of tree element to operate on.
     * @return This instance, with a visitor for each tree element added.
     */
    @SuppressWarnings("unchecked")
    public final <T2 extends T> Refactor<S, T> fold(Iterable<T2> ts, Function<T2, SourceVisitor<T>> refactorForEach) {
        return stream(ts.spliterator(), false)
                .map(refactorForEach)
                .filter(Objects::nonNull)
                .reduce(this, Refactor::visit, (r1, r2) -> r2);
    }

    public Change<S> fix() {
        return fix(10);
    }

    public Change<S> fix(int maxCycles) {
        S acc = original;
        Set<String> rulesThatMadeChanges = new HashSet<>();

        for (int i = 0; i < maxCycles; i++) {
            Set<String> rulesThatMadeChangesThisCycle = new HashSet<>();
            for (SourceVisitor<T> visitor : ops) {
                visitor.nextCycle();

                if (!visitor.isIdempotent() && i > 0) {
                    continue;
                }

                S before = acc;
                acc = transformPipeline(acc, visitor);

                if (before != acc) {
                    // we only report on the top-level visitors, not any andThen() visitors that
                    // are applied as part of the top-level visitor's pipeline
                    if (visitor.getName() != null) {
                        rulesThatMadeChangesThisCycle.add(visitor.getName());
                    }
                }
            }
            if (rulesThatMadeChangesThisCycle.isEmpty()) {
                break;
            }
            rulesThatMadeChanges.addAll(rulesThatMadeChangesThisCycle);
        }

        return new Change<>(original, acc, rulesThatMadeChanges);
    }

    @SuppressWarnings("unchecked")
    private S transformPipeline(S acc, SourceVisitor<T> visitor) {
        // by transforming the AST for each op, we allow for the possibility of overlapping changes
        acc = (S) visitor.visit(acc);
        for (SourceVisitor<T> vis : visitor.andThen()) {
            acc = transformPipeline(acc, vis);
        }
        return acc;
    }
}
