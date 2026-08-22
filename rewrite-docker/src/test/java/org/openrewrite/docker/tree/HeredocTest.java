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

class HeredocTest implements RewriteTest {

    @Test
    void heredocWithShellPath() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF
              addgroup -S docker
              adduser -S --shell /bin/bash --ingroup docker vscode
              EOF
              """
          )
        );
    }

    @Test
    void heredocWithMultipleLines() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF
              apt-get update
              apt-get install -y curl
              EOF
              """
          )
        );
    }

    @Test
    void heredocWithDash() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<-FILE_END
              echo "Hello World"
              echo "Another line"
              FILE_END
              """
          )
        );
    }

    @Test
    void tabIndentedTerminatorClosesADashHeredoc() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<-FILE_END
              	echo "Hello World"
              	FILE_END
              """
          )
        );
    }

    @Test
    void indentedTerminatorIsContentOfAPlainHeredoc() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF
                echo "Hello World"
                EOF
              EOF
              """
          )
        );
    }

    @Test
    void multipleHeredocsInRunCommand() {
        // Multiple heredocs chained together with && - a common pattern for creating multiple files
        // See: https://github.com/Bindernews/minblur/blob/7915e7d8765eb3785da4fabda38e744702ec5985/docker/Dockerfile#L13
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF1 cat >file1.sh &&\\
                  <<EOF2 cat >file2.sh &&\\
                  chmod +x file1.sh file2.sh
              #!/bin/bash
              echo "script 1"
              EOF1
              #!/bin/bash
              echo "script 2"
              EOF2
              """,
            spec -> spec.afterRecipe(file -> {
                var run = (Docker.Run) file.getStages().getFirst().getInstructions().getLast();

                // Verify the command is a HeredocForm
                assertThat(run.getCommand()).isInstanceOf(Docker.HeredocForm.class);
                var heredoc = (Docker.HeredocForm) run.getCommand();

                // Verify the preamble contains heredoc markers and commands
                String preamble = heredoc.getPreamble();
                assertThat(preamble).isEqualTo("""
                  <<EOF1 cat >file1.sh &&\\
                      <<EOF2 cat >file2.sh &&\\
                      chmod +x file1.sh file2.sh\
                  """);

                // Verify we have two heredoc bodies
                assertThat(heredoc.getBodies()).hasSize(2);

                // Verify first heredoc body (EOF1)
                var body1 = heredoc.getBodies().getFirst();
                assertThat(body1.getOpening()).isEqualTo("<<EOF1");
                assertThat(body1.getClosing()).isEqualTo("EOF1");
                assertThat(body1.getContentLines()).anyMatch(line -> line.contains("script 1"));

                // Verify second heredoc body (EOF2)
                var body2 = heredoc.getBodies().getLast();
                assertThat(body2.getOpening()).isEqualTo("<<EOF2");
                assertThat(body2.getClosing()).isEqualTo("EOF2");
                assertThat(body2.getContentLines()).anyMatch(line -> line.contains("script 2"));
            })
          )
        );
    }

    @Test
    void heredocPreambleKeepsAHashWord() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF cat > /etc/motd #banner
              echo "Hello World"
              EOF
              """
          )
        );
    }

    @Test
    void heredocPreambleKeepsAPathWithTwoSlashes() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF cp //a/b dst
              echo "Hello World"
              EOF
              """
          )
        );
    }

    /// A preamble ends at the escape character its file names, and holds the other of the two as text.
    @Test
    void heredocPreambleKeepsTheCharacterThatIsNotTheEscape() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF cat >f1 `
              echo "Hello World"
              EOF
              """,
            spec -> spec.path("linux/Dockerfile").afterRecipe(doc ->
              assertThat(preamble(doc)).isEqualTo("<<EOF cat >f1 `"))
          ),
          docker(
            """
              # escape=`
              FROM ubuntu:20.04
              RUN <<EOF cat >f1 \\
              echo "Hello World"
              EOF
              """,
            spec -> spec.path("windows/Dockerfile").afterRecipe(doc ->
              assertThat(preamble(doc)).isEqualTo("<<EOF cat >f1 \\"))
          )
        );
    }

    @Test
    void heredocPreambleContinuesOnTheEscapeCharacter() {
        rewriteRun(
          docker(
            """
              # escape=`
              FROM ubuntu:20.04
              RUN <<EOF cat >f1 &&`
                  chmod +x f1
              echo "Hello World"
              EOF
              """,
            spec -> spec.afterRecipe(doc ->
              assertThat(preamble(doc)).isEqualTo("<<EOF cat >f1 &&`\n    chmod +x f1"))
          )
        );
    }

    @Test
    void heredocPreambleKeepsACStyleComment() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF cat /*x*/ y
              echo "Hello World"
              EOF
              """
          )
        );
    }

    @Test
    void heredocWithCRLF() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\r\n" +
            "RUN <<EOF\r\n" +
            "echo hi\r\n" +
            "EOF\r\n",
            spec -> spec.afterRecipe(file -> {
                var run = (Docker.Run) file.getStages().getFirst().getInstructions().getLast();
                var heredoc = (Docker.HeredocForm) run.getCommand();

                // The carriage return ends the preamble line, so it belongs to neither the
                // preamble nor the marker, but to the whitespace ahead of the body
                assertThat(heredoc.getPreamble()).isEqualTo("<<EOF");
                assertThat(heredoc.getBodies()).hasSize(1);
                var body = heredoc.getBodies().getFirst();
                assertThat(body.getPrefix().getWhitespace()).isEqualTo("\r\n");
                assertThat(body.getContentLines()).containsExactly("echo hi\r\n");
                assertThat(body.getClosing()).isEqualTo("EOF");
            })
          )
        );
    }

    @Test
    void heredocWithCRLFBeforeAnotherInstruction() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\r\n" +
            "RUN <<EOF\r\n" +
            "echo hi\r\n" +
            "EOF\r\n" +
            "RUN echo done\r\n"
          )
        );
    }

    @Test
    void heredocWithMultipleLinesCRLF() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\r\n" +
            "RUN <<EOF\r\n" +
            "apt-get update\r\n" +
            "apt-get install -y curl\r\n" +
            "EOF\r\n" +
            "RUN echo done\r\n"
          )
        );
    }

    @Test
    void heredocWithDashCRLF() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\r\n" +
            "RUN <<-FILE_END\r\n" +
            "echo \"Hello World\"\r\n" +
            "echo \"Another line\"\r\n" +
            "FILE_END\r\n" +
            "RUN echo done\r\n"
          )
        );
    }

    @Test
    void heredocWithEmptyBodyCRLF() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\r\n" +
            "RUN <<EOF\r\n" +
            "EOF\r\n" +
            "RUN echo done\r\n"
          )
        );
    }

    @Test
    void heredocInCopyWithCRLF() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\r\n" +
            "COPY <<EOF /app/config.txt\r\n" +
            "some content\r\n" +
            "EOF\r\n" +
            "RUN echo done\r\n",
            spec -> spec.afterRecipe(file -> {
                var copy = (Docker.Copy) file.getStages().getFirst().getInstructions().get(0);
                assertThat(copy.getHeredoc()).isNotNull();
                assertThat(copy.getHeredoc().getPreamble()).isEqualTo("<<EOF");
                assertThat(((Docker.Literal) copy.getHeredoc().getDestination().getContents().getFirst()).getText())
                  .isEqualTo("/app/config.txt");
                assertThat(copy.getHeredoc().getBodies().getFirst().getContentLines())
                  .containsExactly("some content\r\n");
            })
          )
        );
    }

    @Test
    void multipleHeredocsInRunCommandCRLF() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\r\n" +
            "RUN <<EOF1 cat >file1.sh &&\\\r\n" +
            "    <<EOF2 cat >file2.sh &&\\\r\n" +
            "    chmod +x file1.sh file2.sh\r\n" +
            "#!/bin/bash\r\n" +
            "echo \"script 1\"\r\n" +
            "EOF1\r\n" +
            "#!/bin/bash\r\n" +
            "echo \"script 2\"\r\n" +
            "EOF2\r\n" +
            "RUN echo done\r\n",
            spec -> spec.afterRecipe(file -> {
                var run = (Docker.Run) file.getStages().getFirst().getInstructions().get(0);
                var heredoc = (Docker.HeredocForm) run.getCommand();

                // The line continuations inside the preamble keep their carriage returns
                assertThat(heredoc.getPreamble()).isEqualTo(
                  "<<EOF1 cat >file1.sh &&\\\r\n" +
                  "    <<EOF2 cat >file2.sh &&\\\r\n" +
                  "    chmod +x file1.sh file2.sh");

                assertThat(heredoc.getBodies()).hasSize(2);
                var body1 = heredoc.getBodies().getFirst();
                assertThat(body1.getClosing()).isEqualTo("EOF1");
                assertThat(body1.getContentLines()).containsExactly("#!/bin/bash\r\n", "echo \"script 1\"\r\n");
                var body2 = heredoc.getBodies().getLast();
                assertThat(body2.getClosing()).isEqualTo("EOF2");
                assertThat(body2.getContentLines()).containsExactly("\r\n", "#!/bin/bash\r\n", "echo \"script 2\"\r\n");
            })
          )
        );
    }

    @Test
    void heredocWithTrailingWhitespaceAfterMarker() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\n" +
            "RUN <<EOF \n" +
            "echo hi\n" +
            "EOF\n" +
            "RUN echo done\n"
          )
        );
    }

    @Test
    void carriageReturnInsideAHeredocLineIsContent() {
        rewriteRun(
          docker(
            "FROM ubuntu:20.04\n" +
            "RUN <<EOF\n" +
            "echo a\rb\n" +
            "EOF\n" +
            "RUN echo done\n",
            spec -> spec.afterRecipe(file -> {
                var run = (Docker.Run) file.getStages().getFirst().getInstructions().get(0);
                var heredoc = (Docker.HeredocForm) run.getCommand();
                assertThat(heredoc.getBodies().getFirst().getContentLines()).containsExactly("echo a\rb\n");
            })
          )
        );
    }

    @Test
    void quotedDelimiterOnCopy() {
        rewriteRun(
          docker(
            """
              FROM scratch
              COPY --chmod=0644 <<'EOF' /etc/config.toml
              a = 1
              EOF
              """,
            spec -> spec.afterRecipe(file -> {
                var copy = (Docker.Copy) file.getStages().getFirst().getInstructions().getFirst();
                assertThat(copy.getHeredoc()).isNotNull();
                assertThat(copy.getHeredoc().getPreamble()).isEqualTo("<<'EOF'");
                assertThat(((Docker.Literal) copy.getHeredoc().getDestination().getContents().getFirst()).getText())
                  .isEqualTo("/etc/config.toml");
                assertThat(copy.getHeredoc().getBodies()).hasSize(1);
                assertThat(copy.getHeredoc().getBodies().getFirst().getClosing()).isEqualTo("EOF");
            })
          )
        );
    }

    @Test
    void quotedDelimiterOnRun() {
        rewriteRun(
          docker(
            """
              FROM scratch
              RUN <<'EOF'
              echo hi
              EOF
              """,
            spec -> spec.afterRecipe(file -> assertThat(preamble(file)).isEqualTo("<<'EOF'"))
          )
        );
    }

    @Test
    void doubleQuotedDelimiterOnADashHeredoc() {
        rewriteRun(
          docker(
            """
              FROM scratch
              RUN <<-"EOF"
              \techo hi
              \tEOF
              """,
            spec -> spec.afterRecipe(file -> assertThat(preamble(file)).isEqualTo("<<-\"EOF\""))
          )
        );
    }

    @Test
    void aQuoteOfTheOtherKindBelongsToTheDelimiter() {
        rewriteRun(
          docker(
            """
              FROM scratch
              RUN <<"it's"
              echo hi
              it's
              RUN echo after
              """,
            spec -> spec.afterRecipe(file -> {
                var instructions = file.getStages().getFirst().getInstructions();
                var heredoc = (Docker.HeredocForm) ((Docker.Run) instructions.getFirst()).getCommand();
                assertThat(heredoc.getBodies()).hasSize(1);
                assertThat(heredoc.getBodies().getFirst().getClosing()).isEqualTo("it's");
                assertThat(instructions).hasSize(2);
            })
          )
        );
    }

    @Test
    void aHereStringInThePreambleOpensNothing() {
        rewriteRun(
          docker(
            """
              FROM scratch
              RUN <<EOF psql <<<'select 1'
              select 2
              EOF
              """,
            spec -> spec.afterRecipe(file -> assertThat(preamble(file)).isEqualTo("<<EOF psql <<<'select 1'"))
          )
        );
    }

    @Test
    void partlyQuotedDelimiterNamesTheWholeWord() {
        rewriteRun(
          docker(
            """
              FROM scratch
              RUN <<E'O'F
              echo hi
              EOF
              """,
            spec -> spec.afterRecipe(file -> {
                var heredoc = (Docker.HeredocForm) ((Docker.Run) file.getStages().getFirst().getInstructions()
                        .getFirst()).getCommand();
                assertThat(heredoc.getBodies().getFirst().getOpening()).isEqualTo("<<E'O'F");
                assertThat(heredoc.getBodies().getFirst().getClosing()).isEqualTo("EOF");
            })
          )
        );
    }

    @Test
    void heredocOpenedAfterARedirect() {
        rewriteRun(
          docker(
            """
              FROM scratch
              RUN cat >> /etc/hosts <<EOF
              127.0.0.1 localhost
              EOF
              """,
            spec -> spec.afterRecipe(file -> assertThat(preamble(file)).isEqualTo("cat >> /etc/hosts <<EOF"))
          )
        );
    }

    @Test
    void multipleHeredocsOpenedAfterRedirects() {
        rewriteRun(
          docker(
            """
              FROM scratch
              RUN cat >/a <<E1 && cat >/b <<E2
              one
              E1
              two
              E2
              """,
            spec -> spec.afterRecipe(file -> {
                var heredoc = (Docker.HeredocForm) ((Docker.Run) file.getStages().getFirst().getInstructions()
                        .getFirst()).getCommand();
                assertThat(heredoc.getPreamble()).isEqualTo("cat >/a <<E1 && cat >/b <<E2");
                assertThat(heredoc.getBodies()).hasSize(2);
                assertThat(heredoc.getBodies().getFirst().getContentLines()).containsExactly("one\n");
                assertThat(heredoc.getBodies().getLast().getContentLines()).contains("two\n");
            })
          )
        );
    }

    private static String preamble(Docker.File doc) {
        var run = (Docker.Run) doc.getStages().getFirst().getInstructions().getLast();
        return ((Docker.HeredocForm) run.getCommand()).getPreamble();
    }
}
