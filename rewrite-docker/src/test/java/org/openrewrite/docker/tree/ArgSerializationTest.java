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
package org.openrewrite.docker.tree;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.marker.Markers;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

/**
 * An {@code ARG} carried one name and one value of its own before it carried a list of pairs, and a
 * tree serialized in that shape outlives the change. Both of these hold the older shape literally
 * rather than building it from the model, so neither can drift with the class it exists to outlive.
 */
class ArgSerializationTest {

    /// `ARG a=1`, as a serializer wrote it before an `ARG` could declare more than one name: no
    /// `pairs`, and the whitespace before the name held by the name.
    @Language("json")
    private static final String LEGACY_ARG = """
      {
        "@c": "org.openrewrite.docker.tree.Docker$Arg",
        "id": "3a438da3-d0b6-4568-b880-aaf363500476",
        "prefix": {"@ref": 1, "whitespace": "\\n", "comments": []},
        "markers": {"@ref": 2, "id": "b4ff7e9c-ba46-4f48-acb9-293bb3c51f76", "markers": []},
        "keyword": "ARG",
        "name": {
          "@c": "org.openrewrite.docker.tree.Docker$Literal",
          "id": "306bdb9a-c04b-4b03-a410-63d10881c18b",
          "prefix": {"@ref": 3, "whitespace": " ", "comments": []},
          "markers": 2,
          "text": "a",
          "quoteStyle": null
        },
        "value": {
          "@c": "org.openrewrite.docker.tree.Docker$Argument",
          "id": "f03937da-dd21-4576-a950-963392dbdca8",
          "prefix": {"@ref": 4, "whitespace": "", "comments": []},
          "markers": 2,
          "contents": [{
            "@c": "org.openrewrite.docker.tree.Docker$Literal",
            "id": "3e1b3ac6-5804-46f1-8ca4-d9182b60cd69",
            "prefix": 4,
            "markers": 2,
            "text": "1",
            "quoteStyle": null
          }]
        }
      }
      """;

    @Test
    void readsAnArgSerializedBeforeItHeldPairs() throws Exception {
        Docker.Arg arg = ObjectMappers.propertyBasedMapper(null).readValue(LEGACY_ARG, Docker.Arg.class);
        assertsTheOneItDescribes(arg);
    }

    /// A serializer that addresses fields by position rather than by name hands the pairs it did not
    /// find back as an empty list, not as null, so an empty one has to fold the same way a missing one
    /// does. This is the one an `ARG` written before the change reaches.
    @Test
    void foldsAnEmptyPairListHoldingTheOlderShape() {
        Docker.Literal name = new Docker.Literal(randomId(), Space.build(" ", emptyList()),
                Markers.EMPTY, "a", null);
        Docker.Argument value = new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY,
                java.util.List.of(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "1", null)));

        assertsTheOneItDescribes(new Docker.Arg(UUID.randomUUID(), Space.build("\n", emptyList()),
                Markers.EMPTY, "ARG", emptyList(), name, value));
    }

    @Test
    void leavesAnArgThatAlreadyHoldsPairsAlone() {
        Docker.Arg arg = new Docker.Arg(randomId(), Space.EMPTY, Markers.EMPTY, "ARG",
                java.util.List.of(new Docker.Arg.ArgPair(randomId(), Space.build(" ", emptyList()),
                        Markers.EMPTY, new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "b", null), null)));

        assertThat(arg.getPairs()).singleElement()
                .satisfies(pair -> assertThat(pair.getName().getText()).isEqualTo("b"));
        assertThat(arg.getName()).isNull();
        assertThat(arg.getValue()).isNull();
    }

    private static void assertsTheOneItDescribes(Docker.Arg arg) {
        assertThat(arg.getKeyword()).isEqualTo("ARG");
        assertThat(arg.getPairs()).singleElement().satisfies(pair -> {
            assertThat(pair.getName().getText()).isEqualTo("a");
            // the whitespace stays where it was serialized, so the instruction prints as it was written
            assertThat(pair.getPrefix().getWhitespace()).isEmpty();
            assertThat(pair.getName().getPrefix().getWhitespace()).isEqualTo(" ");
            assertThat(ArgumentContents.text(pair.getValue())).isEqualTo("1");
        });
        assertThat(arg.getName()).as("folded away, never held by a live tree").isNull();
        assertThat(arg.getValue()).isNull();
    }
}
