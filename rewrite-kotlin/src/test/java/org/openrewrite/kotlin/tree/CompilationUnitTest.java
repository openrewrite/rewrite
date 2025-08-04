/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.kotlin.Assertions.kotlin;

class CompilationUnitTest implements RewriteTest {

    @Test
    void emptyFile() {
        rewriteRun(
          kotlin("")
        );
    }

    @Test
    void packageDecl() {
        rewriteRun(
          kotlin("package kotlin")
        );
    }

    @Test
    void imports() {
        rewriteRun(
          kotlin(
            """
              import java.util.List
              import java.io.*
              class A
              """
          )
        );
    }

    @Test
    void packageAndComments() {
        rewriteRun(
          kotlin(
            """
              /* C0 */
              package a
              import java.util.List

              class A /*C1*/
              // C2
              """,
            SourceSpec::noTrim
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "class Foo",
      "val x = 1",
      """
        import java.util.Objects

        class A
        """,
      """
        package x.y

        class A
        """,
      """
        package x.y
        import java.util.Objects

        class A
        """
    })
    void shebang(String fileContent) {
        rewriteRun(
          kotlin("""
            #!/usr/bin/env who

            %s
            """.formatted(fileContent))
        );
    }
}
