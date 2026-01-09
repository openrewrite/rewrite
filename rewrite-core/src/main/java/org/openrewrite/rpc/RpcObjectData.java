/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;

import java.io.IOException;
import java.util.Map;

/**
 * A single piece of data in a tree, which can be a marker, leaf value, tree element, etc.
 * <p>
 * Serialized as a compact array: [state, valueType, value, ref?, trace?]
 * - 3 elements when ref is null and trace is null
 * - 4 elements when ref is present but trace is null
 * - 5 elements when trace is present
 */
@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonSerialize(using = RpcObjectData.Serializer.class)
@JsonDeserialize(using = RpcObjectData.Deserializer.class)
public class RpcObjectData {
    private static final ObjectMapper mapper = JsonMapper.builder()
            // to be able to construct classes that have @Data and a single field
            // see https://cowtowncoder.medium.com/jackson-2-12-most-wanted-3-5-246624e2d3d0
            .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
            .build()
            .registerModules(new ParameterNamesModule(), new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final int ADDED_LIST_ITEM = -1;

    State state;

    /**
     * Used to construct a new instance of the class with
     * initially only the ID populated. Subsequent {@link RpcObjectData}
     * messages will fill in the object fully.
     */
    @Nullable
    String valueType;

    /**
     * Not always a {@link Tree}. This can be a marker or leaf element
     * value element of a tree as well. At any rate, it's a part of
     * the data modeled by a {@link Tree}.
     * <p>
     * In the case of a {@link Tree} ADD, this is the tree ID.
     */
    @Nullable
    Object value;

    /**
     * Used for instances that should be referentially equal in multiple parts
     * of the tree (e.g. Space, some Marker types, JavaType). The first time the
     * object is seen, it is transmitted with a ref ID. Subsequent references
     * to the same object are transmitted with the ref ID and no value.
     */
    @Nullable
    Integer ref;

    /**
     * The stack trace of the thread that created this object. This is
     * useful in debugging asymmetries between senders/receivers.
     */
    @Nullable
    String trace;

    public RpcObjectData(State state, @Nullable String valueType, @Nullable Object value, @Nullable Integer ref, boolean trace) {
        this.state = state;
        this.valueType = valueType;
        this.value = value;
        this.ref = ref;
        this.trace = trace ? Trace.traceSender() : null;
    }

    public RpcObjectData withoutTrace() {
        return new RpcObjectData(state, valueType, value, ref, false);
    }

    public <V> @Nullable V getValue() {
        if (value instanceof Map && valueType != null) {
            try {
                Class<?> valueClass = Class.forName(valueType);

                // While we know exactly what type of value we are converting to,
                // Jackson will still require the '@c' field in the map when the type
                // we are converting to is annotated with @JsonTypeInfo.
                //noinspection unchecked
                ((Map<String, Object>) value).put("@c", valueType);
                //noinspection unchecked
                ((Map<String, Object>) value).put("@ref", 1);

                //noinspection unchecked
                return (V) mapper.convertValue(value, valueClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // noinspection unchecked
        return (V) value;
    }

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public enum State {
        NO_CHANGE,
        ADD,
        DELETE,
        CHANGE,
        END_OF_OBJECT;

        public static State from(int ordinal) {
            switch (ordinal) {
                case 0: return NO_CHANGE;
                case 1: return ADD;
                case 2: return DELETE;
                case 3: return CHANGE;
                case 4: return END_OF_OBJECT;
                default: throw new IllegalArgumentException("Unknown state ordinal: " + ordinal);
            }
        }
    }

    /**
     * Custom serializer that outputs RpcObjectData as a compact array.
     * Format: [state, valueType, value, ref?, trace?]
     */
    static class Serializer extends JsonSerializer<RpcObjectData> {
        @Override
        public void serialize(RpcObjectData data, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray(data, data.trace != null ? 5 : data.ref != null ? 4 : 3);
            gen.writeNumber(data.state.ordinal());
            gen.writeString(data.valueType);
            gen.writeObject(data.value);
            if (data.ref != null || data.trace != null) {
                if (data.ref != null) {
                    gen.writeNumber(data.ref);
                } else {
                    gen.writeNull();
                }
                if (data.trace != null) {
                    gen.writeString(data.trace);
                }
            }
            gen.writeEndArray();
        }
    }

    /**
     * Custom deserializer that reads RpcObjectData from a compact array.
     * Format: [state, valueType, value, ref?, trace?]
     */
    static class Deserializer extends JsonDeserializer<RpcObjectData> {
        @Override
        public RpcObjectData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() != JsonToken.START_ARRAY) {
                throw new IOException("Expected array start, got: " + p.currentToken());
            }

            // Read state (required)
            p.nextToken();
            State state = State.from(p.getIntValue());

            // Read valueType (may be null)
            p.nextToken();
            String valueType = p.currentToken() == JsonToken.VALUE_NULL ? null : p.getText();

            // Read value (may be null or any type)
            p.nextToken();
            Object value = p.currentToken() == JsonToken.VALUE_NULL ? null : p.readValueAs(Object.class);

            // Read optional ref (may be null or not present)
            Integer ref = null;
            String trace = null;

            if (p.nextToken() != JsonToken.END_ARRAY) {
                ref = p.currentToken() == JsonToken.VALUE_NULL ? null : p.getIntValue();

                // Read optional trace
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    trace = p.currentToken() == JsonToken.VALUE_NULL ? null : p.getText();
                    p.nextToken(); // consume END_ARRAY
                }
            }

            return new RpcObjectData(state, valueType, value, ref, trace);
        }
    }
}
