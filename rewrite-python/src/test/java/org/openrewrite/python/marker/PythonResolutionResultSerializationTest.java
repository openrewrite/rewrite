/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.marker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A resolved Python graph is cyclic — packages depend on each other through extras — so it has to survive both a
 * Jackson round trip and the {@code equals}/{@code hashCode} of any collection that holds it.
 */
class PythonResolutionResultSerializationTest {

    /**
     * Mirrors the mapper configuration that serializes LSTs and recipe edits.
     */
    private static ObjectMapper mapper() {
        ObjectMapper m = JsonMapper.builder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
                .build();
        m.registerModule(new ParameterNamesModule());
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        return m;
    }

    /**
     * Linked the way {@code PythonResolutionLinker} does it: instances shared, lists filled in place.
     */
    private static ResolvedDependency cyclicGraph() {
        ResolvedDependency a = new ResolvedDependency("a", "1.0.0", null, new ArrayList<>());
        ResolvedDependency b = new ResolvedDependency("b", "1.0.0", null, new ArrayList<>());
        a.getDependencies().add(b);
        b.getDependencies().add(a);
        return a;
    }

    @Test
    void roundTripCyclicGraph() throws Exception {
        ObjectMapper mapper = mapper();
        ResolvedDependency read = mapper.readValue(mapper.writeValueAsString(cyclicGraph()), ResolvedDependency.class);

        assertThat(read.getDependencies().get(0).getDependencies().get(0))
                .as("the back-reference closing the cycle must resolve to the same instance")
                .isSameAs(read);
    }

    @Test
    void equalsAndHashCodeTerminateOnCyclicGraph() {
        ResolvedDependency a = cyclicGraph();

        assertThat(a.hashCode()).isEqualTo(a.hashCode());
        assertThat(a).isEqualTo(cyclicGraph());
    }
}
