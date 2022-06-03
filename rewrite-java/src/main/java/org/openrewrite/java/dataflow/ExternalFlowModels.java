package org.openrewrite.java.dataflow;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.beta.CallMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.RecipeSerializer.maybeAddKotlinModule;

class ExternalFlowModels {
    private static final ExternalFlowModels instance = new ExternalFlowModels();
    public static ExternalFlowModels instance() {
        return instance;
    }
    private WeakReference<FlowModels> flowModels;

    private FlowModels getFlowModels() {
        FlowModels f;
        if (this.flowModels == null) {
            f = Loader.create().loadModel();
            this.flowModels = new WeakReference<>(f);
        } else {
            f = this.flowModels.get();
            if (f == null) {
                f = Loader.create().loadModel();
                this.flowModels = new WeakReference<>(f);
            }
        }
        return f;
    }

    boolean isAdditionalTaintStep(
            Expression startExpression,
            Cursor startCursor,
            Expression endExpression,
            Cursor endCursor
    ) {
        return getFlowModels().taint.stream().map(FlowModel::asAdditionalFlowStepPredicate).anyMatch(
                taint -> taint.isAdditionalFlowStep(startExpression, startCursor, endExpression, endCursor)
        );
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

        AdditionalFlowStepPredicate asAdditionalFlowStepPredicate() {
            if ("Argument[-1]".equals(input) && "ReturnValue".equals(output)) {
                final String signature;
                if (this.signature.isEmpty()) {
                    signature = "(..)";
                } else {
                    signature = this.signature;
                }
                CallMatcher matcher =
                        CallMatcher.fromMethodMatcher(
                                new MethodMatcher(
                                        namespace + '.' + type + ' ' + name + signature,
                                        subtypes
                                )
                        );
                return (startExpression, startCursor, endExpression, endCursor) ->
                        matcher.advanced().isSelect(startExpression, startCursor);
            }
            return (startExpression, startCursor, endExpression, endCursor) -> false;
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
