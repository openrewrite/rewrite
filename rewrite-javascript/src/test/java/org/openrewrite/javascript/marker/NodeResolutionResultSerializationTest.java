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
package org.openrewrite.javascript.marker;

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
import org.openrewrite.javascript.internal.LockFileParser;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;

import java.util.ArrayList;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A resolved npm graph is cyclic (peer dependencies point back at their dependents), so the marker has to survive
 * both a Jackson round trip and the {@code equals}/{@code hashCode} of any collection that holds it.
 */
class NodeResolutionResultSerializationTest {

    private static final String CYCLIC_LOCK = "{\n" +
                                              "  \"packages\": {\n" +
                                              "    \"\": { },\n" +
                                              "    \"node_modules/a\": { \"version\": \"1.0.0\", \"dependencies\": { \"b\": \"^1.0.0\" } },\n" +
                                              "    \"node_modules/b\": { \"version\": \"1.0.0\", \"peerDependencies\": { \"a\": \"^1.0.0\" } }\n" +
                                              "  }\n" +
                                              "}";

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

    private static NodeResolutionResult cyclicMarker() {
        LockFileParser.ParseResult result = LockFileParser.parse(CYCLIC_LOCK);
        return new NodeResolutionResult(UUID.randomUUID(), "my-app", "1.0.0", null, "package.json", null,
                singletonList(new Dependency("a", "^1.0.0", result.getTopLevel().get("a"))),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(result.getAll()),
                null, null, null);
    }

    @Test
    void roundTripCyclicGraph() throws Exception {
        ObjectMapper mapper = mapper();
        NodeResolutionResult read = mapper.readValue(mapper.writeValueAsString(cyclicMarker()), NodeResolutionResult.class);

        ResolvedDependency a = read.getDependencies().get(0).getResolved();
        assertThat(a).isNotNull();
        ResolvedDependency b = a.getDependencies().get(0).getResolved();
        assertThat(b).isNotNull();
        assertThat(b.getPeerDependencies().get(0).getResolved())
                .as("the back-reference closing the cycle must resolve to the same instance")
                .isSameAs(a);
    }

    @Test
    void equalsAndHashCodeTerminateOnCyclicGraph() {
        ResolvedDependency a = LockFileParser.parse(CYCLIC_LOCK).getTopLevel().get("a");

        assertThat(a.hashCode()).isEqualTo(a.hashCode());
        assertThat(a).isEqualTo(LockFileParser.parse(CYCLIC_LOCK).getTopLevel().get("a"));
    }
}
