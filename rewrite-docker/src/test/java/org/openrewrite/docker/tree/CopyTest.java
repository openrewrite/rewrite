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

class CopyTest implements RewriteTest {

    @Test
    void simpleCopy() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY app.jar /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                var copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(copy.getSources()).hasSize(1);
                assertThat(((Docker.Literal) copy.getSources().getFirst().getContents().getFirst()).getText()).isEqualTo("app.jar");
                assertThat(((Docker.Literal) copy.getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }

    @Test
    void copyWithFlags() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY --chown=app:app app.jar /app/
              COPY --from=builder /build/output /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                List<Docker.Instruction> instructions = doc.getStages().getFirst().getInstructions();
                var copy1 = (Docker.Copy) instructions.getFirst();
                assertThat(copy1.getFlags()).hasSize(1);
                assertThat(copy1.getFlags().getFirst().getName()).isEqualTo("chown");
                assertThat(((Docker.Literal) copy1.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("app:app");
                var source = (Docker.Literal) copy1.getSources().getFirst().getContents().getFirst();
                assertThat(source.getText()).isEqualTo("app.jar");
                var destination = (Docker.Literal) copy1.getDestination().getContents().getFirst();
                assertThat(destination.getText()).isEqualTo("/app/");

                var copy2 = (Docker.Copy) instructions.getLast();
                assertThat(copy2.getFlags()).hasSize(1);
                assertThat(copy2.getFlags().getFirst().getName()).isEqualTo("from");
                assertThat(((Docker.Literal) copy2.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("builder");
            })
          )
        );
    }

