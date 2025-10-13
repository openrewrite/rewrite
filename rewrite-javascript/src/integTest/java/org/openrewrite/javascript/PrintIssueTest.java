/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.RecipeRun;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.javascript.Assertions.javascript;

public class PrintIssueTest implements RewriteTest {

    @Test
    void findMethods(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          javascript(
            """
              class Body {
                  async blob() {
                      await this.arrayBuffer();
                  }
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                Path log = tempDir.resolve("rpc.log");
                JavaScriptRewriteRpc.shutdownCurrent();
                JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder().log(log)
                    .traceRpcMessages()
//                  .inspectBrk()
                );

                RecipeRun run = new FindMethods("* *(..)", false)
                  .run(new InMemoryLargeSourceSet(List.of(cu)), new InMemoryExecutionContext());

                try {
                    RewriteRpc rpc = JavaScriptRewriteRpc.getOrStart();
                    Files.writeString(log, "PRINTING AFTER --------------------\n", StandardOpenOption.APPEND);
                    requireNonNull(run.getChangeset().getAllResults().getFirst().getAfter()).printAll();
                    rpc.traceGetObject(true, true);
                    Files.writeString(log, "PRINTING BEFORE --------------------\n", StandardOpenOption.APPEND);
                    requireNonNull(run.getChangeset().getAllResults().getFirst().getBefore()).printAll();
                } finally {
                    if (Files.exists(log)) {
                        System.out.println(Files.readString(log));
                    }
                }
            })
          )
        );
    }
}
