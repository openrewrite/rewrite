/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ArrayTest implements RewriteTest {

    @Test
    void notInitialized() {
        rewriteRun(
          kotlin("val arr = IntArray ( 3 )")
        );
    }

    @Test
    void arrayWithTypeParameter() {
        rewriteRun(
          kotlin("val arr = Array < Int > ( 3 ) { 0 }")
        );
    }

    @Test
    void initialized() {
        rewriteRun(
          kotlin("val arr = Array ( 3 ) { i -> i * 1 }")
        );
    }

    @Test
    void constructed() {
        rewriteRun(
          kotlin("val arr = Array ( 3 , { i -> i * 1 } )")
        );
    }

    @Test
    void twoDimensional() {
        rewriteRun(
          kotlin("val arr = Array ( 1 ) { Array < Int > ( 2 ) { 3 } }")
        );
    }

    @Test
    void arrayAccess() {
        rewriteRun(
          kotlin(
            """
              val arr = IntArray ( 1 )
              val a = arr [ 0 ]
              """
          )
        );
    }

    @Test
    void conditionalArraySize() {
        rewriteRun(
          kotlin(
            """
              val arr = IntArray ( if (true) 0 else 1 )
              """
          )
        );
    }

    @Test
    void conditionalArrayAccess() {
        rewriteRun(
          kotlin(
            """
              val arr = IntArray ( 1 )
              val a = arr [ if (true) 0 else 1 ]
              """
          )
        );
    }
}
