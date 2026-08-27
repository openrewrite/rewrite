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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ReplaceConstantWithAnotherConstant;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ReplaceConstantWithAnotherConstantTest implements RewriteTest {

    @Test
    void replaceConstantOnSameOwner() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.io.File.separator")),
          kotlin(
            """
              import java.io.File

              class Test {
                  fun foo() {
                      println(File.pathSeparator)
                      println(java.io.File.pathSeparator)
                  }
              }
              """,
            """
              import java.io.File

              class Test {
                  fun foo() {
                      println(File.separator)
                      println(File.separator)
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceConstantOnAnotherOwner() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.util.jar.JarFile.MANIFEST_NAME")),
          kotlin(
            """
              import java.io.File

              class Test {
                  fun foo() {
                      println(File.pathSeparator)
                  }
              }
              """,
            """
              import java.util.jar.JarFile

              class Test {
                  fun foo() {
                      println(JarFile.MANIFEST_NAME)
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceImportedConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.util.jar.JarFile.MANIFEST_NAME")),
          kotlin(
            """
              import java.io.File.pathSeparator

              class Test {
                  fun foo() {
                      println(pathSeparator)
                  }
              }
              """,
            """
              import java.util.jar.JarFile.MANIFEST_NAME

              class Test {
                  fun foo() {
                      println(MANIFEST_NAME)
                  }
              }
              """
          )
        );
    }
}
