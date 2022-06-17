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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.dataflow.internal.InvocationMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads and stores models from the `model.csv` file to be used for data flow and taint tracking analysis.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ExternalFlowModels {
    private static final String CURSOR_MESSAGE_KEY = "OPTIMIZED_FLOW_MODELS";
    private static final Pattern ARGUMENT_MATCHER = Pattern.compile("Argument\\[(-?\\d+)\\.?\\.?(\\d+)?]");
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
        private final Map<FlowModel.MethodMatcherKey, MethodMatcher> methodMapperCache =
                new HashMap<>();

        private MethodMatcher provideMethodMatcher(FlowModel.MethodMatcherKey key) {
            return methodMapperCache.computeIfAbsent(
                    key,
                    k -> new MethodMatcher(k.signature, k.matchOverrides)
            );
        }

        private Set<MethodMatcher> provideMethodMatchers(Collection<FlowModel> models) {
            return models
                    .stream()
                    .map(FlowModel::asMethodMatcherKey)
                    .map(this::provideMethodMatcher)
                    .collect(Collectors.toSet());
        }

        /**
         * Return the 'optimized' {@link AdditionalFlowStepPredicate} for the {@link MethodMatcher}.
         */
        private AdditionalFlowStepPredicate forFlowFromArgumentIndexToReturn(
                int argumentIndex,
                Set<MethodMatcher> methodMatchers
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
                Matcher argumentMatcher = ARGUMENT_MATCHER.matcher(model.input);

                if (argumentMatcher.matches() && ("ReturnValue".equals(model.output) || model.isConstructor())) {
                    int argumentIndexStart = Integer.parseInt(argumentMatcher.group(1));
                    flowFromArgumentIndexToReturn.computeIfAbsent(argumentIndexStart, k -> new ArrayList<>())
                            .add(model);
                    // argumentMatcher.group(2) is null for Argument[x] since ARGUMENT_MATCHER matches Argument[x] and
                    // Argument[x..y], and so the null check below ensures that no exception is thrown when Argument[x]
                    // is matched
                    if (argumentMatcher.group(2) != null) {
                        int argumentIndexEnd = Integer.parseInt(argumentMatcher.group(2));
                        for (int i = argumentIndexStart + 1; i <= argumentIndexEnd; i++) {
                            flowFromArgumentIndexToReturn.computeIfAbsent(i, k -> new ArrayList<>())
                                    .add(model);
                        }
                    }
                }
            });

            return flowFromArgumentIndexToReturn
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        Set<MethodMatcher> methodMatchers = provideMethodMatchers(entry.getValue());
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
    static class FullyQualifiedNameToFlowModels {
        private final Map<String, List<FlowModel>> value;
        private final Map<String, List<FlowModel>> taint;

        boolean isEmpty() {
            return value.isEmpty() && taint.isEmpty();
        }

        FullyQualifiedNameToFlowModels merge(FullyQualifiedNameToFlowModels other) {
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
            typesInUse
                    .getUsedMethods()
                    .stream()
                    .map(JavaType.Method::getDeclaringType)
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
            return new FullyQualifiedNameToFlowModels(new HashMap<>(), new HashMap<>());
        }
    }

    @AllArgsConstructor
    static class FlowModel {
        // namespace, type, subtypes, name, signature, ext, input, output, kind
        String namespace;
        String type;
        boolean subtypes;
        String name;
        String signature;
        String ext;
        String input;
        String output;
        String kind;

        final String getFullyQualifiedName() {
            return namespace + "." + type;
        }

        boolean isConstructor() {
            // If the type and the name are the same, then this the signature for a constructor
            return this.type.equals(this.name);
        }

        @Deprecated
        AdditionalFlowStepPredicate asAdditionalFlowStepPredicate() {
            MethodMatcherKey key = asMethodMatcherKey();
            InvocationMatcher matcher = InvocationMatcher.fromMethodMatcher(
                    new MethodMatcher(
                            key.signature,
                            key.matchOverrides
                    )
            );
            Matcher argumentMatcher = ARGUMENT_MATCHER.matcher(input);
            if ("Argument[-1]".equals(input) && "ReturnValue".equals(output)) {
                return (srcExpression, srcCursor, sinkExpression, sinkCursor) ->
                        matcher.advanced().isSelect(srcCursor);
            } else if (argumentMatcher.matches() && "ReturnValue".equals(output)) {
                int argumentIndex = Integer.parseInt(argumentMatcher.group(1));
                return (srcExpression, srcCursor, sinkExpression, sinkCursor) ->
                        matcher.advanced().isParameter(srcCursor, argumentIndex);
            }

            return (srcExpression, srcCursor, sinkExpression, sinkCursor) -> false;
        }

        MethodMatcherKey asMethodMatcherKey() {
            final String signature;
            if (this.signature.isEmpty()) {
                signature = "(..)";
            } else {
                signature = this.signature;
            }
            final String fullSignature;
            if (this.isConstructor()) {
                fullSignature = namespace + '.' + type + ' ' + "<constructor>" + signature;
            } else {
                fullSignature = namespace + '.' + type + ' ' + name + signature;
            }
            return new MethodMatcherKey(
                    fullSignature,
                    subtypes
            );
        }

        @Data
        static class MethodMatcherKey {
            final String signature;
            final boolean matchOverrides;
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
            final FullyQualifiedNameToFlowModels[] models = {FullyQualifiedNameToFlowModels.empty()};
            try (ScanResult scanResult = new ClassGraph().acceptPaths("data-flow").enableMemoryMapping().scan()) {
                scanResult.getResourcesWithLeafName("model.csv")
                        .forEachInputStreamIgnoringIOException((res, input) -> models[0] = models[0].merge(loadCvs(input, res.getURI())));
            }
            return models[0];
        }

        private FullyQualifiedNameToFlowModels loadCvs(InputStream input, URI source) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            List<FlowModel> flowModels = new ArrayList<>();
            try {
                //noinspection UnusedAssignment skip the header line
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(";");
                    flowModels.add(new FlowModel(tokens[0], tokens[1], Boolean.parseBoolean(tokens[2]), tokens[3], tokens[4], tokens[5], tokens[6], tokens[7], tokens[8]));
                }
                return createFullyQualifiedNameToFlowModels(flowModels);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read data-flow values from " + source, e);
            }
        }
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
