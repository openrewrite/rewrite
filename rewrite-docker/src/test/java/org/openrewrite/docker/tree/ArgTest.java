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

class ArgTest implements RewriteTest {

    @Test
    void simpleArg() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION=1.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                var arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getName().getText()).isEqualTo("VERSION");
                assertThat(arg.getValue()).isNotNull();
                assertThat(((Docker.Literal) arg.getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void argWithoutValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION
              """,
            spec -> spec.afterRecipe(doc -> {
                var arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getName().getText()).isEqualTo("VERSION");
                assertThat(arg.getValue()).isNull();
            })
          )
        );
    }

    @Test
    void argInstructions() {
        rewriteRun(
          docker(
            """
              ARG BASE_IMAGE=ubuntu:20.04
              FROM ${BASE_IMAGE}
              ARG VERSION
              """
          )
        );
    }

    @Test
    void globalArg() {
        rewriteRun(
          docker(
            """
              ARG VERSION=25
              FROM ubuntu:${VERSION}
              """,
            spec -> spec.afterRecipe(doc -> {
                assertThat(doc.getGlobalArgs()).hasSize(1);
                Docker.Arg globalArg = doc.getGlobalArgs().getFirst();
                assertThat(globalArg.getName().getText()).isEqualTo("VERSION");
            })
          )
        );
    }

    @Test
    void doubleQuotedValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION="1.0.0"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                Docker.Literal literal = (Docker.Literal) value.getContents().getFirst();
                assertThat(literal.getText()).isEqualTo("1.0.0");
                assertThat(literal.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(value.getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void singleQuotedValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION='1.0.0'
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                Docker.Literal literal = (Docker.Literal) value.getContents().getFirst();
                assertThat(literal.getText()).isEqualTo("1.0.0");
                assertThat(literal.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.SINGLE);
                assertThat(value.getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void unquotedValueHasNoQuoteStyle() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION=1.0.0
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(((Docker.Literal) value.getContents().getFirst()).getQuoteStyle()).isNull();
                assertThat(value.getQuoteStyle()).isNull();
                assertThat(value.getText()).isEqualTo("1.0.0");
            })
          )
        );
    }

    @Test
    void quotedValueWithSpaces() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG DESCRIPTION="some value"
              """,
            spec -> spec.afterRecipe(doc -> assertThat(argValue(doc).getText()).isEqualTo("some value"))
          )
        );
    }

    @Test
    void emptyQuotedValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG EMPTY=""
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getText()).isEmpty();
                assertThat(value.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
            })
          )
        );
    }

    @Test
    void environmentVariableValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION=$BASE
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) value.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("BASE");
                assertThat(var.isBraced()).isFalse();
                assertThat(value.hasEnvironmentVariables()).isTrue();
                assertThat(value.getText()).isNull();
                assertThat(value.getTextWithVariables()).isEqualTo("$BASE");
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
              ARG VERSION=${BASE}-suffix
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(2);
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) value.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("BASE");
                assertThat(var.isBraced()).isTrue();
                assertThat(((Docker.Literal) value.getContents().getLast()).getText()).isEqualTo("-suffix");
                assertThat(value.getText()).isNull();
                assertThat(value.getTextWithVariables()).isEqualTo("${BASE}-suffix");
            })
          )
        );
    }

    @Test
    void partiallyQuotedValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION=a"b"c
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(3);
                assertThat(value.getText()).isEqualTo("abc");
            })
          )
        );
    }

    @Test
    void hashInValueIsNotAComment() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG URL=http://example.com/x#fragment
              """,
            spec -> spec.afterRecipe(doc -> assertThat(argValue(doc).getText()).isEqualTo("http://example.com/x#fragment"))
          )
        );
    }

    @Test
    void flagValuedArgKeepsQuotesInTheModel() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG OPTS=--flag="a b"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(2);
                assertThat(((Docker.Literal) value.getContents().getFirst()).getText()).isEqualTo("--flag=");
                Docker.Literal quoted = (Docker.Literal) value.getContents().getLast();
                assertThat(quoted.getText()).isEqualTo("a b");
                assertThat(quoted.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(value.getText()).isEqualTo("--flag=a b");
            })
          )
        );
    }

    @Test
    void singleQuotedFlagValuedArg() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG OPTS=--flag='a b'
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(((Docker.Literal) value.getContents().getLast()).getQuoteStyle())
                  .isEqualTo(Docker.Literal.QuoteStyle.SINGLE);
                assertThat(value.getText()).isEqualTo("--flag=a b");
            })
          )
        );
    }

    @Test
    void flagValuedArgWithEnvironmentVariable() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG OPTS=--flag=$VAR
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.hasEnvironmentVariables()).isTrue();
                assertThat(value.getText()).isNull();
                assertThat(value.getTextWithVariables()).isEqualTo("--flag=$VAR");
            })
          )
        );
    }

    @Test
    void flagValuedArgWithTrailingText() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG OPTS=--flag="a b",more
              """,
            spec -> spec.afterRecipe(doc -> assertThat(argValue(doc).getText()).isEqualTo("--flag=a b,more"))
          )
        );
    }

    @Test
    void emptyQuotedFlagValuedArg() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG OPTS=--flag=""
              """,
            spec -> spec.afterRecipe(doc -> assertThat(argValue(doc).getText()).isEqualTo("--flag="))
          )
        );
    }

    private static Docker.Argument argValue(Docker.File doc) {
        Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
        return assertThat(arg.getValue()).isNotNull().actual();
    }
}
