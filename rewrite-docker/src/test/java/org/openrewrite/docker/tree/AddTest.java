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

class AddTest implements RewriteTest {

    @Test
    void addInstruction() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ADD app.tar.gz /app/
              """,
            spec -> spec.afterRecipe(doc -> {
                var add = (Docker.Add) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(add.getShellForm()).isNotNull();
                assertThat(add.getShellForm().getSources()).hasSize(1);
                assertThat(((Docker.Literal) add.getShellForm().getSources().getFirst().getContents().getFirst()).getText()).isEqualTo("app.tar.gz");
                assertThat(((Docker.Literal) add.getShellForm().getDestination().getContents().getFirst()).getText()).isEqualTo("/app/");
            })
          )
        );
    }

    @Test
    void addWithMultipleSources() {
        // ADD with multiple source files
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ADD file1.txt file2.txt file3.tar.gz /data/
              """,
            spec -> spec.afterRecipe(doc -> {
                var add = (Docker.Add) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(add.getShellForm()).isNotNull();
                assertThat(add.getExecForm()).isNull();
                assertThat(add.getHeredoc()).isNull();

                // Verify sources
                assertThat(add.getShellForm().getSources()).hasSize(3);
                assertThat(((Docker.Literal) add.getShellForm().getSources().getFirst().getContents().getFirst()).getText())
                    .isEqualTo("file1.txt");
                assertThat(((Docker.Literal) add.getShellForm().getSources().get(1).getContents().getFirst()).getText())
                    .isEqualTo("file2.txt");
                assertThat(((Docker.Literal) add.getShellForm().getSources().getLast().getContents().getFirst()).getText())
                    .isEqualTo("file3.tar.gz");

                // Verify destination
                assertThat(((Docker.Literal) add.getShellForm().getDestination().getContents().getFirst()).getText())
                    .isEqualTo("/data/");
            })
          )
        );
    }

    @Test
    void addWithWildcards() {
        // ADD with wildcard patterns
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ADD *.txt /docs/
              ADD config?.yaml /etc/app/
              """,
            spec -> spec.afterRecipe(doc -> {
                var instructions = doc.getStages().getFirst().getInstructions();

                // First ADD with * wildcard
                var add1 = (Docker.Add) instructions.getFirst();
                assertThat(add1.getShellForm()).isNotNull();
                assertThat(add1.getShellForm().getSources()).hasSize(1);
                assertThat(((Docker.Literal) add1.getShellForm().getSources().getFirst().getContents().getFirst()).getText())
                    .isEqualTo("*.txt");
                assertThat(((Docker.Literal) add1.getShellForm().getDestination().getContents().getFirst()).getText())
                    .isEqualTo("/docs/");

                // Second ADD with ? wildcard
                var add2 = (Docker.Add) instructions.getLast();
                assertThat(add2.getShellForm()).isNotNull();
                assertThat(add2.getShellForm().getSources()).hasSize(1);
                assertThat(((Docker.Literal) add2.getShellForm().getSources().getFirst().getContents().getFirst()).getText())
                    .isEqualTo("config?.yaml");
                assertThat(((Docker.Literal) add2.getShellForm().getDestination().getContents().getFirst()).getText())
                    .isEqualTo("/etc/app/");
            })
          )
        );
    }

    @Test
    void addWithFlags() {
        // ADD with --chown and --chmod flags
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ADD --chown=user:group archive.tar.gz /app/
              ADD --chmod=755 script.sh /usr/local/bin/
              ADD --chown=root --chmod=644 config.conf /etc/
              """,
            spec -> spec.afterRecipe(doc -> {
                var instructions = doc.getStages().getFirst().getInstructions();

                // First ADD with --chown
                var add1 = (Docker.Add) instructions.getFirst();
                assertThat(add1.getFlags()).hasSize(1);
                assertThat(add1.getFlags().getFirst().getName()).isEqualTo("chown");
                assertThat(((Docker.Literal) add1.getFlags().getFirst().getValue().getContents().getFirst()).getText())
                    .isEqualTo("user:group");
                assertThat(add1.getShellForm()).isNotNull();
                assertThat(((Docker.Literal) add1.getShellForm().getSources().getFirst().getContents().getFirst()).getText())
                    .isEqualTo("archive.tar.gz");

                // Second ADD with --chmod
                var add2 = (Docker.Add) instructions.get(1);
                assertThat(add2.getFlags()).hasSize(1);
                assertThat(add2.getFlags().getFirst().getName()).isEqualTo("chmod");
                assertThat(((Docker.Literal) add2.getFlags().getFirst().getValue().getContents().getFirst()).getText())
                    .isEqualTo("755");
                assertThat(add2.getShellForm()).isNotNull();
                assertThat(((Docker.Literal) add2.getShellForm().getSources().getFirst().getContents().getFirst()).getText())
                    .isEqualTo("script.sh");

                // Third ADD with both --chown and --chmod
                var add3 = (Docker.Add) instructions.getLast();
                assertThat(add3.getFlags()).hasSize(2);
                assertThat(add3.getFlags().getFirst().getName()).isEqualTo("chown");
                assertThat(((Docker.Literal) add3.getFlags().getFirst().getValue().getContents().getFirst()).getText())
                    .isEqualTo("root");
                assertThat(add3.getFlags().getLast().getName()).isEqualTo("chmod");
                assertThat(((Docker.Literal) add3.getFlags().getLast().getValue().getContents().getFirst()).getText())
                    .isEqualTo("644");
            })
          )
        );
    }

    @Test
    void addWithArrayNotation() {
        // ADD with exec form (JSON array notation)
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ADD ["source file.txt", "another file.txt", "/dest dir/"]
              """,
            spec -> spec.afterRecipe(doc -> {
                var add = (Docker.Add) doc.getStages().getFirst().getInstructions().getLast();

                // Verify exec form is used
                assertThat(add.getExecForm()).isNotNull();
                assertThat(add.getShellForm()).isNull();
                assertThat(add.getHeredoc()).isNull();

                // Verify arguments
                var args = add.getExecForm().getArguments();
                assertThat(args).hasSize(3);

                // First source (with space in name)
                assertThat(args.getFirst().getText()).isEqualTo("source file.txt");
                assertThat(args.getFirst().getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);

                // Second source (with space in name)
                assertThat(args.get(1).getText()).isEqualTo("another file.txt");
                assertThat(args.get(1).getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);

                // Destination (with space in path)
                assertThat(args.getLast().getText()).isEqualTo("/dest dir/");
                assertThat(args.getLast().getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
            })
          )
        );
    }

    @Test
    void addWithArrayNotationAndFlags() {
        // ADD with flags and exec form
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ADD --chown=app:app ["config.json", "/app/config/"]
              """,
            spec -> spec.afterRecipe(doc -> {
                var add = (Docker.Add) doc.getStages().getFirst().getInstructions().getLast();

                // Verify flags
                assertThat(add.getFlags()).hasSize(1);
                assertThat(add.getFlags().getFirst().getName()).isEqualTo("chown");
                assertThat(((Docker.Literal) add.getFlags().getFirst().getValue().getContents().getFirst()).getText())
                    .isEqualTo("app:app");

                // Verify exec form
                assertThat(add.getExecForm()).isNotNull();
                assertThat(add.getShellForm()).isNull();

                var args = add.getExecForm().getArguments();
                assertThat(args).hasSize(2);
                assertThat(args.getFirst().getText()).isEqualTo("config.json");
                assertThat(args.getLast().getText()).isEqualTo("/app/config/");
            })
          )
        );
    }
}