    @Test
    void copyWithHeredoc() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY <<EOF /app/config.txt
              # Configuration file
              setting1=value1
              setting2=value2
              EOF
              """
          )
        );
    }

    @Test
    void copyWithLinkAndFromFlags() {
        // Test COPY with both --link and --from flags (common pattern in multi-stage builds)
        rewriteRun(
          docker(
            """
              FROM scratch
              ENTRYPOINT [ "app.wasm" ]
              COPY --link --from=build /app.wasm /app.wasm
              """
          )
        );
    }

    @Test
    void heredocWithBash() {
        // Test heredoc with bash as command
        // Note: EOT must not be indented - heredoc terminators must match exactly
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOT bash
              set -ex
              apt-get update
              EOT
              """
          )
        );
    }

    @Test
    void copyWithMultipleSources() {
        // COPY with multiple source files should have separate Argument elements
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY file1.txt file2.txt file3.txt /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                var copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();
                // Should have 3 separate source arguments, not 1 combined argument
                assertThat(copy.getSources()).hasSize(3);
                assertThat(((Docker.Literal) copy.getSources().get(0).getContents().getFirst()).getText()).isEqualTo("file1.txt");
                assertThat(((Docker.Literal) copy.getSources().get(1).getContents().getFirst()).getText()).isEqualTo("file2.txt");
                assertThat(((Docker.Literal) copy.getSources().get(2).getContents().getFirst()).getText()).isEqualTo("file3.txt");
                assertThat(((Docker.Literal) copy.getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }

    @Test
    void copyWithMultipleSourcesAndFlags() {
        // COPY with flags and multiple source files
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY --chown=app:app config1.yaml config2.yaml /etc/app/
              """,
            spec -> spec.afterRecipe(doc -> {
                var copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(copy.getFlags()).hasSize(1);
                assertThat(copy.getFlags().getFirst().getName()).isEqualTo("chown");
                assertThat(copy.getSources()).hasSize(2);
                assertThat(((Docker.Literal) copy.getSources().get(0).getContents().getFirst()).getText()).isEqualTo("config1.yaml");
                assertThat(((Docker.Literal) copy.getSources().get(1).getContents().getFirst()).getText()).isEqualTo("config2.yaml");
            })
          )
        );
    }

    @Test
    void copyHeredocEofInOpeningNotDestination() {
        // Verify that the EOF marker is in the HeredocForm.opening field, not in destination
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY <<EOF /app/config.txt
              some content
              EOF
              """,
            spec -> spec.afterRecipe(doc -> {
                var copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(copy.getHeredoc()).isNotNull();

                // The preamble should contain "<<EOF"
                assertThat(copy.getHeredoc().getPreamble()).isEqualTo("<<EOF");

                // The destination should contain "/app/config.txt", NOT the EOF marker
                assertThat(copy.getHeredoc().getDestination()).isNotNull();
                assertThat(((Docker.Literal) copy.getHeredoc().getDestination().getContents().getFirst()).getText())
                    .isEqualTo("/app/config.txt");

                // Should have exactly one body
                assertThat(copy.getHeredoc().getBodies()).hasSize(1);
                var body = copy.getHeredoc().getBodies().getFirst();

                // Content should be the actual content (includes trailing newline)
                assertThat(body.getContentLines()).containsExactly("some content\n");

                // Closing should be "EOF"
                assertThat(body.getClosing()).isEqualTo("EOF");
            })
          )
        );
    }

    @Test
    void copyHeredocWithDifferentMarker() {
        // Verify heredoc with custom marker (not EOF)
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY <<CONFIG /etc/app/settings.ini
              key=value
              CONFIG
              """,
            spec -> spec.afterRecipe(doc -> {
                var copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(copy.getHeredoc()).isNotNull();
                assertThat(copy.getHeredoc().getPreamble()).isEqualTo("<<CONFIG");
                assertThat(copy.getHeredoc().getDestination()).isNotNull();
                assertThat(((Docker.Literal) copy.getHeredoc().getDestination().getContents().getFirst()).getText())
                    .isEqualTo("/etc/app/settings.ini");
                assertThat(copy.getHeredoc().getBodies()).hasSize(1);
                assertThat(copy.getHeredoc().getBodies().getFirst().getClosing()).isEqualTo("CONFIG");
            })
          )
        );
    }

    @Test
    void copyExecFormWithMultipleSources() {
        // COPY with exec form (JSON array) containing multiple sources and one destination
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY ["src/file1.txt", "src/file2.txt", "config.yaml", "/app/"]
              """,
            spec -> spec.afterRecipe(doc -> {
                var copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();

                // Verify exec form is used (not shell form or heredoc)
                assertThat(copy.getExecForm()).isNotNull();
                assertThat(copy.getSources()).isNull();
                assertThat(copy.getDestination()).isNull();
                assertThat(copy.getHeredoc()).isNull();

                // Get the arguments from exec form
                var args = copy.getExecForm().getArguments();

                // Should have 4 arguments: 3 sources + 1 destination
                assertThat(args).hasSize(4);

                // Verify each argument is a quoted literal with correct text
                // First source
                assertThat(args.get(0).getText()).isEqualTo("src/file1.txt");
                assertThat(args.get(0).getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);

                // Second source
                assertThat(args.get(1).getText()).isEqualTo("src/file2.txt");
                assertThat(args.get(1).getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);

                // Third source
                assertThat(args.get(2).getText()).isEqualTo("config.yaml");
                assertThat(args.get(2).getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);

                // Destination (last element)
                assertThat(args.get(3).getText()).isEqualTo("/app/");
                assertThat(args.get(3).getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
            })
          )
        );
    }

    @Test
    void copyExecFormWithFlagsAndMultipleSources() {
        // COPY exec form with --from flag and multiple sources
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              COPY --from=builder ["bin/app", "lib/helper.so", "/usr/local/bin/"]
              """,
            spec -> spec.afterRecipe(doc -> {
                var copy = (Docker.Copy) doc.getStages().getFirst().getInstructions().getLast();

                // Verify flags
                assertThat(copy.getFlags()).hasSize(1);
                assertThat(copy.getFlags().getFirst().getName()).isEqualTo("from");
                assertThat(((Docker.Literal) copy.getFlags().getFirst().getValue().getContents().getFirst()).getText())
                    .isEqualTo("builder");

                // Verify exec form
                assertThat(copy.getExecForm()).isNotNull();
                var args = copy.getExecForm().getArguments();
                assertThat(args).hasSize(3);

                // Sources
                assertThat(args.get(0).getText()).isEqualTo("bin/app");
                assertThat(args.get(1).getText()).isEqualTo("lib/helper.so");

                // Destination
                assertThat(args.get(2).getText()).isEqualTo("/usr/local/bin/");
            })
          )
        );
    }
}
