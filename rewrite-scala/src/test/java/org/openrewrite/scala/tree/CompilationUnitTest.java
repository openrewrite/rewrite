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

class CompilationUnitTest implements RewriteTest {

    @Test
    void emptyFile() {
        rewriteRun(
          scala("")
        );
    }

    @Test
    void singleStatement() {
        rewriteRun(
          scala("val x = 42")
        );
    }

    @Test
    void withPackage() {
        rewriteRun(
          scala(
            """
            package com.example
            
            val x = 42
            """
          )
        );
    }

    @Test
    void withNestedPackage() {
        rewriteRun(
          scala(
            """
            package com.example.scala
            
            val message = "Hello"
            """
          )
        );
    }

    @Test
    void withImports() {
        rewriteRun(
          scala(
            """
            package com.example
            
            import scala.collection.mutable
            import java.util.List
            
            val x = 42
            """
          )
        );
    }

    @Test
    void multipleStatements() {
        rewriteRun(
          scala(
            """
            val x = 1
            val y = 2
            val z = x + y
            """
          )
        );
    }

    @Test
    void withComments() {
        rewriteRun(
          scala(
            """
            // This is a comment
            val x = 42
            
            /* Multi-line
               comment */
            val y = 84
            """
          )
        );
    }

    @Test
    void withDocComment() {
        rewriteRun(
          scala(
            """
            /** This is a doc comment
              * for the value below
              */
            val important = 42
            """
          )
        );
    }

    @Test
    void multipleFilesGetDistinctPaths() {
        // Regression: sourcePathFromSourceText used to hard-code "file.scala" which caused
        // multi-file tests to collide on the same path and garble each other on print.
        rewriteRun(
          scala(
            """
            package com.example

            class A {
              def use(b: com.other.B): String = b.value
            }
            """
          ),
          scala(
            """
            package com.other

            class B {
              val value: String = "hi"
            }
            """
          )
        );
    }

    @Test
    void packageObjectWithMembers() {
        rewriteRun(
          scala(
            """
            package com.example

            package object pkg {
              type Alias = String

              val Constant: Int = 42
            }
            """,
            spec -> spec.path("package.scala")
          )
        );
    }

    @Test
    void withTrailingWhitespace() {
        rewriteRun(
          scala(
            """
            val x = 42
            
            
            """
          )
        );
    }
}
