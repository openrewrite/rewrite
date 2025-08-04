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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.jspecify.annotations.Nullable;
import org.objenesis.ObjenesisStd;
import org.openrewrite.marker.Markers;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public class RpcReceiveQueue {
    private static final ObjenesisStd objenesis = new ObjenesisStd();
    private static final LoadingCache<String, Object> instanceCache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .build((String key) -> {
                try {
                    Class<?> clazz = Class.forName(key);
                    return objenesis.newInstance(clazz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

    private final Deque<RpcObjectData> batch;
    private final Map<Integer, Object> refs;
    private final @Nullable PrintStream logFile;
    private final Supplier<List<RpcObjectData>> pull;
    private final Function<Integer, Object> getRef;

    public RpcReceiveQueue(Map<Integer, Object> refs, @Nullable PrintStream logFile,
                           Supplier<List<RpcObjectData>> pull, Function<Integer, Object> getRef) {
        this.refs = refs;
        this.batch = new ArrayDeque<>();
        this.logFile = logFile;
        this.pull = pull;
        this.getRef = getRef;
    }

    public RpcObjectData take() {
        if (batch.isEmpty()) {
            List<RpcObjectData> data = pull.get();
            batch.addAll(data);
        }
        return batch.remove();
    }

    /**
     * Receive a value from the queue and apply a function to it to convert it to the required
     * type.
     *
     * @param before  The value to apply the function to, which may be null.
     * @param mapping Function to apply in case of ADD or CHANGE to convert the received value to
     *                the desired type. Only applied when the value from the queue is not null.
     * @param <T>     The property value type of the before and returned value.
     * @param <U>     The type of the value as encoded by the data item read from the queue.
     * @return The received and converted value. When the received state is NO_CHANGE then the
     * before value will be returned.
     */
    @SuppressWarnings({"DataFlowIssue", "unchecked"})
    public <T, U> T receiveAndGet(@Nullable T before, Function<U, @Nullable T> mapping) {
        T after = receive(before, null);
        return after != null && after != before ? mapping.apply((U) after) : after;
    }

    public Markers receiveMarkers(Markers markers) {
        return receive(markers, m -> m
                .withId(receiveAndGet(m.getId(), UUID::fromString))
                .withMarkers(receiveList(m.getMarkers(), null)));
    }

    /**
     * Receive a simple value from the remote.
     *
     * @param before The before state.
     * @param <T>    The type of the value being received.
     * @return The received value.
     */
    public <T> T receive(@Nullable T before) {
        return receive(before, null);
    }

    /**
     * Receive a value from the remote and, when it is an ADD or CHANGE, invoke a callback
     * to receive its constituent parts.
     *
     * @param before   The before state.
     * @param onChange When the state is ADD or CHANGE, this function is called to receive the
     *                 pieces of this value. If the callback is null, the value is assumed to
     *                 be in the value part of the message and is deserialized directly.
     * @param <T>      The type of the value being received.
     * @return The received value.
     */
    @SuppressWarnings("DataFlowIssue")
    public <T> @Nullable T receive(@Nullable T before, @Nullable UnaryOperator<T> onChange) {
        RpcObjectData message = take();

        if (logFile != null && message.getTrace() != null) {
            logFile.println(message.withTrace(null));
            logFile.println("  " + message.getTrace());
            logFile.println("  " + Trace.traceReceiver());
            logFile.flush();
        }
        Integer ref = null;
        switch (message.getState()) {
            case NO_CHANGE:
                return before;
            case DELETE:
                return null;
            case ADD:
                ref = message.getRef();
                if (ref != null && message.getValueType() == null && message.getValue() == null) {
                    // This is a pure reference to an existing object
                    if (refs.containsKey(ref)) {
                        //noinspection unchecked
                        return (T) refs.get(ref);
                    } else {
                        // Ref was evicted from cache, fetch it
                        Object refObject = getRef.apply(ref);
                        refs.put(ref, refObject);
                        //noinspection unchecked
                        return (T) refObject;
                    }
                } else {
                    // This is either a new object or a forward declaration with ref
                    before = message.getValueType() == null ?
                            message.getValue() :
                            newObj(message.getValueType());
                }
                // Intentional fall-through...
            case CHANGE:
                T after;

                // TODO handle enums here

                if (onChange != null) {
                    after = onChange.apply(before);
                } else if (before instanceof RpcCodec) {
                    //noinspection unchecked
                    after = (T) ((RpcCodec<Object>) before).rpcReceive(before, this);
                } else if (message.getValueType() == null) {
                    after = message.getValue();
                } else {
                    after = before;
                }
                if (ref != null) {
                    refs.put(ref, after);
                }
                return after;
            default:
                throw new UnsupportedOperationException("Unknown state type " + message.getState());
        }
    }

    public <T> @Nullable List<T> receiveList(@Nullable List<T> before, @Nullable UnaryOperator<T> onChange) {
        RpcObjectData msg = take();
        switch (msg.getState()) {
            case NO_CHANGE:
                return before;
            case DELETE:
                return null;
            case ADD:
                before = new ArrayList<>();
                // Intentional fall-through...
            case CHANGE:
                msg = take(); // the next message should be a CHANGE with a list of positions
                assert msg.getState() == RpcObjectData.State.CHANGE;
                List<Integer> positions = requireNonNull(msg.getValue());
                List<T> after = new ArrayList<>(positions.size());
                for (int beforeIdx : positions) {
                    after.add(receive(beforeIdx >= 0 ? requireNonNull(before).get(beforeIdx) : null, onChange));
                }
                return after;
            default:
                throw new UnsupportedOperationException(msg.getState() + " is not supported for lists.");
        }
    }

    private <T> T newObj(String type) {
        //noinspection unchecked
        return (T) requireNonNull(instanceCache.get(type));
    }

    /**
     * @param enumType The enumeration that we are creating or updating
     * @param <T>      The enum type.
     * @return An enum mapping function that can be used when receiving a string to convert
     * it to an enum value.
     */
    public static <T extends Enum<T>> Function<Object, T> toEnum(Class<T> enumType) {
        return value -> Enum.valueOf(enumType, (String) value);
    }
}
