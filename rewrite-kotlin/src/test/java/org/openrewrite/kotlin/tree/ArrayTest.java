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
import org.openrewrite.Issue;
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

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/291")
    @Test
    void incrementArrayElement() {
        rewriteRun(
          kotlin(
            """
              val array = IntArray(1)
              val x = array[0]++
              """
          )
        );
    }

    @Test
    void IndexedAccessOperator2D() {
        rewriteRun(
          kotlin(
            """
              class Matrix(private val rows: Int, private val cols: Int) {
                  operator fun get(i: Int, j: Int): Int { return 0  }
                  operator fun set(i: Int, j: Int, value: Int) {}
              }

              fun method() {
                  val matrix = Matrix(3, 3)
                  val x = matrix [  1   , 2  ]
                  matrix [1, 2] = 3
              }
              """
          )
        );
    }

    @Test
    void IndexAccessOperatorMulD() {
        rewriteRun(
          kotlin(
            """
              class MultiDimensionArray(private val dimensions: IntArray) {
                  operator fun get(vararg indices: Int): Int { return 0 }
                  operator fun set(vararg indices: Int, value: Int) {}
              }

              fun method() {
                  val array = MultiDimensionArray(intArrayOf(2, 3, 4))
                  array [  1   ,    2 ,  3   ] = 42
                  val x = array[1, 2, 3]
              }
              """
          )
        );
    }

    @Test
    void arrayAccessTrailingComma() {
        rewriteRun(
          kotlin(
            """
              val arr = IntArray ( 1 )
              val a = arr [ 0  ,   ]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/410")
    @Test
    void mapAccessTrailingComma() {
        rewriteRun(
          kotlin(
            """
              val a = mapOf ( 1 to "one" , 2 to "two" )
              val b = a [ 1 , ]
              """
          )
        );
    }
}
