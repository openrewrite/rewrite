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

import org.jspecify.annotations.Nullable;
import org.objenesis.ObjenesisStd;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public class RpcReceiveQueue {
    private final List<RpcObjectData> batch;
    private final Map<Integer, Object> refs;
    private final Supplier<List<RpcObjectData>> pull;

    public RpcReceiveQueue(Map<Integer, Object> refs, Supplier<List<RpcObjectData>> pull) {
        this.refs = refs;
        this.batch = new ArrayList<>();
        this.pull = pull;
    }

    public RpcObjectData take() {
        if (batch.isEmpty()) {
            List<RpcObjectData> data = pull.get();
            batch.addAll(data);
        }
        return batch.remove(0);
    }

    /**
     * Receive a value from the queue and apply a function to it, usually to
     * convert it to a string or fetch some nested object off of it.
     *
     * @param before The value to apply the function to, which may be null.
     * @param apply  A function that is called only when before is non-null.
     * @param <T>    A before value ahead of the function call.
     * @param <U>    The return type of the function. This will match the type that is
     *               being received from the remote.
     * @return The received value. To set the correct before state when the received state
     * is NO_CHANGE or CHANGE, the function is applied to the before parameter, unless before
     * is null in which case the before state is assumed to be null.
     */
    public <T, U> U receiveAndGet(@Nullable T before, Function<T, U> apply) {
        return receive(before == null ? null : apply.apply(before), null);
    }

    public Markers receiveMarkers(Markers markers) {
        return receive(markers, m -> m.withMarkers(
                receiveList(m.getMarkers(), null)));
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
    public <T> T receive(@Nullable T before, @Nullable UnaryOperator<T> onChange) {
        RpcObjectData message = take();
        Integer ref = null;
        switch (message.getState()) {
            case NO_CHANGE:
                return before;
            case DELETE:
                return null;
            case ADD:
                ref = message.getRef();
                if (refs.containsKey(ref)) {
                    //noinspection unchecked
                    return (T) refs.get(ref);
                }
                before = onChange == null || message.getValueType() == null ?
                        message.getValue() :
                        newObj(message.getValueType());
                // Intentional fall-through...
            case CHANGE:
                T after = onChange == null ? message.getValue() : onChange.apply(before);
                if (ref != null) {
                    refs.put(ref, after);
                }
                return after;
            default:
                throw new UnsupportedOperationException("Unknown state type " + message.getState());
        }
    }

    public <T> List<T> receiveList(@Nullable List<T> before, @Nullable UnaryOperator<T> onChange) {
        RpcObjectData msg = take();
        switch (msg.getState()) {
            case NO_CHANGE:
                //noinspection DataFlowIssue
                return before;
            case DELETE:
                //noinspection DataFlowIssue
                return null;
            case ADD:
                before = new ArrayList<>();
                // Intentional fall-through...
            case CHANGE:
                msg = take(); // the next message should be a CHANGE with a list of positions
                assert msg.getState() == RpcObjectData.State.CHANGE;
                List<Integer> positions = msg.getValue();
                List<T> after = new ArrayList<>(positions.size());
                for (int beforeIdx : positions) {
                    after.add(receive(beforeIdx >= 0 ? requireNonNull(before).get(beforeIdx) : null, onChange));
                }
                return after;
            default:
                throw new UnsupportedOperationException(msg.getState() + " is not supported for lists.");
        }
    }

    private static <T> T newObj(String type) {
        try {
            Class<?> clazz = Class.forName(type);
            //noinspection unchecked
            return (T) new ObjenesisStd().newInstance(clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
