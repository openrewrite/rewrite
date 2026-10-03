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
package org.openrewrite.toml.internal.rpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class TomlRpcRoundTripTest {

    private static final ObjectMapper WIRE = JsonMapper.builder().build();

    @Test
    void addRoundTripsEveryTreeTypeAndLiteralValue() {
        // given
        Toml.Document doc = parse(
          """
            # leading comment
            title = "TOML" # trailing comment
            count = 42
            big = 9223372036854775807
            ratio = 3.25
            pos = inf
            neg = -inf
            nan = nan
            enabled = true
            date = 1979-05-27
            datetime = 1979-05-27T07:32:00
            time = 07:32:00.999999
            offset = 1979-05-27T07:32:00-08:00
            points = [ 1, 2, 3, ]
            inline = { x = 1, y = "two" }

            [owner]
            name = "Tom"

            [[products]]
            sku = 738594937
            """
        );

        // when
        Toml.Document received = roundTrip(doc, null, null);

        // then
        assertThat(received.printAll()).isEqualTo(doc.printAll());
        assertThat(received.getId()).isEqualTo(doc.getId());
        assertThat(received.getSourcePath()).isEqualTo(doc.getSourcePath());
        assertThat(literalValues(received)).containsExactly(
          "TOML", 42L, Long.MAX_VALUE, 3.25, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN,
          true, LocalDate.of(1979, 5, 27), LocalDateTime.of(1979, 5, 27, 7, 32),
          LocalTime.of(7, 32, 0, 999_999_000), OffsetDateTime.parse("1979-05-27T07:32:00-08:00"),
          1L, 2L, 3L, 1L, "two", "Tom", 738594937L
        );
    }

    @Test
    void changeAppliesEditedLiteralToTheBeforeTree() {
        // given
        Toml.Document before = parse(
          """
            [project]
            name = "demo"
            version = "0.1.0"
            """
        );
        Toml.Document after = (Toml.Document) new TomlIsoVisitor<Integer>() {
            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, Integer p) {
                return "0.1.0".equals(literal.getValue()) ?
                  literal.withSource("\"0.2.0\"").withValue("0.2.0") : literal;
            }
        }.visitNonNull(before, 0);

        // when
        Toml.Document received = roundTrip(after, before, before);

        // then
        assertThat(received.printAll()).isEqualTo(
          """
            [project]
            name = "demo"
            version = "0.2.0"
            """
        );
    }

    private static Toml.Document parse(String toml) {
        return (Toml.Document) new TomlParser().parse(new InMemoryExecutionContext(), toml)
          .findFirst()
          .orElseThrow();
    }

    private static List<Object> literalValues(Toml.Document doc) {
        List<Object> values = new ArrayList<>();
        new TomlIsoVisitor<List<Object>>() {
            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, List<Object> v) {
                v.add(literal.getValue());
                return literal;
            }
        }.visit(doc, values);
        return values;
    }

    private static Toml.Document roundTrip(Toml.Document after, Toml.@Nullable Document sentBefore,
                                           Toml.@Nullable Document receivedBefore) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(),
          Toml.Document.class.getName(), false);
        sq.send(after, sentBefore, null);
        sq.flush();

        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            all.addAll(batches.removeFirst());
        }
        List<RpcObjectData> wire;
        try {
            wire = WIRE.readValue(WIRE.writeValueAsString(all), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(wire);
        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, Toml.Document.class.getName(), null)
          .receive(receivedBefore);
    }
}
