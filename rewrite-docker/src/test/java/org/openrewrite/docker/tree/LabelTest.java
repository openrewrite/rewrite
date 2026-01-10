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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class LabelTest implements RewriteTest {

    @Test
    void labelSinglePair() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL version=1.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(1);
                assertThat(((Docker.PlainText) label.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("version");
                assertThat(((Docker.PlainText) label.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void labelMultiplePairs() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL version=1.0.0 app=myapp
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(2);
                assertThat(((Docker.PlainText) label.getPairs().get(0).getKey().getContents().getFirst()).getText()).isEqualTo("version");
                assertThat(((Docker.PlainText) label.getPairs().get(1).getKey().getContents().getFirst()).getText()).isEqualTo("app");
            })
          )
        );
    }

    @Test
    void labelWithQuotedValues() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL description="My application" version="1.0.0"
              """
          )
        );
    }

    @Test
    void labelOldFormatWithoutEquals() {
        // Old-style LABEL format: key value (without equals sign)
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL author John Doe
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(1);
                assertThat(label.getPairs().getFirst().isHasEquals()).isFalse();
                assertThat(((Docker.PlainText) label.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("author");
            })
          )
        );
    }

    @Test
    void labelWithInstructionKeywordInValue() {
        // LABEL value containing instruction keywords like "run"
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL install.cmd /usr/bin/docker run -ti
              """
          )
        );
    }

    @Test
    void labelMixedFormats() {
        // Multiple LABEL instructions with different formats
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL version=1.0.0
              LABEL maintainer John Doe
              LABEL description="My app"
              """
          )
        );
    }
}
