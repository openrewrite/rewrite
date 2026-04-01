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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;

import java.util.Map;

/**
 * A single piece of data in a tree, which can be a marker, leaf value, tree element, etc.
 */
@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE, onConstructor_ = @JsonCreator)
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

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public enum State {
        NO_CHANGE,
        ADD,
        DELETE,
        CHANGE,
        END_OF_OBJECT
    }
}
