package org.openrewrite;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.openrewrite.internal.lang.NonNullApi;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.StreamSupport.stream;

/**
 * A refactoring operation on a single source file.
 *
 * @param <S> The root AST element for a particular language
 * @param <T> The common interface to all AST elements for a particular language.
 */
@NonNullApi
public class Refactor<S extends SourceFile, T extends Tree> {
    @Getter
    private final S original;

    private MeterRegistry meterRegistry = Metrics.globalRegistry;

    @Getter
    private final List<SourceVisitor<T>> visitors = new ArrayList<>();

    public Refactor(S original) {
        this.original = original;
    }

    @SafeVarargs
    public final Refactor<S, T> visit(SourceVisitor<T>... visitors) {
        Collections.addAll(this.visitors, visitors);
        return this;
    }

    public final Refactor<S, T> visit(Iterable<SourceVisitor<T>> visitors) {
        visitors.forEach(this.visitors::add);
        return this;
    }

    /**
     * Shortcut for building refactoring operations for a collection of like-typed {@link Tree} elements.
     *
     * @param ts              The list of tree elements to operate on.
     * @param refactorForEach Build a refactoring operation built for each tree item in the collection.
     *                        The function should return null when there is some condition under which an
     *                        item in the list should not be transformed.
     * @param <T2>            The type of tree element to operate on.
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
        Timer.Sample sample = Timer.start();

        S acc = original;
        Set<String> rulesThatMadeChanges = new HashSet<>();

        for (int i = 0; i < maxCycles; i++) {
            Set<String> rulesThatMadeChangesThisCycle = new HashSet<>();
            for (SourceVisitor<T> visitor : visitors) {
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

        sample.stop(Timer.builder("rewrite.refactor.plan")
                .description("The time it takes to execute a refactoring plan consisting of potentially more than one visitor over more than one cycle")
                .tag("file.type", original.getFileType())
                .tag("outcome", rulesThatMadeChanges.isEmpty() ? "Unchanged" : "Changed")
                .register(meterRegistry));

        for (String ruleThatMadeChange : rulesThatMadeChanges) {
            Counter.builder("rewrite.refactor.plan.changes")
                    .description("The number of changes requested by a visitor.")
                    .tag("visitor", ruleThatMadeChange)
                    .tag("file.type", original.getFileType())
                    .register(meterRegistry)
                    .increment();
        }

        return new Change<>(original, acc, rulesThatMadeChanges);
    }

    @SuppressWarnings("unchecked")
    private S transformPipeline(S acc, SourceVisitor<T> visitor) {
        // by transforming the AST for each op, we allow for the possibility of overlapping changes
        Timer.Sample sample = Timer.start();
        acc = (S) visitor.visit(acc);
        for (SourceVisitor<T> vis : visitor.andThen()) {
            acc = transformPipeline(acc, vis);
        }

        sample.stop(Timer.builder("rewrite.refactor.visit")
                .description("The time it takes to visit a single AST with a particular refactoring visitor and its pipeline")
                .tag("visitor", visitor.getName() == null ? "Unnamed visitor" : visitor.getName())
                .tag("file.type", original.getFileType())
                .register(meterRegistry));

        return acc;
    }

    public Refactor<S, T> setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        return this;
    }
}
