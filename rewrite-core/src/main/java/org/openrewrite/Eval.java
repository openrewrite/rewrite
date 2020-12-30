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
import org.openrewrite.internal.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class Eval {
    private static final Logger logger = LoggerFactory.getLogger(Eval.class);
    private final MeterRegistry meterRegistry;
    private final boolean eagerlyThrow;
    private final Collection<EvalVisitor<? extends Tree>> visitors;

    private Eval(MeterRegistry meterRegistry, boolean eagerlyThrow, Collection<EvalVisitor<? extends Tree>> visitors) {
        this.meterRegistry = meterRegistry;
        this.eagerlyThrow = eagerlyThrow;
        this.visitors = visitors;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <S extends SourceFile> S results(S tree) {
        return (S) visit(Collections.singletonList(tree)).iterator().next().getAfter();
    }

    /**
     * Generate a change set by visiting a collection of sources.
     *
     * @param sources The collection of sources don't have to have the same type. They can be a mixture of, for example,
     *                Java source files and Maven POMs.
     * @return A change set.
     */
    public Collection<Result> visit(Iterable<? extends SourceFile> sources) {
        return visit(sources, 3);
    }

    /**
     * Generate a change set by visiting a collection of sources.
     *
     * @param sources   The collection of sources don't have to have the same type. They can be a mixture of, for example,
     *                  Java source files and Maven POMs.
     * @param maxCycles The maximum number of iterations to visit the files.
     * @return A change set.
     */
    public Collection<Result> visit(Iterable<? extends SourceFile> sources, int maxCycles) {
        EvalContext ctx = new EvalContext();
        Timer.Sample sample = Timer.start();

        Map<SourceFile, Result> changesByTree = new HashMap<>();

        List<SourceFile> accumulatedSources = new ArrayList<>();
        sources.forEach(accumulatedSources::add);

        for (int i = 0; i < maxCycles; i++) {
            int visitorsThatMadeChangesThisCycle = 0;
            ctx.next();

            for (int j = 0; j < accumulatedSources.size(); j++) {
                SourceFile originalSource = accumulatedSources.get(j);
                if (originalSource == null) {

                    // source was deleted in a previous iteration
                    continue;
                }

                SourceFile acc = originalSource;

                for (EvalVisitor<? extends Tree> visitor : visitors) {
                    try {
                        visitor.next();

                        if (!visitor.isIdempotent() && i > 0) {
                            continue;
                        }

                        SourceFile before = acc;
                        acc = (SourceFile) transformPipeline(acc, visitor, ctx, null);

                        if (before != acc) {

                            // we should only report on the top-level visitors, not any andThen() visitors that
                            // are applied as part of the top-level visitor's pipeline
                            changesByTree.compute(acc, (acc2, prevChange) -> prevChange == null ?
                                    new Result(originalSource, acc2, Collections.singleton(visitor.getName())) :
                                    new Result(originalSource, acc2, Stream
                                            .concat(prevChange.getVisitorsThatMadeChanges().stream(), Stream.of(visitor.getName()))
                                            .collect(toSet()))
                            );
                            visitorsThatMadeChangesThisCycle++;
                        }
                    } catch (Throwable t) {
                        logger.error("refactor visitor failed", t);
                        Counter.builder("rewrite.visitor.errors")
                                .baseUnit("errors")
                                .description("Visitors that threw exceptions")
                                .tag("visitor", visitor.getName())
                                .tag("tree.type", originalSource.getClass().getName())
                                .tag("exception", t.getClass().getSimpleName())
                                .register(meterRegistry)
                                .increment();
                        if(eagerlyThrow) {
                            throw t;
                        }
                    }
                }

                // we've seen all the files once, so if any new source files needs to be generated by any of the visitors,
                // let's do that now. On the next cycle, these visitors shouldn't generate these files again, but update
                // them in place as necessary.
                for (EvalVisitor<? extends Tree> visitor : visitors) {
                    if(!ctx.generate.isEmpty()) {
                        accumulatedSources.addAll(ctx.generate);
                        visitorsThatMadeChangesThisCycle += ctx.generate.size();
                        for(SourceFile generatedSource : ctx.generate) {
                            // TODO: Think about what should happen if multiple visitors try to generate the same target file
                            Set<String> visitorSet = new HashSet<>();
                            visitorSet.add(visitor.getName());
                            changesByTree.put(generatedSource, new Result(null, generatedSource, visitorSet));
                        }
                    }
                }

                accumulatedSources.set(j, acc);
            }
            // Always do at least two cycles in case all the visitors were ones
            if (visitorsThatMadeChangesThisCycle == 0 && i > 0) {
                break;
            }
        }

        sample.stop(Timer.builder("rewrite.refactor.plan")
                .description("The time it takes to execute a refactoring plan consisting of potentially more than one visitor over more than one cycle")
                .tag("outcome", changesByTree.isEmpty() ? "unchanged" : "changed")
                .register(meterRegistry));

        for (Result result : changesByTree.values()) {
            for (String ruleThatMadeChange : result.getVisitorsThatMadeChanges()) {
                Counter.builder("rewrite.refactor.plan.changes")
                        .description("The number of changes requested by a visitor")
                        .tag("visitor", ruleThatMadeChange)
                        .tag("tree.type", result.getTreeType() == null ? "unknown" : result.getTreeType().getName())
                        .register(meterRegistry)
                        .increment();
            }
        }

        return changesByTree.values();
    }

    private Tree transformPipeline(Tree acc, EvalVisitor<? extends Tree> visitor, EvalContext ctx,
                                   @Nullable Cursor startingCursor) {
        // by transforming the AST for each op, we allow for the possibility of overlapping changes
        Timer.Sample sample = Timer.start();
        Tree before = acc;
        acc = visitor.scan(acc, ctx, startingCursor);
        for (EvalVisitor.Task<? extends Tree> task : visitor.onNext()) {
            if(acc != null) {
                acc = transformPipeline(acc, task.getNext(), ctx, task.getStartingCursor());
            }
        }

        sample.stop(Timer.builder("rewrite.refactor.visit")
                .description("The time it takes to visit a single AST with a particular refactoring visitor and its pipeline")
                .tag("visitor", visitor.getName())
                .tags(visitor.getTags())
                .tag("tree.type", before.getClass().getSimpleName())
                .register(meterRegistry));

        return acc;
    }

    public static class Builder {
        private MeterRegistry meterRegistry = Metrics.globalRegistry;
        private boolean eagerlyThrow = false;
        private final Collection<EvalVisitor<? extends Tree>> visitors = new ArrayList<>();

        public Builder meterRegistry(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            return this;
        }

        public Builder eagerlyThrow(@Nullable Boolean eagerlyThrow) {
            this.eagerlyThrow = eagerlyThrow != null && eagerlyThrow;
            return this;
        }

        @SafeVarargs
        public final Builder visit(EvalVisitor<? extends Tree>... visitors) {
            Collections.addAll(this.visitors, visitors);
            return this;
        }

        public Builder visit(Iterable<EvalVisitor<? extends Tree>> visitors) {
            visitors.forEach(this.visitors::add);
            return this;
        }

        public Eval build() {
            return new Eval(meterRegistry, eagerlyThrow, visitors);
        }
    }
}
