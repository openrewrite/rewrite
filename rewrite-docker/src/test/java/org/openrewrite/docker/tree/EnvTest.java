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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.docker.Assertions.docker;

class EnvTest implements RewriteTest {

    @Test
    void envSingleLine() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION=18.0.0
              ENV PATH=/usr/local/bin:$PATH
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                var env1 = (Docker.Env) instructions.getFirst();
                assertThat(env1.getPairs()).hasSize(1);
                assertThat(env1.getPairs().getFirst().getKey().getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.Literal) env1.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void envMultiplePairs() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION=18.0.0 NPM_VERSION=9.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                var env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(env.getPairs()).hasSize(2);
                assertThat(env.getPairs().get(0).getKey().getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.Literal) env.getPairs().get(0).getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
                assertThat(env.getPairs().get(1).getKey().getText()).isEqualTo("NPM_VERSION");
                assertThat(((Docker.Literal) env.getPairs().get(1).getValue().getContents().getFirst()).getText()).isEqualTo("9.0.0");
            })
          )
        );
    }

    @Test
    void envOldStyleSpaceSeparated() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ENV NODE_VERSION 18.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                var env1 = (Docker.Env) instructions.getFirst();
                assertThat(env1.getPairs()).hasSize(1);
                assertThat(env1.getPairs().getFirst().isHasEquals()).isFalse();
                assertThat(env1.getPairs().getFirst().getKey().getText()).isEqualTo("NODE_VERSION");
                assertThat(((Docker.Literal) env1.getPairs().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void envWithKeywordName() {
        // Test ENV where key is a Docker keyword (like SHELL)
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV SHELL /usr/bin/zsh
              """
          )
        );
    }

    @Test
    void quotedValueInEqualsForm() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV NODE_VERSION="18.0.0"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = envValue(doc);
                Docker.Literal literal = (Docker.Literal) value.getContents().getFirst();
                assertThat(literal.getText()).isEqualTo("18.0.0");
                assertThat(literal.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(ArgumentContents.text(value)).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void quotedValueInSpaceForm() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV NODE_VERSION "18.0.0"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = envValue(doc);
                Docker.Literal literal = (Docker.Literal) value.getContents().getFirst();
                assertThat(literal.getText()).isEqualTo("18.0.0");
                assertThat(literal.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(ArgumentContents.text(value)).isEqualTo("18.0.0");
            })
          )
        );
    }

    @Test
    void environmentVariableValue() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV NODE_VERSION=${BASE}-suffix
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = envValue(doc);
                assertThat(value.getContents()).hasSize(2);
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) value.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("BASE");
                assertThat(var.isBraced()).isTrue();
                assertThat(ArgumentContents.containsVariable(value)).isTrue();
                assertThat(ArgumentContents.text(value)).isNull();
                assertThat(ArgumentContents.textWithVariables(value)).isEqualTo("${BASE}-suffix");
            })
          )
        );
    }

    @Test
    void variableInQuotedPathAppend() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV PATH="/opt/venv/bin:$PATH"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = envValue(doc);
                assertThat(ArgumentContents.containsVariable(value)).isTrue();
                assertThat(ArgumentContents.text(value)).isNull();
                assertThat(ArgumentContents.textWithVariables(value)).isEqualTo("\"/opt/venv/bin:$PATH\"");
            })
          )
        );
    }

    @Test
    void lowercaseVariableInQuotedPathAppend() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV path_prefix=/opt
              ENV PATH="$path_prefix/bin:$PATH"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = envValue(doc);
                assertThat(value.getContents())
                  .filteredOn(Docker.EnvironmentVariable.class::isInstance)
                  .extracting(content -> ((Docker.EnvironmentVariable) content).getName())
                  .containsExactly("path_prefix", "PATH");
                assertThat(ArgumentContents.containsVariable(value)).isTrue();
                assertThat(ArgumentContents.text(value)).isNull();
                assertThat(ArgumentContents.textWithVariables(value)).isEqualTo("\"$path_prefix/bin:$PATH\"");
            })
          )
        );
    }

    @Test
    void quotedValueFollowedByMoreTextIsWhole() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV DESCRIPTION "a title" and more
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = envValue(doc);
                Docker.Literal literal = (Docker.Literal) value.getContents().getFirst();
                assertThat(literal.getText()).isEqualTo("\"a title\" and more");
                assertThat(literal.getQuoteStyle()).isNull();
            })
          )
        );
    }

    @Test
    void flagValueBindsToItsKey() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV NODE_OPTIONS=--max-old-space-size=4096
              """,
            spec -> spec.afterRecipe(doc -> {
                var env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
                Docker.Env.EnvPair pair = assertThat(env.getPairs()).singleElement().actual();
                assertThat(pair.isHasEquals()).isTrue();
                assertThat(pair.getKey().getText()).isEqualTo("NODE_OPTIONS");
                assertThat(ArgumentContents.text(pair.getValue())).isEqualTo("--max-old-space-size=4096");
            })
          )
        );
    }

    @Test
    void flagValuesDoNotSwallowThePairsThatFollowThem() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV NODE_OPTIONS=--max-old-space-size=4096 PIP_OPTIONS=--no-cache-dir
              """,
            spec -> spec.afterRecipe(doc -> {
                var env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(env.getPairs())
                  .extracting(pair -> pair.getKey().getText(), pair -> ArgumentContents.text(pair.getValue()))
                  .containsExactly(
                    tuple("NODE_OPTIONS", "--max-old-space-size=4096"),
                    tuple("PIP_OPTIONS", "--no-cache-dir"));
            })
          )
        );
    }

    @Test
    void bracketedValueBindsToItsKey() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV SEEDS=[a,b] MODE=fast
              """,
            spec -> spec.afterRecipe(doc -> {
                var env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(env.getPairs())
                  .extracting(Docker.Env.EnvPair::isHasEquals, pair -> ArgumentContents.text(pair.getValue()))
                  .containsExactly(tuple(true, "[a,b]"), tuple(true, "fast"));
            })
          )
        );
    }

    @Test
    void aValueSeparatedFromItsKeyIsTheLegacyForm() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV KEY =value
              """,
            spec -> spec.afterRecipe(doc -> {
                var env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
                Docker.Env.EnvPair pair = assertThat(env.getPairs()).singleElement().actual();
                assertThat(pair.isHasEquals()).isFalse();
                assertThat(ArgumentContents.text(pair.getValue())).isEqualTo("=value");
            })
          )
        );
    }

    @Test
    void aContinuationDoesNotSeparateAValueFromItsKey() {
        rewriteRun(
          docker(
            """
              FROM alpine:latest
              ENV KEY\
              =value
              """,
            spec -> spec.afterRecipe(doc -> {
                var env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
                Docker.Env.EnvPair pair = assertThat(env.getPairs()).singleElement().actual();
                assertThat(pair.isHasEquals()).isTrue();
                assertThat(ArgumentContents.text(pair.getValue())).isEqualTo("value");
            })
          )
        );
    }

    private static Docker.Argument envValue(Docker.File doc) {
        var env = (Docker.Env) doc.getStages().getFirst().getInstructions().getLast();
        return assertThat(env.getPairs()).singleElement().actual().getValue();
    }
}
