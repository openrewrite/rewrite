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
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An `ARG` used to carry one name and one value of its own rather than a list of pairs, and LSTs
 * written in that shape outlive the change: the Moderne CLI persists a parsed tree to a `.lst` and
 * reads it back before a recipe runs, so a reader has to understand both shapes.
 */
class ArgSerializationTest {

    private static final ObjectMapper MAPPER = ObjectMappers.propertyBasedMapper(null);

    /// Written by a serializer from before an `ARG` could declare more than one name, `ARG a=1` in a
    /// stage of its own. Held as text rather than built from the model so that it cannot drift with it.
    @Language("json")
    private static final String LEGACY_ARG_LST = """
      {
        "@c": "org.openrewrite.docker.tree.Docker$File",
        "id": "a86c9ed0-449b-4bf9-bfa4-9e72e2097dd5",
        "sourcePath": "Dockerfile",
        "prefix": {"@ref": 1, "whitespace": "", "comments": []},
        "markers": {"@ref": 2, "id": "ce0b3122-007a-43fe-b83c-e2aaba8b73e9", "markers": []},
        "charsetName": "UTF-8",
        "charsetBomMarked": false,
        "checksum": null,
        "fileAttributes": null,
        "globalArgs": [],
        "stages": [{
          "@c": "org.openrewrite.docker.tree.Docker$Stage",
          "id": "788a2fad-ced4-4305-8ffb-56f2bbfedf5a",
          "prefix": 1,
          "markers": 2,
          "from": {
            "@c": "org.openrewrite.docker.tree.Docker$From",
            "id": "e68a0264-c880-401b-a079-3aeee8ed161d",
            "prefix": 1,
            "markers": 2,
            "keyword": "FROM",
            "flags": null,
            "imageName": {
              "@c": "org.openrewrite.docker.tree.Docker$Argument",
              "id": "cb3390d0-cfe8-4780-9552-f96b48ea8d24",
              "prefix": {"@ref": 3, "whitespace": " ", "comments": []},
              "markers": 2,
              "contents": [{
                "@c": "org.openrewrite.docker.tree.Docker$Literal",
                "id": "90d3e4bd-042d-44a8-927f-97298de5fbe5",
                "prefix": 1,
                "markers": 2,
                "text": "scratch",
                "quoteStyle": null
              }]
            },
            "tag": null,
            "digest": null,
            "as": null
          },
          "instructions": [{
            "@c": "org.openrewrite.docker.tree.Docker$Arg",
            "id": "3a438da3-d0b6-4568-b880-aaf363500476",
            "prefix": {"@ref": 4, "whitespace": "\\n", "comments": []},
            "markers": 2,
            "keyword": "ARG",
            "name": {
              "@c": "org.openrewrite.docker.tree.Docker$Literal",
              "id": "306bdb9a-c04b-4b03-a410-63d10881c18b",
              "prefix": 3,
              "markers": 2,
              "text": "a",
              "quoteStyle": null
            },
            "value": {
              "@c": "org.openrewrite.docker.tree.Docker$Argument",
              "id": "f03937da-dd21-4576-a950-963392dbdca8",
              "prefix": 1,
              "markers": 2,
              "contents": [{
                "@c": "org.openrewrite.docker.tree.Docker$Literal",
                "id": "3e1b3ac6-5804-46f1-8ca4-d9182b60cd69",
                "prefix": 1,
                "markers": 2,
                "text": "1",
                "quoteStyle": null
              }]
            }
          }]
        }],
        "eof": 4,
        "charset": "UTF-8"
      }
      """;

    @Test
    void readsAnArgSerializedBeforeItHeldPairs() throws Exception {
        Docker.File file = MAPPER.readValue(LEGACY_ARG_LST, Docker.File.class);

        Docker.Arg arg = (Docker.Arg) file.getStages().getFirst().getInstructions().getFirst();
        Docker.Arg.ArgPair pair = assertThat(arg.getPairs()).singleElement().actual();
        assertThat(pair.getName().getText()).isEqualTo("a");
        assertThat(pair.getValue()).isNotNull();
        assertThat(pair.getValue().getContents()).singleElement()
          .satisfies(content -> assertThat(((Docker.Literal) content).getText()).isEqualTo("1"));

        assertThat(file.printAll()).isEqualTo("FROM scratch\nARG a=1\n");
    }

    @Test
    void roundTripsAnArgOfSeveralNames() throws Exception {
        SourceFile parsed = org.openrewrite.docker.DockerParser.builder().build()
          .parse("FROM scratch\nARG a b=2 c\n")
          .findFirst().orElseThrow();

        SourceFile back = MAPPER.readValue(MAPPER.writeValueAsBytes(parsed), Docker.File.class);

        assertThat(((Docker.Arg) ((Docker.File) back).getStages().getFirst().getInstructions().getFirst())
          .getPairs()).hasSize(3);
        assertThat(back.printAll()).isEqualTo(parsed.printAll());
    }
}
