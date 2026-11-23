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
package org.openrewrite.javascript.rpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BigIntLiteralRpcTest {

    private static final String SOURCE_FILE_TYPE = JS.CompilationUnit.class.getName();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void bigIntValueTravelsAsItsDecimalText() {
        // given
        BigInteger value = new BigInteger("123456789012345678901234567890");
        J.Literal literal = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, value,
                "123456789012345678901234567890n", null, JavaType.Primitive.Long);

        // when
        List<RpcObjectData> wire = send(literal);
        J.Literal received = receive(wire);

        // then
        assertThat(wire).extracting(RpcObjectData::getValue).contains("123456789012345678901234567890");
        assertThat(received.getValue()).isEqualTo(value);
        assertThat(received.getValueSource()).isEqualTo("123456789012345678901234567890n");
    }

    @Test
    void numericValueOfAnotherLiteralIsLeftAsIs() {
        // given
        J.Literal literal = new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, 31,
                "0x1f", null, JavaType.Primitive.Double);

        // when
        J.Literal received = receive(send(literal));

        // then
        assertThat(received.getValue()).isEqualTo(31);
    }

    private static List<RpcObjectData> send(J.Literal literal) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), SOURCE_FILE_TYPE, false);
        sq.send(literal, null, null);
        sq.flush();
        List<RpcObjectData> all = new ArrayList<>();
        batches.forEach(batch -> batch.forEach(data -> all.add(new RpcObjectData(data.getState(), data.getValueType(),
                overJson(data.getValue()), data.getRef(), false))));
        return all;
    }

    private static @Nullable Object overJson(@Nullable Object value) {
        try {
            return value == null ? null : MAPPER.readValue(MAPPER.writeValueAsString(value), Object.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static J.Literal receive(List<RpcObjectData> wire) {
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(wire);
        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, SOURCE_FILE_TYPE, null).receive(null);
    }
}
