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
package org.openrewrite.python;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.python.tree.Py;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.python;

class ShebangIntegTest implements RewriteTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .log(tempDir.resolve("python-rpc.log"))
                .traceRpcMessages()
        );
    }

    @AfterEach
    void after() throws IOException {
        PythonRewriteRpc.shutdownCurrent();
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder());
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shebangRoundTripsAndIsAFirstClassStatement() {
        rewriteRun(
                python(
                        """
                        #!/usr/bin/env python3
                        print("Hello, world!")
                        """,
                        spec -> spec.afterRecipe(cu ->
                                assertThat(cu.getStatements().get(0))
                                        .isInstanceOfSatisfying(Py.Shebang.class, shebang ->
                                                assertThat(shebang.getText()).isEqualTo("#!/usr/bin/env python3")))
                )
        );
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void python2ShebangRoutesThroughParsoParserAndIsAShebang() {
        rewriteRun(
                python(
                        """
                        #!/usr/bin/env python2
                        print 'Hello, world!'
                        """,
                        spec -> spec.afterRecipe(cu ->
                                assertThat(cu.getStatements().get(0))
                                        .isInstanceOfSatisfying(Py.Shebang.class, shebang ->
                                                assertThat(shebang.getText()).isEqualTo("#!/usr/bin/env python2")))
                )
        );
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shebangWithBlankLineAndComment() {
        rewriteRun(
                python(
                        """
                        #!/usr/bin/env python3

                        # a comment
                        x = 1
                        """
                )
        );
    }
}
