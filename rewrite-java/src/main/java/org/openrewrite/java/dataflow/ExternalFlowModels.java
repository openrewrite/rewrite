package org.openrewrite.java.dataflow;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.beta.CallMatcher;
import org.openrewrite.java.tree.Expression;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.RecipeSerializer.maybeAddKotlinModule;

class ExternalFlowModels {
    private static final Pattern ARGUMENT_MATCHER = Pattern.compile("Argument\\[(-?\\d+)\\]");
    private static final ExternalFlowModels instance = new ExternalFlowModels();

    public static ExternalFlowModels instance() {
        return instance;
    }

    private WeakReference<OptimizedFlowModels> optimizedFlowModels;

    private OptimizedFlowModels getOptimizedFlowModels() {
        OptimizedFlowModels f;
        if (this.optimizedFlowModels == null) {
            f = Optimizer.optimize(Loader.create().loadModel());
            this.optimizedFlowModels = new WeakReference<>(f);
        } else {
            f = this.optimizedFlowModels.get();
            if (f == null) {
                f = Optimizer.optimize(Loader.create().loadModel());
                this.optimizedFlowModels = new WeakReference<>(f);
            }
        }
        return f;
    }

    boolean isAdditionalFlowStep(
            Expression startExpression,
            Cursor startCursor,
            Expression endExpression,
            Cursor endCursor
    ) {
        return getOptimizedFlowModels().value.stream().anyMatch(
                taint -> taint.isAdditionalFlowStep(startExpression, startCursor, endExpression, endCursor)
        );
    }

    boolean isAdditionalTaintStep(
            Expression startExpression,
            Cursor startCursor,
            Expression endExpression,
            Cursor endCursor
    ) {
        return getOptimizedFlowModels().taint.stream().anyMatch(
                taint -> taint.isAdditionalFlowStep(startExpression, startCursor, endExpression, endCursor)
        );
    }

    @AllArgsConstructor
    private static class OptimizedFlowModels {
        private final List<AdditionalFlowStepPredicate> value;
        private final List<AdditionalFlowStepPredicate> taint;
    }

    /**
     * Dedicated optimization step that attempts to optimize the {@link AdditionalFlowStepPredicate}s
     * and reduce the number of them by merging similar method signatures into a single {@link CallMatcher}.
     * <p>
     * <p>
     * As an example, take the following model method signatures:
     * <ul>
     *     <li>{@code java.lang;String;false;toLowerCase;;;Argument[-1];ReturnValue;taint}</li>
     *     <li>{@code java.lang;String;false;toUpperCase;;;Argument[-1];ReturnValue;taint}</li>
     *     <li>{@code java.lang;String;false;trim;;;Argument[-1];ReturnValue;taint}</li>
     * </ul>
     * <p>
     * These can be merged into a single {@link CallMatcher} that matches all these methods.
     * <p>
     * From there, a single {@link CallMatcher.AdvancedCallMatcher} can be called by the
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
            CallMatcher callMatcher = CallMatcher.fromMethodMatchers(methodMatchers);
            if (argumentIndex == -1) {
                // Argument[-1] is the 'select' or 'qualifier' of a method call
                return (startExpression, startCursor, endExpression, endCursor) ->
                        callMatcher.advanced().isSelect(startExpression, startCursor);
            } else {
                return (startExpression, startCursor, endExpression, endCursor) ->
                        callMatcher.advanced().isArgument(startExpression, startCursor, argumentIndex);
            }
        }

        private List<AdditionalFlowStepPredicate> optimize(List<FlowModel> models) {
            Map<Integer, List<FlowModel>> flowFromArgumentIndexToReturn = new HashMap<>();

            models.forEach(model -> {
                Matcher argumentMatcher = ARGUMENT_MATCHER.matcher(model.input);
                if (argumentMatcher.matches() && ("ReturnValue".equals(model.output) || model.isConstructor())) {
                    int argumentIndex = Integer.parseInt(argumentMatcher.group(1));
                    flowFromArgumentIndexToReturn.computeIfAbsent(argumentIndex, k -> new ArrayList<>())
                            .add(model);
                }
            });

            List<AdditionalFlowStepPredicate> additionalFlowStepPredicates =
                    flowFromArgumentIndexToReturn
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
            return additionalFlowStepPredicates;
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
        private final List<FlowModel> value;
        private final List<FlowModel> taint;

        boolean isEmpty() {
            return value.isEmpty() && taint.isEmpty();
        }

        FlowModels merge(FlowModels other) {
            if (this.isEmpty()) {
                return other;
            } else if (other.isEmpty()) {
                return this;
            }
            List<FlowModel> value = new ArrayList<>(this.value);
            value.addAll(other.value);
            List<FlowModel> taint = new ArrayList<>(this.taint);
            taint.addAll(other.taint);
            return new FlowModels(value, taint);
        }

        static FlowModels empty() {
            return new FlowModels(emptyList(), emptyList());
        }
    }

    @AllArgsConstructor
    private static class FlowModel {
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

        boolean isConstructor() {
            // If the type and the name are the same, then this the signature for a constructor
            return this.type.equals(this.name);
        }

        AdditionalFlowStepPredicate asAdditionalFlowStepPredicate() {
            MethodMatcherKey key = asMethodMatcherKey();
            CallMatcher matcher = CallMatcher.fromMethodMatcher(
                    new MethodMatcher(
                            key.signature,
                            key.matchOverrides
                    )
            );
            Matcher argumentMatcher = ARGUMENT_MATCHER.matcher(input);
            if ("Argument[-1]".equals(input) && "ReturnValue".equals(output)) {
                return (startExpression, startCursor, endExpression, endCursor) ->
                        matcher.advanced().isSelect(startExpression, startCursor);
            } else if (argumentMatcher.matches() && "ReturnValue".equals(output)) {
                int argumentIndex = Integer.parseInt(argumentMatcher.group(1));
                return (startExpression, startCursor, endExpression, endCursor) ->
                        matcher.advanced().isArgument(startExpression, startCursor, argumentIndex);
            }

            return (startExpression, startCursor, endExpression, endCursor) -> false;
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

        FlowModels loadModel() {
            final FlowModels[] models = {FlowModels.empty()};
            try (ScanResult scanResult = new ClassGraph().acceptPaths("data-flow").enableMemoryMapping().scan()) {
                scanResult.getResourcesWithLeafName("model.csv").forEachInputStreamIgnoringIOException((res, input) -> {
                    models[0] = models[0].merge(loadCvs(input, res.getURI()));
                });
            }
            return models[0];
        }

        private static FlowModels loadCvs(InputStream input, URI source) {
            CsvMapper mapper = CsvMapper
                    .builder()
                    .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                    .build();
            mapper.registerModule(new ParameterNamesModule());
            maybeAddKotlinModule(mapper);
            CsvSchema schema = mapper
                    .schemaFor(FlowModel.class)
                    .withHeader()
                    .withColumnSeparator(';');
            try (MappingIterator<FlowModel> iterator =
                         mapper.readerFor(FlowModel.class).with(schema).readValues(input)) {
                List<FlowModel> value = new ArrayList<>();
                List<FlowModel> taint = new ArrayList<>();
                for (FlowModel model : (Iterable<FlowModel>) () -> iterator) {
                    if ("value".equals(model.kind)) {
                        value.add(model);
                    } else if ("taint".equals(model.kind)) {
                        taint.add(model);
                    } else {
                        throw new IllegalArgumentException("Unknown kind: " + model.kind + " in " + source);
                    }
                }
                return new FlowModels(value, taint);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read values from " + source, e);
            }
        }
    }
}
