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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;

import java.io.IOException;

/**
 * Bridges the two object-id wire formats so that a reader can deserialize representations written both before and after
 * {@code @JsonIdentityInfo} was added to a type.
 * <p>
 * {@code @JsonIdentityInfo(generator = IntSequenceGenerator.class, property = "@ref")} lets a writer deduplicate an
 * instance that is reachable by reference more than once: the first occurrence is written in full and tagged with a
 * {@code @ref} id, and later occurrences are written as a bare integer back-reference. This is a worthwhile size
 * optimization on large dependency graphs, but it changes the wire format. The problem is asymmetric on the read side:
 * for an immutable {@code @Value} type the constructor is a property-based creator, and Jackson's
 * {@code PropertyValueBuffer} throws {@code "No Object Id found for an instance of ..."} <em>unconditionally</em> when
 * the {@code @ref} property is absent — there is no {@code DeserializationFeature} that relaxes it. So a reader built
 * after the annotation was introduced cannot deserialize an older payload that was produced before it existed (no
 * {@code @ref} anywhere), nor one produced by a writer that simply never shared the instance.
 * <p>
 * This module wraps the bean deserializer for any non-{@link Marker} type that carries an object-id reader so that all
 * three forms read correctly, regardless of which version produced them:
 * <ul>
 *     <li>a bare integer is resolved as a back-reference by the id-aware delegate (newer payloads);</li>
 *     <li>a full object <em>with</em> the id property is read by the id-aware delegate, registering the id so later
 *         back-references resolve (newer payloads);</li>
 *     <li>a full object <em>without</em> the id property is read by an id-free delegate, so older/un-deduplicated
 *         payloads deserialize cleanly instead of throwing.</li>
 * </ul>
 * The wire format produced by writers is unchanged — only the read path learns to accept both encodings — so this is
 * safe to deploy ahead of, or behind, writers that do or do not emit {@code @ref}.
 * <p>
 * {@link Marker} types are intentionally excluded for two reasons. First, they are deserialized polymorphically (the
 * concrete type is resolved from a type-id property mid-object), which does not compose with the buffer-and-replay
 * approach used here. Second, they do not need it: marker types that carry the annotation (e.g.
 * {@code MavenResolutionResult}) are built through a no-argument {@code @JsonCreator}, i.e. the default-creator path,
 * which already tolerates a missing id rather than throwing — so older marker payloads deserialize without help.
 */
public class BackwardCompatibleObjectIdModule extends SimpleModule {

    public BackwardCompatibleObjectIdModule() {
        super("BackwardCompatibleObjectIdModule");
        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                if (deserializer instanceof BeanDeserializerBase &&
                    deserializer.getObjectIdReader() != null &&
                    !Marker.class.isAssignableFrom(beanDesc.getBeanClass())) {
                    return new BackwardCompatibleObjectIdDeserializer((BeanDeserializerBase) deserializer);
                }
                return deserializer;
            }
        });
    }

    private static class BackwardCompatibleObjectIdDeserializer extends DelegatingDeserializer {
        private final String idPropertyName;
        private transient volatile @Nullable JsonDeserializer<Object> withoutId;

        BackwardCompatibleObjectIdDeserializer(BeanDeserializerBase delegate) {
            super(delegate);
            this.idPropertyName = delegate.getObjectIdReader().propertyName.getSimpleName();
        }

        @Override
        protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
            return new BackwardCompatibleObjectIdDeserializer((BeanDeserializerBase) newDelegatee);
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.START_OBJECT) {
                // Buffer the object so we can peek for the id property, then replay it into the appropriate delegate.
                TokenBuffer buffer = new TokenBuffer(p, ctxt);
                buffer.copyCurrentStructure(p);
                boolean hasId = containsField(buffer, p, idPropertyName);
                JsonParser replay = buffer.asParser(p);
                replay.nextToken();
                return hasId ? _delegatee.deserialize(replay, ctxt) : withoutId().deserialize(replay, ctxt);
            }
            // A bare integer back-reference (or null): the id-aware delegate resolves it as it always has.
            return _delegatee.deserialize(p, ctxt);
        }

        private static boolean containsField(TokenBuffer buffer, JsonParser src, String field) throws IOException {
            try (JsonParser scan = buffer.asParser(src)) {
                scan.nextToken(); // position at START_OBJECT
                while (scan.nextToken() == JsonToken.FIELD_NAME) {
                    boolean match = field.equals(scan.currentName());
                    scan.nextToken();
                    if (match) {
                        return true;
                    }
                    scan.skipChildren();
                }
                return false;
            }
        }

        private JsonDeserializer<Object> withoutId() {
            JsonDeserializer<Object> w = withoutId;
            if (w == null) {
                // Same bean deserializer, minus object-id handling: reads a full object and tolerates a missing id.
                w = ((BeanDeserializerBase) _delegatee).withObjectIdReader(null);
                withoutId = w;
            }
            return w;
        }
    }
}
