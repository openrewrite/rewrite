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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.javascript.Assertions.javascript;

public class PrintIssueTest implements RewriteTest {

    @Test
    void findMethods(@TempDir Path tempDir) {
        rewriteRun(
          javascript(
            """
              import Stream from 'node:stream';
              import {promisify} from 'node:util';
              import {
                    Buffer
              } from 'node:buffer';
              
              const pipeline = promisify(Stream.pipeline);
              const INTERNALS = Symbol('Body internals');
              
              class Body {
                  async arrayBuffer() {
                      const {buffer, byteOffset, byteLength} = await consumeBody(this);
                      return buffer.slice(byteOffset, byteOffset + byteLength);
                  }
              
                  async json() {
                      const text = await this.text();
                      return JSON.parse(text);
                  }
              
                  async blob() {
                      const ct = (this.headers && this.headers.get('content-type')) || (this[INTERNALS].body && this[INTERNALS].body.type) || '';
                      const buf = await this.arrayBuffer();
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
//                    rpc.traceGetObject(true, true);
                    requireNonNull(run.getChangeset().getAllResults().getFirst().getBefore()).printAll();
                    requireNonNull(run.getChangeset().getAllResults().getFirst().getAfter()).printAll();
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
