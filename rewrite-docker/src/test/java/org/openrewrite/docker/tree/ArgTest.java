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
                Docker.Arg.ArgPair pair = onlyPair(doc);
                assertThat(pair.getName().getText()).isEqualTo("VERSION");
                assertThat(pair.getValue()).isNotNull();
                assertThat(((Docker.Literal) pair.getValue().getContents().getFirst()).getText()).isEqualTo("1.0.0");
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
                Docker.Arg.ArgPair pair = onlyPair(doc);
                assertThat(pair.getName().getText()).isEqualTo("VERSION");
                assertThat(pair.getValue()).isNull();
            })
          )
        );
    }

    @Test
    void severalNames() {
        rewriteRun(
          docker(
            """
              FROM scratch
              ARG TARGETOS TARGETARCH
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getPairs()).satisfiesExactly(
                  os -> {
                      assertThat(os.getName().getText()).isEqualTo("TARGETOS");
                      assertThat(os.getValue()).isNull();
                  },
                  arch -> {
                      assertThat(arch.getName().getText()).isEqualTo("TARGETARCH");
                      assertThat(arch.getValue()).isNull();
                  });
            })
          )
        );
    }

    @Test
    void severalNamesWithDefaults() {
        rewriteRun(
          docker(
            """
              FROM scratch
              ARG a=1 b=2
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getPairs()).satisfiesExactly(
                  a -> {
                      assertThat(a.getName().getText()).isEqualTo("a");
                      assertThat(ArgumentContents.text(a.getValue())).isEqualTo("1");
                  },
                  b -> {
                      assertThat(b.getName().getText()).isEqualTo("b");
                      assertThat(ArgumentContents.text(b.getValue())).isEqualTo("2");
                  });
            })
          )
        );
    }

    @Test
    void severalNamesOnlySomeWithDefaults() {
        rewriteRun(
          docker(
            """
              FROM scratch
              ARG a b=2 c
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getPairs()).satisfiesExactly(
                  a -> {
                      assertThat(a.getName().getText()).isEqualTo("a");
                      assertThat(a.getValue()).isNull();
                  },
                  b -> {
                      assertThat(b.getName().getText()).isEqualTo("b");
                      assertThat(ArgumentContents.text(b.getValue())).isEqualTo("2");
                  },
                  c -> {
                      assertThat(c.getName().getText()).isEqualTo("c");
                      assertThat(c.getValue()).isNull();
                  });
            })
          )
        );
    }

    @Test
    void whitespaceAfterADefaultBeginsAnotherName() {
        rewriteRun(
          docker(
            """
              FROM scratch
              ARG a=hello world
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getPairs()).satisfiesExactly(
                  a -> assertThat(ArgumentContents.text(a.getValue())).isEqualTo("hello"),
                  world -> {
                      assertThat(world.getName().getText()).isEqualTo("world");
                      assertThat(world.getValue()).isNull();
                  });
            })
          )
        );
    }

    @Test
    void severalNamesSpanningAContinuation() {
        rewriteRun(
          docker(
            """
              FROM scratch
              ARG a \
                  b=2
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getPairs()).satisfiesExactly(
                  a -> assertThat(a.getValue()).isNull(),
                  b -> assertThat(ArgumentContents.text(b.getValue())).isEqualTo("2"));
            })
          )
        );
    }

    @Test
    void severalGlobalNames() {
        rewriteRun(
          docker(
            """
              ARG TARGETOS TARGETARCH
              FROM scratch
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getGlobalArgs()).singleElement()
              .satisfies(arg -> assertThat(arg.getPairs()).hasSize(2)))
          )
        );
    }

    /// Docker joins the lines a continuation holds together before it reads what they say.
    @Test
    void aDefaultBoundAcrossAContinuation() {
        rewriteRun(
          docker(
            """
              FROM scratch
              ARG a=\\
              1
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getPairs()).singleElement().satisfies(a ->
                  assertThat(ArgumentContents.text(a.getValue())).isEqualTo("1"));
            })
          )
        );
    }

    /// The continuation stays in the text of the default, as it does in the value of an `ENV`.
    @Test
    void aDefaultSplitByAContinuation() {
        rewriteRun(
          docker(
            """
              FROM scratch
              ARG a=he\\
              llo
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getPairs()).singleElement().satisfies(a ->
                  assertThat(ArgumentContents.text(a.getValue())).isEqualTo("he\\\nllo"));
            })
          )
        );
    }

    @Test
    void emptyDefault() {
        rewriteRun(
          docker(
            """
              ARG MAKE_JOBS=
              FROM scratch
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg.ArgPair pair = doc.getGlobalArgs().getFirst().getPairs().getFirst();
                assertThat(pair.getName().getText()).isEqualTo("MAKE_JOBS");
                assertThat(pair.getValue()).isNotNull();
                assertThat(ArgumentContents.text(pair.getValue())).isEmpty();
            })
          )
        );
    }

    @Test
    void emptyDefaultAmongOthers() {
        rewriteRun(
          docker(
            """
              FROM scratch
              ARG a= b=2 c
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(arg.getPairs()).satisfiesExactly(
                  a -> assertThat(ArgumentContents.text(a.getValue())).isEmpty(),
                  b -> assertThat(ArgumentContents.text(b.getValue())).isEqualTo("2"),
                  c -> assertThat(c.getValue()).isNull());
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
                assertThat(globalArg.getPairs().getFirst().getName().getText()).isEqualTo("VERSION");
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
                assertThat(ArgumentContents.text(value)).isEqualTo("1.0.0");
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
                assertThat(ArgumentContents.text(value)).isEqualTo("1.0.0");
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
                assertThat(ArgumentContents.quoteStyle(value)).isNull();
                assertThat(ArgumentContents.text(value)).isEqualTo("1.0.0");
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
            spec -> spec.afterRecipe(doc -> assertThat(ArgumentContents.text(argValue(doc))).isEqualTo("some value"))
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
                assertThat(ArgumentContents.text(value)).isEmpty();
                assertThat(ArgumentContents.quoteStyle(value)).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
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
                assertThat(ArgumentContents.containsVariable(value)).isTrue();
                assertThat(ArgumentContents.text(value)).isNull();
                assertThat(ArgumentContents.textWithVariables(value)).isEqualTo("$BASE");
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
                assertThat(ArgumentContents.text(value)).isNull();
                assertThat(ArgumentContents.textWithVariables(value)).isEqualTo("${BASE}-suffix");
            })
          )
        );
    }

    @Test
    void partiallyQuotedValueKeepsItsQuotes() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG VERSION=a"b"c
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(1);
                assertThat(ArgumentContents.quoteStyle(value)).isNull();
                assertThat(ArgumentContents.text(value)).isEqualTo("a\"b\"c");
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
            spec -> spec.afterRecipe(doc -> assertThat(ArgumentContents.text(argValue(doc))).isEqualTo("http://example.com/x#fragment"))
          )
        );
    }

    @Test
    void valueThatLooksLikeAnOptionIsKeptWhole() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG OPTS=--flag="a b"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(1);
                assertThat(ArgumentContents.quoteStyle(value)).isNull();
                assertThat(ArgumentContents.text(value)).isEqualTo("--flag=\"a b\"");
            })
          )
        );
    }

    @Test
    void valueSpanningWhitespaceIsOneLiteral() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG OPTS="a b",more
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(1);
                assertThat(ArgumentContents.text(value)).isEqualTo("\"a b\",more");
            })
          )
        );
    }

    @Test
    void variableInsideDoubleQuotedValueIsSplitOut() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG GREETING="pre $V post"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(3);
                assertThat(((Docker.Literal) value.getContents().get(0)).getText()).isEqualTo("\"pre ");
                assertThat(((Docker.EnvironmentVariable) value.getContents().get(1)).getName()).isEqualTo("V");
                assertThat(((Docker.Literal) value.getContents().get(2)).getText()).isEqualTo(" post\"");
                assertThat(ArgumentContents.quoteStyle(value)).isNull();
                assertThat(ArgumentContents.containsVariable(value)).isTrue();
                assertThat(ArgumentContents.textWithVariables(value)).isEqualTo("\"pre $V post\"");
            })
          )
        );
    }

    @Test
    void variableInsideSingleQuotedValueStaysLiteral() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG GREETING='pre $V post'
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(1);
                assertThat(ArgumentContents.quoteStyle(value)).isEqualTo(Docker.Literal.QuoteStyle.SINGLE);
                assertThat(ArgumentContents.containsVariable(value)).isFalse();
                assertThat(ArgumentContents.text(value)).isEqualTo("pre $V post");
            })
          )
        );
    }

    @Test
    void escapedDollarInQuotedValueStaysLiteral() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG GREETING="pre \\$V post"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(1);
                assertThat(ArgumentContents.quoteStyle(value)).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(ArgumentContents.containsVariable(value)).isFalse();
            })
          )
        );
    }

    @Test
    void quotedValueWithoutVariablesKeepsItsQuoteStyle() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG GREETING="no vars here"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(1);
                assertThat(ArgumentContents.quoteStyle(value)).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(ArgumentContents.text(value)).isEqualTo("no vars here");
            })
          )
        );
    }

    @Test
    void lowercaseEnvironmentVariableValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG version=$base
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) value.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("base");
                assertThat(var.isBraced()).isFalse();
                assertThat(ArgumentContents.containsVariable(value)).isTrue();
                assertThat(ArgumentContents.text(value)).isNull();
                assertThat(ArgumentContents.textWithVariables(value)).isEqualTo("$base");
            })
          )
        );
    }

    @Test
    void mixedCaseEnvironmentVariableValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ARG version=$Java_Version
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument value = argValue(doc);
                assertThat(value.getContents()).hasSize(1);
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) value.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("Java_Version");
                assertThat(var.isBraced()).isFalse();
                assertThat(ArgumentContents.containsVariable(value)).isTrue();
                assertThat(ArgumentContents.text(value)).isNull();
                assertThat(ArgumentContents.textWithVariables(value)).isEqualTo("$Java_Version");
            })
          )
        );
    }

    private static Docker.Arg.ArgPair onlyPair(Docker.File doc) {
        Docker.Arg arg = (Docker.Arg) doc.getStages().getFirst().getInstructions().getLast();
        return assertThat(arg.getPairs()).singleElement().actual();
    }

    private static Docker.Argument argValue(Docker.File doc) {
        return assertThat(onlyPair(doc).getValue()).isNotNull().actual();
    }
}
