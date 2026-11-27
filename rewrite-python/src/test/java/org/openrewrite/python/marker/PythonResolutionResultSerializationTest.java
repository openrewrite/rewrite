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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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

    private static PythonResolutionResult markerWithProjectUrls() {
        Map<String, String> dependencyUrls = new LinkedHashMap<>();
        dependencyUrls.put("Source", "https://github.com/psf/requests");
        dependencyUrls.put("Documentation", "https://requests.readthedocs.io");
        ResolvedDependency requests = new ResolvedDependency("requests", "2.31.0", null, null, dependencyUrls);
        Map<String, String> projectUrls = new LinkedHashMap<>();
        projectUrls.put("Repository", "https://github.com/example/my-app");
        projectUrls.put("Homepage", "https://example.com");
        return new PythonResolutionResult(UUID.randomUUID(), "my-app", "1.0.0", null, null, "pyproject.toml",
                null, null, emptyList(),
                singletonList(new PythonResolutionResult.Dependency("requests", ">=2", null, null, requests)),
                emptyMap(), emptyMap(), emptyList(), emptyList(), singletonList(requests), null, null, projectUrls);
    }

    @Test
    void roundTripProjectUrls() throws Exception {
        ObjectMapper mapper = mapper();
        PythonResolutionResult marker = markerWithProjectUrls();

        PythonResolutionResult read = mapper.readValue(mapper.writeValueAsString(marker), PythonResolutionResult.class);

        assertThat(read.getProjectUrls()).containsExactly(
                entry("Repository", "https://github.com/example/my-app"),
                entry("Homepage", "https://example.com"));
        assertThat(read.getResolvedDependencies().get(0).getProjectUrls()).containsExactly(
                entry("Source", "https://github.com/psf/requests"),
                entry("Documentation", "https://requests.readthedocs.io"));
        assertThat(read.getDependencies().get(0).getResolved()).isSameAs(read.getResolvedDependencies().get(0));
    }

    @Test
    void readsMarkerWrittenBeforeProjectUrls() throws Exception {
        ObjectMapper mapper = mapper();
        ObjectNode written = (ObjectNode) mapper.readTree(mapper.writeValueAsString(markerWithProjectUrls()));
        written.remove("projectUrls");
        // the declared dependency comes first, so the resolution is written out there
        ((ObjectNode) written.get("dependencies").get(0).get("resolved")).remove("projectUrls");
        assertThat(written.toString()).doesNotContain("projectUrls");

        PythonResolutionResult read = mapper.readValue(written.toString(), PythonResolutionResult.class);

        assertThat(read.getName()).isEqualTo("my-app");
        assertThat(read.getProjectUrls()).isNull();
        assertThat(read.getResolvedDependencies()).singleElement().satisfies(requests -> {
            assertThat(requests.getVersion()).isEqualTo("2.31.0");
            assertThat(requests.getProjectUrls()).isNull();
        });
    }
}
