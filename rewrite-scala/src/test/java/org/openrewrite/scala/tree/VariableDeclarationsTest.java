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

class VariableDeclarationsTest implements RewriteTest {

    @Test
    void valDeclaration() {
        rewriteRun(
          scala("val x = 5")
        );
    }

    @Test
    void varDeclaration() {
        rewriteRun(
          scala("var y = 10")
        );
    }

    @Test
    void valWithTypeAnnotation() {
        rewriteRun(
          scala("val x: Int = 5")
        );
    }

    @Test
    void varWithTypeAnnotation() {
        rewriteRun(
          scala("var y: String = \"hello\"")
        );
    }

    @Test
    void lazyVal() {
        rewriteRun(
          scala("lazy val z = compute()")
        );
    }

    @Test
    void patternDeclaration() {
        rewriteRun(
          scala("val (a, b) = (1, 2)")
        );
    }

    @Test
    void multipleDeclarations() {
        rewriteRun(
          scala(
            """
            val x = 1
            val y = 2
            val z = 3
            """
          )
        );
    }

    @Test
    void privateVal() {
        rewriteRun(
          scala("private val secret = 42")
        );
    }

    @Test
    void protectedVar() {
        rewriteRun(
          scala("protected var count = 0")
        );
    }

    @Test
    void finalVal() {
        rewriteRun(
          scala("final val constant = 3.14")
        );
    }

    @Test
    void valWithComplexType() {
        rewriteRun(
          scala("val list: List[Int] = List(1, 2, 3)")
        );
    }

    @Test
    void valWithNoInitializer() {
        rewriteRun(
          scala("var x: Int = _")
        );
    }
}