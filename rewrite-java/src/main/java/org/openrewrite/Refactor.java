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
import org.openrewrite.tree.J;
import org.openrewrite.tree.Tree;
import org.openrewrite.visitor.refactor.RefactorVisitor;
import org.openrewrite.visitor.refactor.TransformVisitor;

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.stream.StreamSupport.stream;

@NonNullApi
public class Refactor {
    private final J.CompilationUnit original;

    private final List<RefactorVisitor> ops = new ArrayList<>();

    public Refactor(J.CompilationUnit original) {
        this.original = original;
    }

    public Refactor visit(Iterable<RefactorVisitor> visitors) {
        visitors.forEach(ops::add);
        return this;
    }

    public Refactor visit(RefactorVisitor... visitors) {
        return visit(asList(visitors));
    }

    /**
     * Shortcut for building refactoring operations for a collection of like-typed {@link Tree} elements.
     *
     * @param ts              The list of tree elements to operate on.
     * @param refactorForEach Build a refactoring operation built for each tree item in the collection.
     *                        The function should return null when there is some condition under which an
     *                        item in the list should not be transformed.
     * @param <T>             The type of tree element to operate on.
     * @return This instance, with a visitor for each tree element added.
     */
    public <T extends Tree> Refactor fold(Iterable<T> ts, Function<T, RefactorVisitor> refactorForEach) {
        return stream(ts.spliterator(), false)
                .map(refactorForEach)
                .filter(Objects::nonNull)
                .reduce(this, Refactor::visit, (r1, r2) -> r2);
    }

    public JavaRefactorResult fix() {
        return fix(10);
    }

    public JavaRefactorResult fix(int maxCycles) {
        J.CompilationUnit acc = original;
        Set<String> rulesThatMadeChanges = new HashSet<>();

        for (int i = 0; i < maxCycles; i++) {
            Set<String> rulesThatMadeChangesThisCycle = new HashSet<>();
            for (RefactorVisitor visitor : ops) {
                // only for use in debugging visitors
                visitor.setCycle(i);

                if (visitor.isSingleRun() && i > 0) {
                    continue;
                }

                var before = acc;
                acc = transformRecursive(acc, visitor);
                if (before != acc) {
                    // we only report on the top-level visitors, not any andThen() visitors that
                    // are applied as part of the top-level visitor's pipeline
                    rulesThatMadeChangesThisCycle.add(visitor.getRuleName());
                }
            }
            if (rulesThatMadeChangesThisCycle.isEmpty()) {
                break;
            }
            rulesThatMadeChanges.addAll(rulesThatMadeChangesThisCycle);
        }

        return new JavaRefactorResult(original, acc, rulesThatMadeChanges);
    }

    private J.CompilationUnit transformRecursive(J.CompilationUnit acc, RefactorVisitor visitor) {
        // by transforming the AST for each op, we allow for the possibility of overlapping changes
        acc = (J.CompilationUnit) new TransformVisitor(visitor.visit(acc)).visit(acc);
        for (RefactorVisitor vis : visitor.andThen()) {
            acc = transformRecursive(acc, vis);
        }
        return acc;
    }
}
