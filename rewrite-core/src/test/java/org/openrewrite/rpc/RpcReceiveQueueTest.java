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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.Checksum;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Markers;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcReceiveQueueTest {

    private Deque<List<RpcObjectData>> batches;
    private RpcSendQueue sq;
    private RpcReceiveQueue rq;

    @BeforeEach
    void setUp() {
        batches = new ArrayDeque<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();
        sq = new RpcSendQueue(1, e -> batches.addLast(encode(e)), localRefs, PlainText.class.getName(), false);
        rq = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, PlainText.class.getName(), null);
    }

    @Test
    void add() {
        PlainText before = PlainText.builder()
          .sourcePath(Path.of("foo.txt"))
          .text("hello")
          .build();

        sq.send(before, null, null);
        assertThat(batches).hasSize(14);
        PlainText after = rq.receive(null);

        assertThat(after).isEqualTo(before);
        assertThat(after.getId()).isEqualTo(before.getId());
        assertThat(after.getSourcePath()).isEqualTo(before.getSourcePath());
    }

    @Test
    @SuppressWarnings("UnnecessaryLocalVariable")
    void noChange() {
        PlainText before = PlainText.builder()
          .sourcePath(Path.of("foo.txt"))
          .text("hello")
          .build();
        PlainText noChange = before;

        sq.send(noChange, before, null);
        assertThat(batches).hasSize(1);
        PlainText after = rq.receive(noChange);

        assertThat(after).isEqualTo(noChange);
        assertThat(after.getId()).isEqualTo(noChange.getId());
    }

    @Test
    void changeId() {
        PlainText before = PlainText.builder()
          .sourcePath(Path.of("foo.txt"))
          .text("hello")
          .build();
        PlainText newId = before.withId(Tree.randomId());

        sq.send(newId, before, null);
        assertThat(batches).hasSize(10);
        PlainText after = rq.receive(before);

        assertThat(after).isEqualTo(newId);
        assertThat(after.getId()).isEqualTo(newId.getId());
    }

    @Test
    void changePropertyType() {
        // Test changing a property from FileAttributes to Checksum
        // This simulates a recipe that changes the type of an object assigned to a property
        FileAttributes beforeAttr = new FileAttributes(null, null, null, true, true, false, 100);
        Checksum afterChecksum = new Checksum("SHA-256", new byte[]{1, 2, 3});

        sq.send(afterChecksum, beforeAttr, null);
        assertThat(batches).isNotEmpty();

        Object received = rq.receive(beforeAttr);

        assertThat(received).isInstanceOf(Checksum.class);
        assertThat(((Checksum) received).getAlgorithm()).isEqualTo("SHA-256");
    }

    @Test
    void changedMarkerWithSameIdRoundTrips() {
        BuildTool buildTool = new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.0");
        Markers before = Markers.build(List.of(buildTool));
        Markers after = before.withMarkers(List.of(buildTool.withVersion("8.0")));

        sq.send(after, before, null);
        Markers received = rq.receive(before);

        assertThat(received.findFirst(BuildTool.class)).hasValueSatisfying(bt ->
          assertThat(bt.getVersion()).isEqualTo("8.0"));
    }

    @Test
    void plainMapRoundTripsWithoutLeakingRefMetadata() throws Exception {
        // A Map<String, String> (e.g. NodeResolutionResult.Npmrc#properties) must survive
        // the round trip without gaining "@c"/"@ref" entries. Those keys are only meaningful
        // for POJOs annotated with @JsonTypeInfo/@JsonIdentityInfo; leaking them into a Map
        // puts a String "@c" and an Integer "@ref" into a Map<String, String>.
        Map<String, String> before = new LinkedHashMap<>();
        before.put("registry", "https://registry.npmjs.org/");
        before.put("save-exact", "true");

        sq.send(before, null, null);
        Map<String, String> after = rq.receive(null);

        assertThat(after).doesNotContainKeys("@c", "@ref");
        assertThat(after).isEqualTo(before);

        // Serializing the received map the way the V2 edit writer does -- as a declared
        // Map<String, String> -- must not throw. Before the fix the leaked "@ref" -> 1
        // (Integer) entry blew up here with the exact failure customers reported:
        // "java.lang.Integer cannot be cast to java.lang.String".
        assertThatCode(() -> new ObjectMapper()
                .writerFor(new TypeReference<Map<String, String>>() {})
                .writeValueAsString(after))
                .doesNotThrowAnyException();
    }

    @Test
    void fileAttributeTimestampsTravelAsStrings() {
        ZonedDateTime created = ZonedDateTime.parse("2026-08-16T10:15:30.123456789+02:00[Europe/Berlin]");
        FileAttributes before = new FileAttributes(created, created.plusHours(1), created.plusHours(2),
          true, true, false, 100);

        sq.send(before, null, null);

        // A raw ZonedDateTime is written as a bare epoch number and arrives as a Double -- a CCE
        // in rpcReceive, and even coerced it would have lost the zone and the nanos below.
        List<RpcObjectData> timestamps = batches.stream().flatMap(List::stream).collect(toList()).subList(1, 4);
        assertThat(timestamps).allSatisfy(data -> {
            assertThat(data.getValueType()).isNull();
            assertThat((Object) data.getValue()).isInstanceOf(String.class);
        });

        FileAttributes after = rq.receive(null);
        assertThat(after).isEqualTo(before);
        assertThat(after.getCreationTime()).isEqualTo(created);
        assertThat(requireNonNull(after.getCreationTime()).getZone()).isEqualTo(created.getZone());
        assertThat(after.getCreationTime().getNano()).isEqualTo(123456789);
    }

    @Test
    void detectsMissingCodecOnReceiverSide() {
        // given
        batches.addLast(List.of(
            new RpcObjectData(RpcObjectData.State.ADD, "java.lang.StringBuilder", null, null, false)
        ));

        // when / then
        assertThatThrownBy(() -> rq.receive(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No RPC codec registered on the Java side for 'java.lang.StringBuilder'");
    }

    private List<RpcObjectData> encode(List<RpcObjectData> batch) {
        List<RpcObjectData> encoded = new ArrayList<>();
        for (RpcObjectData data : batch) {
            if (data.getValue() instanceof UUID || data.getValue() instanceof Path) {
                encoded.add(new RpcObjectData(data.getState(), data.getValueType(), data.getValue().toString(), data.getRef(), false));
            } else {
                encoded.add(data);
            }
        }
        return encoded;
    }
}
