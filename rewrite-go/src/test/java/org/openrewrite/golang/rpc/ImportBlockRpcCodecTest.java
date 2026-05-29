/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.golang.marker.ImportBlock;
import org.openrewrite.java.tree.Space;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.text.PlainText;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-Java round-trip for the {@link ImportBlock} marker's {@code RpcCodec}.
 * <p>
 * The {@code grouped} / {@code closePrevious} flags are sent as primitive
 * booleans by {@code rpcSend}, so {@code rpcReceive} must read them back as
 * booleans. Decoding them with a {@code Function<String, Boolean>} (e.g.
 * {@code Boolean::parseBoolean}) causes the wire {@code Boolean} to be cast to
 * {@code String}, throwing a {@link ClassCastException} — the crash that broke
 * parsing of cgo / grouped-import Go files.
 */
class ImportBlockRpcCodecTest {

    @Test
    void groupedAndClosePreviousBooleansRoundTrip() {
        UUID id = Tree.randomId();
        // Defaults: both flags false, empty spaces.
        ImportBlock before = new ImportBlock(id, false, Space.EMPTY, false, Space.EMPTY);
        // The cgo / grouped-import case: both flags flipped to true.
        ImportBlock after = before
                .withClosePrevious(true)
                .withBefore(Space.format("\n"))
                .withGrouped(true)
                .withGroupedBefore(Space.format("\n"));

        ImportBlock received = roundTrip(after, before);

        assertThat(received.isClosePrevious()).isTrue();
        assertThat(received.isGrouped()).isTrue();
        assertThat(received.getBefore().getWhitespace()).isEqualTo("\n");
        assertThat(received.getGroupedBefore().getWhitespace()).isEqualTo("\n");
        assertThat(received.getId()).isEqualTo(id);
        assertThat(received).isEqualTo(after);
    }

    private static ImportBlock roundTrip(ImportBlock after, ImportBlock before) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1, batches::addLast,
                new IdentityHashMap<>(), PlainText.class.getName(), false);
        sq.send(after, before, null);
        sq.flush();

        RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(),
                () -> encode(batches.removeFirst()), PlainText.class.getName(), null);
        return rq.receive(before);
    }

    /**
     * Mirror the wire encoding for value types that travel as strings (UUID,
     * Path). Booleans and whitespace strings are passed through unchanged,
     * exactly as a JSON transport would deliver them.
     */
    private static List<RpcObjectData> encode(List<RpcObjectData> batch) {
        List<RpcObjectData> encoded = new ArrayList<>();
        for (RpcObjectData data : batch) {
            if (data.getValue() instanceof UUID) {
                encoded.add(new RpcObjectData(data.getState(), data.getValueType(),
                        data.getValue().toString(), data.getRef(), false));
            } else {
                encoded.add(data);
            }
        }
        return encoded;
    }
}
