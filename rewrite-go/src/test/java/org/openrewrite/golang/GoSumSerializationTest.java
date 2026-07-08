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
import org.openrewrite.golang.tree.GoSum;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the {@code go.sum} LST against the same {@code .lst} round-trip failure
 * {@link GoModSerializationTest} covers for go.mod: {@link GoSum.Line} must carry
 * {@code Tree}'s {@code @c} polymorphic type id, otherwise each
 * {@link JRightPadded#getElement() line element} deserializes to {@code null} and
 * later NPEs in {@code GoSumRpcCodec.rpcSend}.
 */
class GoSumSerializationTest {

    private static final ObjectMapper MAPPER = ObjectMappers.propertyBasedMapper(null);

    @Test
    void lineElementsSurviveLstRoundTrip() throws Exception {
        // given: a parsed-shape GoSum with a zip + go.mod line
        GoSum before = goSumWithOnePair();
        assertThat(before.getLines()).allSatisfy(rp ->
                assertThat(rp.getElement()).as("element non-null before serialization").isNotNull());

        // when: round-tripped through the LST serializer (mirrors mod build -> mod run)
        byte[] bytes = MAPPER.writeValueAsBytes(before);
        GoSum after = MAPPER.readValue(bytes, GoSum.class);

        // then: every line still has its concrete element
        assertThat(after.getLines()).hasSize(before.getLines().size());
        assertThat(after.getLines()).allSatisfy(rp ->
                assertThat(rp.getElement())
                        .as("line element must survive the .lst round-trip")
                        .isNotNull());
        assertThat(after.getLines().get(1).getElement().isGoMod()).isTrue();
    }

    private static GoSum goSumWithOnePair() {
        List<JRightPadded<GoSum.Line>> lines = new ArrayList<>();
        lines.add(line("github.com/a/b", "v1.0.0", false, "h1:zip="));
        lines.add(line("github.com/a/b", "v1.0.0", true, "h1:mod="));
        return new GoSum(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                Paths.get("go.sum"),
                "UTF-8",
                false,
                null,
                null,
                lines,
                Space.EMPTY);
    }

    private static JRightPadded<GoSum.Line> line(String modulePath, String version, boolean goMod, String hash) {
        GoSum.Line l = new GoSum.Line(Tree.randomId(), Space.EMPTY, Markers.EMPTY, modulePath, version, goMod, hash);
        return new JRightPadded<>(l, Space.format("\n"), Markers.EMPTY);
    }
}
