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
package org.openrewrite.golang;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.golang.tree.GoMod.GoModStatement;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the production go.mod RPC panic at its true source: the LST
 * serialize/deserialize step the Moderne CLI runs between {@code mod build}
 * and {@code mod run}.
 * <p>
 * The CLI persists the parsed {@link GoMod} to a {@code .lst} and reads it back
 * before the recipe runs. Polymorphic LST nodes rely on the {@code @c} type id
 * declared by {@link Tree} ({@code @JsonTypeInfo(use = Id.CLASS, property = "@c")}).
 * {@link GoModStatement} must carry the same type id, otherwise the
 * {@link JRightPadded#getElement() element} of each statement deserializes to
 * {@code null} — which later NPEs in {@code GoModRpcCodec.rpcSend} when the tree
 * is shipped to the Go visitor.
 */
class GoModSerializationTest {

    private static final ObjectMapper MAPPER = ObjectMappers.propertyBasedMapper(null);

    @Test
    void statementElementsSurviveLstRoundTrip() throws Exception {
        // given: a parsed-shape GoMod with two directive statements (module, go)
        GoMod before = goModWithModuleAndGo();
        assertThat(before.getStatements()).allSatisfy(rp ->
                assertThat(rp.getElement()).as("element non-null before serialization").isNotNull());

        // when: round-tripped through the LST serializer (mirrors mod build -> mod run)
        byte[] bytes = MAPPER.writeValueAsBytes(before);
        GoMod after = MAPPER.readValue(bytes, GoMod.class);

        // then: every statement still has its concrete element (no null JRightPadded element)
        assertThat(after.getStatements()).hasSize(before.getStatements().size());
        assertThat(after.getStatements()).allSatisfy(rp ->
                assertThat(rp.getElement())
                        .as("statement element must survive the .lst round-trip")
                        .isNotNull());
    }

    private static GoMod goModWithModuleAndGo() {
        List<JRightPadded<GoModStatement>> stmts = new ArrayList<>();
        stmts.add(directive("module", "example.com/two"));
        stmts.add(directive("go", "1.21"));
        return new GoMod(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                Paths.get("go.mod"),
                "UTF-8",
                false,
                null,
                null,
                stmts,
                Space.EMPTY);
    }

    private static JRightPadded<GoModStatement> directive(String keyword, String value) {
        GoMod.Value v = new GoMod.Value(Tree.randomId(), Space.format(" "), Markers.EMPTY, value);
        GoMod.Directive d = new GoMod.Directive(Tree.randomId(), Space.EMPTY, Markers.EMPTY, keyword, List.of(v));
        return new JRightPadded<>(d, Space.format("\n"), Markers.EMPTY);
    }
}
