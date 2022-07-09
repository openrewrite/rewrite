/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.dataflow.internal.InvocationMatcher;
import org.openrewrite.java.dataflow.internal.csv.CsvLoader;
import org.openrewrite.java.dataflow.internal.csv.GenericExternalModel;
import org.openrewrite.java.dataflow.internal.csv.Mergeable;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;

@Incubating(since = "7.25.0")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExternalSinkModels {
    private static final String CURSOR_MESSAGE_KEY = "OPTIMIZED_SINK_MODELS";
    private static final ExternalSinkModels instance = new ExternalSinkModels();

    public static ExternalSinkModels getInstance() {
        return instance;
    }

    private WeakReference<FullyQualifiedNameToSinkModels> fullyQualifiedNameToSinkModel;

    private FullyQualifiedNameToSinkModels getFullyQualifiedNameToSinkModel() {
        FullyQualifiedNameToSinkModels f;
        if (fullyQualifiedNameToSinkModel == null) {
            f = Loader.load();
            fullyQualifiedNameToSinkModel = new WeakReference<>(f);
        } else {
            f = fullyQualifiedNameToSinkModel.get();
            if (f == null) {
                f = Loader.load();
                fullyQualifiedNameToSinkModel = new WeakReference<>(f);
            }
        }
        return f;
    }

    private OptimizedSinkModels getOptimizedSinkModelsForTypesInUse(TypesInUse typesInUse) {
        return Optimizer.optimize(getFullyQualifiedNameToSinkModel().forTypesInUse(typesInUse));
    }

    private OptimizedSinkModels getOrComputeOptimizedSinkModels(Cursor cursor) {
        Cursor cuCursor = cursor.dropParentUntil(J.CompilationUnit.class::isInstance);
        return cuCursor.computeMessageIfAbsent(
                CURSOR_MESSAGE_KEY,
                __ -> getOptimizedSinkModelsForTypesInUse(cuCursor.<J.CompilationUnit>getValue().getTypesInUse())
        );
    }

    /**
     * True if the {@code expression} {@code cursor} is specified as a sink with the given {@code kind} in the
     * CSV flow model.
     *
     * @return If this is a sink of the given {@code kind}.
     */
    public boolean isSinkNode(Expression expression, Cursor cursor, String kind) {
        return getOrComputeOptimizedSinkModels(cursor)
                .forKind(kind)
                .stream()
                .anyMatch(predicate -> predicate.isSinkNode(expression, cursor));
    }

    private interface SinkNodePredicate {
        boolean isSinkNode(Expression expression, Cursor cursor);
    }

    @AllArgsConstructor
    private static class OptimizedSinkModels {
        private final Map<String, List<SinkNodePredicate>> sinkKindToPredicates;

        private List<SinkNodePredicate> forKind(String kind) {
            return sinkKindToPredicates.getOrDefault(kind, Collections.emptyList());
        }
    }

    private static class Optimizer {
        private final MethodMatcherCache methodMatcherCache = MethodMatcherCache.create();

        private SinkNodePredicate sinkNodePredicateForArgumentIndex(
                int argumentIndex,
                Collection<MethodMatcher> methodMatchers
        ) {
            InvocationMatcher invocationMatcher = InvocationMatcher.fromMethodMatchers(methodMatchers);
            return argumentIndex == -1 ?
                    ((expression, cursor) -> invocationMatcher.advanced().isSelect(cursor)) :
                    ((expression, cursor) -> invocationMatcher.advanced().isParameter(cursor, argumentIndex));
        }

        private List<SinkNodePredicate> optimize(Collection<SinkModel> models) {
            Map<Integer, List<SinkModel>> sinkForArgument = new HashMap<>();
            for (SinkModel model : models) {
                model.getArgumentRange().ifPresent(argumentRange -> {
                    for (int i = argumentRange.getStart(); i <= argumentRange.getEnd(); i++) {
                        sinkForArgument.computeIfAbsent(i, __ -> new ArrayList<>()).add(model);
                    }
                });
            }
            return sinkForArgument
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        Collection<MethodMatcher> methodMatchers = methodMatcherCache.provideMethodMatchers(entry.getValue());
                        return sinkNodePredicateForArgumentIndex(
                                entry.getKey(),
                                methodMatchers
                        );
                    })
                    .collect(Collectors.toList());
        }

        private static OptimizedSinkModels optimize(SinkModels sinkModels) {
            Optimizer optimizer = new Optimizer();
            Map<String, List<SinkNodePredicate>> sinkKindToPredicates =
                    sinkModels
                            .sinkModels
                            .entrySet()
                            .stream()
                            .map(e -> new AbstractMap.SimpleEntry<>(
                                    e.getKey(),
                                    optimizer.optimize(e.getValue())
                            ))
                            .collect(Collectors.toMap(
                                    AbstractMap.SimpleEntry::getKey,
                                    AbstractMap.SimpleEntry::getValue
                            ));
            return new OptimizedSinkModels(sinkKindToPredicates);
        }
    }

    @AllArgsConstructor
    static class SinkModels {
        Map<String /* SinkModel.kind */, Set<SinkModel>> sinkModels;
    }

    @AllArgsConstructor
    static class FullyQualifiedNameToSinkModels implements Mergeable<FullyQualifiedNameToSinkModels> {
        private final Map<String, List<SinkModel>> fqnToSinkModels;

        boolean isEmpty() {
            return fqnToSinkModels.isEmpty();
        }

        @Override
        public FullyQualifiedNameToSinkModels merge(FullyQualifiedNameToSinkModels other) {
            if (other.isEmpty()) {
                return this;
            } else if (isEmpty()) {
                return other;
            }
            Map<String, List<SinkModel>> merged = new HashMap<>(this.fqnToSinkModels);
            other.fqnToSinkModels.forEach((k, v) -> merged.computeIfAbsent(k, kk -> new ArrayList<>(v.size())).addAll(v));
            return new FullyQualifiedNameToSinkModels(merged);
        }

        /**
         * Loads the subset of {@link SinkModel}s that are relevant to the given {@link TypesInUse}.
         * <p>
         * This optimization prevents the generation of {@link AdditionalFlowStepPredicate} and {@link InvocationMatcher}
         * for method signatures that aren't even present in {@link J.CompilationUnit}.
         */
        SinkModels forTypesInUse(TypesInUse typesInUse) {
            Map<String, Set<SinkModel>> sinkModels = new HashMap<>();
            typesInUse
                    .getUsedMethods()
                    .stream()
                    .map(JavaType.Method::getDeclaringType)
                    .map(JavaType.FullyQualified::getFullyQualifiedName)
                    .distinct()
                    .flatMap(fqn -> fqnToSinkModels.getOrDefault(
                                    fqn,
                                    Collections.emptyList()
                            ).stream()
                    ).forEach(sinkModel -> {
                        sinkModels.computeIfAbsent(sinkModel.kind, k -> new HashSet<>(1)).add(sinkModel);
                    });
            return new SinkModels(sinkModels);
        }

        static FullyQualifiedNameToSinkModels empty() {
            return new FullyQualifiedNameToSinkModels(new HashMap<>(0));
        }
    }

    @AllArgsConstructor
    static class SinkModel implements GenericExternalModel {
        // namespace, type, subtypes, name, signature, ext, input, kind, generated
        @Getter
        String namespace;

        @Getter
        String type;

        @Getter
        boolean subtypes;

        @Getter
        String name;

        @Getter
        String signature;

        String ext;
        String input;
        String kind;
        boolean generated;

        @Override
        public String getArguments() {
            return input;
        }
    }

    static class Loader {
        static FullyQualifiedNameToSinkModels load() {
            return CsvLoader.loadFromFile(
                    "sinks.csv",
                    FullyQualifiedNameToSinkModels.empty(),
                    Loader::createFullyQualifiedNameToFlowModels,
                    tokens -> new SinkModel(
                            tokens[0],
                            tokens[1],
                            Boolean.parseBoolean(tokens[2]),
                            tokens[3],
                            tokens[4],
                            tokens[5],
                            tokens[6],
                            tokens[7],
                            Boolean.parseBoolean(tokens[8])
                    )
            );
        }

        private static FullyQualifiedNameToSinkModels createFullyQualifiedNameToFlowModels(Iterable<SinkModel> sinkModels) {
            Map<String, List<SinkModel>> fqnToSinkModels = new HashMap<>();
            for (SinkModel sinkModel : sinkModels) {
                fqnToSinkModels.computeIfAbsent(sinkModel.getFullyQualifiedName(), k -> new ArrayList<>()).add(sinkModel);
            }
            return new FullyQualifiedNameToSinkModels(fqnToSinkModels);
        }
    }
}
