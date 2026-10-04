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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.With;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.golang.BundledEngine;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcMarker;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.rpc.RpcObjectData.State.ADD;

/**
 * What crosses between Java and the RPC engine apart from trees both sides model, and what is left on
 * each side when a transfer fails part way.
 */
@Timeout(value = 10, unit = TimeUnit.MINUTES)
class GoRpcTransferTest {
    private static final String SOURCE = "package main\n\nvar a = 1\n\nfunc f(xs []string) int {\n\treturn len(xs) + a\n}\n";
    private static final String SOURCE_FILE_TYPE = Go.CompilationUnit.class.getName();

    @BeforeAll
    static void startEngine() throws Exception {
        BundledEngine.use();
    }

    @AfterAll
    static void stopEngine() {
        GoRewriteRpc.shutdownCurrent();
    }

    @BeforeEach
    void forgetEarlierTransfers() {
        GoRewriteRpc.getOrStart().reset();
    }

    /**
     * The engine has no tree for a ternary, and fails on one after Java has sent the whole file and
     * counted the file, and every ref it sent with it, as received.
     */
    @Test
    void receiveThatFailsInTheEngineIsRolledBackOnBothPeers() {
        Go.CompilationUnit cu = parse(SOURCE);
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        Go.CompilationUnit undecodable = (Go.CompilationUnit) new GolangVisitor<Integer>() {
            @Override
            public J visitLiteral(J.Literal literal, Integer p) {
                return new J.Ternary(randomId(), literal.getPrefix(), Markers.EMPTY, literal,
                        JLeftPadded.build(literal), JLeftPadded.build(literal), literal.getType());
            }
        }.visitNonNull(cu, 0);

        assertThatThrownBy(() -> rpc.print(undecodable)).hasMessageContaining("J$Ternary");

        // the parts of this file went out with the failed transfer, under refs the engine never took
        assertThat(rpc.print(cu.withId(randomId()))).isEqualTo(SOURCE);
        assertThat(rpc.print(cu)).isEqualTo(SOURCE);
    }

    /**
     * Java fails to read a marker after the engine has handed over the whole file and counted it as received.
     */
    @Test
    void receiveThatFailsInJavaIsRolledBackOnBothPeers() {
        Go.CompilationUnit cu = parse(SOURCE);
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        FragileMarker marker = new FragileMarker(randomId(), "kept");
        // printing it leaves the engine holding the marked file
        assertThat(rpc.print(cu.withMarkers(cu.getMarkers().add(marker)))).isEqualTo(SOURCE);

        String id = cu.getId().toString();
        FragileMarker.unreadable = true;
        try {
            assertThatThrownBy(() -> rpc.getObject(id, SOURCE_FILE_TYPE)).hasStackTraceContaining("unreadable");
        } finally {
            FragileMarker.unreadable = false;
        }

        Go.CompilationUnit returned = rpc.getObject(id, SOURCE_FILE_TYPE);
        assertThat(returned.getMarkers().findFirst(FragileMarker.class)).contains(marker);
        assertThat(returned.printAll()).isEqualTo(SOURCE);
    }

    @Value
    @With
    static class FragileMarker implements Marker {
        static volatile boolean unreadable;

        UUID id;
        String note;

        @JsonCreator
        FragileMarker(@JsonProperty("id") UUID id, @JsonProperty("note") String note) {
            if (unreadable) {
                throw new IllegalStateException("unreadable");
            }
            this.id = id;
            this.note = note;
        }
    }

    /**
     * The engine has no type for a marker that Java has no codec for, and returns it as it arrived.
     */
    @Test
    void markersWithoutACodecComeBackFromTheEngine() {
        Go.CompilationUnit cu = parse(SOURCE);
        GeneralFormatStyle shared = new GeneralFormatStyle(false);
        NamedStyles first = new NamedStyles(randomId(), "first", "first", "first", emptySet(), singletonList(shared));
        NamedStyles second = new NamedStyles(randomId(), "second", "second", "second", emptySet(), singletonList(shared));
        BuildTool tool = new BuildTool(randomId(), BuildTool.Type.Gradle, "8.0");
        GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
        rpc.print(cu.withMarkers(cu.getMarkers().add(first).add(second).add(tool)));

        Go.CompilationUnit returned = rpc.getObject(cu.getId().toString(), SOURCE_FILE_TYPE);

        assertThat(returned.getMarkers().findAll(NamedStyles.class))
                .extracting(NamedStyles::getName, NamedStyles::getStyles)
                .containsExactly(tuple("first", singletonList(shared)), tuple("second", singletonList(shared)));
        assertThat(returned.getMarkers().findFirst(BuildTool.class)).get().usingRecursiveComparison().isEqualTo(tool);
    }

    /**
     * A marker a recipe declares in the engine has no class in Java, which holds it as an
     * {@code RpcMarker} and writes it as it arrived, its fields beside its id.
     */
    @Test
    void markerWithoutAClassIsWrittenAsItArrived() {
        Map<String, Object> fromTheEngine = new HashMap<>();
        fromTheEngine.put("id", randomId().toString());
        fromTheEngine.put("Note", "kept by the recipe");

        RpcMarker held = new RpcObjectData(ADD, RpcMarker.class.getName(), new HashMap<>(fromTheEngine), null, false).getValue();
        assertThat(held.getData()).containsOnlyKeys("Note");

        Map<String, Object> written = new ObjectMapper().convertValue(held, new TypeReference<Map<String, Object>>() {
        });
        assertThat(written).containsAllEntriesOf(fromTheEngine).doesNotContainKey("data");
    }

    private static Go.CompilationUnit parse(String source) {
        return (Go.CompilationUnit) GolangParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(Paths.get("transfer.go"), source)), null,
                        new InMemoryExecutionContext())
                .findFirst().orElseThrow();
    }
}
