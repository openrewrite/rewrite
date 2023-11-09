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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("All")
class AssignmentTest implements RewriteTest {

    @Test
    void assignment() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  var s : String
                  s = "42"
              }
              """
          )
        );
    }

    @Test
    void unaryMinus() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  var i = - 1
                  i = -1
                  var l = - 2L
                  l = - 2L
                  var f = - 3.0f
                  f = - 3.0f
                  var d = - 4.0
                  d = - 4.0
              }
              """
          )
        );
    }

    @Test
    void unaryPlus() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  var i = + 1
                  i = + 1
                  var l = + 2L
                  l = + 2L
                  var f = + 3.0f
                  f = + 3.0f
                  var d = + 4.0
                  d = + 4.0
              }
              """
          )
        );
    }

    @Test
    void preDecrement() {
        rewriteRun(
          kotlin(
            """
              var a = 42
              val b = -- a
              """
          )
        );
    }

    @Test
    void preIncrement() {
        rewriteRun(
          kotlin(
            """
              var a = 42
              val b = ++ a
              """
          )
        );
    }

    @Test
    void postDecrement() {
        rewriteRun(
          kotlin(
            """
              var a = 42
              val b = a --
              """
          )
        );
    }

    @Test
    void postIncrement() {
        rewriteRun(
          kotlin(
            """
              var a = 42
              val b = a ++
              """
          )
        );
    }

    @Test
    void not() {
        rewriteRun(
          kotlin(
            """
              val a = true
              val b = ! a
              """
          )
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          kotlin(
            """
              fun test() {
                  var a: Boolean
                  @Suppress
                  a = true
              }
              """
          )
        );
    }
}
