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
package org.openrewrite.docker;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.docker.tree.Comment;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.docker.Assertions.docker;
import static org.openrewrite.test.RewriteTest.toRecipe;

/// Each test here mutates a parsed tree the way a recipe assembling one by hand would, and asserts
/// that validation says so. Mutating a real parse rather than driving a recipe is deliberate: the
/// validation a recipe result goes through only runs when the recipe changed the printed source, and
/// several of these defects change nothing about what the tree prints.
class AssertionsTest implements RewriteTest {

    @Test
    void catchesATreeThatPrintsCorrectlyButModelsTheWrongThing() {
        assertThatThrownBy(() -> rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new DockerIsoVisitor<ExecutionContext>() {
              @Override
              public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                  // Appending the tag to the image name prints as "alpine:3.14", but leaves the tag
                  // unmodelled, so From#getTag stays null.
                  Docker.Literal name = (Docker.Literal) from.getImageName().getContents().getFirst();
                  if (name.getText().contains(":")) {
                      return from;
                  }
                  return from.withImageName(from.getImageName().withContents(
                    singletonList(name.withText(name.getText() + ":3.14"))));
              }
          })),
          docker(
            """
              FROM alpine
              """,
            """
              FROM alpine:3.14
              """
          )
        )).isInstanceOf(AssertionError.class)
          .hasMessageContaining("Expected the tree to model what it prints");
    }

    /// The other tests here call the structural checks directly, so this one proves that a recipe
    /// result reaches them at all.
    @Test
    void validatesTheTreeARecipeProduces() {
        assertThatThrownBy(() -> rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new DockerIsoVisitor<ExecutionContext>() {
              @Override
              public Docker.Run visitRun(Docker.Run run, ExecutionContext ctx) {
                  return "RUN".equals(run.getKeyword()) ? run.withKeyword("EXEC") : run;
              }
          })),
          docker(
            """
              FROM alpine
              RUN echo hi
              """,
            """
              FROM alpine
              EXEC echo hi
              """
          )
        )).isInstanceOf(AssertionError.class)
          .hasMessageContaining("expected the keyword of a Run to be RUN");
    }

    @Test
    void catchesASubtreeCopiedWithoutFreshIds() {
        UUID shared = randomId();
        assertMalformed(
          """
            FROM alpine
            RUN echo hi
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Literal visitLiteral(Docker.Literal literal, ExecutionContext ctx) {
                  return literal.withId(shared);
              }
          },
          "expected every element to have its own id");
    }

    @Test
    void catchesAKeywordThatDisagreesWithItsInstruction() {
        assertMalformed(
          """
            FROM alpine
            RUN echo hi
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Run visitRun(Docker.Run run, ExecutionContext ctx) {
                  return run.withKeyword("EXEC");
              }
          },
          "expected the keyword of a Run to be RUN");
    }

    @Test
    void catchesAKeywordThatCarriesWhitespace() {
        assertMalformed(
          """
            FROM alpine AS build
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.From.As visitFromAs(Docker.From.As as, ExecutionContext ctx) {
                  return as.withKeyword(" AS");
              }
          },
          "expected the keyword of a As to be AS");
    }

    @Test
    void catchesAnInstructionThatDoesNotStartItsOwnLine() {
        assertMalformed(
          """
            FROM alpine
            RUN echo hi
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Run visitRun(Docker.Run run, ExecutionContext ctx) {
                  return run.withPrefix(Space.SINGLE_SPACE);
              }
          },
          "expected the Run to start its own line");
    }

    @Test
    void catchesAStageThatDoesNotStartItsOwnLine() {
        assertMalformed(
          """
            ARG VERSION=3.14
            FROM alpine:${VERSION}
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Stage visitStage(Docker.Stage stage, ExecutionContext ctx) {
                  return stage.withPrefix(Space.EMPTY).withFrom(stage.getFrom().withPrefix(Space.SINGLE_SPACE));
              }
          },
          "expected the Stage to start its own line");
    }

    @Test
    void catchesAGlobalArgThatDoesNotStartItsOwnLine() {
        assertMalformed(
          """
            ARG VERSION=3.14
            ARG DIGEST=sha256
            FROM alpine:${VERSION}
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Arg visitArg(Docker.Arg arg, ExecutionContext ctx) {
                  return "DIGEST".equals(arg.getName().getText()) ? arg.withPrefix(Space.SINGLE_SPACE) : arg;
              }
          },
          "expected the Arg to start its own line");
    }

    @Test
    void catchesAFlagNameThatAlreadyCarriesItsDashes() {
        assertMalformed(
          """
            FROM --platform=linux/amd64 alpine
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Flag visitFlag(Docker.Flag flag, ExecutionContext ctx) {
                  return flag.withName("--" + flag.getName());
              }
          },
          "expected the flag name \"--platform\" to be bare");
    }

    @Test
    void catchesFlagsOnAHealthcheckNoneThatThePrinterDrops() {
        assertMalformed(
          """
            FROM alpine
            HEALTHCHECK NONE
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Healthcheck visitHealthcheck(Docker.Healthcheck healthcheck, ExecutionContext ctx) {
                  return healthcheck.withFlags(singletonList(new Docker.Flag(
                    randomId(), Space.SINGLE_SPACE, Markers.EMPTY, "interval", null)));
              }
          },
          "expected HEALTHCHECK NONE to carry no flags");
    }

    @Test
    void catchesAnExecFormArgumentThatLostItsQuotes() {
        assertMalformed(
          """
            FROM alpine
            ENTRYPOINT ["java", "-jar", "app.jar"]
            """,
          unquoteLiterals(),
          "expected the JSON array element \"java\" to be double quoted");
    }

    @Test
    void catchesAShellArgumentThatLostItsQuotes() {
        assertMalformed(
          """
            FROM alpine
            SHELL ["/bin/bash", "-c"]
            """,
          unquoteLiterals(),
          "expected the JSON array element \"/bin/bash\" to be double quoted");
    }

    @Test
    void catchesAJsonFormVolumeValueThatLostItsQuotes() {
        assertMalformed(
          """
            FROM alpine
            VOLUME ["/data"]
            """,
          unquoteLiterals(),
          "expected the JSON array element \"/data\" to be double quoted");
    }

    @Test
    void catchesAPortThatDisagreesWithItsText() {
        assertMalformed(
          """
            FROM alpine
            EXPOSE 8080/udp
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Port visitPort(Docker.Port port, ExecutionContext ctx) {
                  return port.withStart(9090);
              }
          },
          "expected the port \"8080/udp\" to model 8080 UDP, but it models 9090 UDP");
    }

    @Test
    void catchesAPortOutsideTheRangeAPortCanHave() {
        assertMalformed(
          """
            FROM alpine
            EXPOSE 8080
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Port visitPort(Docker.Port port, ExecutionContext ctx) {
                  return port.withText("99999").withStart(99999);
              }
          },
          "to be between 0 and 65535");
    }

    @Test
    void catchesAPortWhoseProtocolDockerDoesNotKnow() {
        assertMalformed(
          """
            FROM alpine
            EXPOSE 8080/tcp
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Port visitPort(Docker.Port port, ExecutionContext ctx) {
                  return port.withText("8080/sctp");
              }
          },
          "expected the port \"8080/sctp\" to name a protocol Docker knows");
    }

    @Test
    void catchesAPortWhoseTextIsNotAPortAtAll() {
        assertMalformed(
          """
            FROM alpine
            EXPOSE 8080
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Port visitPort(Docker.Port port, ExecutionContext ctx) {
                  return port.withText("http");
              }
          },
          "expected the port \"http\" to hold a port number or a range of them");
    }

    @Test
    void catchesAHeredocThatOpensWithoutAMarker() {
        assertMalformed(
          """
            FROM alpine
            RUN <<EOF
            echo hi
            EOF
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.HeredocForm visitHeredocForm(Docker.HeredocForm form, ExecutionContext ctx) {
                  return form.withBodies(ListUtils.map(form.getBodies(), body -> body.withOpening("EOF")));
              }
          },
          "expected the heredoc opening \"EOF\" to start with \"<<\"");
    }

    @Test
    void catchesAHeredocThatClosesWithADifferentMarker() {
        assertMalformed(
          """
            FROM alpine
            RUN <<EOF
            echo hi
            EOF
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.HeredocForm visitHeredocForm(Docker.HeredocForm form, ExecutionContext ctx) {
                  return form.withBodies(ListUtils.map(form.getBodies(), body -> body.withClosing("DONE")));
              }
          },
          "expected the heredoc opened by \"EOF\" to close with it, but it closes with \"DONE\"");
    }

    @Test
    void catchesAHeredocBodyWithNoMarkerToOpenIt() {
        assertMalformed(
          """
            FROM alpine
            RUN <<EOF
            echo hi
            EOF
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.HeredocForm visitHeredocForm(Docker.HeredocForm form, ExecutionContext ctx) {
                  return form.withPreamble(form.getPreamble().replace("<<EOF", "cat"));
              }
          },
          "to open one heredoc per body, but it opens 0 for 1 bodies");
    }

    @Test
    void catchesAHeredocBodyPulledOntoThePreambleLine() {
        assertMalformed(
          """
            FROM alpine
            RUN <<EOF
            echo hi
            EOF
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.HeredocForm visitHeredocForm(Docker.HeredocForm form, ExecutionContext ctx) {
                  return form.withBodies(ListUtils.map(form.getBodies(), body -> body.withPrefix(Space.EMPTY)));
              }
          },
          "expected the heredoc opened by the preamble \"<<EOF\" to begin on the line after it");
    }

    @Test
    void catchesAHeredocLineThatSwallowsTheNextOne() {
        assertMalformed(
          """
            FROM alpine
            RUN <<EOF
            echo hi
            EOF
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.HeredocForm visitHeredocForm(Docker.HeredocForm form, ExecutionContext ctx) {
                  return form.withBodies(ListUtils.map(form.getBodies(), body -> body.withContentLines(
                    ListUtils.map(body.getContentLines(), line -> line.replace("\n", "")))));
              }
          },
          "to end with a newline");
    }

    @Test
    void catchesACommentThatSwallowsWhatFollowsIt() {
        assertMalformed(
          """
            # syntax=docker/dockerfile:1
            FROM alpine
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, ExecutionContext ctx) {
                  return space.getComments().isEmpty() ? space : space.withWhitespace(" ");
              }
          },
          "expected a newline after the comment \"# syntax=docker/dockerfile:1\"");
    }

    @Test
    void catchesACommentThatIsNotOne() {
        assertMalformed(
          """
            # syntax=docker/dockerfile:1
            FROM alpine
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, ExecutionContext ctx) {
                  return space.withComments(ListUtils.map(space.getComments(),
                    comment -> comment.withText(comment.getText().substring(1))));
              }
          },
          "to start with \"#\"");
    }

    @Test
    void catchesACommentSpanningMoreThanItsOwnLine() {
        assertMalformed(
          """
            # first
            FROM alpine
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, ExecutionContext ctx) {
                  return space.withComments(ListUtils.map(space.getComments(),
                    comment -> comment.withText(comment.getText() + "\nRUN echo hi")));
              }
          },
          "to hold a single line");
    }

    @Test
    void catchesACommentPrecededBySomethingOtherThanWhitespace() {
        assertMalformed(
          """
            # first
            FROM alpine
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, ExecutionContext ctx) {
                  return space.withComments(ListUtils.map(space.getComments(),
                    comment -> new Comment(comment.getText(), "RUN echo hi", Markers.EMPTY)));
              }
          },
          "to be whitespace, but it is \"RUN echo hi\"");
    }

    @Test
    void catchesAnArgumentWhoseContentsAreSplitApart() {
        assertMalformed(
          """
            FROM alpine
            ENV KEY=prefix$SUFFIX
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Argument visitArgument(Docker.Argument argument, ExecutionContext ctx) {
                  return argument.getContents().size() < 2 ? argument :
                    argument.withContents(ListUtils.map(argument.getContents(),
                      (i, content) -> i == 0 ? content : content.withPrefix(Space.SINGLE_SPACE)));
              }
          },
          "to be contiguous");
    }

    @Test
    void catchesAnImageNameWithNothingInIt() {
        assertMalformed(
          """
            FROM alpine
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                  return from.withImageName(from.getImageName().withContents(emptyList()));
              }
          },
          "expected the image name of a From to hold at least one content");
    }

    @Test
    void catchesAWorkdirPathWithNothingInIt() {
        assertMalformed(
          """
            FROM alpine
            WORKDIR /app
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Workdir visitWorkdir(Docker.Workdir workdir, ExecutionContext ctx) {
                  return workdir.withPath(workdir.getPath().withContents(emptyList()));
              }
          },
          "expected the path of a Workdir to hold at least one content");
    }

    @Test
    void catchesACopyDestinationWithNothingInIt() {
        assertMalformed(
          """
            FROM alpine
            COPY app.jar /app/
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker visitCopyShellForm(Docker.CopyShellForm form, ExecutionContext ctx) {
                  return form.withDestination(form.getDestination().withContents(emptyList()));
              }
          },
          "expected the destination of a CopyShellForm to hold at least one content");
    }

    @Test
    void catchesAQuotedLiteralHoldingTheQuoteThatEndsIt() {
        assertMalformed(
          """
            FROM alpine
            ENTRYPOINT ["java"]
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Literal visitLiteral(Docker.Literal literal, ExecutionContext ctx) {
                  return literal.withText("ja\"va");
              }
          },
          "to hold no unescaped \"");
    }

    @Test
    void catchesASingleQuotedLiteralHoldingTheQuoteThatEndsIt() {
        assertMalformed(
          """
            FROM alpine
            ENV KEY='value'
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Literal visitLiteral(Docker.Literal literal, ExecutionContext ctx) {
                  return literal.getQuoteStyle() == Docker.Literal.QuoteStyle.SINGLE ?
                    literal.withText("va'lue") : literal;
              }
          },
          "to hold no unescaped '");
    }

    @Test
    void catchesALiteralHoldingTheWhitespaceAroundIt() {
        assertMalformed(
          """
            FROM alpine
            WORKDIR /app
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Literal visitLiteral(Docker.Literal literal, ExecutionContext ctx) {
                  return literal.withText(literal.getText() + " ");
              }
          },
          "to hold only its value, but it starts or ends with whitespace");
    }

    /// An ARG's name, like an ENV's key, is a whole value of its own rather than one content of an
    /// argument, and the check reaches it just the same.
    @Test
    void catchesAValueOfItsOwnHoldingTheWhitespaceAroundIt() {
        assertMalformed(
          """
            FROM alpine
            ARG VERSION=1
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.Arg visitArg(Docker.Arg arg, ExecutionContext ctx) {
                  return arg.withName(arg.getName().withText(arg.getName().getText() + " "));
              }
          },
          "to hold only its value, but it starts or ends with whitespace");
    }

    @Test
    void catchesWhitespaceAroundAValueThatASeparatorSplitIntoParts() {
        assertMalformed(
          """
            FROM ubuntu:22.04
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                  return from.withImageName(from.getImageName().withContents(ListUtils.mapFirst(
                    from.getImageName().getContents(),
                    content -> ((Docker.Literal) content).withText(" ubuntu"))));
              }
          },
          "to hold only its value, but it starts or ends with whitespace");
    }

    /// A quoted literal's quotes delimit it, so whitespace inside them is what the value means.
    @Test
    void acceptsAQuotedLiteralHoldingWhitespaceOfItsOwn() {
        Assertions.assertWellFormed(parse("FROM alpine\nENV KEY=\" value \"\n"));
    }

    /// An ONBUILD's instruction, and a HEALTHCHECK's CMD, correctly sit on the same line as the
    /// instruction that introduces them.
    @Test
    void acceptsAnInstructionSharingALineWithTheOneThatIntroducesIt() {
        Assertions.assertWellFormed(parse(
          """
            FROM alpine
            ONBUILD RUN echo hi
            HEALTHCHECK CMD curl -f http://localhost/
            """));
    }

    /// Nothing follows the last comment in a file, so nothing can be swallowed into it.
    @Test
    void acceptsAFileEndingInACommentWithNoNewlineAfterIt() {
        Assertions.assertWellFormed(parse("FROM alpine\n# no newline after this"));
    }

    /// That exemption is only from needing a newline. A trailing comment lives in the file's `eof`
    /// [Space], which every other check still reaches.
    @Test
    void catchesATrailingCommentThatIsNotOne() {
        assertMalformed(
          """
            FROM alpine
            # trailing
            """,
          new DockerIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, ExecutionContext ctx) {
                  return space.withComments(ListUtils.map(space.getComments(),
                    comment -> comment.withText(comment.getText().substring(1))));
              }
          },
          "to start with \"#\"");
    }

    /// The same defect in three places the checks reach separately: an exec form, a SHELL and a
    /// JSON form VOLUME.
    private static DockerIsoVisitor<ExecutionContext> unquoteLiterals() {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.Literal visitLiteral(Docker.Literal literal, ExecutionContext ctx) {
                return literal.withQuoteStyle(null);
            }
        };
    }

    private static void assertMalformed(@Language("dockerfile") String source,
                                        DockerIsoVisitor<ExecutionContext> mutation,
                                        String violation) {
        SourceFile parsed = parse(source);
        Assertions.assertWellFormed(parsed);
        SourceFile mutated = (SourceFile) requireNonNull(mutation.visit(parsed, new InMemoryExecutionContext()));
        assertThatThrownBy(() -> Assertions.assertWellFormed(mutated))
          .isInstanceOf(AssertionError.class)
          .hasMessageContaining(violation);
    }

    private static SourceFile parse(@Language("dockerfile") String source) {
        return DockerParser.builder().build()
          .parse(new InMemoryExecutionContext(), source)
          .findFirst()
          .orElseThrow();
    }
}
