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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moderne.jsonrpc.JsonRpcSuccess;
import io.moderne.jsonrpc.RawJson;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Marker;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AccessMode;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.rpc.RpcObjectData.State.ADD;

class RpcSendQueueTest {

    @Test
    void sendList() throws Exception {
        List<String> before = List.of("A", "B", "C", "D");
        List<String> after = List.of("A", "E", "F", "C");

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.CHANGE, null, null, null, false),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(0, -1, -1, 2), null, false),
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null, false) /* A */,
              new RpcObjectData(RpcObjectData.State.ADD, null, "E", null, false),
              new RpcObjectData(RpcObjectData.State.ADD, null, "F", null, false),
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null, false) /* C */
            );
            latch.countDown();
        }, new IdentityHashMap<>(), null, false);

        q.sendList(after, before, Function.identity(), null, false);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void sendEnum() throws Exception {
        List<AccessMode> before = List.of(AccessMode.READ);
        List<AccessMode> after = List.of(AccessMode.READ, AccessMode.WRITE);

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.CHANGE, null, null, null, false),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(0, -1), null, false),
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null, false) /* READ */,
              new RpcObjectData(RpcObjectData.State.ADD, null, AccessMode.WRITE, null, false)
            );
            latch.countDown();
        }, new IdentityHashMap<>(), null, false);

        q.sendList(after, before, Function.identity(), null, false);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void changedListElementWithoutCodecIsInlined() throws Exception {
        BuildTool before = new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.0");
        BuildTool after = before.withVersion("8.0");

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.CHANGE, null, null, null, false),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(0), null, false),
              // codec-less elements carry their value inline on CHANGE
              new RpcObjectData(RpcObjectData.State.CHANGE, BuildTool.class.getName(), after, null, false)
            );
            latch.countDown();
        }, new IdentityHashMap<>(), null, false);

        q.sendList(List.of(after), List.of(before), Marker::getId, null, false);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void changedRefSlotIsReAddedInsteadOfChanged() {
        List<RpcObjectData> sent = new ArrayList<>();
        RpcSendQueue q = new RpcSendQueue(100, sent::addAll, new IdentityHashMap<>(), null, false);

        String t1 = "T1";
        String t2 = "T2";

        q.send(Reference.asRef(t1), null, null);
        q.send(Reference.asRef(t2), Reference.asRef(t1), null);
        // A repeat of the same transition dedups against the ref registered by the re-add
        q.send(Reference.asRef(t2), Reference.asRef(t1), null);
        q.flush();

        assertThat(sent).containsExactly(
          new RpcObjectData(ADD, null, "T1", 1, false),
          new RpcObjectData(ADD, null, "T2", 2, false),
          new RpcObjectData(ADD, null, null, 2, false)
        );
    }

    @Test
    void deletedRefSlotIsSent() {
        RefSlotDiff diff = diffRefSlot("java.util.List", null);
        assertThat(diff.data())
          .extracting(RpcObjectData::getState)
          .containsExactly(RpcObjectData.State.CHANGE, RpcObjectData.State.DELETE);
        assertThat(diff.consumed()).isNull();
    }

    @Test
    void identicalRefSlotIsNoChange() {
        Object type = new Object();
        RefSlotDiff diff = diffRefSlot(type, type);
        assertThat(diff.data())
          .extracting(RpcObjectData::getState)
          .containsExactly(RpcObjectData.State.CHANGE, RpcObjectData.State.NO_CHANGE);
        assertThat(diff.consumed()).isNull();
    }

    @Test
    void changedRefSlotOfDifferentClassIsAdded() {
        RefSlotDiff diff = diffRefSlot(1, "java.util.ArrayList");
        assertThat(diff.data())
          .extracting(RpcObjectData::getState)
          .containsExactly(RpcObjectData.State.CHANGE, RpcObjectData.State.ADD);
        assertThat(diff.consumed()).isEqualTo("java.util.ArrayList");
    }

    /**
     * Diffs one ref-wrapped slot between a before and an after holder. The slot is emitted
     * from within the holders' own diff (send &rarr; onChange &rarr; getAndSend), which is what
     * seeds the queue's before context; a bare getAndSend would diff against null and always ADD.
     */
    private RefSlotDiff diffRefSlot(@Nullable Object beforeValue, @Nullable Object afterValue) {
        List<RpcObjectData> data = new ArrayList<>();
        RpcSendQueue q = new RpcSendQueue(100, data::addAll, new IdentityHashMap<>(), null, false);
        Holder after = new Holder(afterValue);
        AtomicReference<Object> consumed = new AtomicReference<>();
        q.send(after, new Holder(beforeValue), () ->
          q.getAndSend(after, h -> Reference.asRef(h.value), v -> consumed.set(Reference.getValue(v))));
        q.flush();
        return new RefSlotDiff(data, consumed.get());
    }

    private record RefSlotDiff(List<RpcObjectData> data, @Nullable Object consumed) {
    }

    private record Holder(@Nullable Object value) {
    }

    @Test
    void listImplementationClassMayDiffer() {
        List<String> before = new ArrayList<>(List.of("A", "B"));
        List<String> after = List.of("A");

        assertThat(roundTripList(after, before)).isEqualTo(after);
    }

    @Test
    void listImplementationClassMayDifferReverse() {
        List<String> before = List.of("A");
        List<String> after = new ArrayList<>(List.of("A", "B"));

        assertThat(roundTripList(after, before)).isEqualTo(after);
    }

    /**
     * The positions array is what lets a reorder cost one integer per element instead of
     * re-sending the elements themselves; an event stream without a move event cannot.
     */
    @Test
    void reorderedElementsAreRepositionedNotResent() throws Exception {
        List<String> before = List.of("A", "B", "C");
        List<String> after = List.of("C", "A", "B");

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.CHANGE, null, null, null, false),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(2, 0, 1), null, false),
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null, false) /* C */,
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null, false) /* A */,
              new RpcObjectData(RpcObjectData.State.NO_CHANGE, null, null, null, false) /* B */
            );
            latch.countDown();
        }, new IdentityHashMap<>(), null, false);

        q.sendList(after, before, Function.identity(), null, false);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(roundTripList(after, before)).isEqualTo(after);
    }

    @Test
    void everyElementIsAddedWhenTheBeforeListIsEmpty() throws Exception {
        List<String> before = List.of();
        List<String> after = List.of("A", "B");

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.CHANGE, null, null, null, false),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(-1, -1), null, false),
              new RpcObjectData(ADD, null, "A", null, false),
              new RpcObjectData(ADD, null, "B", null, false)
            );
            latch.countDown();
        }, new IdentityHashMap<>(), null, false);

        q.sendList(after, before, Function.identity(), null, false);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(roundTripList(after, before)).isEqualTo(after);
    }

    @Test
    void nullElementsRoundTrip() {
        List<@Nullable String> after = Arrays.asList(null, "A", null);

        assertThat(roundTripList(after, List.of())).isEqualTo(after);
        assertThat(roundTripList(after, List.of("B", "A"))).isEqualTo(after);
    }

    @Test
    void elementReplacedByNullRoundTrips() {
        List<@Nullable String> before = List.of("A");
        List<@Nullable String> after = Collections.singletonList(null);

        assertThat(roundTripList(after, before, s -> "same")).isEqualTo(after);
    }

    @Test
    void markerWithoutAClassIsWrittenAsItArrived() throws Exception {
        Map<String, Object> arrived = new HashMap<>();
        arrived.put("id", Tree.randomId().toString());
        arrived.put("tool", "example");
        RpcMarker held = new RpcObjectData(ADD, RpcMarker.class.getName(), new HashMap<>(arrived), null, false).getValue();

        Map<String, Object> written = onTheWire(held).get(0);
        assertThat(written).containsAllEntriesOf(arrived).doesNotContainKey("data");
        assertThat(new RpcObjectData(ADD, RpcMarker.class.getName(), written, null, false).<RpcMarker>getValue())
          .isEqualTo(held);
    }

    @Test
    void markerWithoutAClassIsReadFromTheShapeItWasOnceStoredIn() {
        Map<String, Object> stored = new HashMap<>();
        stored.put("id", Tree.randomId().toString());
        stored.put("data", new HashMap<>(Map.of("tool", "example")));

        RpcMarker held = new RpcObjectData(ADD, RpcMarker.class.getName(), stored, null, false).getValue();
        assertThat(held.getData()).isEqualTo(Map.of("tool", "example"));
    }

    @Test
    void markersInOneBatchEachCarryTheStyleTheyShare() throws Exception {
        GeneralFormatStyle shared = new GeneralFormatStyle(false);
        List<Map<String, Object>> written = onTheWire(
          new NamedStyles(Tree.randomId(), "a", "a", "a", Set.of(), List.of(shared)),
          new NamedStyles(Tree.randomId(), "b", "b", "b", Set.of(), List.of(shared)));

        for (Map<String, Object> value : written) {
            NamedStyles read = new RpcObjectData(ADD, NamedStyles.class.getName(), value, null, false).getValue();
            assertThat(read.getStyles()).containsExactly(shared);
        }
    }

    /**
     * The values of one {@code GetObject} response, as its JSON holds them.
     */
    private static List<Map<String, Object>> onTheWire(Object... values) throws IOException {
        List<RpcObjectData> batch = new ArrayList<>();
        for (Object value : values) {
            batch.add(new RpcObjectData(ADD, value.getClass().getName(), value, null, false));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonMessageFormatter().serialize(new JsonRpcSuccess(1, RawJson.of(batch)), out);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> written = new ArrayList<>();
        for (JsonNode data : mapper.readTree(out.toByteArray()).get("result")) {
            written.add(mapper.convertValue(data.get("value"), new TypeReference<Map<String, Object>>() {
            }));
        }
        return written;
    }

    private List<String> roundTripList(List<String> after, List<String> before) {
        return roundTripList(after, before, Function.identity());
    }

    private List<String> roundTripList(List<String> after, List<String> before, Function<String, ?> id) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1, batches::addLast, new IdentityHashMap<>(), null, false);
        RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, null, null);

        sq.sendList(after, before, id, null, false);
        sq.flush();
        return rq.receiveList(before, null);
    }

    @Test
    void emptyList() throws Exception {
        List<String> after = List.of();

        CountDownLatch latch = new CountDownLatch(1);
        RpcSendQueue q = new RpcSendQueue(10, t -> {
            assertThat(t).containsExactly(
              new RpcObjectData(RpcObjectData.State.ADD, null, null, null, false),
              new RpcObjectData(RpcObjectData.State.CHANGE, null, List.of(), null, false)
            );
            latch.countDown();
        }, new IdentityHashMap<>(), null, false);

        q.sendList(after, null, Function.identity(), null, false);
        q.flush();

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}
