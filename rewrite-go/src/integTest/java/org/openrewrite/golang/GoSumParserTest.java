/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.golang;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.tree.GoSum;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.golang.Assertions.goSum;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GoSumParserTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void singleModulePair() {
        rewriteRun(
                goSum(
                        """
                                github.com/stretchr/testify v1.8.0 h1:aaaa=
                                github.com/stretchr/testify v1.8.0/go.mod h1:bbbb=
                                """,
                        spec -> spec.afterRecipe(doc -> {
                            assertThat(doc.getLines()).hasSize(2);
                            GoSum.Line zip = doc.getLines().get(0).getElement();
                            assertThat(zip.getModulePath()).isEqualTo("github.com/stretchr/testify");
                            assertThat(zip.getVersion()).isEqualTo("v1.8.0");
                            assertThat(zip.isGoMod()).isFalse();
                            assertThat(zip.getHash()).isEqualTo("h1:aaaa=");

                            GoSum.Line mod = doc.getLines().get(1).getElement();
                            assertThat(mod.isGoMod()).isTrue();
                            assertThat(mod.getHash()).isEqualTo("h1:bbbb=");
                        })
                )
        );
    }

    @Test
    void multipleModulesWithBlankLine() {
        rewriteRun(
                goSum(
                        """
                                github.com/a/b v1.0.0 h1:aaaa=
                                github.com/a/b v1.0.0/go.mod h1:bbbb=

                                github.com/c/d v2.3.4 h1:cccc=
                                github.com/c/d v2.3.4/go.mod h1:dddd=
                                """,
                        spec -> spec.afterRecipe(doc -> assertThat(doc.getLines()).hasSize(4))
                )
        );
    }
}
