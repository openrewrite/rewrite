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
import org.openrewrite.internal.ThrowingConsumer;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.openrewrite.rpc.Reference.asRef;
import static org.openrewrite.rpc.RpcObjectData.ADDED_LIST_ITEM;
import static org.openrewrite.rpc.RpcObjectData.State.*;

public class RpcSendQueue {
    private final int batchSize;
    private final List<RpcObjectData> batch;
    private final Consumer<List<RpcObjectData>> drain;
    private final Map<Object, Integer> refs;

    private @Nullable Object before;

    public RpcSendQueue(int batchSize, ThrowingConsumer<List<RpcObjectData>> drain, Map<Object, Integer> refs) {
        this.batchSize = batchSize;
        this.batch = new ArrayList<>(batchSize);
        this.drain = drain;
        this.refs = refs;
    }

    public void put(RpcObjectData rpcObjectData) {
        batch.add(rpcObjectData);
        if (batch.size() == batchSize) {
            flush();
        }
    }

    /**
     * Called whenever the batch size is reached or at the end of the tree.
     */
    public void flush() {
        if (batch.isEmpty()) {
            return;
        }
        drain.accept(new ArrayList<>(batch));
        batch.clear();
    }

    public <T> void sendMarkers(@Nullable T parent, Function<T, Markers> markersFn) {
        getAndSend(parent, t2 -> asRef(markersFn.apply(t2)), markersRef -> {
            Markers markers = Reference.getValue(markersRef);
            getAndSendList(markers, Markers::getMarkers, Marker::getId, null);
        });
    }

    public <T, U> void getAndSend(@Nullable T parent, Function<T, @Nullable U> value) {
        getAndSend(parent, value, null);
    }

    public <T, U> void getAndSend(@Nullable T parent, Function<T, @Nullable U> value, @Nullable Consumer<U> onChange) {
        U after = value.apply(parent);
        //noinspection unchecked
        U before = this.before == null ? null : value.apply((T) this.before);
        send(after, before, onChange == null ? null : () -> onChange.accept(after));
    }

    public <T, U> void getAndSendList(@Nullable T parent,
                                      Function<T, @Nullable List<U>> values,
                                      Function<U, ?> id,
                                      @Nullable Consumer<U> onChange) {
        List<U> after = values.apply(parent);
        //noinspection unchecked
        List<U> before = this.before == null ? null : values.apply((T) this.before);
        sendList(after, before, id, onChange);
    }

    public <T> void send(@Nullable T after, @Nullable T before, @Nullable Runnable onChange) {
        Object afterVal = Reference.getValue(after);
        Object beforeVal = Reference.getValue(before);

        if (beforeVal == afterVal) {
            put(new RpcObjectData(NO_CHANGE, null, null, null));
        } else if (beforeVal == null) {
            add(after, onChange);
        } else if (afterVal == null) {
            put(new RpcObjectData(DELETE, null, null, null));
        } else {
            put(new RpcObjectData(CHANGE, null, onChange == null ? afterVal : null, null));
            doChange(after, before, onChange);
        }
    }

    public <T> void sendList(@Nullable List<T> after,
                             @Nullable List<T> before,
                             Function<T, ?> id,
                             @Nullable Consumer<T> onChange) {
        send(after, before, () -> {
            assert after != null : "A DELETE event should have been sent.";

            Map<Object, Integer> beforeIdx = putListPositions(after, before, id);

            for (T anAfter : after) {
                Integer beforePos = beforeIdx.get(id.apply(anAfter));
                Runnable onChangeRun = onChange == null ? null : () -> onChange.accept(anAfter);
                if (beforePos == null) {
                    add(anAfter, onChangeRun);
                } else {
                    T aBefore = before == null ? null : before.get(beforePos);
                    if (aBefore == anAfter) {
                        put(new RpcObjectData(NO_CHANGE, null, null, null));
                    } else {
                        put(new RpcObjectData(CHANGE, null, null, null));
                        doChange(anAfter, aBefore, onChangeRun);
                    }
                }
            }
        });
    }

    private <T> Map<Object, Integer> putListPositions(List<T> after, @Nullable List<T> before, Function<T, ?> id) {
        Map<Object, Integer> beforeIdx = new IdentityHashMap<>();
        if (before != null) {
            for (int i = 0; i < before.size(); i++) {
                beforeIdx.put(id.apply(before.get(i)), i);
            }
        }
        List<Integer> positions = new ArrayList<>();
        for (T t : after) {
            Integer beforePos = beforeIdx.get(id.apply(t));
            positions.add(beforePos == null ? ADDED_LIST_ITEM : beforePos);
        }
        put(new RpcObjectData(CHANGE, null, positions, null));
        return beforeIdx;
    }

    private void add(@Nullable Object after, @Nullable Runnable onChange) {
        Object afterVal = Reference.getValue(after);
        Integer ref = null;
        if (afterVal != null && after != afterVal /* Is a reference */) {
            if (refs.containsKey(afterVal)) {
                put(new RpcObjectData(ADD, getValueType(afterVal), null, refs.get(afterVal)));
                // No onChange call because the remote will be using an instance from its ref cache
                return;
            }
            ref = refs.size() + 1;
            refs.put(afterVal, ref);
        }
        put(new RpcObjectData(ADD, getValueType(afterVal),
                onChange == null ? afterVal : null, ref));
        doChange(afterVal, null, onChange);
    }

    private void doChange(@Nullable Object after, @Nullable Object before, @Nullable Runnable onChange) {
        if (onChange != null) {
            Object lastBefore = this.before;
            this.before = before;
            if (after != null) {
                onChange.run();
            }
            this.before = lastBefore;
        }
    }

    private static @Nullable String getValueType(@Nullable Object after) {
        if (after == null) {
            return null;
        }
        Class<?> type = after.getClass();
        if (type.isPrimitive() || type.getPackage().getName().startsWith("java.lang") ||
            type.equals(UUID.class) || Iterable.class.isAssignableFrom(type)) {
            return null;
        }
        return type.getName();
    }
}
