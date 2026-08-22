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
import org.openrewrite.docker.internal.ArgumentContents;
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
                var label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(1);
                assertThat(((Docker.Literal) label.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("version");
                assertThat(((Docker.Literal) label.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
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
                var label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(2);
                assertThat(((Docker.Literal) label.getPairs().get(0).getKey().getContents().getFirst()).getText()).isEqualTo("version");
                assertThat(((Docker.Literal) label.getPairs().get(1).getKey().getContents().getFirst()).getText()).isEqualTo("app");
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
                var label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(1);
                assertThat(label.getPairs().getFirst().isHasEquals()).isFalse();
                assertThat(((Docker.Literal) label.getPairs().getFirst().getKey().getContents().getFirst()).getText()).isEqualTo("author");
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

    @Test
    void labelMaintainerKeyword() {
        // LABEL with 'maintainer' as key (keyword used as label key)
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              LABEL maintainer "Jessie Frazelle <jess@linux.com>"
              RUN apk add curl
              """
          )
        );
    }

    @Test
    void labelRunKeyword() {
        // LABEL with 'RUN' as key (instruction keyword used as label key with equals)
        rewriteRun(
          docker(
            """
              FROM centos:7
              LABEL RUN='/usr/bin/docker run -d --name myapp ${IMAGE}'
              RUN yum install -y curl
              """
          )
        );
    }

    @Test
    void environmentVariableValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL version=$VAL
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label.LabelPair pair = onlyPair(doc);
                assertThat(pair.isHasEquals()).isTrue();
                assertThat(ArgumentContents.text(pair.getKey())).isEqualTo("version");
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) pair.getValue().getContents().getFirst();
                assertThat(var.getName()).isEqualTo("VAL");
                assertThat(var.isBraced()).isFalse();
                assertThat(ArgumentContents.containsVariable(pair.getValue())).isTrue();
                assertThat(ArgumentContents.text(pair.getValue())).isNull();
                assertThat(ArgumentContents.textWithVariables(pair.getValue())).isEqualTo("$VAL");
            })
          )
        );
    }

    @Test
    void bracedEnvironmentVariableWithSuffix() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL version=${BASE}-suffix
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label.LabelPair pair = onlyPair(doc);
                assertThat(pair.isHasEquals()).isTrue();
                assertThat(pair.getValue().getContents()).hasSize(2);
                assertThat(ArgumentContents.textWithVariables(pair.getValue())).isEqualTo("${BASE}-suffix");
            })
          )
        );
    }

    @Test
    void multiplePairsWithEnvironmentVariable() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL a=1 b=$X c=3
              """,
            spec -> spec.afterRecipe(doc -> {
                var label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(label.getPairs()).hasSize(3);
                assertThat(label.getPairs()).allSatisfy(pair -> assertThat(pair.isHasEquals()).isTrue());
                assertThat(ArgumentContents.textWithVariables(label.getPairs().get(0).getValue())).isEqualTo("1");
                assertThat(ArgumentContents.textWithVariables(label.getPairs().get(1).getValue())).isEqualTo("$X");
                assertThat(ArgumentContents.textWithVariables(label.getPairs().get(2).getValue())).isEqualTo("3");
            })
          )
        );
    }

    @Test
    void oldFormatValueKeepsSpacesBetweenWords() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL author John Doe
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label.LabelPair pair = onlyPair(doc);
                assertThat(pair.isHasEquals()).isFalse();
                assertThat(ArgumentContents.text(pair.getValue())).isEqualTo("John Doe");
            })
          )
        );
    }

    @Test
    void oldFormatValueIsOneLiteralHoldingSourceText() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL author "John Doe" of ACME
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label.LabelPair pair = onlyPair(doc);
                assertThat(pair.isHasEquals()).isFalse();
                assertThat(pair.getValue().getContents()).hasSize(1);
                assertThat(ArgumentContents.quoteStyle(pair.getValue())).isNull();
                assertThat(ArgumentContents.text(pair.getValue())).isEqualTo("\"John Doe\" of ACME");
            })
          )
        );
    }

    @Test
    void singleQuotedSectionOfAValueDoesNotExpand() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              LABEL author 'John $D' of ACME
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label.LabelPair pair = onlyPair(doc);
                assertThat(pair.getValue().getContents()).hasSize(1);
                assertThat(ArgumentContents.containsVariable(pair.getValue())).isFalse();
                assertThat(ArgumentContents.text(pair.getValue())).isEqualTo("'John $D' of ACME");
            })
          )
        );
    }

    @Test
    void theEscapeCharacterHoldsAQuoteInsideAValue() {
        rewriteRun(
          docker(
            """
              # escape=`
              FROM ubuntu:20.04
              LABEL author="John `"Doe`" Smith"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Label.LabelPair pair = onlyPair(doc);
                assertThat(ArgumentContents.quoteStyle(pair.getValue())).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(ArgumentContents.text(pair.getValue())).isEqualTo("John `\"Doe`\" Smith");
            })
          )
        );
    }

    private static Docker.Label.LabelPair onlyPair(Docker.File doc) {
        var label = (Docker.Label) doc.getStages().getFirst().getInstructions().getLast();
        return assertThat(label.getPairs()).singleElement().actual();
    }
}
