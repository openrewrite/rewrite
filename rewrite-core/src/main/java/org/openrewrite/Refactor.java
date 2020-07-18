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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.openrewrite.internal.lang.NonNullApi;

import java.util.*;

/**
 * A refactoring operation on a single source file involving one or more top-level refactoring visitors.
 *
 * @param <T> The common interface to all AST elements for a particular language.
 */
@NonNullApi
public class Refactor<T extends Tree> {
    @Getter
    private final T original;

    private MeterRegistry meterRegistry = Metrics.globalRegistry;

    @Getter
    private final List<SourceVisitor<? extends Tree>> visitors = new ArrayList<>();

    public Refactor(T original) {
        this.original = original;
    }

    @SafeVarargs
    public final Refactor<T> visit(SourceVisitor<? extends Tree>... visitors) {
        Collections.addAll(this.visitors, visitors);
        return this;
    }

    public final Refactor<T> visit(Iterable<SourceVisitor<? extends Tree>> visitors) {
        visitors.forEach(this.visitors::add);
        return this;
    }

    public Change<T> fix() {
        return fix(10);
    }

    public Change<T> fix(int maxCycles) {
        Timer.Sample sample = Timer.start();

        T acc = original;
        Set<String> rulesThatMadeChanges = new HashSet<>();

        for (int i = 0; i < maxCycles; i++) {
            Set<String> rulesThatMadeChangesThisCycle = new HashSet<>();
            for (SourceVisitor<? extends Tree> visitor : visitors) {
                visitor.nextCycle();

                if (!visitor.isIdempotent() && i > 0) {
                    continue;
                }

                T before = acc;
                acc = transformPipeline(acc, visitor);

                if (before != acc) {
                    // we should only report on the top-level visitors, not any andThen() visitors that
                    // are applied as part of the top-level visitor's pipeline
                    rulesThatMadeChangesThisCycle.add(visitor.getClass().getName());
                }
            }
            if (rulesThatMadeChangesThisCycle.isEmpty()) {
                break;
            }
            rulesThatMadeChanges.addAll(rulesThatMadeChangesThisCycle);
        }

        sample.stop(Timer.builder("rewrite.refactor.plan")
                .description("The time it takes to execute a refactoring plan consisting of potentially more than one visitor over more than one cycle")
                .tag("tree.type", original.getClass().getSimpleName())
                .tag("outcome", rulesThatMadeChanges.isEmpty() ? "Unchanged" : "Changed")
                .register(meterRegistry));

        for (String ruleThatMadeChange : rulesThatMadeChanges) {
            Counter.builder("rewrite.refactor.plan.changes")
                    .description("The number of changes requested by a visitor.")
                    .tag("visitor", ruleThatMadeChange)
                    .tag("tree.type", original.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();
        }

        return new Change<>(original, acc, rulesThatMadeChanges);
    }

    @SuppressWarnings("unchecked")
    private T transformPipeline(T acc, SourceVisitor<? extends Tree> visitor) {
        // by transforming the AST for each op, we allow for the possibility of overlapping changes
        Timer.Sample sample = Timer.start();
        acc = (T) visitor.visit(acc);
        for (SourceVisitor<? extends Tree> vis : visitor.andThen()) {
            acc = transformPipeline(acc, vis);
        }

        sample.stop(Timer.builder("rewrite.refactor.visit")
                .description("The time it takes to visit a single AST with a particular refactoring visitor and its pipeline")
                .tag("visitor", visitor.getClass().getName())
                .tags(visitor.getTags())
                .tag("tree.type", original.getClass().getSimpleName())
                .register(meterRegistry));

        return acc;
    }

    public Refactor<T> setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        return this;
    }
}
