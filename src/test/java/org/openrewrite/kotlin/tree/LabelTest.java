/*
 * Copyright 2021 the original author or authors.
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

class LabelTest implements RewriteTest {

    @Test
    void anonymousFunction() {
        rewriteRun(
          kotlin(
            """
              fun foo() {
                  run loop@ {
                      listOf ( 1 , 2 , 3 , 4 , 5 ) . forEach {
                          if ( it == 3 ) return@loop
                          println ( it )
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void breakFromLabeledWhileLoop() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  labeled@ while ( true ) {
                      break@labeled
                  }
              }
              """
          )
        );
    }

    @Test
    void continueFromLabeledWhileLoop() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun test ( ) {
                      labeled@ while ( true ) continue@labeled
                  }
              }
              """
          )
        );
    }

    @Test
    void doWhileLoop() {
        rewriteRun(
          kotlin(
            """
              fun test ( ) {
                  var i = 0
                  labeled@ do {
                      i ++
                      break@labeled
                  } while ( i < 10 )
              }
              """
          )
        );
    }

    @Test
    void forLoop() {
        rewriteRun(
          kotlin(
            """
              fun test ( ) {
                  labeled@ for ( i in 1 .. 10 ) {
                      break@labeled
                  }
              }
              """
          )
        );
    }
}
