/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.marker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Markers must round-trip through a field-based Jackson {@link ObjectMapper} (the same shape
 * exercised by {@code AndroidProjectSerializationTest}). {@link NullSafe} declares hand-written
 * constructors, so — unlike the Lombok-based markers, whose generated {@code @ConstructorProperties}
 * gives Jackson a property-based creator — it needs an explicit creator. Without one,
 * deserialization fails with "Cannot construct instance of NullSafe (no Creators ...)", even with
 * {@link ParameterNamesModule} and properties-based constructor detection enabled.
 */
class NullSafeSerializationTest {

    private final ObjectMapper mapper = newMapper();

    private static ObjectMapper newMapper() {
        ObjectMapper m = JsonMapper.builder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .build()
                .registerModule(new ParameterNamesModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    @Test
    void roundTripEmptyDotPrefix() throws Exception {
        NullSafe original = new NullSafe(UUID.randomUUID(), Space.EMPTY);

        String json = mapper.writeValueAsString(original);
        NullSafe restored = mapper.readValue(json, NullSafe.class);

        assertThat(restored).isEqualTo(original);
        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getDotPrefix()).isEqualTo(Space.EMPTY);
    }

    @Test
    void roundTripWhitespaceAndCommentDotPrefix() throws Exception {
        Space dotPrefix = Space.build("\n    ",
                singletonList(new TextComment(false, " comment ", "", Markers.EMPTY)));
        NullSafe original = new NullSafe(UUID.randomUUID(), dotPrefix);

        String json = mapper.writeValueAsString(original);
        NullSafe restored = mapper.readValue(json, NullSafe.class);

        assertThat(restored).isEqualTo(original);
        assertThat(restored.getDotPrefix().getWhitespace()).isEqualTo("\n    ");
        assertThat(restored.getDotPrefix().getComments()).hasSize(1);
    }
}
