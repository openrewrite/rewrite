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
package org.openrewrite.internal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.marker.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class BackwardCompatibleObjectIdModuleTest {

    /**
     * Built like {@link ObjectMappers#propertyBasedMapper}, but over the JSON (not Smile) factory so fixtures are
     * readable: property-based creator detection, {@link ParameterNamesModule}, and the module under test.
     */
    private final ObjectMapper mapper = configure(JsonMapper.builder()
      .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
      .build()
      .registerModule(new ParameterNamesModule())
      .registerModule(new BackwardCompatibleObjectIdModule()));

    private static ObjectMapper configure(ObjectMapper m) {
        // Mirror RecipeSerializer's visibility so package-private final fields participate, as for @Value LST types.
        return m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
          .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    /**
     * The "new" wire format writes the {@code @ref} id as the first property of a shared {@code @Value} type, so a
     * bounded reader can decide the encoding from the prefix alone and stream the (potentially large) remainder. The
     * old reader instead copies the entire nested subtree into a {@code TokenBuffer} before constructing anything,
     * which is the allocation churn this change removes.
     * <p>
     * Total tokens read from the source parser are identical either way — the difference is <em>when</em> they are
     * read relative to object construction. We assert that the first {@link Node} is constructed while the source
     * parser is still near the start of the stream. Against the current implementation the whole subtree is drained
     * first, so the source-token count at first construction equals the full token total and this fails.
     */
    @Test
    void newFormatObjectIsBuiltBeforeSubtreeIsBuffered() throws IOException {
        int childCount = 5000;
        Node root = largeRefFirstGraph(childCount);
        String json = mapper.writeValueAsString(root);

        Probe.reset();
        AtomicLong sourceTokens = new AtomicLong();
        Node parsed;
        try (JsonParser src = new CountingParser(mapper.getFactory().createParser(json), sourceTokens)) {
            parsed = mapper.readValue(src, Node.class);
        }

        assertThat(parsed).isEqualTo(root);
        // After the fix the root is built from the @ref prefix only; before it, the whole subtree is buffered first
        // so the count at first construction is on the order of the full token total (> childCount).
        assertThat(Probe.minSourceTokensAtBuild).isLessThan(200L);
    }

    @Test
    void readsNewFormatObjectWithIdPropertyAndResolvesBackReference() throws IOException {
        Node shared = new Node("shared", Collections.emptyList());
        String json = mapper.writeValueAsString(new Wrapper(shared, shared));

        Wrapper parsed = mapper.readValue(json, Wrapper.class);

        assertThat(parsed.first).isEqualTo(shared);
        // The second occurrence is written as a bare integer back-reference; it must resolve to the same instance.
        assertThat(parsed.first).isSameAs(parsed.second);
    }

    @Test
    void roundTripsThroughSmileWithSharedReferenceAndStreamedSubtree() throws IOException {
        // The production reader (RecipeSerializer) uses the binary Smile format, and the streaming path adds a
        // JsonParserSequence; this exercises both, plus a non-trivial subtree streamed from the live parser and a
        // bare-integer back-reference, in binary.
        ObjectMapper smile = configure(JsonMapper.builder(new SmileFactory())
          .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
          .build()
          .registerModule(new ParameterNamesModule())
          .registerModule(new BackwardCompatibleObjectIdModule()));
        Node shared = new Node("shared", largeRefFirstGraph(100).children);
        byte[] bytes = smile.writeValueAsBytes(new Wrapper(shared, shared));

        Wrapper parsed = smile.readValue(bytes, Wrapper.class);

        assertThat(parsed.first).isEqualTo(shared);
        assertThat(parsed.first).isSameAs(parsed.second);
    }

    @Test
    void readsLegacyObjectWithoutIdProperty() throws IOException {
        // A payload produced before @JsonIdentityInfo existed has no @ref anywhere; it must deserialize cleanly
        // instead of throwing "No Object Id found for an instance of ...".
        Node parsed = mapper.readValue("{\"payload\":\"x\",\"children\":[]}", Node.class);

        assertThat(parsed).isEqualTo(new Node("x", Collections.emptyList()));
    }

    @Test
    void detectsIdPropertyWhenNotFirst() throws IOException {
        // Hand-crafted so the id property follows other fields: the scan must keep going past non-id fields and still
        // register the id, so that the bare-integer back-reference resolves to the same instance.
        String json = "{\"first\":{\"payload\":\"x\",\"children\":[],\"@ref\":1},\"second\":1}";

        Wrapper parsed = mapper.readValue(json, Wrapper.class);

        assertThat(parsed.first.payload).isEqualTo("x");
        assertThat(parsed.first).isSameAs(parsed.second);
    }

    @Test
    void idPropertyIsAlwaysSerializedFirst() throws IOException {
        // Jackson emits the @JsonIdentityInfo id property immediately after START_OBJECT, before any bean field, so a
        // reader could decide the encoding from the first field alone. We deliberately do NOT rely on that (the scan in
        // deserialize() tolerates any position) because this module reads LSTs at runtime — if the invariant were ever
        // broken (a custom serializer on a @ref type, an off-spec writer), short-circuiting on the first field would
        // throw UnresolvedForwardReference and take down the whole recipe run. This test pins the invariant in both the
        // JSON and the production Smile path, so a change that moved @ref fails loudly here instead.
        assertIdPropertyFirst(mapper);
        assertIdPropertyFirst(configure(JsonMapper.builder(new SmileFactory())
          .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
          .build()
          .registerModule(new ParameterNamesModule())
          .registerModule(new BackwardCompatibleObjectIdModule())));
    }

    private static void assertIdPropertyFirst(ObjectMapper m) throws IOException {
        byte[] bytes = m.writeValueAsBytes(new Node("payload", Collections.emptyList()));
        try (JsonParser p = m.getFactory().createParser(bytes)) {
            assertThat(p.nextToken()).isEqualTo(JsonToken.START_OBJECT);
            assertThat(p.nextToken()).isEqualTo(JsonToken.FIELD_NAME);
            assertThat(p.currentName()).isEqualTo("@ref");
        }
    }

    @Test
    void markerTypesRoundTripWithTheModuleInstalled() throws IOException {
        // Markers are deserialized polymorphically (see @JsonTypeInfo on Marker) and are excluded from this module's
        // wrapping. The exclusion has no observable behavioral consequence (polymorphic dispatch means a wrapped
        // marker deserializer would never see START_OBJECT anyway); this guards that the module's presence does not
        // otherwise disturb a marker's normal round-trip.
        MarkerNode marker = new MarkerNode(UUID.randomUUID(), "p");
        String json = mapper.writeValueAsString(marker);

        MarkerNode parsed = mapper.readValue(json, MarkerNode.class);

        assertThat(parsed).isEqualTo(marker);
    }

    private static Node largeRefFirstGraph(int childCount) {
        List<Node> children = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            children.add(new Node("child-" + i, Collections.emptyList()));
        }
        return new Node("root", children);
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    static class Node {
        final String payload;
        final List<Node> children;

        @JsonCreator
        Node(@JsonProperty("payload") String payload,
             @JsonProperty("children") @Nullable List<Node> children) {
            this.payload = payload;
            this.children = children == null ? Collections.emptyList() : children;
            Probe.recordBuild();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Node)) {
                return false;
            }
            Node node = (Node) o;
            return Objects.equals(payload, node.payload) && children.equals(node.children);
        }

        @Override
        public int hashCode() {
            return Objects.hash(payload, children.size());
        }
    }

    /** Holds two references so that a shared {@link Node} is written as a full object then a bare-integer back-ref. */
    static class Wrapper {
        final Node first;
        final Node second;

        @JsonCreator
        Wrapper(@JsonProperty("first") Node first, @JsonProperty("second") Node second) {
            this.first = first;
            this.second = second;
        }
    }

    /** A {@link Marker} (hence polymorphic, via the {@code @JsonTypeInfo} on the interface) with a property creator. */
    static class MarkerNode implements Marker {
        final UUID id;
        final String payload;

        @JsonCreator
        MarkerNode(@JsonProperty("id") UUID id, @JsonProperty("payload") String payload) {
            this.id = id;
            this.payload = payload;
        }

        @Override
        public UUID getId() {
            return id;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <M extends Marker> M withId(UUID id) {
            return (M) new MarkerNode(id, payload);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MarkerNode)) {
                return false;
            }
            MarkerNode that = (MarkerNode) o;
            return Objects.equals(id, that.id) && Objects.equals(payload, that.payload);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, payload);
        }
    }

    /**
     * Captures the source-parser token count at the moment each {@link Node} is constructed, keeping the minimum —
     * i.e. the count when the first (shallowest) node is built.
     */
    static final class Probe {
        static AtomicLong sourceTokens = new AtomicLong();
        static volatile long minSourceTokensAtBuild = Long.MAX_VALUE;

        static void reset() {
            sourceTokens = new AtomicLong();
            minSourceTokensAtBuild = Long.MAX_VALUE;
        }

        static void recordBuild() {
            long now = sourceTokens.get();
            if (now < minSourceTokensAtBuild) {
                minSourceTokensAtBuild = now;
            }
        }
    }

    /**
     * Counts {@link #nextToken()} calls on the wrapped source parser into both the shared {@link Probe} counter and a
     * caller-supplied counter, so a test can observe how far the source has been drained at any point.
     */
    private static final class CountingParser extends JsonParserDelegate {
        private final AtomicLong count;

        CountingParser(JsonParser delegate, AtomicLong count) {
            super(delegate);
            this.count = count;
        }

        @Override
        public JsonToken nextToken() throws IOException {
            JsonToken t = super.nextToken();
            count.incrementAndGet();
            Probe.sourceTokens.incrementAndGet();
            return t;
        }
    }
}
