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

class UnaryTest implements RewriteTest {

    @Test
    void unary() {
        rewriteRun(
          kotlin(
            """
              var n = 0
              val a =  n   --
              val b =  --   n
              """
          )
        );
    }

    @Test
    void notNull() {
        rewriteRun(
          kotlin(
            """
              val l = listOf ( "x" )
              val a = l [ 0 ] !!
              """
          )
        );
    }

    @Test
    void checkNotNull() {
        rewriteRun(
          kotlin(
            """
              class A {
                  fun method ( ) : Int ? {
                      return 1
                  }
              }
              """
          ),
          kotlin(
            """
              val a = A ( )
              val b = a . method ( ) !!
              val c = b !!
              """
          )
        );
    }
}
