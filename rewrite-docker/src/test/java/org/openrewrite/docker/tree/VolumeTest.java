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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class VolumeTest implements RewriteTest {

    @Test
    void volumeWithJsonArray() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME ["/data", "/logs"]
              """,
            spec -> spec.afterRecipe(doc -> {
                var volume = (Docker.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(2);
                assertThat(volume.isJsonForm()).isTrue();
            })
          )
        );
    }

    @Test
    void volumeWithPathList() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME /data /logs
              """,
            spec -> spec.afterRecipe(doc -> {
                var volume = (Docker.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(2);
                assertThat(volume.isJsonForm()).isFalse();
                assertThat(((Docker.Literal) volume.getValues().get(0).getContents().getFirst()).getText()).isEqualTo("/data");
                assertThat(((Docker.Literal) volume.getValues().get(1).getContents().getFirst()).getText()).isEqualTo("/logs");
            })
          )
        );
    }

    @Test
    void volumeJsonArrayWithSpaces() {
        // VOLUME with spaces inside JSON array: VOLUME [ "/data" ]
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME [ "/data" ]
              """
          )
        );
    }

    @Test
    void volumeWithEnvironmentVariable() {
        // VOLUME with environment variable reference
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ENV DATA_DIR=/data
              VOLUME ${DATA_DIR}
              """
          )
        );
    }

    @Test
    void volumeWithMultipleSpacesBeforeBracket() {
        // VOLUME with multiple spaces between keyword and opening bracket
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME  ["/data"]
              """
          )
        );
    }

    @Test
    void volumeWithTabBeforeBracket() {
        // VOLUME with tab between keyword and opening bracket
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME\t["/data"]
              """
          )
        );
    }

    @Test
    void volumeWithMultipleSpacesBeforePath() {
        // VOLUME with multiple spaces between keyword and path (non-JSON form)
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME  /data
              """
          )
        );
    }

    @Test
    void pathHoldingAVariableReferenceIsOnePath() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME ${DIR}/data /logs
              """,
            spec -> spec.afterRecipe(doc -> {
                var volume = (Docker.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(2);
                List<Docker.ArgumentContent> contents = volume.getValues().getFirst().getContents();
                assertThat(contents).hasSize(2);
                assertThat(((Docker.EnvironmentVariable) contents.getFirst()).getName()).isEqualTo("DIR");
                assertThat(((Docker.Literal) contents.getLast()).getText()).isEqualTo("/data");
                assertThat(text(volume.getValues().getLast())).isEqualTo("/logs");
            })
          )
        );
    }

    @Test
    void pathContainingEquals() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME /a=b
              """,
            spec -> spec.afterRecipe(doc -> {
                var volume = (Docker.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(1);
                assertThat(text(volume.getValues().getFirst())).isEqualTo("/a=b");
            })
          )
        );
    }

    @Test
    void pathContainingLoneDollar() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME /cost$.txt
              """,
            spec -> spec.afterRecipe(doc -> {
                var volume = (Docker.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(1);
                assertThat(text(volume.getValues().getFirst())).isEqualTo("/cost$.txt");
            })
          )
        );
    }

    @Test
    void quotedPath() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              VOLUME "/my data" '/more data'
              """,
            spec -> spec.afterRecipe(doc -> {
                var volume = (Docker.Volume) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(volume.getValues()).hasSize(2);
                var doubleQuoted = (Docker.Literal) volume.getValues().getFirst().getContents().getFirst();
                assertThat(doubleQuoted.getText()).isEqualTo("/my data");
                assertThat(doubleQuoted.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                var singleQuoted = (Docker.Literal) volume.getValues().getLast().getContents().getFirst();
                assertThat(singleQuoted.getText()).isEqualTo("/more data");
                assertThat(singleQuoted.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.SINGLE);
            })
          )
        );
    }

    private static String text(Docker.Argument argument) {
        return ((Docker.Literal) argument.getContents().getFirst()).getText();
    }
}
