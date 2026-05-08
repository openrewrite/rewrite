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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class ExportTest implements RewriteTest {

    @Test
    void singleExport() {
        rewriteRun(
          scala(
            """
            class C(inner: Inner) {
              export inner.foo
            }
            """
          )
        );
    }

    @Test
    void nestedPathExport() {
        rewriteRun(
          scala(
            """
            class C(a: A) {
              export a.b.c
            }
            """
          )
        );
    }

    @Test
    void wildcardExportStar() {
        rewriteRun(
          scala(
            """
            class C(inner: Inner) {
              export inner.*
            }
            """
          )
        );
    }

    @Test
    void wildcardExportUnderscore() {
        // `_` is the legacy wildcard form still accepted by the compiler;
        // exports themselves are Scala 3 only.
        rewriteRun(
          scala(
            """
            class C(inner: Inner) {
              export inner._
            }
            """
          )
        );
    }

    @Test
    void multipleExports() {
        rewriteRun(
          scala(
            """
            class C(a: A, b: B) {
              export a.foo
              export b.bar
            }
            """
          )
        );
    }
}
