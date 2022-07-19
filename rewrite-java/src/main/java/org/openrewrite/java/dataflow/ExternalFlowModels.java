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

/**
 * Loads and stores models from the `model.csv` file to be used for data flow and taint tracking analysis.
 */
@Incubating(since = "7.24.1")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ExternalFlowModels {
    private static final String CURSOR_MESSAGE_KEY = "OPTIMIZED_FLOW_MODELS";
    private static final ExternalFlowModels instance = new ExternalFlowModels();

    public static ExternalFlowModels instance() {
        return instance;
    }

    private WeakReference<FullyQualifiedNameToFlowModels> fullyQualifiedNameToFlowModels;

    private FullyQualifiedNameToFlowModels getFullyQualifiedNameToFlowModels() {
        FullyQualifiedNameToFlowModels f;
        if (this.fullyQualifiedNameToFlowModels == null) {
            f = Loader.create().load();
            this.fullyQualifiedNameToFlowModels = new WeakReference<>(f);
        } else {
            f = this.fullyQualifiedNameToFlowModels.get();
            if (f == null) {
                f = Loader.create().load();
                this.fullyQualifiedNameToFlowModels = new WeakReference<>(f);
            }
        }
        return f;
    }

    private OptimizedFlowModels getOptimizedFlowModelsForTypesInUse(TypesInUse typesInUse) {
        return Optimizer.optimize(getFullyQualifiedNameToFlowModels().forTypesInUse(typesInUse));
    }

    private OptimizedFlowModels getOrComputeOptimizedFlowModels(Cursor cursor) {
        Cursor cuCursor = cursor.dropParentUntil(J.CompilationUnit.class::isInstance);
        return cuCursor.computeMessageIfAbsent(
                CURSOR_MESSAGE_KEY,
                __ -> getOptimizedFlowModelsForTypesInUse(cuCursor.<J.CompilationUnit>getValue().getTypesInUse())
        );
    }

    boolean isAdditionalFlowStep(
            Expression srcExpression,
            Cursor srcCursor,
            Expression sinkExpression,
            Cursor sinkCursor
    ) {
        return getOrComputeOptimizedFlowModels(srcCursor).value.stream().anyMatch(
                value -> value.isAdditionalFlowStep(srcExpression, srcCursor, sinkExpression, sinkCursor)
        );
    }

    boolean isAdditionalTaintStep(
            Expression srcExpression,
            Cursor srcCursor,
            Expression sinkExpression,
            Cursor sinkCursor
    ) {
        return getOrComputeOptimizedFlowModels(srcCursor).taint.stream().anyMatch(
                taint -> taint.isAdditionalFlowStep(srcExpression, srcCursor, sinkExpression, sinkCursor)
        );
    }

    @AllArgsConstructor
    private static class OptimizedFlowModels {
        private final List<AdditionalFlowStepPredicate> value;
        private final List<AdditionalFlowStepPredicate> taint;
    }

    /**
     * Dedicated optimization step that attempts to optimize the {@link AdditionalFlowStepPredicate}s
     * and reduce the number of them by merging similar method signatures into a single {@link InvocationMatcher}.
     * <p>
     * <p>
     * As an example, take the following model method signatures:
     * <ul>
     *     <li>{@code java.lang;String;false;toLowerCase;;;Argument[-1];ReturnValue;taint}</li>
     *     <li>{@code java.lang;String;false;toUpperCase;;;Argument[-1];ReturnValue;taint}</li>
     *     <li>{@code java.lang;String;false;trim;;;Argument[-1];ReturnValue;taint}</li>
     * </ul>
     * <p>
     * These can be merged into a single {@link InvocationMatcher} that matches all these methods.
     * <p>
     * From there, a single {@link InvocationMatcher.AdvancedInvocationMatcher} can be called by the
     * {@link AdditionalFlowStepPredicate}.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Optimizer {
        private final MethodMatcherCache methodMatcherCache = MethodMatcherCache.create();

        /**
         * Return the 'optimized' {@link AdditionalFlowStepPredicate} for the {@link MethodMatcher}.
         */
        private AdditionalFlowStepPredicate forFlowFromArgumentIndexToReturn(
                int argumentIndex,
                Collection<MethodMatcher> methodMatchers
        ) {
            InvocationMatcher callMatcher = InvocationMatcher.fromMethodMatchers(methodMatchers);
            if (argumentIndex == -1) {
                // Argument[-1] is the 'select' or 'qualifier' of a method call
                return (srcExpression, srcCursor, sinkExpression, sinkCursor) ->
                        callMatcher.advanced().isSelect(srcCursor);
            } else {
                return (srcExpression, srcCursor, sinkExpression, sinkCursor) ->
                        callMatcher.advanced().isParameter(srcCursor, argumentIndex);
            }
        }

        private List<AdditionalFlowStepPredicate> optimize(Collection<FlowModel> models) {
            Map<Integer, List<FlowModel>> flowFromArgumentIndexToReturn = new HashMap<>();
            models.forEach(model -> {
                if ("ReturnValue".equals(model.output) || model.isConstructor()) {
                    model.getArgumentRange().ifPresent(argumentRange -> {
                        for (int i = argumentRange.getStart(); i <= argumentRange.getEnd(); i++) {
                            flowFromArgumentIndexToReturn.computeIfAbsent(i, __ -> new ArrayList<>())
                                    .add(model);
                        }
                    });
                }
            });

            return flowFromArgumentIndexToReturn
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        Collection<MethodMatcher> methodMatchers = methodMatcherCache.provideMethodMatchers(entry.getValue());
                        return forFlowFromArgumentIndexToReturn(
                                entry.getKey(),
                                methodMatchers
                        );
                    })
                    .collect(Collectors.toList());
        }

        private static OptimizedFlowModels optimize(FlowModels flowModels) {
            Optimizer optimizer = new Optimizer();
            return new OptimizedFlowModels(
                    optimizer.optimize(flowModels.value),
                    optimizer.optimize(flowModels.taint)
            );
        }
    }

    @AllArgsConstructor
    static class FlowModels {
        Set<FlowModel> value;
        Set<FlowModel> taint;
    }

    @AllArgsConstructor
    static class FullyQualifiedNameToFlowModels implements Mergeable<FullyQualifiedNameToFlowModels> {
        private final Map<String, List<FlowModel>> value;
        private final Map<String, List<FlowModel>> taint;

        boolean isEmpty() {
            return value.isEmpty() && taint.isEmpty();
        }

        @Override
        public FullyQualifiedNameToFlowModels merge(FullyQualifiedNameToFlowModels other) {
            if (this.isEmpty()) {
                return other;
            } else if (other.isEmpty()) {
                return this;
            }
            Map<String, List<FlowModel>> value = new HashMap<>(this.value);
            other.value.forEach((k, v) -> value.computeIfAbsent(k, kk -> new ArrayList<>(v.size())).addAll(v));
            Map<String, List<FlowModel>> taint = new HashMap<>(this.taint);
            other.taint.forEach((k, v) -> taint.computeIfAbsent(k, kk -> new ArrayList<>(v.size())).addAll(v));
            return new FullyQualifiedNameToFlowModels(value, taint);
        }

        /**
         * Loads the subset of {@link FlowModel}s that are relevant for the given {@link TypesInUse}.
         * <p>
         * This optimization prevents the generation of {@link AdditionalFlowStepPredicate} and {@link InvocationMatcher}
         * for method signatures that aren't even present in {@link J.CompilationUnit}.
         */
        FlowModels forTypesInUse(TypesInUse typesInUse) {
            Set<FlowModel> value = new HashSet<>();
            Set<FlowModel> taint = new HashSet<>();
            //noinspection ConstantConditions
            typesInUse
                    .getUsedMethods()
                    .stream()
                    .map(JavaType.Method::getDeclaringType)
                    .filter(o -> o != null && !(o instanceof JavaType.Unknown))
                    .map(JavaType.FullyQualified::getFullyQualifiedName)
                    .distinct()
                    .forEach(fqn -> {
                        value.addAll(this.value.getOrDefault(
                                fqn,
                                Collections.emptyList()
                        ));
                        taint.addAll(this.taint.getOrDefault(
                                fqn,
                                Collections.emptyList()
                        ));
                    });
            return new FlowModels(
                    value,
                    taint
            );
        }

        static FullyQualifiedNameToFlowModels empty() {
            return new FullyQualifiedNameToFlowModels(new HashMap<>(0), new HashMap<>(0));
        }
    }

    @AllArgsConstructor
    static class FlowModel implements GenericExternalModel {
        // namespace, type, subtypes, name, signature, ext, input, output, kind
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
        String output;
        String kind;

        @Override
        public String getArguments() {
            return input;
        }
    }

    private static class Loader {

        private static Loader create() {
            return new Loader();
        }

        FullyQualifiedNameToFlowModels load() {
            return loadModelFromFile();
        }

        private FullyQualifiedNameToFlowModels loadModelFromFile() {
            return CsvLoader.loadFromFile(
                    "model.csv",
                    FullyQualifiedNameToFlowModels.empty(),
                    Loader::createFullyQualifiedNameToFlowModels,
                    tokens -> new FlowModel(
                            tokens[0],
                            tokens[1],
                            Boolean.parseBoolean(tokens[2]),
                            tokens[3],
                            tokens[4],
                            tokens[5],
                            tokens[6],
                            tokens[7],
                            tokens[8]
                    )
            );
        }

        private static FullyQualifiedNameToFlowModels createFullyQualifiedNameToFlowModels(Iterable<FlowModel> flowModels) {
            Map<String, List<FlowModel>> value = new HashMap<>();
            Map<String, List<FlowModel>> taint = new HashMap<>();
            for (FlowModel model : flowModels) {
                if ("value".equals(model.kind)) {
                    value.computeIfAbsent(model.getFullyQualifiedName(), k -> new ArrayList<>()).add(model);
                } else if ("taint".equals(model.kind)) {
                    taint.computeIfAbsent(model.getFullyQualifiedName(), k -> new ArrayList<>()).add(model);
                } else {
                    throw new IllegalArgumentException("Unknown kind: " + model.kind);
                }
            }
            return new FullyQualifiedNameToFlowModels(value, taint);
        }
    }
}
