/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

@SuppressWarnings({"TypeAnnotation", "JavaMutatorMethodAccessedAsParameterless"})
class FieldAccessTest implements RewriteTest {

    @Test
    void simpleFieldAccess() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj = new Object()
                val field = obj.toString
              }
              """
          )
        );
    }

    @Test
    void chainedFieldAccess() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = System.out.println
              }
              """
          )
        );
    }

    @Test
    void nestedFieldAccess() {
        rewriteRun(
          scala(
            """
              object Test {
                val deep = java.lang.System.out
              }
              """
          )
        );
    }

    @Test
    void packageFieldAccess() {
        rewriteRun(
          scala(
            """
              object Test {
                val pkg = scala.collection.mutable
              }
              """
          )
        );
    }

    @Test
    void thisFieldAccess() {
        rewriteRun(
          scala(
            """
              class Test {
                val field = "test"
                val ref = this.field
              }
              """
          )
        );
    }

    @Test
    void fieldAccessWithParentheses() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = (System.out).println
              }
              """
          )
        );
    }

    @Test
    void fieldAccessInExpression() {
        rewriteRun(
          scala(
            """
              object Test {
                val length = "hello".length + 1
              }
              """
          )
        );
    }

    @Test
    void fieldAccessAsMethodArgument() {
        rewriteRun(
          scala(
            """
              object Test {
                println(System.currentTimeMillis)
              }
              """
          )
        );
    }
}